package com.spbu.projecttrack.rating.common

import com.spbu.projecttrack.core.settings.localizeRuntime
import kotlin.math.roundToInt

private const val MILLIS_PER_SECOND = 1_000.0
private const val MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND
private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR

fun formatDurationMillisLabel(milliseconds: Double?): String? {
    if (milliseconds == null || milliseconds <= 0.0) return null
    if (milliseconds < MILLIS_PER_MINUTE) {
        val seconds = (milliseconds / MILLIS_PER_SECOND).roundToInt().coerceIn(1, 59)
        val unit = localizeRuntime("сек", "s")
        return "$seconds $unit"
    }

    val minutes = (milliseconds / MILLIS_PER_MINUTE).roundToInt()
    return formatDurationMinutesLabel(minutes)
}

fun formatDurationMinutesLabel(minutes: Int?): String {
    if (minutes == null) return "—"
    val minUnit = localizeRuntime("мин", "min")
    if (minutes < MINUTES_PER_HOUR) return "$minutes $minUnit"
    val hUnit = localizeRuntime("ч", "h")
    if (minutes < MINUTES_PER_DAY) {
        val hours = minutes / MINUTES_PER_HOUR
        val restMinutes = minutes % MINUTES_PER_HOUR
        return if (restMinutes == 0) "$hours $hUnit" else "$hours $hUnit $restMinutes $minUnit"
    }

    val days = minutes / MINUTES_PER_DAY
    val restHours = (minutes % MINUTES_PER_DAY) / MINUTES_PER_HOUR
    val dayWord = localizeRuntime(
        pluralizeRussian(days, "день", "дня", "дней"),
        if (days == 1) "day" else "days",
    )
    return if (restHours == 0) "$days $dayWord" else "$days $dayWord $restHours $hUnit"
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
