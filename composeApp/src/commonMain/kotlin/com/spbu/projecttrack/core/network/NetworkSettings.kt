package com.spbu.projecttrack.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Настройки сети - управление IP-адресом для локальной разработки
 */
object NetworkSettings {
    private val _customHostIP = MutableStateFlow<String?>(null)
    val customHostIP: StateFlow<String?> = _customHostIP.asStateFlow()
    
    /**
     * Установить пользовательский IP-адрес хоста
     */
    fun setCustomHostIP(ip: String?) {
        _customHostIP.value = ip?.takeIf { it.isNotBlank() }
        println("🔧 Установлен пользовательский IP: ${ip ?: "авто"}")
    }
    
    /**
     * Получить текущий IP хоста (пользовательский или автоматический)
     */
    fun getEffectiveHostIP(): String {
        val custom = _customHostIP.value
        return if (!custom.isNullOrBlank()) {
            println("✅ Используется пользовательский IP: $custom")
            custom
        } else {
            val auto = DeviceInfo.getLocalHostAddress()
            println("🤖 Используется автоматический IP: $auto")
            auto
        }
    }
    
    /**
     * Сбросить на автоматическое определение
     */
    fun resetToAuto() {
        setCustomHostIP(null)
    }
    
    /**
     * Проверить, установлен ли пользовательский IP
     */
    fun isCustomIPSet(): Boolean = !_customHostIP.value.isNullOrBlank()
}

