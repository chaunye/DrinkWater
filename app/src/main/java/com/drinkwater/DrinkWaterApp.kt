package com.drinkwater

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.drinkwater.data.db.AppDatabase

class DrinkWaterApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val reminderChannel = NotificationChannel(
            CHANNEL_REMINDER,
            "提醒通知",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "定时提醒和背词提醒"
        }

        val serviceChannel = NotificationChannel(
            CHANNEL_SERVICE,
            "后台服务",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "保持应用监控服务运行"
        }

        manager.createNotificationChannel(reminderChannel)
        manager.createNotificationChannel(serviceChannel)
    }

    companion object {
        const val CHANNEL_REMINDER = "reminder_channel"
        const val CHANNEL_SERVICE = "service_channel"
    }
}
