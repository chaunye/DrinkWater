package com.drinkwater.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.drinkwater.DrinkWaterApp
import com.drinkwater.MainActivity
import com.drinkwater.R
import com.drinkwater.data.db.AppDatabase
import com.drinkwater.data.db.SettingsDataStore
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class TimedReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val settings = SettingsDataStore(applicationContext)

        if (!settings.notificationsEnabled.first()) return Result.success()

        val timedReminders = db.timedReminderDao().getEnabled().first()
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        for (reminder in timedReminders) {
            if (reminder.hour == currentHour && reminder.minute == currentMinute) {
                showNotification(reminder.content)
            }
        }

        return Result.success()
    }

    private fun showNotification(content: String) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, DrinkWaterApp.CHANNEL_REMINDER)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("DrinkWater 提醒")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(content.hashCode(), notification)
    }

    companion object {
        private const val WORK_NAME = "timed_reminder_check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<TimedReminderWorker>(
                15, TimeUnit.MINUTES
            ).setInitialDelay(
                calculateInitialDelay(), TimeUnit.MILLISECONDS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        private fun calculateInitialDelay(): Long {
            val now = Calendar.getInstance()
            val next = Calendar.getInstance().apply {
                add(Calendar.MINUTE, 15 - (now.get(Calendar.MINUTE) % 15))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return next.timeInMillis - now.timeInMillis
        }
    }
}
