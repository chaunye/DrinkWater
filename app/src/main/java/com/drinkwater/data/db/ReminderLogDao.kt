package com.drinkwater.data.db

import androidx.room.*
import com.drinkwater.data.model.ReminderAction
import com.drinkwater.data.model.ReminderLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderLogDao {
    @Insert
    suspend fun insert(log: ReminderLog)

    @Query("SELECT * FROM reminder_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int = 100): Flow<List<ReminderLog>>

    @Query("""
        SELECT COUNT(*) FROM reminder_logs
        WHERE timestamp >= :startOfDay AND timestamp < :endOfDay
    """)
    fun getTodayCount(startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM reminder_logs
        WHERE timestamp >= :startOfDay AND timestamp < :endOfDay AND action = :action
    """)
    fun getTodayCountByAction(startOfDay: Long, endOfDay: Long, action: ReminderAction): Flow<Int>

    @Query("""
        SELECT DATE(timestamp / 1000, 'unixepoch', 'localtime') as day, COUNT(*) as count
        FROM reminder_logs
        WHERE timestamp >= :startTime
        GROUP BY day
        ORDER BY day ASC
    """)
    fun getDailyCounts(startTime: Long): Flow<List<DailyCount>>

    @Query("""
        SELECT DATE(timestamp / 1000, 'unixepoch', 'localtime') as day,
               action, COUNT(*) as count
        FROM reminder_logs
        WHERE timestamp >= :startTime
        GROUP BY day, action
        ORDER BY day ASC
    """)
    fun getDailyCountsByAction(startTime: Long): Flow<List<DailyActionCount>>
}

data class DailyCount(val day: String, val count: Int)
data class DailyActionCount(val day: String, val action: ReminderAction, val count: Int)
