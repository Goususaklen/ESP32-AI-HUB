package com.example.data.repository

import android.content.Context
import com.example.ai.AiService
import com.example.ble.BleManager
import com.example.data.db.AppDatabase
import com.example.data.db.ChatMessageEntity
import com.example.data.db.SensorLogEntity
import com.example.data.model.BleConnectionState
import com.example.data.model.ChatMessage
import com.example.data.model.Esp32Data
import com.example.data.model.MessageSender
import com.example.speech.VoiceAssistantManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class Esp32Repository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    val bleManager = BleManager(context)
    val voiceManager = VoiceAssistantManager(context)
    private val aiService = AiService()

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    val connectionState: StateFlow<BleConnectionState> = bleManager.connectionState
    val sensorData: StateFlow<Esp32Data> = bleManager.sensorData
    val scannedDevices = bleManager.scannedDevices
    val autoReconnect = bleManager.autoReconnect
    val isSimulatorMode = bleManager.isSimulatorMode

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _temperatureUnit = MutableStateFlow("Celsius (°C)") // "Celsius (°C)" or "Fahrenheit (°F)"
    val temperatureUnit: StateFlow<String> = _temperatureUnit.asStateFlow()

    private val _tempThreshold = MutableStateFlow(32.0f)
    val tempThreshold: StateFlow<Float> = _tempThreshold.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    val sensorLogs: Flow<List<SensorLogEntity>> = db.sensorLogDao().getRecentLogs()

    val chatMessages: Flow<List<ChatMessage>> = db.chatDao().getAllMessages().map { entities ->
        entities.map { entity ->
            ChatMessage(
                id = entity.id,
                sender = MessageSender.valueOf(entity.sender),
                text = entity.text,
                timestamp = entity.timestamp,
                commandExecuted = entity.commandExecuted
            )
        }
    }

    init {
        // Automatically save sensor logs to Room periodically
        scope.launch {
            var lastSaveTime = 0L
            sensorData.collectLatest { data ->
                val now = System.currentTimeMillis()
                if (now - lastSaveTime > 5000) { // save log every 5s
                    lastSaveTime = now
                    db.sensorLogDao().insertLog(
                        SensorLogEntity(
                            temperature = data.temperature,
                            humidity = data.humidity,
                            ldr = data.ldr,
                            motion = data.motion,
                            lightOn = data.light,
                            fanOn = data.fan,
                            batteryLevel = data.battery,
                            timestamp = now
                        )
                    )
                }
            }
        }
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
    }

    fun setTemperatureUnit(unit: String) {
        _temperatureUnit.value = unit
    }

    fun setTempThreshold(value: Float) {
        _tempThreshold.value = value
    }

    fun toggleLight() {
        val current = sensorData.value.light
        val cmd = if (current) "LIGHT_OFF" else "LIGHT_ON"
        bleManager.sendCommand(cmd)
    }

    fun toggleFan() {
        val current = sensorData.value.fan
        val cmd = if (current) "FAN_OFF" else "FAN_ON"
        bleManager.sendCommand(cmd)
    }

    fun toggleAutoMode() {
        val current = sensorData.value.autoMode
        val cmd = if (current) "AUTO_MODE_OFF" else "AUTO_MODE_ON"
        bleManager.sendCommand(cmd)
    }

    fun toggleAiControlMode() {
        val current = sensorData.value.aiControlMode
        val cmd = if (current) "AI_MODE_OFF" else "AI_MODE_ON"
        bleManager.sendCommand(cmd)
    }

    fun sendCommand(cmd: String) {
        bleManager.sendCommand(cmd)
    }

    fun sendMessageToAi(userText: String, isVoice: Boolean = false) {
        if (userText.isBlank()) return

        scope.launch {
            // Insert User Message
            val userMsg = ChatMessageEntity(
                sender = if (isVoice) MessageSender.VOICE.name else MessageSender.USER.name,
                text = userText,
                timestamp = System.currentTimeMillis()
            )
            db.chatDao().insertMessage(userMsg)

            _isAiLoading.value = true

            val aiResult = aiService.generateResponse(
                userPrompt = userText,
                sensorData = sensorData.value,
                connectionState = connectionState.value,
                customApiKey = _customApiKey.value
            )

            // If command detected, dispatch to BLE
            aiResult.detectedCommand?.let { cmd ->
                bleManager.sendCommand(cmd)
            }

            // Save AI Message
            val aiMsg = ChatMessageEntity(
                sender = MessageSender.AI.name,
                text = aiResult.replyText,
                timestamp = System.currentTimeMillis(),
                commandExecuted = aiResult.detectedCommand
            )
            db.chatDao().insertMessage(aiMsg)

            _isAiLoading.value = false

            // Read AI response aloud if TTS enabled
            voiceManager.speak(aiResult.replyText)
        }
    }

    fun clearChatHistory() {
        scope.launch {
            db.chatDao().clearHistory()
        }
    }

    fun clearSensorHistory() {
        scope.launch {
            db.sensorLogDao().clearAll()
        }
    }
}
