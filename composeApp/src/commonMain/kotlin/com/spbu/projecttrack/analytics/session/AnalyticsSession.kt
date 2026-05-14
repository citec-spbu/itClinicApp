package com.spbu.projecttrack.analytics.session

import com.spbu.projecttrack.core.storage.AppPreferences
import com.spbu.projecttrack.core.time.PlatformTime

/**
 * Управляет session_id и user_id для аналитики.
 * Сессия истекает через [sessionTimeoutMs] мс бездействия (по умолчанию 30 минут).
 */
class AnalyticsSession(private val prefs: AppPreferences) {

    companion object {
        private const val SESSION_TIMEOUT_MS = 30 * 60 * 1_000L
    }

    /** Текущий session_id. Создаётся автоматически при первом обращении. */
    val sessionId: String
        get() = prefs.getAnalyticsSessionId() ?: createNewSession()

    /** user_id берётся из токена авторизации через [refreshUserId]. */
    val userId: String?
        get() = prefs.getAnalyticsUserId()

    /** Постоянный анонимный ID устройства/установки для различения персон без деанонимизации. */
    val anonymousId: String
        get() = prefs.getAnalyticsAnonymousId() ?: createAnonymousId()

    /**
     * Вызывать при старте приложения и возврате из фона.
     * Если прошло больше [SESSION_TIMEOUT_MS] — создаёт новую сессию.
     */
    fun refreshSession() {
        val lastTs = prefs.getAnalyticsSessionTimestamp() ?: 0L
        val now = PlatformTime.currentTimeMillis()
        if (now - lastTs > SESSION_TIMEOUT_MS) {
            createNewSession()
        } else {
            prefs.saveAnalyticsSessionTimestamp(now)
        }
    }

    fun setUserId(userId: String?) {
        if (userId.isNullOrBlank()) {
            prefs.saveAnalyticsUserId(null)
        } else {
            prefs.saveAnalyticsUserId(userId)
        }
    }

    fun reset() {
        prefs.saveAnalyticsSessionId(null)
        prefs.saveAnalyticsUserId(null)
        prefs.saveAnalyticsSessionTimestamp(null)
    }

    private fun createNewSession(): String {
        val newId = generateUuid()
        val now = PlatformTime.currentTimeMillis()
        prefs.saveAnalyticsSessionId(newId)
        prefs.saveAnalyticsSessionTimestamp(now)
        return newId
    }

    private fun createAnonymousId(): String {
        val id = "anon_${generateUuid()}"
        prefs.saveAnalyticsAnonymousId(id)
        return id
    }
}

/** Генерация UUID — платформо-зависимая реализация. */
expect fun generateUuid(): String
