package com.spbu.projecttrack.rating.data

import com.spbu.projecttrack.rating.data.model.RankingDateRangeFilter
import com.spbu.projecttrack.rating.data.model.RankingFilterTemplate
import com.spbu.projecttrack.rating.data.model.RankingFilters
import com.spbu.projecttrack.rating.data.model.RankingMetricFilter
import com.spbu.projecttrack.rating.data.model.RankingMetricKey
import com.spbu.projecttrack.rating.data.model.RankingPeriodPreset
import com.spbu.projecttrack.rating.data.model.RankingThresholdPreset
import com.spbu.projecttrack.rating.data.model.RankingWeekDay
import com.spbu.projecttrack.rating.data.model.rankingDefaultFilters
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class RankingFilterTemplatesFile(
    val templates: List<RankingFilterTemplateDto> = emptyList(),
    val selectedTemplateId: String = "none",
)

@Serializable
internal data class RankingFilterTemplateDto(
    val id: String,
    val title: String,
    val filters: RankingFiltersDto? = null,
)

@Serializable
internal data class RankingFiltersDto(
    val metrics: Map<String, RankingMetricFilterDto> = emptyMap(),
    val dateRange: RankingDateRangeDto? = null,
)

@Serializable
internal data class RankingMetricFilterDto(
    val enabled: Boolean = false,
    val periodPreset: String = RankingPeriodPreset.TwoWeeks.name,
    val thresholdPreset: String = RankingThresholdPreset.TwoAndHalfHours.name,
    val weekDay: String = RankingWeekDay.Thursday.name,
)

@Serializable
internal data class RankingDateRangeDto(
    val startMillis: Long? = null,
    val endMillis: Long? = null,
)

private val persistenceJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private fun RankingMetricFilter.toDto(): RankingMetricFilterDto =
    RankingMetricFilterDto(
        enabled = enabled,
        periodPreset = periodPreset.name,
        thresholdPreset = thresholdPreset.name,
        weekDay = weekDay.name,
    )

private fun RankingMetricFilterDto.toModel(): RankingMetricFilter =
    RankingMetricFilter(
        enabled = enabled,
        periodPreset = enumValueOrDefault(periodPreset, RankingPeriodPreset.TwoWeeks),
        thresholdPreset = enumValueOrDefault(thresholdPreset, RankingThresholdPreset.TwoAndHalfHours),
        weekDay = enumValueOrDefault(weekDay, RankingWeekDay.Thursday),
    )

private inline fun <reified E : Enum<E>> enumValueOrDefault(name: String, default: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: default

private fun RankingFilters.toDto(): RankingFiltersDto {
    val map = metrics.mapKeys { it.key.name }.mapValues { it.value.toDto() }
    return RankingFiltersDto(
        metrics = map,
        dateRange = RankingDateRangeDto(
            startMillis = dateRange.startMillis,
            endMillis = dateRange.endMillis,
        ),
    )
}

private fun RankingFiltersDto.toModel(): RankingFilters {
    val base = rankingDefaultFilters()
    val mergedMetrics = RankingMetricKey.entries.associateWith { key ->
        metrics[key.name]?.toModel() ?: base.metric(key)
    }
    val dr = dateRange
    return RankingFilters(
        metrics = mergedMetrics,
        dateRange = RankingDateRangeFilter(
            startMillis = dr?.startMillis,
            endMillis = dr?.endMillis,
        ),
    )
}

private fun RankingFilterTemplate.toDto(): RankingFilterTemplateDto =
    RankingFilterTemplateDto(
        id = id,
        title = title,
        filters = filters?.toDto(),
    )

private fun RankingFilterTemplateDto.toModel(): RankingFilterTemplate? {
    if (id.isBlank() || title.isBlank()) return null
    return RankingFilterTemplate(
        id = id,
        title = title,
        filters = filters?.toModel(),
        isBuiltIn = false,
    )
}

object RankingFilterPersistence {
    fun encode(templates: List<RankingFilterTemplate>, selectedTemplateId: String): String {
        val file = RankingFilterTemplatesFile(
            templates = templates.map { it.toDto() },
            selectedTemplateId = selectedTemplateId,
        )
        return persistenceJson.encodeToString(
            RankingFilterTemplatesFile.serializer(),
            file,
        )
    }

    fun decode(raw: String?): Pair<List<RankingFilterTemplate>, String> {
        if (raw.isNullOrBlank()) return emptyList<RankingFilterTemplate>() to "none"
        return try {
            val file = persistenceJson.decodeFromString(
                RankingFilterTemplatesFile.serializer(),
                raw,
            )
            val list = file.templates.mapNotNull { it.toModel() }
            val sid = file.selectedTemplateId.ifBlank { "none" }
            list to sid
        } catch (_: Exception) {
            emptyList<RankingFilterTemplate>() to "none"
        }
    }
}
