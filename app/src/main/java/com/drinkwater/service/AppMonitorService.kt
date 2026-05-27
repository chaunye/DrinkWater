package com.drinkwater.service

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import com.drinkwater.DrinkWaterApp
import com.drinkwater.MainActivity
import com.drinkwater.data.db.AppDatabase
import com.drinkwater.data.db.SettingsDataStore
import com.drinkwater.data.model.PopupMode
import com.drinkwater.ui.components.ReminderActivity
import com.drinkwater.util.TimeUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class AppMonitorService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastTriggeredPackage: String? = null
    private var lastTriggerTime: Long = 0
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return

        // Skip own app and system UI
        if (packageName == applicationContext.packageName) return
        if (packageName == "com.android.systemui") return

        // Debounce: ignore if same package within 2 seconds
        val now = System.currentTimeMillis()
        if (packageName == lastTriggeredPackage && now - lastTriggerTime < 2000) return

        scope.launch {
            try {
                checkAndTrigger(packageName, now)
            } catch (e: Exception) {
                // Log but don't crash
                e.printStackTrace()
            }
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

        // Collect all possible reminders
        val allReminders = mutableListOf<String>()

        // Get app-specific reminders
        val appReminders = db.reminderDao().getByAppOnce(packageName)
        allReminders.addAll(appReminders.map { it.content })

        // Get global todo items (applyToAll = true, not completed)
        val globalTodos = db.todoItemDao().getGlobalUncompleted()
        val globalTimeEnabled = settings.globalTimeEnabled.first()
        val globalStartTime = settings.globalStartTime.first()
        val globalEndTime = settings.globalEndTime.first()

        globalTodos.forEach { todo ->
            // Check if todo has its own time setting or use global
            val inTimeRange = if (!todo.isAllDay) {
                TimeUtil.isInTimeRange(todo.startTime, todo.endTime)
            } else if (globalTimeEnabled) {
                TimeUtil.isInTimeRange(globalStartTime, globalEndTime)
            } else {
                true // No time restriction
            }
            if (inTimeRange) {
                allReminders.add(todo.content)
            }
        }

        // Get non-global todo items that are uncompleted
        val otherTodos = db.todoItemDao().getAllUncompleted().filter { !it.applyToAll }
        otherTodos.forEach { todo ->
            val inTimeRange = if (!todo.isAllDay) {
                TimeUtil.isInTimeRange(todo.startTime, todo.endTime)
            } else if (globalTimeEnabled) {
                TimeUtil.isInTimeRange(globalStartTime, globalEndTime)
            } else {
                true
            }
            if (inTimeRange) {
                allReminders.add(todo.content)
            }
        }

        if (allReminders.isEmpty()) return
        val reminderContent = allReminders.random()

        // Update debounce
        lastTriggeredPackage = packageName
        lastTriggerTime = now

        // Play alert sound
        val globalSoundEnabled = settings.alertSoundEnabled.first()
        if (app.alertEnabled && globalSoundEnabled) {
            playAlertSound(app.alertSoundUri)
        }

        // Get popup mode - use Activity approach for reliable top-of-screen display
        val globalPopupMode = settings.popupMode.first()
        val popupMode = if (app.popupMode == PopupMode.FLOATING) "floating" else globalPopupMode

        // Always use Activity approach - it gets proper z-order from the system
        // (like alarm/call/SMS apps), unlike WindowManager overlay which gets covered
        val intent = Intent(applicationContext, ReminderActivity::class.java).apply {
            putExtra(ReminderActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(ReminderActivity.EXTRA_APP_NAME, app.appName)
            putExtra(ReminderActivity.EXTRA_REMINDER_CONTENT, reminderContent)
            putExtra(ReminderActivity.EXTRA_POPUP_MODE, popupMode)
            putExtra(ReminderActivity.EXTRA_POPUP_IMAGE, app.popupImageUri)
            putExtra(ReminderActivity.EXTRA_BACKGROUND_IMAGE, app.backgroundImageUri)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)

        // Log the action after Activity finishes (via ReminderActivity.handleAction)
        // No need for overlay fallback anymore
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        acquireWakeLock()
        startForegroundNotification()
        ServiceKeepAliveWorker.schedule(applicationContext)
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DrinkWater:MonitorService")
        wakeLock?.acquire(24 * 60 * 60 * 1000L) // 24 hours max
    }

    private fun startForegroundNotification() {
        val channelId = DrinkWaterApp.CHANNEL_SERVICE
        val manager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "后台服务",
                NotificationManager.IMPORTANCE_DEFAULT
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

    private fun playAlertSound(customUri: String? = null) {
        try {
            mediaPlayer?.release()
            mediaPlayer = null

            if (!customUri.isNullOrBlank()) {
                // Play custom sound from URI
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(applicationContext, Uri.parse(customUri))
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setOnCompletionListener { mp -> mp.release(); mediaPlayer = null }
                    prepare()
                    start()
                }
            } else {
                // Try bundled resource first, fallback to ToneGenerator
                val resId = resources.getIdentifier("tomori_gugagaga", "raw", packageName)
                if (resId != 0) {
                    mediaPlayer = MediaPlayer.create(applicationContext, resId)?.apply {
                        setOnCompletionListener { mp -> mp.release(); mediaPlayer = null }
                        start()
                    }
                } else {
                    // Fallback: simple beep
                    scope.launch(Dispatchers.IO) {
                        val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                        toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 200)
                        delay(250)
                        toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 200)
                        delay(300)
                        toneGen.release()
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore sound errors - don't crash the service
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        mediaPlayer?.release()
        mediaPlayer = null
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

        fun stopAlertSound() {
            instance?.mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            instance?.mediaPlayer = null
        }
    }
}
