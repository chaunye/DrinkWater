package com.drinkwater.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = MonitoredApp::class,
            parentColumns = ["packageName"],
            childColumns = ["appPackageName"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("appPackageName")]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appPackageName: String,
    val content: String
)
