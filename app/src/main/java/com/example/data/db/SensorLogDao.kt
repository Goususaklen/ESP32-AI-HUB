package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorLogDao {
    @Query("SELECT * FROM sensor_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<SensorLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SensorLogEntity)

    @Query("DELETE FROM sensor_logs WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOldLogs(cutoffTimestamp: Long)

    @Query("DELETE FROM sensor_logs")
    suspend fun clearAll()
}
