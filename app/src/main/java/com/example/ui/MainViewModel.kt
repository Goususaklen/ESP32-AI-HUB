package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BleConnectionState
import com.example.data.model.ChatMessage
import com.example.data.model.Esp32Data
import com.example.data.repository.Esp32Repository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = Esp32Repository(application)

    val connectionState: StateFlow<BleConnectionState> = repository.connectionState
    val sensorData: StateFlow<Esp32Data> = repository.sensorData
    val scannedDevices = repository.scannedDevices
    val autoReconnect = repository.autoReconnect
    val isSimulatorMode = repository.isSimulatorMode

    val customApiKey = repository.customApiKey
    val temperatureUnit = repository.temperatureUnit
    val tempThreshold = repository.tempThreshold
    val isAiLoading = repository.isAiLoading

    val chatMessages: StateFlow<List<ChatMessage>> = repository.chatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sensorLogs = repository.sensorLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isListening = repository.voiceManager.isListening
    val ttsEnabled = repository.voiceManager.ttsEnabled

    fun sendMessageToAi(text: String) {
        repository.sendMessageToAi(text, isVoice = false)
    }

    fun startVoiceInput() {
        repository.voiceManager.startListening { spokenText ->
            repository.sendMessageToAi(spokenText, isVoice = true)
        }
    }

    fun stopVoiceInput() {
        repository.voiceManager.stopListening()
    }

    fun toggleTts(enabled: Boolean) {
        repository.voiceManager.setTtsEnabled(enabled)
    }

    fun toggleLight() = repository.toggleLight()
    fun toggleFan() = repository.toggleFan()
    fun toggleAutoMode() = repository.toggleAutoMode()
    fun toggleAiControlMode() = repository.toggleAiControlMode()
    fun sendCommand(cmd: String) = repository.sendCommand(cmd)

    fun startScan() = repository.bleManager.startScan()
    fun stopScan() = repository.bleManager.stopScan()
    fun connectToDevice(mac: String, name: String) = repository.bleManager.connectToDevice(mac, name)
    fun disconnectDevice() = repository.bleManager.disconnect()

    fun setSimulatorMode(enabled: Boolean) = repository.bleManager.setSimulatorMode(enabled)
    fun setAutoReconnect(enabled: Boolean) = repository.bleManager.setAutoReconnect(enabled)

    fun setCustomApiKey(key: String) = repository.setCustomApiKey(key)
    fun setTemperatureUnit(unit: String) = repository.setTemperatureUnit(unit)
    fun setTempThreshold(valDeg: Float) = repository.setTempThreshold(valDeg)

    fun clearChat() = repository.clearChatHistory()
    fun clearLogs() = repository.clearSensorHistory()

    override fun onCleared() {
        super.onCleared()
        repository.voiceManager.shutdown()
    }
}
