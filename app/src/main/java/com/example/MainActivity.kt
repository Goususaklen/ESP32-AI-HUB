package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.MainViewModel
import com.example.ui.screens.*
import com.example.ui.theme.Esp32AiHubTheme
import com.example.ui.theme.TechCyan

sealed class NavTab(val route: String, val title: String, val icon: ImageVector) {
    object Chat : NavTab("chat", "AI Chat", Icons.Default.Psychology)
    object Dashboard : NavTab("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Controls : NavTab("controls", "Controls", Icons.Default.Tune)
    object Device : NavTab("device", "Device", Icons.Default.Bluetooth)
    object Settings : NavTab("settings", "Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkAndRequestPermissions()

        setContent {
            Esp32AiHubTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (permissionsNeeded.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsNeeded.toTypedArray())
        }
    }
}

@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    var currentTab by remember { mutableStateOf<NavTab>(NavTab.Dashboard) }

    val chatMessages by viewModel.chatMessages.collectAsState()
    val sensorData by viewModel.sensorData.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val sensorLogs by viewModel.sensorLogs.collectAsState()

    val autoReconnect by viewModel.autoReconnect.collectAsState()
    val isSimulatorMode by viewModel.isSimulatorMode.collectAsState()

    val customApiKey by viewModel.customApiKey.collectAsState()
    val temperatureUnit by viewModel.temperatureUnit.collectAsState()
    val tempThreshold by viewModel.tempThreshold.collectAsState()

    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val ttsEnabled by viewModel.ttsEnabled.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val tabs = listOf(
                    NavTab.Chat,
                    NavTab.Dashboard,
                    NavTab.Controls,
                    NavTab.Device,
                    NavTab.Settings
                )

                tabs.forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = tab.title,
                                tint = if (selected) TechCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                tab.title,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) TechCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.testTag("tab_${tab.route}")
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                NavTab.Chat -> ChatScreen(
                    chatMessages = chatMessages,
                    sensorData = sensorData,
                    connectionState = connectionState,
                    isAiLoading = isAiLoading,
                    isListening = isListening,
                    ttsEnabled = ttsEnabled,
                    onSendMessage = { viewModel.sendMessageToAi(it) },
                    onStartVoiceInput = { viewModel.startVoiceInput() },
                    onStopVoiceInput = { viewModel.stopVoiceInput() },
                    onToggleTts = { viewModel.toggleTts(it) },
                    onClearChat = { viewModel.clearChat() }
                )

                NavTab.Dashboard -> DashboardScreen(
                    sensorData = sensorData,
                    connectionState = connectionState,
                    sensorLogs = sensorLogs,
                    temperatureUnit = temperatureUnit,
                    onToggleLight = { viewModel.toggleLight() },
                    onToggleFan = { viewModel.toggleFan() },
                    onNavigateToChatWithQuery = { query ->
                        currentTab = NavTab.Chat
                        viewModel.sendMessageToAi(query)
                    }
                )

                NavTab.Controls -> ControlsScreen(
                    sensorData = sensorData,
                    onToggleLight = { viewModel.toggleLight() },
                    onToggleFan = { viewModel.toggleFan() },
                    onToggleAutoMode = { viewModel.toggleAutoMode() },
                    onToggleAiControlMode = { viewModel.toggleAiControlMode() },
                    onSendCommand = { viewModel.sendCommand(it) }
                )

                NavTab.Device -> DeviceScreen(
                    connectionState = connectionState,
                    scannedDevices = scannedDevices,
                    autoReconnect = autoReconnect,
                    isSimulatorMode = isSimulatorMode,
                    onStartScan = { viewModel.startScan() },
                    onStopScan = { viewModel.stopScan() },
                    onConnectToDevice = { mac, name -> viewModel.connectToDevice(mac, name) },
                    onDisconnectDevice = { viewModel.disconnectDevice() },
                    onSetAutoReconnect = { viewModel.setAutoReconnect(it) },
                    onSetSimulatorMode = { viewModel.setSimulatorMode(it) }
                )

                NavTab.Settings -> SettingsScreen(
                    customApiKey = customApiKey,
                    temperatureUnit = temperatureUnit,
                    tempThreshold = tempThreshold,
                    ttsEnabled = ttsEnabled,
                    onSetCustomApiKey = { viewModel.setCustomApiKey(it) },
                    onSetTemperatureUnit = { viewModel.setTemperatureUnit(it) },
                    onSetTempThreshold = { viewModel.setTempThreshold(it) },
                    onToggleTts = { viewModel.toggleTts(it) },
                    onClearChat = { viewModel.clearChat() },
                    onClearLogs = { viewModel.clearLogs() }
                )
            }
        }
    }
}
