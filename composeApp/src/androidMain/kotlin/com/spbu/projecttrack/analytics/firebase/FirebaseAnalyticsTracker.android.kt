package com.spbu.projecttrack.analytics.firebase

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.spbu.projecttrack.AppContextHolder

internal actual object PlatformFirebaseAnalytics {
    actual fun logEvent(name: String, payload: FirebaseAnalyticsPayload) {
        analytics()?.logEvent(name, payload.toBundle())
    }

    actual fun setUserId(userId: String?) {
        analytics()?.setUserId(userId)
    }

    actual fun setUserProperty(name: String, value: String?) {
        analytics()?.setUserProperty(name, value)
    }

    actual fun resetAnalyticsData() {
        analytics()?.resetAnalyticsData()
    }

    private fun analytics(): FirebaseAnalytics? {
        val context = AppContextHolder.applicationContext ?: return null
        return FirebaseAnalytics.getInstance(context)
    }
}

private fun FirebaseAnalyticsPayload.toBundle(): Bundle = Bundle().apply {
    strings.forEach { (key, value) -> putString(key, value) }
    longs.forEach { (key, value) -> putLong(key, value) }
    doubles.forEach { (key, value) -> putDouble(key, value) }
}
