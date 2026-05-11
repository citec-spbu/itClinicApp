package com.spbu.projecttrack.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NetworkSettings {
    private val _customHostIP = MutableStateFlow<String?>(null)
    val customHostIP: StateFlow<String?> = _customHostIP.asStateFlow()
    
    fun setCustomHostIP(ip: String?) {
        _customHostIP.value = ip?.takeIf { it.isNotBlank() }
        println("🔧 Custom host IP set to: ${ip ?: "auto"}")
    }
    
    fun getEffectiveHostIP(): String {
        val custom = _customHostIP.value
        return if (!custom.isNullOrBlank()) {
            println("✅ Using custom host IP: $custom")
            custom
        } else {
            val auto = DeviceInfo.getLocalHostAddress()
            println("🤖 Using auto-detected host IP: $auto")
            auto
        }
    }
    
    fun resetToAuto() {
        setCustomHostIP(null)
    }
    
    fun isCustomIPSet(): Boolean = !_customHostIP.value.isNullOrBlank()
}
