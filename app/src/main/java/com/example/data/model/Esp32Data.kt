package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Esp32Data(
    val temperature: Float = 24.5f,
    val humidity: Float = 55.0f,
    val ldr: Int = 450, // 0 (bright) - 1023 (dark) or lux value
    val motion: Boolean = false,
    val light: Boolean = false,
    val fan: Boolean = false,
    val autoMode: Boolean = true,
    val aiControlMode: Boolean = false,
    val battery: Int = 95, // battery % or power voltage status
    val timestamp: Long = System.currentTimeMillis()
) {
    val lightLevelDescription: String
        get() = when {
            ldr < 200 -> "Bright Light"
            ldr < 600 -> "Moderate Ambient Light"
            ldr < 900 -> "Dim / Evening Light"
            else -> "Dark Room"
        }

    val motionDescription: String
        get() = if (motion) "Motion Detected 🏃" else "No Motion (Clear) 🟢"
}

data class Esp32Command(
    val command: String,
    val parameter: String? = null
)
