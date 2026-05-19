package com.drinkwater.data.db

import androidx.room.*
import com.drinkwater.data.model.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE appPackageName = :packageName")
    fun getByApp(packageName: String): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE appPackageName = :packageName")
    suspend fun getByAppOnce(packageName: String): List<Reminder>

    @Insert
    suspend fun insert(reminder: Reminder): Long

    @Insert
    suspend fun insertAll(reminders: List<Reminder>)

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE appPackageName = :packageName")
    suspend fun deleteByApp(packageName: String)
}
