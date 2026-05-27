package com.drinkwater.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.drinkwater.DrinkWaterApp
import com.drinkwater.MainActivity
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

class ServiceKeepAliveWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Check if accessibility service should be running
        if (!isAccessibilityServiceEnabled()) return Result.success()

        // If service is not running, the system should restart it automatically
        // for accessibility services. But on Chinese ROMs, we need to be more aggressive.
        if (!AppMonitorService.isRunning()) {
            // Try to keep the service alive by re-checking
            // AccessibilityService is managed by the system, but we can
            // ensure our foreground notification is visible
            delay(3000)

            // If still not running after delay, send a notification to prompt user
            if (!AppMonitorService.isRunning()) {
                notifyServiceStopped()
            }
        }

        return Result.success()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val serviceName = "${applicationContext.packageName}/.service.AppMonitorService"
        val enabledServices = Settings.Secure.getString(
            applicationContext.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(serviceName)
    }

    private fun notifyServiceStopped() {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra("open_accessibility", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, DrinkWaterApp.CHANNEL_REMINDER)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("DrinkWater 监控已停止")
            .setContentText("点击重新开启无障碍服务")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(9999, notification)
    }

    companion object {
        private const val WORK_NAME = "service_keep_alive"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ServiceKeepAliveWorker>(
                15, TimeUnit.MINUTES
            ).setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
