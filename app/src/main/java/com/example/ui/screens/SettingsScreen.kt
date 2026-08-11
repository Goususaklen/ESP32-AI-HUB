package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.theme.RoseAlert
import com.example.ui.theme.TechCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    customApiKey: String,
    temperatureUnit: String,
    tempThreshold: Float,
    ttsEnabled: Boolean,
    onSetCustomApiKey: (String) -> Unit,
    onSetTemperatureUnit: (String) -> Unit,
    onSetTempThreshold: (Float) -> Unit,
    onToggleTts: (Boolean) -> Unit,
    onClearChat: () -> Unit,
    onClearLogs: () -> Unit
) {
    var apiKeyText by remember { mutableStateOf(customApiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = TechCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "App & AI Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "API Keys, Voice & Units Preferences",
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
            // OpenAI / Gemini Cloud AI Key
            item {
                Text(
                    "AI Intelligence Integration",
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = TechCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("OpenAI / Gemini API Key", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            "Secret API keys are stored securely on the client side. Never share your key.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = apiKeyText,
                            onValueChange = { apiKeyText = it },
                            placeholder = { Text("Enter custom API key...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api_key_input"),
                            singleLine = true,
                            visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                    Icon(
                                        imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle key visibility"
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                onSetCustomApiKey(apiKeyText)
                                Toast.makeText(context, "API Key saved successfully!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Save Key", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Display & Units Preferences
            item {
                Text(
                    "Display & Telemetry Settings",
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
                        Text("Temperature Scale", fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FilterChip(
                                selected = temperatureUnit.contains("Celsius"),
                                onClick = { onSetTemperatureUnit("Celsius (°C)") },
                                label = { Text("Celsius (°C)") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = temperatureUnit.contains("Fahrenheit"),
                                onClick = { onSetTemperatureUnit("Fahrenheit (°F)") },
                                label = { Text("Fahrenheit (°F)") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Text-to-Speech AI Voice Responses", fontWeight = FontWeight.Bold)
                                Text("Speak AI responses automatically", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = ttsEnabled,
                                onCheckedChange = onToggleTts,
                                colors = SwitchDefaults.colors(checkedThumbColor = TechCyan)
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        Text("Temperature Alarm Threshold: ${tempThreshold.toInt()}°C", fontWeight = FontWeight.Bold)
                        Slider(
                            value = tempThreshold,
                            onValueChange = onSetTempThreshold,
                            valueRange = 20f..50f,
                            steps = 30,
                            colors = SliderDefaults.colors(thumbColor = TechCyan, activeTrackColor = TechCyan)
                        )
                    }
                }
            }

            // Maintenance & Data Clearing
            item {
                Text(
                    "Database & Maintenance",
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
                        OutlinedButton(
                            onClick = {
                                onClearChat()
                                Toast.makeText(context, "Chat history cleared", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = RoseAlert)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear AI Chat History", color = RoseAlert)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                onClearLogs()
                                Toast.makeText(context, "Room telemetry database cleared", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, tint = RoseAlert)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear Sensor Database Logs", color = RoseAlert)
                        }
                    }
                }
            }

            // About App Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("ESP32 AI Hub v1.0", fontWeight = FontWeight.Bold)
                        Text("Powered by Kotlin Jetpack Compose, Room DB, BLE GATT, Speech Services & Cloud AI.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
