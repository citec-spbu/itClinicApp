package com.spbu.projecttrack.user.data.model

import com.spbu.projecttrack.core.time.PlatformTime

/** In-memory profile cache that lives for the app session and expires after 5 minutes. */
object UserProfileCache {
    private const val TTL_MS = 5 * 60 * 1000L

    private var cachedProfile: UserProfileResponse? = null
    private var cachedAt: Long = 0L

    fun get(): UserProfileResponse? {
        val profile = cachedProfile ?: return null
        val age = PlatformTime.currentTimeMillis() - cachedAt
        return if (age < TTL_MS) profile else null
    }

    fun set(profile: UserProfileResponse) {
        cachedProfile = profile
        cachedAt = PlatformTime.currentTimeMillis()
    }

    fun invalidate() {
        cachedProfile = null
        cachedAt = 0L
    }
}
