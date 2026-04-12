package com.spbu.projecttrack.core.storage

import android.content.Context
import android.content.SharedPreferences

class AppPreferencesImpl(context: Context) : AppPreferences {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "app_preferences",
        Context.MODE_PRIVATE
    )
    
    override fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }
    
    override fun setOnboardingCompleted() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
    }
    
    override fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }
    
    override fun saveAccessToken(token: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }
    
    override fun clearTokens() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
    }

    override fun getCustomHostIP(): String? {
        return prefs.getString(KEY_CUSTOM_HOST_IP, null)
    }

    override fun saveCustomHostIP(ip: String) {
        prefs.edit().putString(KEY_CUSTOM_HOST_IP, ip).apply()
    }

    override fun clearCustomHostIP() {
        prefs.edit().remove(KEY_CUSTOM_HOST_IP).apply()
    }

    override fun getAppLanguage(): String? {
        return prefs.getString(KEY_APP_LANGUAGE, null)
    }

    override fun saveAppLanguage(language: String) {
        prefs.edit().putString(KEY_APP_LANGUAGE, language).apply()
    }

    override fun getAppThemeMode(): String? {
        return prefs.getString(KEY_APP_THEME_MODE, null)
    }

    override fun saveAppThemeMode(themeMode: String) {
        prefs.edit().putString(KEY_APP_THEME_MODE, themeMode).apply()
    }

    override fun isProjectStatusNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_PROJECT_STATUS_NOTIFICATIONS, true)
    }

    override fun setProjectStatusNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PROJECT_STATUS_NOTIFICATIONS, enabled).apply()
    }
    
    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_CUSTOM_HOST_IP = "custom_host_ip"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_APP_THEME_MODE = "app_theme_mode"
        private const val KEY_PROJECT_STATUS_NOTIFICATIONS = "project_status_notifications"
    }
}

// Глобальная переменная для хранения единственного экземпляра
private var instance: AppPreferences? = null

actual fun createAppPreferences(): AppPreferences {
    return instance ?: throw IllegalStateException(
        "AppPreferences not initialized. Call initAppPreferences(context) first."
    )
}

// Функция инициализации (вызывается в MainActivity)
fun initAppPreferences(context: Context) {
    if (instance == null) {
        instance = AppPreferencesImpl(context.applicationContext)
    }
}
