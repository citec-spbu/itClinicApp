package com.spbu.projecttrack.core.storage

interface AppPreferences {
    fun isOnboardingCompleted(): Boolean
    fun setOnboardingCompleted()
    fun getAccessToken(): String?
    fun saveAccessToken(token: String)
    fun clearTokens()
    fun getCustomHostIP(): String?
    fun saveCustomHostIP(ip: String)
    fun clearCustomHostIP()
}

// Factory function для создания платформо-специфичной реализации
expect fun createAppPreferences(): AppPreferences
