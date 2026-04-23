package com.spbu.projecttrack.rating.common

import kotlin.math.roundToInt

private const val MILLIS_PER_SECOND = 1_000.0
private const val MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND
private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR

fun formatDurationMillisLabel(milliseconds: Double?): String? {
    if (milliseconds == null || milliseconds <= 0.0) return null
    if (milliseconds < MILLIS_PER_MINUTE) {
        val seconds = (milliseconds / MILLIS_PER_SECOND).roundToInt().coerceIn(1, 59)
        return "$seconds сек"
    }

    val minutes = (milliseconds / MILLIS_PER_MINUTE).roundToInt()
    return formatDurationMinutesLabel(minutes)
}

fun formatDurationMinutesLabel(minutes: Int?): String {
    if (minutes == null) return "—"
    if (minutes < MINUTES_PER_HOUR) return "$minutes мин"
    if (minutes < MINUTES_PER_DAY) {
        val hours = minutes / MINUTES_PER_HOUR
        val restMinutes = minutes % MINUTES_PER_HOUR
        return if (restMinutes == 0) "$hours ч" else "$hours ч $restMinutes мин"
    }

    val days = minutes / MINUTES_PER_DAY
    val restHours = (minutes % MINUTES_PER_DAY) / MINUTES_PER_HOUR
    val dayWord = pluralizeRussian(days, "день", "дня", "дней")
    return if (restHours == 0) "$days $dayWord" else "$days $dayWord $restHours ч"
}

private fun pluralizeRussian(
    count: Int,
    one: String,
    few: String,
    many: String,
): String {
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..14 -> many
        mod10 == 1 -> one
        mod10 in 2..4 -> few
        else -> many
    }
}
