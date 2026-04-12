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
    fun getAppLanguage(): String?
    fun saveAppLanguage(language: String)
    fun getAppThemeMode(): String?
    fun saveAppThemeMode(themeMode: String)
    fun isProjectStatusNotificationsEnabled(): Boolean
    fun setProjectStatusNotificationsEnabled(enabled: Boolean)
}

// Factory function для создания платформо-специфичной реализации
expect fun createAppPreferences(): AppPreferences
