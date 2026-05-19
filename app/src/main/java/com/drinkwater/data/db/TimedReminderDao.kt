package com.drinkwater.data.db

import androidx.room.*
import com.drinkwater.data.model.TimedReminder
import kotlinx.coroutines.flow.Flow

@Dao
interface TimedReminderDao {
    @Query("SELECT * FROM timed_reminders ORDER BY hour ASC, minute ASC")
    fun getAll(): Flow<List<TimedReminder>>

    @Query("SELECT * FROM timed_reminders WHERE isEnabled = 1")
    fun getEnabled(): Flow<List<TimedReminder>>

    @Insert
    suspend fun insert(reminder: TimedReminder): Long

    @Update
    suspend fun update(reminder: TimedReminder)

    @Delete
    suspend fun delete(reminder: TimedReminder)
}
