package com.spbu.projecttrack.core.storage

interface AppPreferences {
    fun isOnboardingCompleted(): Boolean
    fun setOnboardingCompleted()
    fun getAccessToken(): String?
    fun saveAccessToken(token: String)
    fun getRefreshToken(): String?
    fun saveRefreshToken(token: String)
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

    fun getRankingFilterTemplatesJson(): String?
    fun saveRankingFilterTemplatesJson(json: String)

    fun getProjectStatsScreenSettingsJson(): String?
    fun saveProjectStatsScreenSettingsJson(json: String)

    fun getUserStatsScreenSettingsJson(): String?
    fun saveUserStatsScreenSettingsJson(json: String)

    // Analytics session storage
    fun getAnalyticsSessionId(): String?
    fun saveAnalyticsSessionId(id: String?)
    fun getAnalyticsSessionTimestamp(): Long?
    fun saveAnalyticsSessionTimestamp(ts: Long?)
    fun getAnalyticsUserId(): String?
    fun saveAnalyticsUserId(userId: String?)
    fun getAnalyticsAnonymousId(): String?
    fun saveAnalyticsAnonymousId(id: String?)
}
expect fun createAppPreferences(): AppPreferences
