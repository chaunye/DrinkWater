package com.drinkwater.data.db

import androidx.room.*
import com.drinkwater.data.model.MonitoredApp
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitoredAppDao {
    @Query("SELECT * FROM monitored_apps ORDER BY appName ASC")
    fun getAll(): Flow<List<MonitoredApp>>

    @Query("SELECT * FROM monitored_apps WHERE isEnabled = 1")
    fun getEnabled(): Flow<List<MonitoredApp>>

    @Query("SELECT * FROM monitored_apps WHERE packageName = :packageName")
    suspend fun getByPackageName(packageName: String): MonitoredApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: MonitoredApp)

    @Update
    suspend fun update(app: MonitoredApp)

    @Delete
    suspend fun delete(app: MonitoredApp)

    @Query("SELECT EXISTS(SELECT 1 FROM monitored_apps WHERE packageName = :packageName AND isEnabled = 1)")
    suspend fun isMonitored(packageName: String): Boolean
}
