package com.drinkwater.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
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
        val mode = if (popupMode == "floating" || app.popupMode == PopupMode.FLOATING) {
            PopupMode.FLOATING
        } else {
            PopupMode.DEFAULT
        }

        // Launch reminder
        val intent = Intent(applicationContext, ReminderActivity::class.java).apply {
            putExtra(ReminderActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(ReminderActivity.EXTRA_APP_NAME, app.appName)
            putExtra(ReminderActivity.EXTRA_REMINDER_CONTENT, reminder.content)
            putExtra(ReminderActivity.EXTRA_POPUP_MODE, mode.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        instance = null
    }

    companion object {
        var instance: AppMonitorService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }
}
