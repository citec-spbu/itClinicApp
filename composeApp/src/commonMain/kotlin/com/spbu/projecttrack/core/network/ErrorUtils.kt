package com.spbu.projecttrack.core.network

import com.spbu.projecttrack.core.settings.localizeRuntime

/**
 * Converts a throwable to a short, user-facing error message.
 *
 * HTTP errors (already formatted as "HTTP <code> <description>" by the API layer)
 * are kept as-is — they are concise and informative (e.g. "HTTP 404 Not Found").
 *
 * All other exceptions (network failures, serialization errors, etc.) produce
 * long technical strings that are not suitable for the UI, so they are replaced
 * by the provided localized fallback text.
 */
fun Throwable?.toShortMessage(fallbackRu: String, fallbackEn: String): String {
    val msg = this?.message ?: return localizeRuntime(fallbackRu, fallbackEn)
    return if (msg.startsWith("HTTP ")) msg
    else localizeRuntime(fallbackRu, fallbackEn)
}
