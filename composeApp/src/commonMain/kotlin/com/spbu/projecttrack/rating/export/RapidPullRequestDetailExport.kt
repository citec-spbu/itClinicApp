package com.spbu.projecttrack.rating.export

import com.spbu.projecttrack.core.settings.localizePluralRuntime
import com.spbu.projecttrack.core.settings.localizeRuntime
import com.spbu.projecttrack.rating.common.StatsExportCopy
import com.spbu.projecttrack.rating.common.formatDurationMinutesLabel
import com.spbu.projecttrack.rating.data.model.ProjectStatsThresholdUi
import com.spbu.projecttrack.rating.data.model.StatsDetailDataUi
import com.spbu.projecttrack.rating.data.model.StatsDetailParticipantUi
import com.spbu.projecttrack.rating.data.model.StatsDetailPullRequestUi
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt

internal data class RapidPullRequestDetailExportContent(
    val summaryCards: List<ProjectStatsSummaryCard>,
    val sections: List<ProjectStatsSection>,
)

internal fun buildRapidPullRequestDetailExportContent(
    details: StatsDetailDataUi,
    participantId: String?,
    rapidThreshold: ProjectStatsThresholdUi,
    overallRank: Int?,
    overallScore: Double?,
): RapidPullRequestDetailExportContent {
    val filteredPullRequests = filterPullRequests(details, participantId)
    val rapidPullRequests = filteredPullRequests.filter { isRapidPullRequest(it, rapidThreshold.totalMinutes) }
    val sortedRapidPullRequests = sortPullRequestsByNewest(rapidPullRequests)
    val participantSnapshots = buildParticipantSnapshots(details, rapidThreshold.totalMinutes)
    val selectedSnapshot = participantId?.let(participantSnapshots::get)
    val rankCaption = if (participantId == null) {
        StatsExportCopy.rankInRating()
    } else {
        StatsExportCopy.rankInTeam()
    }
    val leader = participantSnapshots.values
        .filter { it.rapidPullRequestCount > 0 }
        .maxByOrNull { it.rapidPullRequestCount }
    val score = if (participantId == null) {
        overallScore ?: rapidPullScore(filteredPullRequests, rapidThreshold.totalMinutes)
    } else {
        selectedSnapshot?.rapidPullScore
    }
    val rank = if (participantId == null) {
        overallRank
    } else {
        resolveRank(
            specificScore = selectedSnapshot?.rapidPullScore,
            peerScores = participantSnapshots
                .filterKeys { it != participantId }
                .values
                .map { it.rapidPullScore },
        )
    }
    val share = percentLabel(rapidPullRequests.size, filteredPullRequests.size)
    val contributorRows = participantSnapshots.values
        .sortedByDescending { it.rapidPullRequestCount }
        .distinctBy { it.participant.name.trim().lowercase() }
        .map { snapshot -> listOf(snapshot.participant.name, snapshot.rapidPullRequestCount.toString()) }
    val chartPoints = buildRapidPullRequestChartPoints(rapidPullRequests.mapNotNull { it.closedAtIso })
    val thresholdLabel = formatRapidThreshold(rapidThreshold)
    val summaryCards = listOf(
        ProjectStatsSummaryCard(
            StatsExportCopy.rapidPrShort(),
            rapidPullRequests.size.toString(),
            StatsExportCopy.total(),
        ),
        ProjectStatsSummaryCard(rankCaption.replaceFirstChar { it.uppercase() }, rank?.toString() ?: "—"),
        ProjectStatsSummaryCard(
            localizeRuntime("Доля быстрых PR", "Rapid PR share"),
            share,
            localizeRuntime(
                "из ${filteredPullRequests.size} PR",
                "of ${filteredPullRequests.size} PRs",
            ),
        ),
        ProjectStatsSummaryCard(
            localizeRuntime("Порог быстроты", "Rapid threshold"),
            thresholdLabel,
        ),
    )

    val sections = buildList {
        add(
            ProjectStatsSection(
                title = localizeRuntime("Быстрые PR", "Rapid PRs"),
                score = score,
                rows = listOf(
                    ProjectStatsTableRow(
                        label = localizeRuntime("Всего быстрых PR", "Total rapid PRs"),
                        value = rapidPullRequests.size.toString(),
                        note = localizeRuntime(
                            "${rapidPullRequests.size} из ${filteredPullRequests.size} Pull Request",
                            "${rapidPullRequests.size} of ${filteredPullRequests.size} pull requests",
                        ),
                    ),
                    ProjectStatsTableRow(
                        label = rankCaption.replaceFirstChar { it.uppercase() },
                        value = rank?.toString() ?: "—",
                    ),
                    ProjectStatsTableRow(
                        label = localizeRuntime("Доля быстрых PR", "Rapid PR share"),
                        value = share,
                        note = localizeRuntime(
                            "${rapidPullRequests.size} из ${filteredPullRequests.size}",
                            "${rapidPullRequests.size} of ${filteredPullRequests.size}",
                        ),
                    ),
                    ProjectStatsTableRow(
                        label = localizeRuntime("Лидер по быстрым PR", "Rapid PR leader"),
                        value = leader?.participant?.name ?: "—",
                        note = leader?.rapidPullRequestCount?.let { count ->
                            localizeRuntime(
                                "$count быстрых PR",
                                "$count rapid ${if (count == 1) "PR" else "PRs"}",
                            )
                        },
                    ),
                    ProjectStatsTableRow(
                        label = localizeRuntime("Порог быстроты", "Rapid threshold"),
                        value = thresholdLabel,
                    ),
                    ProjectStatsTableRow(
                        label = localizeRuntime("Оценка быстрых PR", "Rapid PR score"),
                        value = score?.let(::formatScoreValue) ?: "—",
                    ),
                ),
                table = ProjectStatsTable(
                    title = localizeRuntime(
                        "Количество быстрых Pull Request",
                        "Rapid pull request count",
                    ),
                    headers = listOf(
                        localizeRuntime("Участник", "Participant"),
                        localizeRuntime("Количество", "Count"),
                    ),
                    rows = contributorRows.ifEmpty {
                        listOf(listOf(StatsExportCopy.noData(), "—"))
                    },
                    columnFractions = listOf(0.72f, 0.28f),
                ),
                chart = chartPoints.takeIf { it.isNotEmpty() }?.let { points ->
                    val chartTitle = localizeRuntime("График быстрых PR", "Rapid PR chart")
                    ProjectStatsChart.Bar(
                        title = chartTitle,
                        points = points,
                    )
                },
            )
        )
        add(
            ProjectStatsSection(
                title = localizeRuntime("Список быстрых Pull Requests", "Rapid pull request list"),
                table = ProjectStatsTable(
                    headers = listOf(
                        "PR",
                        localizeRuntime("Статус", "Status"),
                        localizeRuntime("Закрыт за", "Closed in"),
                        localizeRuntime("Автор", "Author"),
                        localizeRuntime("Назначенные", "Assignees"),
                        localizeRuntime("Создано / закрыто", "Created / closed"),
                        localizeRuntime("Комментарии", "Comments"),
                        localizeRuntime("Ссылка", "Link"),
                    ),
                    rows = sortedRapidPullRequests
                        .map(::rapidPullRequestTableRow)
                        .ifEmpty { listOf(listOf(StatsExportCopy.noData(), "—", "", "", "", "", "", "")) },
                    columnFractions = listOf(0.20f, 0.08f, 0.09f, 0.10f, 0.11f, 0.24f, 0.08f, 0.10f),
                    pdfStyle = ProjectStatsTablePdfStyle.RapidPullRequestList,
                ),
            )
        )
    }

    return RapidPullRequestDetailExportContent(
        summaryCards = summaryCards,
        sections = sections,
    )
}

private data class RapidPullRequestParticipantSnapshot(
    val participant: StatsDetailParticipantUi,
    val rapidPullRequestCount: Int,
    val rapidPullScore: Double?,
)

private fun buildParticipantSnapshots(
    details: StatsDetailDataUi,
    rapidThresholdMinutes: Int,
): Map<String, RapidPullRequestParticipantSnapshot> {
    val participants = linkedMapOf<String, StatsDetailParticipantUi>()
    details.participants.forEach { participants[it.id] = it }
    details.pullRequests.forEach { pullRequest ->
        pullRequest.authorId?.let { id ->
            if (!participants.containsKey(id)) {
                participants[id] = StatsDetailParticipantUi(
                    id = id,
                    name = pullRequest.authorName,
                    subtitle = StatsExportCopy.participant(),
                )
            }
        }
    }
    return participants.mapValues { (_, participant) ->
        val userPullRequests = details.pullRequests.filter { it.authorId == participant.id }
        RapidPullRequestParticipantSnapshot(
            participant = participant,
            rapidPullRequestCount = userPullRequests.count {
                isRapidPullRequest(it, rapidThresholdMinutes)
            },
            rapidPullScore = rapidPullScore(userPullRequests, rapidThresholdMinutes),
        )
    }
}

private fun rapidPullRequestTableRow(pullRequest: StatsDetailPullRequestUi): List<String> {
    val duration = durationMinutes(pullRequest.createdAtIso, pullRequest.effectiveEndAtIso)
    return listOf(
        buildString {
            pullRequest.number?.let { append("#$it ") }
            append(pullRequest.title)
        },
        pullRequest.state?.uppercase().orEmpty(),
        duration?.let(::formatDurationMinutesLabel).orEmpty(),
        pullRequest.authorName,
        pullRequest.assigneeNames.joinToString(separator = "\n"),
        buildString {
            append(pullRequest.createdAtLabel)
            pullRequest.closedAtLabel?.takeIf { it.isNotBlank() }?.let {
                append('\n')
                append(it)
            }
        },
        pullRequest.comments?.toString().orEmpty(),
        pullRequest.url?.takeIf { it.isNotBlank() }?.let { url ->
            "@LINK:$url|${localizeRuntime("Ссылка", "Link")}"
        }.orEmpty(),
    )
}

private fun buildRapidPullRequestChartPoints(
    dates: List<String>,
): List<ProjectStatsChartPoint> {
    if (dates.isEmpty()) return emptyList()
    data class ChartEntry(
        val label: String,
        val epochMs: Long,
        var count: Int,
    )

    val grouped = linkedMapOf<String, ChartEntry>()
    dates.mapNotNull(::parseInstant).forEach { instant ->
        val date = instant.toLocalDateTime(TimeZone.UTC).date
        val label = formatDate(date.dayOfMonth, date.monthNumber, date.year)
        val existing = grouped[label]
        if (existing == null) {
            grouped[label] = ChartEntry(
                label = label,
                epochMs = instant.toEpochMilliseconds(),
                count = 1,
            )
        } else {
            existing.count += 1
        }
    }

    return grouped.values
        .sortedBy { it.epochMs }
        .map { entry ->
            ProjectStatsChartPoint(
                label = entry.label,
                value = entry.count.toDouble(),
                note = localizeRuntime(
                    "${entry.count} быстрых PR",
                    "${entry.count} rapid ${if (entry.count == 1) "PR" else "PRs"}",
                ),
            )
        }
}

private fun sortPullRequestsByNewest(
    pullRequests: List<StatsDetailPullRequestUi>,
): List<StatsDetailPullRequestUi> {
    return pullRequests
        .distinctBy(::pullRequestStableKey)
        .sortedByDescending { parseInstant(it.createdAtIso)?.toEpochMilliseconds() ?: Long.MIN_VALUE }
}

private fun pullRequestStableKey(
    pullRequest: StatsDetailPullRequestUi,
): String {
    val number = pullRequest.number
    if (number != null) return "number:$number"
    val url = pullRequest.url?.trim()?.lowercase()
    if (!url.isNullOrBlank()) return "url:$url"
    return buildString {
        append(pullRequest.authorId ?: pullRequest.authorName.lowercase())
        append(':')
        append(pullRequest.title.trim().lowercase())
        append(':')
        append(pullRequest.createdAtIso.orEmpty())
    }
}

private fun filterPullRequests(
    details: StatsDetailDataUi,
    participantId: String?,
): List<StatsDetailPullRequestUi> {
    if (participantId == null) return details.pullRequests
    return details.pullRequests.filter { it.authorId == participantId }
}

private fun resolveRank(
    specificScore: Double?,
    peerScores: List<Double?>,
): Int? {
    if (specificScore == null) return null
    val ranked = (peerScores + specificScore).filterNotNull().sortedDescending()
    return ranked.indexOfFirst { it == specificScore }
        .takeIf { it >= 0 }
        ?.plus(1)
}

private fun rapidPullScore(
    pullRequests: List<StatsDetailPullRequestUi>,
    thresholdMinutes: Int,
): Double? {
    if (pullRequests.isEmpty()) return null
    val rapidCount = pullRequests.count { isRapidPullRequest(it, thresholdMinutes) }
    return round2((1 - rapidCount.toDouble() / pullRequests.size) * 3 + 2)
}

private fun isRapidPullRequest(
    pullRequest: StatsDetailPullRequestUi,
    thresholdMinutes: Int,
): Boolean {
    val duration = durationMinutes(pullRequest.createdAtIso, pullRequest.effectiveEndAtIso) ?: return false
    return duration < thresholdMinutes
}

private fun durationMinutes(
    startIso: String?,
    endIso: String?,
): Int? {
    val start = parseInstant(startIso)?.toEpochMilliseconds() ?: return null
    val end = parseInstant(endIso)?.toEpochMilliseconds() ?: return null
    if (end <= start) return null
    val minutes = ((end - start) / MILLIS_PER_MINUTE).toInt()
    return maxOf(1, minutes)
}

private fun percentLabel(
    value: Int,
    total: Int,
): String {
    if (total <= 0) return "0%"
    return "${((value.toDouble() / total.toDouble()) * 100.0).roundToInt()}%"
}

private fun formatRapidThreshold(threshold: ProjectStatsThresholdUi): String {
    val parts = buildList {
        if (threshold.days > 0) {
            val d = threshold.days
            add(
                "$d ${
                    localizePluralRuntime(
                        d,
                        "день",
                        "дня",
                        "дней",
                        "day",
                        "days",
                    )
                }",
            )
        }
        if (threshold.hours > 0) {
            val h = threshold.hours
            add(
                "$h ${
                    localizePluralRuntime(
                        h,
                        "час",
                        "часа",
                        "часов",
                        "hour",
                        "hours",
                    )
                }",
            )
        }
        if (threshold.minutes > 0) {
            val m = threshold.minutes
            add(
                "$m ${
                    localizePluralRuntime(
                        m,
                        "минута",
                        "минуты",
                        "минут",
                        "minute",
                        "minutes",
                    )
                }",
            )
        }
    }
    return parts.joinToString(" ").ifBlank {
        val t = threshold.totalMinutes
        "$t ${
            localizePluralRuntime(
                t,
                "минута",
                "минуты",
                "минут",
                "minute",
                "minutes",
            )
        }"
    }
}

private fun formatScoreValue(score: Double): String {
    val rounded = (score * 10.0).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString().replace('.', ',')
    }
}

private fun formatDate(
    dayOfMonth: Int,
    monthNumber: Int,
    year: Int,
): String {
    val day = dayOfMonth.toString().padStart(2, '0')
    val month = monthNumber.toString().padStart(2, '0')
    val shortYear = (year % 100).toString().padStart(2, '0')
    return "$day.$month.$shortYear"
}

private fun parseInstant(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return runCatching { Instant.parse(value) }.getOrNull()
}

private fun round2(value: Double): Double = ((value * 100.0).roundToInt() / 100.0)

private const val MILLIS_PER_MINUTE = 60_000L
