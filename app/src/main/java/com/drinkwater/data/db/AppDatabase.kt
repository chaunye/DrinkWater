package com.drinkwater.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.drinkwater.data.model.*

@Database(
    entities = [
        MonitoredApp::class,
        Reminder::class,
        ReminderLog::class,
        WordList::class,
        Word::class,
        TimedReminder::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun monitoredAppDao(): MonitoredAppDao
    abstract fun reminderDao(): ReminderDao
    abstract fun reminderLogDao(): ReminderLogDao
    abstract fun wordListDao(): WordListDao
    abstract fun wordDao(): WordDao
    abstract fun timedReminderDao(): TimedReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "drinkwater.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
