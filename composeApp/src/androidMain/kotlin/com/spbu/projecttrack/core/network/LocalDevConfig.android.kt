package com.spbu.projecttrack.core.network

import com.spbu.projecttrack.BuildConfig
import java.net.NetworkInterface
import java.net.Inet4Address

object LocalDevConfig {
    private val configuredHostIp: String?
        get() = BuildConfig.LOCAL_HOST_IP.takeIf { it.isNotBlank() }

    private fun getDeviceIP(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            
            for (networkInterface in interfaces) {
                if (networkInterface.isLoopback || !networkInterface.isUp) {
                    continue
                }
                
                val addresses = networkInterface.inetAddresses
                for (address in addresses) {
                    if (address is Inet4Address && 
                        !address.isLoopbackAddress && 
                        !address.isLinkLocalAddress) {
                        val ip = address.hostAddress
                        
                        if (ip != null && !ip.startsWith("10.0.2.")) {
                            println("📱 IP устройства: $ip (${networkInterface.name})")
                            return ip
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            println("❌ Ошибка получения IP устройства: ${e.message}")
            null
        }
    }
    
    private fun getHostIPCandidates(deviceIP: String): List<String> {
        val parts = deviceIP.split(".")
        if (parts.size != 4) return emptyList()
        
        val subnet = "${parts[0]}.${parts[1]}.${parts[2]}"
        val candidates = mutableListOf<String>()
        
        candidates.add("$subnet.1")
        
        for (i in 2..10) {
            candidates.add("$subnet.$i")
        }
        
        for (i in 100..110) {
            candidates.add("$subnet.$i")
        }
        
        val deviceLastOctet = parts[3].toIntOrNull() ?: 0
        for (offset in listOf(-2, -1, 1, 2)) {
            val newOctet = deviceLastOctet + offset
            if (newOctet in 2..254 && !candidates.contains("$subnet.$newOctet")) {
                candidates.add("$subnet.$newOctet")
            }
        }
        
        println("🔍 Кандидаты IP хоста: ${candidates.take(5)}... (всего ${candidates.size})")
        return candidates
    }
    
    fun getHostIP(): String {
        return try {
            configuredHostIp?.let { configured ->
                println("✅ Используется LOCAL_HOST_IP из BuildConfig: $configured")
                return configured
            }

            val deviceIP = getDeviceIP()
            
            if (deviceIP != null) {
                val candidates = getHostIPCandidates(deviceIP)
                
                if (candidates.isNotEmpty()) {
                    // Prefer typical workstation ranges before falling back to the full list.
                    val selectedIP = candidates.firstOrNull { 
                        (it.split(".")[3].toIntOrNull() ?: 0) in 2..10
                    } ?: candidates.firstOrNull {
                        (it.split(".")[3].toIntOrNull() ?: 0) in 100..110
                    } ?: candidates.first()
                    
                    println("✅ Выбран IP хоста: $selectedIP (на основе устройства $deviceIP)")
                    println("💡 Если не работает, проверьте реальный IP компьютера и настройте вручную")
                    return selectedIP
                }
            }
            
            val fallback = "192.168.1.2"
            println("⚠️  Не удалось определить IP автоматически, использую $fallback")
            println("💡 Узнайте IP компьютера: ipconfig (Windows) или ifconfig (Mac/Linux)")
            fallback
        } catch (e: Exception) {
            println("❌ Ошибка определения IP: ${e.message}")
            "192.168.1.2"
        }
    }
    
    fun getAllHostCandidates(): List<String> {
        val deviceIP = getDeviceIP() ?: return emptyList()
        return getHostIPCandidates(deviceIP)
    }
    
    fun getNetworkInfo(): String = buildString {
        try {
            appendLine("=== Сетевые интерфейсы ===")
            val interfaces = NetworkInterface.getNetworkInterfaces()
            
            for (networkInterface in interfaces) {
                appendLine("Интерфейс: ${networkInterface.name}")
                appendLine("  Активен: ${networkInterface.isUp}")
                appendLine("  Loopback: ${networkInterface.isLoopback}")
                
                val addresses = networkInterface.inetAddresses
                for (address in addresses) {
                    if (address is Inet4Address) {
                        appendLine("  IPv4: ${address.hostAddress}")
                    }
                }
                appendLine()
            }
        } catch (e: Exception) {
            appendLine("Ошибка: ${e.message}")
        }
    }
}
