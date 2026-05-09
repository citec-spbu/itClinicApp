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

    /** JSON: templates list + selectedTemplateId (ranking filters screen). */
    fun getRankingFilterTemplatesJson(): String?
    fun saveRankingFilterTemplatesJson(json: String)

    /** JSON: active section ids for project statistics screen settings. */
    fun getProjectStatsScreenSettingsJson(): String?
    fun saveProjectStatsScreenSettingsJson(json: String)

    /** JSON: active section ids for personal statistics screen settings. */
    fun getUserStatsScreenSettingsJson(): String?
    fun saveUserStatsScreenSettingsJson(json: String)
}

// Factory function для создания платформо-специфичной реализации
expect fun createAppPreferences(): AppPreferences
