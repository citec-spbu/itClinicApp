package com.spbu.projecttrack.analytics.firebase

interface NativeFirebaseAnalyticsSink {
    fun logEvent(name: String, payloadJson: String)
    fun setUserId(userId: String?)
    fun setUserProperty(name: String, value: String?)
    fun resetAnalyticsData()
}

private var firebaseAnalyticsSink: NativeFirebaseAnalyticsSink? = null

fun registerFirebaseAnalyticsSink(sink: NativeFirebaseAnalyticsSink?) {
    firebaseAnalyticsSink = sink
}

internal actual object PlatformFirebaseAnalytics {
    actual fun logEvent(name: String, payload: FirebaseAnalyticsPayload) {
        firebaseAnalyticsSink?.logEvent(name = name, payloadJson = payload.toJson())
    }

    actual fun setUserId(userId: String?) {
        firebaseAnalyticsSink?.setUserId(userId)
    }

    actual fun setUserProperty(name: String, value: String?) {
        firebaseAnalyticsSink?.setUserProperty(name, value)
    }

    actual fun resetAnalyticsData() {
        firebaseAnalyticsSink?.resetAnalyticsData()
    }
}
