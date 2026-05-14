package com.spbu.projecttrack.analytics.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import com.spbu.projecttrack.analytics.AnalyticsTracker
import com.spbu.projecttrack.analytics.NoOpAnalyticsTracker
import com.spbu.projecttrack.analytics.session.AnalyticsSession

val LocalAnalyticsTracker = staticCompositionLocalOf<AnalyticsTracker> {
    NoOpAnalyticsTracker()
}

val LocalAnalyticsSession = staticCompositionLocalOf<AnalyticsSession?> { null }

/**
 * Удобный контейнер для доступа к трекеру и сессии.
 * Вызывается один раз в composable через `remember {}` — не вызывает recomposition.
 */
data class AnalyticsContext(
    val tracker: AnalyticsTracker,
    val session: AnalyticsSession?,
) {
    val sessionId: String get() = session?.sessionId ?: "no_session"
    val userId: String?  get() = session?.userId ?: session?.anonymousId
}

@Composable
fun rememberAnalyticsContext(): AnalyticsContext {
    val tracker = LocalAnalyticsTracker.current
    val session = LocalAnalyticsSession.current
    return remember(tracker, session) { AnalyticsContext(tracker, session) }
}
