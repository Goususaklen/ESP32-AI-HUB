package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_logs")
data class SensorLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val temperature: Float,
    val humidity: Float,
    val ldr: Int,
    val motion: Boolean,
    val lightOn: Boolean,
    val fanOn: Boolean,
    val batteryLevel: Int,
    val timestamp: Long = System.currentTimeMillis()
)
