package com.spbu.projecttrack.core.auth

import com.spbu.projecttrack.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Менеджер авторизации для управления токенами
 * 
 * Использование:
 * - AuthManager.setToken(token) - установить токен после авторизации
 * - AuthManager.clearToken() - очистить токен при выходе
 * - AuthManager.isAuthorized - проверить статус авторизации
 * - AuthManager.getToken() - получить текущий токен
 */
object AuthManager {
    private val _authToken = MutableStateFlow<String?>(null)
    val authToken: StateFlow<String?> = _authToken.asStateFlow()
    
    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    private val _currentUserId = MutableStateFlow<Int?>(null)
    val currentUserId: StateFlow<Int?> = _currentUserId.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Установить токен авторизации
     * @param token JWT токен
     */
    fun setToken(token: String) {
        _authToken.value = token
        _isAuthorized.value = true
        _currentUserId.value = decodeUserId(token)
    }
    
    /**
     * Очистить токен (выход из системы)
     */
    fun clearToken() {
        _authToken.value = null
        _isAuthorized.value = false
        _currentUserId.value = null
    }
    
    /**
     * Получить текущий токен
     * @return JWT токен или null
     */
    fun getToken(): String? = _authToken.value
    
    /**
     * Установить тестовый токен для разработки
     * Токен берется из BuildConfig.kt
     */
    fun setTestToken() {
        setToken(BuildConfig.TEST_TOKEN)
        println("🔑 Установлен тестовый токен для разработки")
    }

    @Serializable
    private data class JwtPayload(val id: Int? = null)

    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeUserId(token: String): Int? {
        val parts = token.split(".")
        if (parts.size < 2) return null
        var payload = parts[1]
        payload = payload.replace('-', '+').replace('_', '/')
        val pad = (4 - payload.length % 4) % 4
        if (pad > 0) payload += "=".repeat(pad)
        val jsonString = runCatching { Base64.decode(payload).decodeToString() }.getOrNull() ?: return null
        return runCatching { json.decodeFromString<JwtPayload>(jsonString).id }.getOrNull()
    }
}
