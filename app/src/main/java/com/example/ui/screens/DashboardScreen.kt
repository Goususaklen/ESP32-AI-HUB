package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.SensorLogEntity
import com.example.data.model.BleConnectionState
import com.example.data.model.Esp32Data
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.RoseAlert
import com.example.ui.theme.TechBlue
import com.example.ui.theme.TechCyan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    sensorData: Esp32Data,
    connectionState: BleConnectionState,
    sensorLogs: List<SensorLogEntity>,
    temperatureUnit: String,
    onToggleLight: () -> Unit,
    onToggleFan: () -> Unit,
    onNavigateToChatWithQuery: (String) -> Unit
) {
    val isConnected = connectionState is BleConnectionState.Connected
    val isFahrenheit = temperatureUnit.contains("Fahrenheit")

    val tempDisplay = if (isFahrenheit) {
        val f = (sensorData.temperature * 1.8f) + 32
        "%.1f°F".format(f)
    } else {
        "%.1f°C".format(sensorData.temperature)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Dashboard,
                            contentDescription = null,
                            tint = TechCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "ESP32 Sensor Telemetry",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Live IoT Monitor & Controller",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
            // Connection Status Bar
            item {
                ConnectionHeaderCard(connectionState = connectionState)
            }

            // AI Insight Callout Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = TechCyan,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "AI Room Analysis",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Ask AI Assistant to evaluate climate, motion & light conditions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onNavigateToChatWithQuery("What is happening in my room?") },
                            colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("ai_room_analysis_btn")
                        ) {
                            Text("Analyze", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Main Sensor Cards Grid
            item {
                Text(
                    "Primary Sensor Telemetry",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Temperature Card
                    SensorGaugeCard(
                        modifier = Modifier.weight(1f),
                        title = "Temperature",
                        value = tempDisplay,
                        subtext = if (sensorData.temperature > 30f) "Hot / High Temp" else "Normal",
                        icon = Icons.Default.Thermostat,
                        accentColor = if (sensorData.temperature > 30f) RoseAlert else TechCyan,
                        progress = (sensorData.temperature / 50f).coerceIn(0f, 1f)
                    )

                    // Humidity Card
                    SensorGaugeCard(
                        modifier = Modifier.weight(1f),
                        title = "Humidity",
                        value = "${sensorData.humidity.toInt()}%",
                        subtext = "Relative RH",
                        icon = Icons.Default.WaterDrop,
                        accentColor = TechBlue,
                        progress = (sensorData.humidity / 100f).coerceIn(0f, 1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Light Sensor LDR Card
                    SensorGaugeCard(
                        modifier = Modifier.weight(1f),
                        title = "Light Level",
                        value = "${sensorData.ldr}",
                        subtext = sensorData.lightLevelDescription,
                        icon = Icons.Default.WbSunny,
                        accentColor = AmberWarning,
                        progress = ((1023 - sensorData.ldr) / 1023f).coerceIn(0f, 1f)
                    )

                    // Motion Sensor Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (sensorData.motion) RoseAlert.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (sensorData.motion) Icons.Default.DirectionsRun else Icons.Default.Accessibility,
                                    contentDescription = null,
                                    tint = if (sensorData.motion) RoseAlert else EmeraldActive,
                                    modifier = Modifier.size(28.dp)
                                )
                                Surface(
                                    color = if (sensorData.motion) RoseAlert else EmeraldActive,
                                    shape = CircleShape
                                ) {
                                    Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
                                        Text(
                                            if (sensorData.motion) "MOTION" else "CLEAR",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("IR / Motion Sensor", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                if (sensorData.motion) "Detected" else "No Movement",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (sensorData.motion) RoseAlert else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Power & Hardware Actuator Cards
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = EmeraldActive)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ESP32-S3 Power & Relays", fontWeight = FontWeight.Bold)
                            }
                            Text("${sensorData.battery}% Power", fontWeight = FontWeight.Bold, color = EmeraldActive)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Light Quick Relay Toggle
                            OutlinedCard(
                                onClick = onToggleLight,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("light_relay_toggle"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = if (sensorData.light) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Light", style = MaterialTheme.typography.labelMedium)
                                        Text(
                                            if (sensorData.light) "ON" else "OFF",
                                            fontWeight = FontWeight.Bold,
                                            color = if (sensorData.light) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Fan Quick Relay Toggle
                            OutlinedCard(
                                onClick = onToggleFan,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("fan_relay_toggle"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.ModeFanOff,
                                        contentDescription = null,
                                        tint = if (sensorData.fan) TechCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Fan", style = MaterialTheme.typography.labelMedium)
                                        Text(
                                            if (sensorData.fan) "ON" else "OFF",
                                            fontWeight = FontWeight.Bold,
                                            color = if (sensorData.fan) TechCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Room Database Log Stream
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Room Telemetry History",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${sensorLogs.size} logs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(sensorLogs.take(8)) { log ->
                SensorLogRow(log = log)
            }
        }
    }
}

@Composable
fun SensorGaugeCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    progress: Float
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
                Text(subtext, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun ConnectionHeaderCard(connectionState: BleConnectionState) {
    val isConnected = connectionState is BleConnectionState.Connected
    val isScanning = connectionState is BleConnectionState.Scanning

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isConnected -> EmeraldActive.copy(alpha = 0.15f)
                isScanning -> TechCyan.copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isConnected -> EmeraldActive
                            isScanning -> TechCyan
                            else -> MaterialTheme.colorScheme.error
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isConnected -> Icons.Default.BluetoothConnected
                        isScanning -> Icons.Default.BluetoothSearching
                        else -> Icons.Default.BluetoothDisabled
                    },
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (connectionState) {
                        is BleConnectionState.Connected -> "ESP32-S3 Connected"
                        is BleConnectionState.Scanning -> "Scanning for ESP32..."
                        is BleConnectionState.Connecting -> "Connecting to ESP32..."
                        is BleConnectionState.Error -> "BLE Connection Error"
                        else -> "ESP32 Offline"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (connectionState) {
                        is BleConnectionState.Connected -> "BLE MAC: ${connectionState.macAddress} | RSSI: ${connectionState.rssi} dBm"
                        is BleConnectionState.Error -> connectionState.message
                        else -> "Automatic reconnection enabled"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SensorLogRow(log: SensorLogEntity) {
    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(timeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${log.temperature}°C", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            Text("${log.humidity}% RH", style = MaterialTheme.typography.bodySmall)
            Text("LDR ${log.ldr}", style = MaterialTheme.typography.bodySmall)
            Text(
                if (log.motion) "Motion" else "Clear",
                style = MaterialTheme.typography.labelSmall,
                color = if (log.motion) RoseAlert else EmeraldActive
            )
        }
    }
}
