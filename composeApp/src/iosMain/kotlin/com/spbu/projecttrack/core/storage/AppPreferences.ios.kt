package com.spbu.projecttrack.core.storage

import platform.Foundation.NSUserDefaults

class AppPreferencesImpl : AppPreferences {
    
    private val userDefaults = NSUserDefaults.standardUserDefaults
    
    override fun isOnboardingCompleted(): Boolean {
        return userDefaults.boolForKey(KEY_ONBOARDING_COMPLETED)
    }
    
    override fun setOnboardingCompleted() {
        userDefaults.setBool(true, KEY_ONBOARDING_COMPLETED)
    }
    
    override fun getAccessToken(): String? {
        return userDefaults.stringForKey(KEY_ACCESS_TOKEN)
    }
    
    override fun saveAccessToken(token: String) {
        userDefaults.setObject(token, KEY_ACCESS_TOKEN)
    }

    override fun getRefreshToken(): String? {
        return userDefaults.stringForKey(KEY_REFRESH_TOKEN)
    }

    override fun saveRefreshToken(token: String) {
        userDefaults.setObject(token, KEY_REFRESH_TOKEN)
    }
    
    override fun clearTokens() {
        userDefaults.removeObjectForKey(KEY_ACCESS_TOKEN)
        userDefaults.removeObjectForKey(KEY_REFRESH_TOKEN)
    }

    override fun getCustomHostIP(): String? {
        return userDefaults.stringForKey(KEY_CUSTOM_HOST_IP)
    }

    override fun saveCustomHostIP(ip: String) {
        userDefaults.setObject(ip, KEY_CUSTOM_HOST_IP)
    }

    override fun clearCustomHostIP() {
        userDefaults.removeObjectForKey(KEY_CUSTOM_HOST_IP)
    }

    override fun getAppLanguage(): String? {
        return userDefaults.stringForKey(KEY_APP_LANGUAGE)
    }

    override fun saveAppLanguage(language: String) {
        userDefaults.setObject(language, KEY_APP_LANGUAGE)
    }

    override fun getAppThemeMode(): String? {
        return userDefaults.stringForKey(KEY_APP_THEME_MODE)
    }

    override fun saveAppThemeMode(themeMode: String) {
        userDefaults.setObject(themeMode, KEY_APP_THEME_MODE)
    }

    override fun isProjectStatusNotificationsEnabled(): Boolean {
        return if (userDefaults.objectForKey(KEY_PROJECT_STATUS_NOTIFICATIONS) != null) {
            userDefaults.boolForKey(KEY_PROJECT_STATUS_NOTIFICATIONS)
        } else {
            true
        }
    }

    override fun setProjectStatusNotificationsEnabled(enabled: Boolean) {
        userDefaults.setBool(enabled, KEY_PROJECT_STATUS_NOTIFICATIONS)
    }

    override fun getRankingFilterTemplatesJson(): String? {
        return userDefaults.stringForKey(KEY_RANKING_FILTER_TEMPLATES)
    }

    override fun saveRankingFilterTemplatesJson(json: String) {
        userDefaults.setObject(json, KEY_RANKING_FILTER_TEMPLATES)
    }
    
    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_CUSTOM_HOST_IP = "custom_host_ip"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_APP_THEME_MODE = "app_theme_mode"
        private const val KEY_PROJECT_STATUS_NOTIFICATIONS = "project_status_notifications"
        private const val KEY_RANKING_FILTER_TEMPLATES = "ranking_filter_templates_json"
    }
}

// Singleton для iOS
private val instance: AppPreferences by lazy { AppPreferencesImpl() }

actual fun createAppPreferences(): AppPreferences {
    return instance
}
