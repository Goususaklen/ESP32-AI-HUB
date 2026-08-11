package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.BleConnectionState
import com.example.data.model.Esp32Data
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiResponseResult(
    val replyText: String,
    val detectedCommand: String? = null
)

class AiService {

    private val TAG = "AiService"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun generateResponse(
        userPrompt: String,
        sensorData: Esp32Data,
        connectionState: BleConnectionState,
        customApiKey: String? = null
    ): AiResponseResult = withContext(Dispatchers.IO) {

        // Check command keywords locally for instant responsive feel
        val localCommand = detectCommandInPrompt(userPrompt)

        // Build sensor context block
        val isConnected = connectionState is BleConnectionState.Connected
        val sensorContext = if (isConnected) {
            """
            LIVE ESP32-S3 SENSOR READINGS:
            - Temperature: ${sensorData.temperature}°C (${(sensorData.temperature * 1.8f + 32).toInt()}°F)
            - Humidity: ${sensorData.humidity}%
            - Light Level (LDR): ${sensorData.ldr} (${sensorData.lightLevelDescription})
            - Motion Sensor (IR/PIR): ${if (sensorData.motion) "MOTION DETECTED" else "No Motion Detected"}
            - Light Relay: ${if (sensorData.light) "ON" else "OFF"}
            - Fan Relay: ${if (sensorData.fan) "ON" else "OFF"}
            - System Mode: ${if (sensorData.autoMode) "AUTOMATIC" else "MANUAL"}
            - Battery/Power Status: ${sensorData.battery}%
            """.trimIndent()
        } else {
            "LIVE SENSOR DATA STATUS: ESP32 IS DISCONNECTED. Real-time sensor readings are currently UNAVAILABLE. Do not invent fake readings."
        }

        val apiKeyToUse = when {
            !customApiKey.isNullOrEmpty() -> customApiKey
            BuildConfig.GEMINI_API_KEY.isNotEmpty() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            BuildConfig.OPENAI_API_KEY.isNotEmpty() && BuildConfig.OPENAI_API_KEY != "MY_OPENAI_API_KEY" -> BuildConfig.OPENAI_API_KEY
            else -> ""
        }

        // Fast fallback if no API key set or if local quick intent command
        if (apiKeyToUse.isEmpty()) {
            val fallbackReply = generateFallbackResponse(userPrompt, sensorData, isConnected, localCommand)
            return@withContext AiResponseResult(fallbackReply, localCommand)
        }

        try {
            // Call Gemini API via REST endpoint
            val systemInstruction = """
                You are ESP32 AI Hub Assistant, an intelligent AI for controlling and monitoring ESP32-S3 hardware electronics.
                
                $sensorContext
                
                RULES:
                1. Always refer accurately to the sensor numbers above. NEVER invent or hallucinate sensor readings.
                2. If the ESP32 is disconnected, explicitly state that sensor readings are unavailable.
                3. Keep answers concise, helpful, friendly, and knowledgeable in electronics, IoT, physics, math, and code.
            """.trimIndent()

            val responseText = callGeminiRestApi(apiKeyToUse, systemInstruction, userPrompt)
            return@withContext AiResponseResult(responseText, localCommand)
        } catch (e: Exception) {
            Log.e(TAG, "AI API Call failed, fallback to local rule engine", e)
            val fallback = generateFallbackResponse(userPrompt, sensorData, isConnected, localCommand)
            return@withContext AiResponseResult(fallback, localCommand)
        }
    }

    private fun detectCommandInPrompt(prompt: String): String? {
        val p = prompt.lowercase()
        return when {
            p.contains("turn on light") || p.contains("light on") || p.contains("switch on light") -> "LIGHT_ON"
            p.contains("turn off light") || p.contains("light off") || p.contains("switch off light") -> "LIGHT_OFF"
            p.contains("turn on fan") || p.contains("fan on") || p.contains("switch on fan") -> "FAN_ON"
            p.contains("turn off fan") || p.contains("fan off") || p.contains("switch off fan") -> "FAN_OFF"
            p.contains("enable auto") || p.contains("auto mode on") || p.contains("automatic mode") -> "AUTO_MODE_ON"
            p.contains("disable auto") || p.contains("manual mode") -> "AUTO_MODE_OFF"
            else -> null
        }
    }

    private fun generateFallbackResponse(
        prompt: String,
        sensor: Esp32Data,
        isConnected: Boolean,
        commandExecuted: String?
    ): String {
        val p = prompt.lowercase()

        if (!isConnected) {
            return "The ESP32-S3 board is currently disconnected, so sensor readings are unavailable. Please check the Device tab to connect."
        }

        if (commandExecuted != null) {
            return when (commandExecuted) {
                "LIGHT_ON" -> "I have sent the command to turn ON the light. Relay 1 activated."
                "LIGHT_OFF" -> "I have sent the command to turn OFF the light. Relay 1 deactivated."
                "FAN_ON" -> "I have sent the command to turn ON the fan. Relay 2 activated."
                "FAN_OFF" -> "I have sent the command to turn OFF the fan. Relay 2 deactivated."
                "AUTO_MODE_ON" -> "Automatic control mode is now ENABLED. Sensors will now control relays autonomously."
                "AUTO_MODE_OFF" -> "Automatic control mode is now DISABLED. Switched to manual mode."
                else -> "Command sent to ESP32."
            }
        }

        return when {
            p.contains("happening in my room") || p.contains("room status") || p.contains("environment") -> {
                val motionText = if (sensor.motion) "motion is currently detected!" else "no motion is detected."
                "Temperature is ${sensor.temperature}°C, humidity is ${sensor.humidity}%, light level is ${sensor.ldr} (${sensor.lightLevelDescription}), and $motionText"
            }
            p.contains("temperature") || p.contains("temp") || p.contains("how hot") || p.contains("how cold") -> {
                "The current room temperature is ${sensor.temperature}°C (${(sensor.temperature * 1.8f + 32).toInt()}°F)."
            }
            p.contains("humidity") -> {
                "The relative humidity is ${sensor.humidity}%."
            }
            p.contains("light") || p.contains("dark") || p.contains("bright") -> {
                val stateStr = if (sensor.light) "ON" else "OFF"
                "Light level is ${sensor.ldr} (${sensor.lightLevelDescription}). The light switch is currently $stateStr."
            }
            p.contains("someone") || p.contains("motion") || p.contains("movement") -> {
                if (sensor.motion) "Yes, motion is detected in the room right now! 🏃" else "No motion detected in the room at the moment. 🟢"
            }
            p.contains("analyze") || p.contains("summary") || p.contains("report") -> {
                "Sensor Analysis: Room temperature is ${sensor.temperature}°C, humidity is ${sensor.humidity}%, light is ${sensor.ldr}. Climate conditions are within normal parameters."
            }
            else -> {
                "I am monitoring your ESP32-S3. Temperature: ${sensor.temperature}°C | Humidity: ${sensor.humidity}% | Motion: ${if (sensor.motion) "Yes" else "No"} | Light: ${if (sensor.light) "ON" else "OFF"} | Fan: ${if (sensor.fan) "ON" else "OFF"}."
            }
        }
    }

    private fun callGeminiRestApi(apiKey: String, systemPrompt: String, userPrompt: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        val jsonBody = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            })
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
            ))
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP Error: ${response.code} - ${response.message}")
            }
            val respStr = response.body?.string() ?: ""
            val jsonResp = JSONObject(respStr)
            val candidates = jsonResp.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val content = candidates.getJSONObject(0).optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "No text response generated.")
                }
            }
            return "AI response completed."
        }
    }
}
