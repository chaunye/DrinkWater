package com.drinkwater.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timed_reminders")
data class TimedReminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val repeatDaily: Boolean = true
)
