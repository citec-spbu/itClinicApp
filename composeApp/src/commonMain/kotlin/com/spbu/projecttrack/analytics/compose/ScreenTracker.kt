package com.spbu.projecttrack.analytics.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.spbu.projecttrack.analytics.events.ScreenEvents
import com.spbu.projecttrack.core.time.PlatformTime

/**
 * Отслеживает вход и выход с экрана.
 * Вызывать в корне каждого экрана один раз.
 *
 * Отправляет:
 * - screen_viewed — при входе
 * - screen_left   — при выходе (с временем на экране)
 */
@Composable
fun TrackScreen(
    screenName: String,
    analyticsContext: AnalyticsContext,
    referrer: String? = null,
) {
    val enterTime = remember { PlatformTime.currentTimeMillis() }

    LaunchedEffect(screenName) {
        analyticsContext.tracker.track(
            ScreenEvents.viewed(
                screenName = screenName,
                sessionId = analyticsContext.sessionId,
                userId = analyticsContext.userId,
                referrer = referrer,
            )
        )
    }

    DisposableEffect(screenName) {
        onDispose {
            val timeOnScreen = PlatformTime.currentTimeMillis() - enterTime
            analyticsContext.tracker.track(
                ScreenEvents.left(
                    screenName = screenName,
                    timeOnScreenMs = timeOnScreen,
                    sessionId = analyticsContext.sessionId,
                    userId = analyticsContext.userId,
                )
            )
        }
    }
}
