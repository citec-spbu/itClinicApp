package com.spbu.projecttrack.rating.data

import com.spbu.projecttrack.rating.presentation.settings.StatsScreenSection
import com.spbu.projecttrack.rating.presentation.settings.defaultStatsScreenSectionIds
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class StatsScreenSettingsFile(
    val activeSectionIds: List<String> = emptyList(),
)

private val statsScreenSettingsJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

object StatsScreenSettingsPersistence {
    fun encode(activeSectionIds: List<String>): String {
        val sanitizedIds = activeSectionIds
            .mapNotNull(StatsScreenSection::fromId)
            .map(StatsScreenSection::id)
            .distinct()
        return statsScreenSettingsJson.encodeToString(
            StatsScreenSettingsFile.serializer(),
            StatsScreenSettingsFile(activeSectionIds = sanitizedIds),
        )
    }

    fun decode(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return defaultStatsScreenSectionIds()
        return try {
            statsScreenSettingsJson.decodeFromString(
                StatsScreenSettingsFile.serializer(),
                raw,
            ).activeSectionIds
                .mapNotNull(StatsScreenSection::fromId)
                .map(StatsScreenSection::id)
                .distinct()
        } catch (_: Exception) {
            defaultStatsScreenSectionIds()
        }
    }
}
