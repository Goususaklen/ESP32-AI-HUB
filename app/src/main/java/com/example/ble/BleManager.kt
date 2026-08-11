package com.example.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import com.example.data.model.BleConnectionState
import com.example.data.model.BleDeviceItem
import com.example.data.model.Esp32Data
import com.example.firmware.Esp32FirmwareCode
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    private val TAG = "BleManager"

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothGatt: BluetoothGatt? = null

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BleDeviceItem>>(emptyList())
    val scannedDevices: StateFlow<List<BleDeviceItem>> = _scannedDevices.asStateFlow()

    private val _sensorData = MutableStateFlow(Esp32Data())
    val sensorData: StateFlow<Esp32Data> = _sensorData.asStateFlow()

    private val _autoReconnect = MutableStateFlow(true)
    val autoReconnect: StateFlow<Boolean> = _autoReconnect.asStateFlow()

    private val _isSimulatorMode = MutableStateFlow(true)
    val isSimulatorMode: StateFlow<Boolean> = _isSimulatorMode.asStateFlow()

    private var simulatorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private var targetMacAddress: String? = null
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    init {
        startSimulatorEngine()
    }

    fun setSimulatorMode(enabled: Boolean) {
        _isSimulatorMode.value = enabled
        if (enabled) {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
            startSimulatorEngine()
        } else {
            simulatorJob?.cancel()
            _connectionState.value = BleConnectionState.Disconnected
            if (_autoReconnect.value) {
                autoConnectLastDevice()
            }
        }
    }

    fun setAutoReconnect(enabled: Boolean) {
        _autoReconnect.value = enabled
    }

    fun startScan() {
        if (_isSimulatorMode.value) {
            _connectionState.value = BleConnectionState.Scanning
            scope.launch {
                val mockDevices = listOf(
                    BleDeviceItem("ESP32-S3-AI-HUB", "24:6F:28:AE:5B:C2", rssi = -58, isESP32 = true),
                    BleDeviceItem("ESP32-S3-SIMULATOR", "30:AE:A4:00:11:22", rssi = -64, isESP32 = true),
                    BleDeviceItem("Smart Beacon", "EC:DA:3B:12:34:56", rssi = -82, isESP32 = false)
                )
                _scannedDevices.value = mockDevices
                delay(2500)
                if (_connectionState.value is BleConnectionState.Scanning) {
                    _connectionState.value = BleConnectionState.Connected(
                        deviceName = "ESP32-S3-AI-HUB",
                        macAddress = "24:6F:28:AE:5B:C2",
                        rssi = -58,
                        isSimulator = true
                    )
                }
            }
            return
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _connectionState.value = BleConnectionState.Error("Bluetooth disabled on phone")
            return
        }

        _connectionState.value = BleConnectionState.Scanning
        _scannedDevices.value = emptyList()

        try {
            val scanner = bluetoothAdapter.bluetoothLeScanner
            scanner?.startScan(scanCallback)
            scope.launch {
                delay(10000)
                stopScan()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting scan", e)
            _connectionState.value = BleConnectionState.Error("Scan failed: ${e.localizedMessage}")
        }
    }

    fun stopScan() {
        if (!_isSimulatorMode.value) {
            try {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping scan", e)
            }
        }
        if (_connectionState.value is BleConnectionState.Scanning) {
            _connectionState.value = BleConnectionState.Disconnected
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                val name = device.name ?: "Unknown Device"
                val address = device.address
                val rssi = result.rssi
                val isEsp32 = name.contains("ESP32", ignoreCase = true) || name.contains("HUB", ignoreCase = true)

                val current = _scannedDevices.value.toMutableList()
                val index = current.indexOfFirst { it.address == address }
                val newItem = BleDeviceItem(name, address, rssi, isEsp32)
                if (index >= 0) {
                    current[index] = newItem
                } else {
                    current.add(newItem)
                }
                _scannedDevices.value = current

                // Auto-connect if matching remembered device or first ESP32 found
                if (_autoReconnect.value && isEsp32 && _connectionState.value !is BleConnectionState.Connected) {
                    stopScan()
                    connectToDevice(address, name)
                }
            }
        }
    }

    fun connectToDevice(macAddress: String, name: String = "ESP32-S3-AI-HUB") {
        targetMacAddress = macAddress
        _connectionState.value = BleConnectionState.Connecting(name, macAddress)

        if (_isSimulatorMode.value) {
            scope.launch {
                delay(1200)
                _connectionState.value = BleConnectionState.Connected(
                    deviceName = name,
                    macAddress = macAddress,
                    rssi = -60,
                    isSimulator = true
                )
            }
            return
        }

        try {
            val device = bluetoothAdapter?.getRemoteDevice(macAddress)
            if (device == null) {
                _connectionState.value = BleConnectionState.Error("Device not found: $macAddress")
                return
            }
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
        } catch (e: Exception) {
            _connectionState.value = BleConnectionState.Error("Connection error: ${e.localizedMessage}")
        }
    }

    fun disconnect() {
        if (_isSimulatorMode.value) {
            _connectionState.value = BleConnectionState.Disconnected
            return
        }
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting GATT", e)
        }
        _connectionState.value = BleConnectionState.Disconnected
    }

    fun sendCommand(command: String) {
        val jsonMsg = JSONObject().put("command", command).toString()
        Log.d(TAG, "Sending Command: $jsonMsg")

        if (_isSimulatorMode.value) {
            // Immediately apply command in simulator
            val current = _sensorData.value
            val updated = when (command) {
                "LIGHT_ON" -> current.copy(light = true)
                "LIGHT_OFF" -> current.copy(light = false)
                "FAN_ON" -> current.copy(fan = true)
                "FAN_OFF" -> current.copy(fan = false)
                "AUTO_MODE_ON" -> current.copy(autoMode = true)
                "AUTO_MODE_OFF" -> current.copy(autoMode = false)
                "AI_MODE_ON" -> current.copy(aiControlMode = true)
                "AI_MODE_OFF" -> current.copy(aiControlMode = false)
                else -> current
            }
            _sensorData.value = updated
            return
        }

        // Real BLE write
        bluetoothGatt?.let { gatt ->
            val service = gatt.getService(UUID.fromString(Esp32FirmwareCode.SERVICE_UUID))
            val charRx = service?.getCharacteristic(UUID.fromString(Esp32FirmwareCode.RX_CHAR_UUID))
            if (charRx != null) {
                charRx.value = jsonMsg.toByteArray(Charsets.UTF_8)
                charRx.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                gatt.writeCharacteristic(charRx)
            } else {
                Log.w(TAG, "RX Characteristic not found")
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.")
                gatt?.discoverServices()
                _connectionState.value = BleConnectionState.Connected(
                    deviceName = gatt?.device?.name ?: "ESP32-S3",
                    macAddress = gatt?.device?.address ?: "",
                    rssi = -65,
                    isSimulator = false
                )
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.")
                _connectionState.value = BleConnectionState.Disconnected
                if (_autoReconnect.value) {
                    scope.launch {
                        delay(3000)
                        autoConnectLastDevice()
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                val service = gatt.getService(UUID.fromString(Esp32FirmwareCode.SERVICE_UUID))
                val txChar = service?.getCharacteristic(UUID.fromString(Esp32FirmwareCode.TX_CHAR_UUID))
                if (txChar != null) {
                    gatt.setCharacteristicNotification(txChar, true)
                    val descriptor = txChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    if (descriptor != null) {
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)
                    }
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val dataStr = String(characteristic.value ?: byteArrayOf(), Charsets.UTF_8)
            parseSensorJson(dataStr)
        }
    }

    private fun parseSensorJson(jsonStr: String) {
        try {
            val json = JSONObject(jsonStr)
            val current = _sensorData.value
            val updated = current.copy(
                temperature = json.optDouble("temperature", current.temperature.toDouble()).toFloat(),
                humidity = json.optDouble("humidity", current.humidity.toDouble()).toFloat(),
                ldr = json.optInt("ldr", current.ldr),
                motion = json.optBoolean("motion", current.motion),
                light = json.optBoolean("light", current.light),
                fan = json.optBoolean("fan", current.fan),
                autoMode = json.optBoolean("autoMode", current.autoMode),
                aiControlMode = json.optBoolean("aiControl", current.aiControlMode),
                battery = json.optInt("battery", current.battery),
                timestamp = System.currentTimeMillis()
            )
            _sensorData.value = updated
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse sensor JSON: $jsonStr", e)
        }
    }

    private fun autoConnectLastDevice() {
        if (targetMacAddress != null && !_isSimulatorMode.value) {
            connectToDevice(targetMacAddress!!)
        } else if (_isSimulatorMode.value) {
            _connectionState.value = BleConnectionState.Connected(
                deviceName = "ESP32-S3-AI-HUB",
                macAddress = "24:6F:28:AE:5B:C2",
                rssi = -58,
                isSimulator = true
            )
        }
    }

    private fun startSimulatorEngine() {
        simulatorJob?.cancel()
        simulatorJob = scope.launch {
            _connectionState.value = BleConnectionState.Connected(
                deviceName = "ESP32-S3-AI-HUB (Simulated)",
                macAddress = "24:6F:28:AE:5B:C2",
                rssi = -58,
                isSimulator = true
            )

            var step = 0
            while (isActive) {
                delay(2000)
                if (_isSimulatorMode.value && _connectionState.value is BleConnectionState.Connected) {
                    step++
                    val current = _sensorData.value

                    // Slight environmental temperature & humidity drift
                    val tempDrift = (Math.sin(step * 0.2) * 0.8).toFloat()
                    val humidityDrift = (Math.cos(step * 0.15) * 1.5).toFloat()
                    val newTemp = (27.5f + tempDrift).coerceIn(18.0f, 42.0f)
                    val newHum = (62.0f + humidityDrift).coerceIn(30.0f, 95.0f)

                    // Periodic motion trigger every ~12 cycles
                    val motionTrigger = (step % 12 == 0 || step % 12 == 1)

                    // LDR changes based on light state or motion
                    val ldrVal = if (current.light) 120 else if (motionTrigger) 780 else 450

                    // Auto mode reaction
                    var newLight = current.light
                    var newFan = current.fan
                    if (current.autoMode) {
                        if (motionTrigger && ldrVal > 600) newLight = true
                        if (newTemp > 28.5f) newFan = true else if (newTemp < 26.0f) newFan = false
                    }

                    _sensorData.value = current.copy(
                        temperature = Math.round(newTemp * 10) / 10.0f,
                        humidity = Math.round(newHum * 10) / 10.0f,
                        ldr = ldrVal,
                        motion = motionTrigger,
                        light = newLight,
                        fan = newFan,
                        timestamp = System.currentTimeMillis()
                    )
                }
            }
        }
    }
}
