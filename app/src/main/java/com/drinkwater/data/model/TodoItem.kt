package com.drinkwater.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo_items")
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val isCompleted: Boolean = false,
    val applyToAll: Boolean = false,
    val isAllDay: Boolean = true,
    val startTime: String = "00:00",
    val endTime: String = "23:59",
    val createdAt: Long = System.currentTimeMillis()
)
