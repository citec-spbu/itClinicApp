package com.spbu.projecttrack.main.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spbu.projecttrack.core.network.ApiConfig
import com.spbu.projecttrack.core.network.MetricApiConfig
import com.spbu.projecttrack.core.network.NetworkSettings

@Composable
fun InfoScreen(
    onNetworkDebugClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customIp by NetworkSettings.customHostIP.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "Информация",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Здесь можно открыть настройку сети для локального backend и быстро проверить, куда сейчас смотрит приложение.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 2.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Локальная сеть",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Основной API: ${ApiConfig.baseUrl}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Метрики: ${MetricApiConfig.baseUrl}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (customIp.isNullOrBlank()) {
                        "IP хоста: автоопределение"
                    } else {
                        "IP хоста: $customIp"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onNetworkDebugClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Настроить IP ноутбука",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Для iPhone",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Телефон и ноутбук должны быть в одной Wi‑Fi сети. Если автоматический IP не сработает, открой настройку сети и введи IP Mac вручную.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}









