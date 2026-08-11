package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Esp32Data
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.RoseAlert
import com.example.ui.theme.TechCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlsScreen(
    sensorData: Esp32Data,
    onToggleLight: () -> Unit,
    onToggleFan: () -> Unit,
    onToggleAutoMode: () -> Unit,
    onToggleAiControlMode: () -> Unit,
    onSendCommand: (String) -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            tint = TechCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "ESP32 Hardware Controls",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Relays, Modes & Direct Commands",
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
            // Relays Control Section
            item {
                Text(
                    "Actuator Relays",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Light Relay Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = if (sensorData.light) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Light Circuit (Relay 1)", fontWeight = FontWeight.Bold)
                                    Text(
                                        if (sensorData.light) "Status: ACTIVE (ON)" else "Status: INACTIVE (OFF)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (sensorData.light) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = sensorData.light,
                                onCheckedChange = { onToggleLight() },
                                colors = SwitchDefaults.colors(checkedThumbColor = AmberWarning),
                                modifier = Modifier.testTag("light_switch")
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // Fan Relay Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ModeFanOff,
                                    contentDescription = null,
                                    tint = if (sensorData.fan) TechCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Fan / Cooler (Relay 2)", fontWeight = FontWeight.Bold)
                                    Text(
                                        if (sensorData.fan) "Status: ACTIVE (ON)" else "Status: INACTIVE (OFF)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (sensorData.fan) TechCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = sensorData.fan,
                                onCheckedChange = { onToggleFan() },
                                colors = SwitchDefaults.colors(checkedThumbColor = TechCyan),
                                modifier = Modifier.testTag("fan_switch")
                            )
                        }
                    }
                }
            }

            // Operating Modes
            item {
                Text(
                    "System Operation Modes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Auto Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = if (sensorData.autoMode) EmeraldActive else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Automatic Sensor Mode", fontWeight = FontWeight.Bold)
                                    Text(
                                        "ESP32 automatically triggers relays based on temp & motion",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = sensorData.autoMode,
                                onCheckedChange = { onToggleAutoMode() },
                                colors = SwitchDefaults.colors(checkedThumbColor = EmeraldActive),
                                modifier = Modifier.testTag("auto_mode_switch")
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // AI Autonomous Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = if (sensorData.aiControlMode) TechCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("AI Autonomous Control Mode", fontWeight = FontWeight.Bold)
                                    Text(
                                        "Allow AI Agent to send hardware overrides when requested",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = sensorData.aiControlMode,
                                onCheckedChange = {
                                    if (!sensorData.aiControlMode) {
                                        showConfirmDialog = "AI_MODE_ON"
                                    } else {
                                        onToggleAiControlMode()
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = TechCyan),
                                modifier = Modifier.testTag("ai_control_mode_switch")
                            )
                        }
                    }
                }
            }

            // Direct Protocol Commands
            item {
                Text(
                    "Direct ESP32 Protocol Commands",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onSendCommand("GET_SENSOR_DATA") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("GET_SENSOR_DATA")
                        }
                        Button(
                            onClick = { onSendCommand("DEVICE_STATUS") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("DEVICE_STATUS")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSendCommand("LIGHT_ON") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("LIGHT_ON")
                        }
                        OutlinedButton(
                            onClick = { onSendCommand("LIGHT_OFF") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("LIGHT_OFF")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSendCommand("FAN_ON") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("FAN_ON")
                        }
                        OutlinedButton(
                            onClick = { onSendCommand("FAN_OFF") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("FAN_OFF")
                        }
                    }
                }
            }

            // ESP32 GPIO Pin Map Guide Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = TechCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ESP32-S3 Hardware Wiring Pinmap", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("• DHT11 Temp/Humidity Sensor: GPIO 4", style = MaterialTheme.typography.bodySmall)
                        Text("• LDR Light Sensor: GPIO 1 (ADC1)", style = MaterialTheme.typography.bodySmall)
                        Text("• PIR IR Motion Sensor: GPIO 5", style = MaterialTheme.typography.bodySmall)
                        Text("• Light Control Relay: GPIO 6", style = MaterialTheme.typography.bodySmall)
                        Text("• Fan Control Relay: GPIO 7", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    // Safety Confirmation Dialog
    showConfirmDialog?.let { cmd ->
        AlertDialog(
            onDismissRequest = { showConfirmDialog = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = RoseAlert) },
            title = { Text("Confirm AI Autonomous Override") },
            text = { Text("Enabling AI Control Mode will allow the cloud AI agent to directly execute hardware relay operations on your ESP32-S3 board. Are you sure?") },
            confirmButton = {
                Button(
                    onClick = {
                        onToggleAiControlMode()
                        showConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseAlert)
                ) {
                    Text("Enable AI Override")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
