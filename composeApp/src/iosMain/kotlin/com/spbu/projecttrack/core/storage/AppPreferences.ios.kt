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
    
    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_CUSTOM_HOST_IP = "custom_host_ip"
    }
}

// Singleton для iOS
private val instance: AppPreferences by lazy { AppPreferencesImpl() }

actual fun createAppPreferences(): AppPreferences {
    return instance
}
