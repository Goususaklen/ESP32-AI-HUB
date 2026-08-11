package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BleConnectionState
import com.example.data.model.BleDeviceItem
import com.example.firmware.Esp32FirmwareCode
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.TechCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    connectionState: BleConnectionState,
    scannedDevices: List<BleDeviceItem>,
    autoReconnect: Boolean,
    isSimulatorMode: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnectToDevice: (String, String) -> Unit,
    onDisconnectDevice: () -> Unit,
    onSetAutoReconnect: (Boolean) -> Unit,
    onSetSimulatorMode: (Boolean) -> Unit
) {
    var showFirmwareDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val isConnected = connectionState is BleConnectionState.Connected
    val isScanning = connectionState is BleConnectionState.Scanning

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = TechCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "ESP32-S3 BLE Connection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Bluetooth LE Pairing & Hardware",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showFirmwareDialog = true }) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = "Firmware Code",
                            tint = TechCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current Device Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isConnected) EmeraldActive.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(if (isConnected) EmeraldActive else MaterialTheme.colorScheme.error)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (isConnected) "CONNECTED DEVICE" else "DISCONNECTED",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isConnected) EmeraldActive else MaterialTheme.colorScheme.error
                                )
                            }

                            if (isConnected) {
                                OutlinedButton(
                                    onClick = onDisconnectDevice,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("disconnect_btn")
                                ) {
                                    Text("Disconnect")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (connectionState is BleConnectionState.Connected) {
                            Text(
                                connectionState.deviceName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "MAC Address: ${connectionState.macAddress}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                "Signal Strength (RSSI): ${connectionState.rssi} dBm",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (connectionState.isSimulator) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = TechCyan.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "⚡ Running in Built-in Simulator Mode",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TechCyan,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Text(
                                "No ESP32 board currently connected",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onStartScan,
                                enabled = !isScanning,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                                modifier = Modifier.testTag("scan_now_btn")
                            ) {
                                Icon(Icons.Default.BluetoothSearching, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan for Devices", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Connection Preferences & Switches
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Automatic Reconnect", fontWeight = FontWeight.Bold)
                                Text(
                                    "Attempt auto-reconnection when connection drops",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoReconnect,
                                onCheckedChange = onSetAutoReconnect,
                                colors = SwitchDefaults.colors(checkedThumbColor = TechCyan)
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ESP32 Board Simulator Mode", fontWeight = FontWeight.Bold)
                                Text(
                                    "Simulate ESP32-S3 sensor data & BLE telemetry without physical hardware",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isSimulatorMode,
                                onCheckedChange = onSetSimulatorMode,
                                colors = SwitchDefaults.colors(checkedThumbColor = TechCyan),
                                modifier = Modifier.testTag("simulator_mode_switch")
                            )
                        }
                    }
                }
            }

            // Scanned BLE Devices List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Discovered BLE Devices",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = TechCyan)
                    } else {
                        TextButton(onClick = onStartScan) {
                            Text("Refresh Scan", color = TechCyan)
                        }
                    }
                }
            }

            if (scannedDevices.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Devices, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (isScanning) "Searching for nearby Bluetooth LE devices..." else "Tap 'Scan for Devices' to find your ESP32-S3",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(scannedDevices) { device ->
                    BleDeviceRow(
                        device = device,
                        onConnect = { onConnectToDevice(device.address, device.name) }
                    )
                }
            }

            // Firmware Button Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = TechCyan, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ESP32-S3 Arduino C++ Firmware", fontWeight = FontWeight.Bold)
                            Text("View & copy complete C++ sketch for ESP32-S3 Arduino Core", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = { showFirmwareDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("View C++", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Firmware Dialog Modal
    if (showFirmwareDialog) {
        AlertDialog(
            onDismissRequest = { showFirmwareDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = TechCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ESP32-S3 Arduino Sketch")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Flash this code to your ESP32-S3 board using Arduino IDE or PlatformIO:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A)
                    ) {
                        LazyColumn(modifier = Modifier.padding(12.dp)) {
                            item {
                                Text(
                                    Esp32FirmwareCode.ARDUINO_SKETCH,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("ESP32 Firmware", Esp32FirmwareCode.ARDUINO_SKETCH)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Firmware copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TechCyan)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Sketch", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFirmwareDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun BleDeviceRow(device: BleDeviceItem, onConnect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (device.isESP32) Icons.Default.Memory else Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = if (device.isESP32) TechCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(device.name, fontWeight = FontWeight.Bold)
                    Text(device.address, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    Text("RSSI: ${device.rssi} dBm", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Button(
                onClick = onConnect,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (device.isESP32) TechCyan else MaterialTheme.colorScheme.primary)
            ) {
                Text("Pair", color = if (device.isESP32) Color.Black else Color.White)
            }
        }
    }
}
