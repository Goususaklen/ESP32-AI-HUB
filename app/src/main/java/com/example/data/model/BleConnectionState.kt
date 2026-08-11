package com.example.data.model

sealed class BleConnectionState {
    object Disconnected : BleConnectionState()
    object Scanning : BleConnectionState()
    data class Connecting(val deviceName: String, val macAddress: String) : BleConnectionState()
    data class Connected(
        val deviceName: String,
        val macAddress: String,
        val rssi: Int = -62,
        val isSimulator: Boolean = false
    ) : BleConnectionState()
    data class Error(val message: String) : BleConnectionState()
}

data class BleDeviceItem(
    val name: String,
    val address: String,
    val rssi: Int = -70,
    val isESP32: Boolean = true,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)
