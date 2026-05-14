package com.spbu.projecttrack.analytics

internal fun stableAnalyticsHash(value: String): String {
    var hash = 1125899906842597L
    for (char in value) {
        hash = 31L * hash + char.code.toLong()
    }
    return hash.toULong().toString(16)
}

internal fun analyticsUserId(rawUserId: String): String = "usr_${stableAnalyticsHash(rawUserId)}"

internal fun analyticsHashOrNull(value: String?): String? {
    val normalized = value?.trim().orEmpty()
    return normalized.takeIf { it.isNotEmpty() }?.let(::stableAnalyticsHash)
}
