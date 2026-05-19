package com.drinkwater.service

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.drinkwater.DrinkWaterApp
import com.drinkwater.MainActivity
import com.drinkwater.data.db.AppDatabase
import com.drinkwater.data.db.SettingsDataStore
import com.drinkwater.data.model.PopupMode
import com.drinkwater.data.model.ReminderAction
import com.drinkwater.data.model.ReminderLog
import com.drinkwater.ui.components.ReminderActivity
import com.drinkwater.util.TimeUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class AppMonitorService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastTriggeredPackage: String? = null
    private var lastTriggerTime: Long = 0
    private var overlay: ReminderOverlay? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return

        // Skip own app
        if (packageName == applicationContext.packageName) return

        // Debounce: ignore if same package within 2 seconds
        val now = System.currentTimeMillis()
        if (packageName == lastTriggeredPackage && now - lastTriggerTime < 2000) return

        scope.launch {
            checkAndTrigger(packageName, now)
        }
    }

    private suspend fun checkAndTrigger(packageName: String, now: Long) {
        val db = AppDatabase.getInstance(applicationContext)
        val settings = SettingsDataStore(applicationContext)

        // Check if this app is monitored and enabled
        val app = db.monitoredAppDao().getByPackageName(packageName) ?: return
        if (!app.isEnabled) return

        // Check time range
        if (!app.isAllDay && !TimeUtil.isInTimeRange(app.startTime, app.endTime)) return

        // Get a random reminder
        val reminders = db.reminderDao().getByAppOnce(packageName)
        if (reminders.isEmpty()) return
        val reminder = reminders.random()

        // Update debounce
        lastTriggeredPackage = packageName
        lastTriggerTime = now

        // Get popup mode
        val popupMode = settings.popupMode.first()
        val useOverlay = popupMode == "floating" || app.popupMode == PopupMode.FLOATING

        val handleAction = { action: ReminderAction ->
            scope.launch {
                db.reminderLogDao().insert(
                    ReminderLog(
                        appPackageName = packageName,
                        reminderContent = reminder.content,
                        action = action
                    )
                )
            }
            if (action == ReminderAction.CONFIRMED) {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    startActivity(launchIntent)
                }
            }
        }

        if (useOverlay && Settings.canDrawOverlays(applicationContext)) {
            // Use overlay window (shows on top of other apps)
            withContext(Dispatchers.Main) {
                if (overlay == null) overlay = ReminderOverlay(applicationContext)
                overlay?.show(
                    appName = app.appName,
                    content = reminder.content,
                    onConfirm = { handleAction(ReminderAction.CONFIRMED) },
                    onDelay = { handleAction(ReminderAction.DELAYED) },
                    onCancel = { handleAction(ReminderAction.CANCELLED) }
                )
            }
        } else {
            // Fallback to activity
            val intent = Intent(applicationContext, ReminderActivity::class.java).apply {
                putExtra(ReminderActivity.EXTRA_PACKAGE_NAME, packageName)
                putExtra(ReminderActivity.EXTRA_APP_NAME, app.appName)
                putExtra(ReminderActivity.EXTRA_REMINDER_CONTENT, reminder.content)
                putExtra(ReminderActivity.EXTRA_POPUP_MODE, PopupMode.DEFAULT.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        startForegroundNotification()
    }

    private fun startForegroundNotification() {
        val channelId = DrinkWaterApp.CHANNEL_SERVICE
        val manager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "后台服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持应用监控服务运行"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("DrinkWater 运行中")
            .setContentText("正在监控应用，点击查看详情")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        try {
            startForeground(1, notification)
        } catch (e: Exception) {
            // Some ROMs may not support foreground service for accessibility
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlay?.dismiss()
        overlay = null
        scope.cancel()
        instance = null
        // Schedule restart
        scheduleRestart()
    }

    private fun scheduleRestart() {
        val intent = Intent(applicationContext, RestartReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(android.app.AlarmManager::class.java)
        try {
            alarmManager.set(
                android.app.AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 1000,
                pendingIntent
            )
        } catch (e: Exception) {
            // Ignore
        }
    }

    companion object {
        var instance: AppMonitorService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }
}
