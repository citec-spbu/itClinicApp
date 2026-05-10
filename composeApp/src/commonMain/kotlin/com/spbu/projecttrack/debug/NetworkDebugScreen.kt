package com.spbu.projecttrack.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.spbu.projecttrack.core.network.ApiConfig
import com.spbu.projecttrack.core.network.DeviceInfo
import com.spbu.projecttrack.core.network.NetworkSettings

/**
 * Экран отладки сети для проверки IP адресов и подключения
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkDebugScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Отладка сети") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
                    }
                }
            )
        }
    ) { paddingValues ->
        var customIP by remember { mutableStateOf(NetworkSettings.customHostIP.value ?: "") }
        val isCustomSet by NetworkSettings.customHostIP.collectAsState()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Manual IP Configuration
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔧 Настройка IP вручную",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Если автоматическое определение не работает, введите IP компьютера:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = customIP,
                        onValueChange = { customIP = it },
                        label = { Text("IP адрес (например, 192.168.1.5)") },
                        placeholder = { Text("192.168.1.x") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                NetworkSettings.setCustomHostIP(customIP)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Применить")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                NetworkSettings.resetToAuto()
                                customIP = ""
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Авто")
                        }
                    }
                    
                    if (isCustomSet != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✅ Используется: $isCustomSet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // API Configuration
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "API Configuration",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    InfoRow("Base URL", ApiConfig.baseUrl)
                    InfoRow("Local API", if (ApiConfig.isLocalApi) "✅ Enabled" else "❌ Disabled")
                    InfoRow("IP Source", if (NetworkSettings.isCustomIPSet()) "🔧 Manual" else "🤖 Auto")
                }
            }
            
            // Device Info
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Device Info",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    InfoRow("Device Type", if (DeviceInfo.isEmulator()) "🖥️ Emulator" else "📱 Real Device")
                    InfoRow("Host Address", DeviceInfo.getLocalHostAddress())
                }
            }
            
            // Network Interfaces info removed (platform-specific)
            
            // Debug Info
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Full Debug Info",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = ApiConfig.getDebugInfo(),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ℹ️ Инструкции",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = """
                            1. Убедитесь, что Docker запущен на компьютере
                            2. Проверьте, что порты доступны: 0.0.0.0:8000
                            3. Телефон и компьютер в одной сети
                            4. IP адрес определяется автоматически
                            
                            Если не работает:
                            - Перезапустите приложение
                            - Проверьте docker-compose.yaml
                            - Используйте продакшн API (ApiConfig.USE_LOCAL_API = false)
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace
        )
    }
}

