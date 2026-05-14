package com.spbu.projecttrack

/**
 * Шаблон для локального `AnalyticsConfig.kt`.
 *
 * Скопируй в `AnalyticsConfig.kt`, вставь ключ из PostHog → Settings → Project API Key.
 * Файл AnalyticsConfig.kt НЕ коммитится (добавлен в .gitignore).
 *
 * Для локальной разработки достаточно иметь в local.properties:
 *   posthog_api_key=phc_xxxxxx
 * Тогда build.gradle.kts создаст AnalyticsConfig.kt автоматически при первой сборке.
 */
object AnalyticsConfigExample {
    const val POSTHOG_API_KEY = "phc_your_key_here"

    /**
     * EU регион: https://eu.i.posthog.com
     * US регион: https://us.i.posthog.com
     */
    const val POSTHOG_HOST = "https://eu.i.posthog.com"

    /** Отключить отправку событий полностью (например, для debug-сборок) */
    const val ANALYTICS_ENABLED = true
}
