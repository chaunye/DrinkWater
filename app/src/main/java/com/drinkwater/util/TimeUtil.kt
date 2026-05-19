package com.drinkwater.util

import java.util.Calendar

object TimeUtil {
    fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getEndOfDay(): Long {
        return getStartOfDay() + 24 * 60 * 60 * 1000
    }

    fun getDaysAgo(days: Int): Long {
        return getStartOfDay() - days * 24 * 60 * 60 * 1000L
    }

    fun formatTime(hour: Int, minute: Int): String {
        return "%02d:%02d".format(hour, minute)
    }

    fun isInTimeRange(startTime: String, endTime: String): Boolean {
        if (startTime == "00:00" && endTime == "23:59") return true
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val startParts = startTime.split(":")
        val endParts = endTime.split(":")
        val startMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()
        val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()
        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes..endMinutes
        } else {
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }
}
