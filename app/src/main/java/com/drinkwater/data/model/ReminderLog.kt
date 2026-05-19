package com.drinkwater.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminder_logs")
data class ReminderLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appPackageName: String,
    val reminderContent: String,
    val timestamp: Long = System.currentTimeMillis(),
    val action: ReminderAction
)

enum class ReminderAction {
    CONFIRMED, DELAYED, CANCELLED
}
