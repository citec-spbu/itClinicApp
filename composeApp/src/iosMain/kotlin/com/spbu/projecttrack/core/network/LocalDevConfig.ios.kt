package com.spbu.projecttrack.core.network

import com.spbu.projecttrack.BuildConfig
import platform.Foundation.*
import kotlinx.cinterop.*

object LocalDevConfig {
    @OptIn(ExperimentalForeignApi::class)
    private fun getDeviceIP(): String? {
        return try {
            val wifiIP = getWiFiIP()
            if (wifiIP != null) {
                println("📱 IP устройства (iOS): $wifiIP")
                return wifiIP
            }
            
            println("⚠️  Не удалось получить IP устройства (iOS)")
            null
        } catch (e: Exception) {
            println("❌ Ошибка получения IP устройства (iOS): ${e.message}")
            null
        }
    }
    
    @OptIn(ExperimentalForeignApi::class)
    private fun getWiFiIP(): String? {
        // Kept as a stub because KMP code does not currently bridge SystemConfiguration.
        return null
    }
    
    var userDefinedIp: String = ""
    
    val LOCAL_MACHINE_IP: String
        get() = getHostIP()
    
    private val configuredHostIp: String?
        get() = BuildConfig.LOCAL_HOST_IP.takeIf { it.isNotBlank() }

    private const val DEFAULT_FALLBACK_IP = "192.168.1.2"
    
    fun getHostIP(): String {
        if (userDefinedIp.isNotBlank()) {
            println("✅ Используется пользовательский IP (iOS): $userDefinedIp")
            return userDefinedIp
        }

        configuredHostIp?.let { configured ->
            println("✅ Используется LOCAL_HOST_IP из BuildConfig (iOS): $configured")
            return configured
        }
        
        val deviceIP = getDeviceIP()
        if (deviceIP != null) {
            // Mirror the Android fallback and avoid `.1`, which is usually the router.
            val parts = deviceIP.split(".")
            if (parts.size == 4) {
                val subnet = "${parts[0]}.${parts[1]}.${parts[2]}"
                val hostIP = "$subnet.2"
                println("✅ IP хоста (iOS): $hostIP (на основе устройства $deviceIP)")
                return hostIP
            }
        }
        
        println("⚠️  Не удалось определить IP автоматически (iOS), использую $DEFAULT_FALLBACK_IP")
        println("💡 Совет: Откройте NetworkDebugScreen в приложении для ручного ввода IP")
        println("💡 Текущий IP вашего Mac: используйте 'ifconfig' в терминале для проверки")
        return DEFAULT_FALLBACK_IP
    }
    
    fun getNetworkInfo(): String {
        val deviceIP = getDeviceIP() ?: "Не определен"
        val hostIP = getHostIP()
        val source = when {
            userDefinedIp.isNotBlank() -> "Пользовательский"
            deviceIP != null -> "Автоматически"
            else -> "Fallback"
        }
        return "iOS устройство: $deviceIP\nХост: $hostIP\nИсточник: $source"
    }
}
