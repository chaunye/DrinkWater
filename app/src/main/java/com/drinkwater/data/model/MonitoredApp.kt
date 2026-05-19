package com.drinkwater.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitored_apps")
data class MonitoredApp(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isEnabled: Boolean = true,
    val popupMode: PopupMode = PopupMode.DEFAULT,
    val startTime: String = "00:00",
    val endTime: String = "23:59",
    val isAllDay: Boolean = true,
    val popupImageUri: String? = null,
    val backgroundImageUri: String? = null
)

enum class PopupMode {
    DEFAULT, FLOATING
}
