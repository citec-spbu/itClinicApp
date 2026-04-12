package com.spbu.projecttrack.rating.data.repository

import com.spbu.projecttrack.core.time.PlatformTime
import com.spbu.projecttrack.rating.data.model.MetricProjectDetail
import com.spbu.projecttrack.rating.data.model.MetricProjectMetric
import com.spbu.projecttrack.rating.data.model.MetricProjectResource
import com.spbu.projecttrack.rating.data.model.RankingFilters
import com.spbu.projecttrack.rating.data.model.RankingMetricKey
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal object RankingScoreEngine {
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    private const val METRIC_COMMITS = "Commits"
    private const val METRIC_ISSUES = "Issues"
    private const val METRIC_PULL_REQUESTS = "Pull Requests"

    fun calculateProjectScore(
        project: MetricProjectDetail,
        filters: RankingFilters,
        baseNowMillis: Long = PlatformTime.currentTimeMillis(),
    ): Double? {
        val contributionKeys = resolveContributionKeys(filters)
        if (contributionKeys.isEmpty()) return null

        val resourceScores = project.resources.mapNotNull { resource ->
            calculateResourceScore(
                resource = resource,
                filters = filters,
                contributionKeys = contributionKeys,
                selectedUsers = emptySet(),
                baseNowMillis = baseNowMillis,
            )
        }

        return average(resourceScores)
    }

    fun calculateUserScore(
        project: MetricProjectDetail,
        filters: RankingFilters,
        selectedUsers: Set<String>,
        baseNowMillis: Long = PlatformTime.currentTimeMillis(),
    ): Double? {
        if (selectedUsers.isEmpty()) return null

        val contributionKeys = resolveContributionKeys(filters)
        if (contributionKeys.isEmpty()) return null

        val normalizedUsers = selectedUsers.map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()
        if (normalizedUsers.isEmpty()) return null

        val resourceScores = project.resources.mapNotNull { resource ->
            calculateResourceScore(
                resource = resource,
                filters = filters,
                contributionKeys = contributionKeys,
                selectedUsers = normalizedUsers,
                baseNowMillis = baseNowMillis,
            )
        }

        return average(resourceScores)
    }

    private fun calculateResourceScore(
        resource: MetricProjectResource,
        filters: RankingFilters,
        contributionKeys: Set<ContributionKey>,
        selectedUsers: Set<String>,
        baseNowMillis: Long,
    ): Double? {
        if (resource.metrics.isEmpty()) return null

        val metricsByName = resource.metrics.associateBy { it.name }
        val commitsMetric = metricsByName[METRIC_COMMITS]
        val issuesMetric = metricsByName[METRIC_ISSUES]
        val pullRequestsMetric = metricsByName[METRIC_PULL_REQUESTS]

        val grades = contributionKeys.mapNotNull { contribution ->
            when (contribution) {
                ContributionKey.TotalCommits -> {
                    val commits = extractCommits(
                        metric = commitsMetric,
                        filters = filters,
                        selectedUsers = selectedUsers,
                        periodKeys = listOf(
                            RankingMetricKey.Commits,
                            RankingMetricKey.TotalCommits,
                        ),
                        baseNowMillis = baseNowMillis,
                    )
                    totalCommitsGrade(commits, selectedUsers.size)
                }

                ContributionKey.IssueCompleteness -> {
                    val issues = extractIssues(
                        metric = issuesMetric,
                        filters = filters,
                        selectedUsers = selectedUsers,
                        periodKeys = listOf(
                            RankingMetricKey.Issues,
                            RankingMetricKey.IssueCompleteness,
                        ),
                        baseNowMillis = baseNowMillis,
                    )
                    issueCompletenessGrade(issues)
                }

                ContributionKey.PullRequestHangTime -> {
                    val pullRequests = extractPullRequests(
                        metric = pullRequestsMetric,
                        filters = filters,
                        selectedUsers = selectedUsers,
                        periodKeys = listOf(
                            RankingMetricKey.PullRequests,
                            RankingMetricKey.PullRequestHangTime,
                        ),
                        baseNowMillis = baseNowMillis,
                    )
                    pullRequestHangTimeGrade(pullRequests)
                }

                ContributionKey.RapidPullRequests -> {
                    val pullRequests = extractPullRequests(
                        metric = pullRequestsMetric,
                        filters = filters,
                        selectedUsers = selectedUsers,
                        periodKeys = listOf(
                            RankingMetricKey.PullRequests,
                            RankingMetricKey.RapidPullRequests,
                        ),
                        baseNowMillis = baseNowMillis,
                    )
                    rapidPullRequestsGrade(pullRequests, filters.metric(RankingMetricKey.RapidPullRequests).thresholdPreset.minutes)
                }

                ContributionKey.CodeChurn -> {
                    val commits = extractCommits(
                        metric = commitsMetric,
                        filters = filters,
                        selectedUsers = selectedUsers,
                        periodKeys = listOf(RankingMetricKey.CodeChurn),
                        baseNowMillis = baseNowMillis,
                    )
                    codeChurnGrade(commits)
                }

                ContributionKey.CodeOwnership -> {
                    val commits = extractCommits(
                        metric = commitsMetric,
                        filters = filters,
                        selectedUsers = selectedUsers,
                        periodKeys = listOf(RankingMetricKey.CodeOwnership),
                        baseNowMillis = baseNowMillis,
                    )
                    codeOwnershipGrade(commits)
                }

                ContributionKey.DominantWeekDay -> {
                    val commits = extractCommits(
                        metric = commitsMetric,
                        filters = filters,
                        selectedUsers = selectedUsers,
                        periodKeys = listOf(RankingMetricKey.DominantWeekDay),
                        baseNowMillis = baseNowMillis,
                    )
                    val issues = extractIssues(
                        metric = issuesMetric,
                        filters = filters,
                        selectedUsers = selectedUsers,
                        periodKeys = listOf(RankingMetricKey.DominantWeekDay),
                        baseNowMillis = baseNowMillis,
                    )
                    val pullRequests = extractPullRequests(
                        metric = pullRequestsMetric,
                        filters = filters,
                        selectedUsers = selectedUsers,
                        periodKeys = listOf(RankingMetricKey.DominantWeekDay),
                        baseNowMillis = baseNowMillis,
                    )
                    dominantWeekDayGrade(
                        commits = commits,
                        issues = issues,
                        pullRequests = pullRequests,
                        unwantedDay = filters.metric(RankingMetricKey.DominantWeekDay).weekDay.backendValue,
                    )
                }
            }
        }

        return average(grades)
    }

    private fun resolveContributionKeys(filters: RankingFilters): Set<ContributionKey> {
        val enabledMetricKeys = filters.activeMetricKeys().filterNot { it == RankingMetricKey.PerformanceGrade }
        val mappedKeys = enabledMetricKeys.map(::mapMetricToContribution).toSet()
        if (mappedKeys.isNotEmpty()) return mappedKeys

        return if (filters.isEnabled(RankingMetricKey.PerformanceGrade)) {
            linkedSetOf(
                ContributionKey.TotalCommits,
                ContributionKey.IssueCompleteness,
                ContributionKey.PullRequestHangTime,
                ContributionKey.RapidPullRequests,
                ContributionKey.CodeOwnership,
                ContributionKey.DominantWeekDay,
            )
        } else {
            emptySet()
        }
    }

    private fun mapMetricToContribution(metric: RankingMetricKey): ContributionKey {
        return when (metric) {
            RankingMetricKey.Commits,
            RankingMetricKey.TotalCommits,
            -> ContributionKey.TotalCommits

            RankingMetricKey.Issues,
            RankingMetricKey.IssueCompleteness,
            -> ContributionKey.IssueCompleteness

            RankingMetricKey.PullRequests,
            RankingMetricKey.PullRequestHangTime,
            -> ContributionKey.PullRequestHangTime

            RankingMetricKey.RapidPullRequests -> ContributionKey.RapidPullRequests
            RankingMetricKey.CodeChurn -> ContributionKey.CodeChurn
            RankingMetricKey.CodeOwnership -> ContributionKey.CodeOwnership
            RankingMetricKey.DominantWeekDay -> ContributionKey.DominantWeekDay
            RankingMetricKey.PerformanceGrade -> ContributionKey.TotalCommits
        }
    }

    private fun extractCommits(
        metric: MetricProjectMetric?,
        filters: RankingFilters,
        selectedUsers: Set<String>,
        periodKeys: List<RankingMetricKey>,
        baseNowMillis: Long,
    ): List<CommitSnapshot> {
        val window = resolveWindow(filters, periodKeys, baseNowMillis)
        return metric.snapshotObjects().mapNotNull { snapshot ->
            val authorLogin = snapshot.objectOrNull("author")
                ?.string("login")
                ?.trim()
                ?.lowercase()
            if (selectedUsers.isNotEmpty() && authorLogin !in selectedUsers) {
                return@mapNotNull null
            }

            val authoredAt = snapshot.objectOrNull("commit")
                ?.objectOrNull("author")
                ?.string("date")
                ?.toInstantMillis()
            if (!window.matches(authoredAt)) return@mapNotNull null

            CommitSnapshot(
                authorLogin = authorLogin,
                authoredAt = authoredAt,
                files = snapshot.arrayOrNull("files").orEmpty().mapNotNull { file ->
                    val fileObject = file as? JsonObject ?: return@mapNotNull null
                    CommitFile(
                        filename = fileObject.string("filename"),
                        status = fileObject.string("status"),
                        additions = fileObject.number("additions") ?: 0,
                        deletions = fileObject.number("deletions") ?: 0,
                        changes = fileObject.number("changes") ?: 0,
                    )
                },
            )
        }
    }

    private fun extractIssues(
        metric: MetricProjectMetric?,
        filters: RankingFilters,
        selectedUsers: Set<String>,
        periodKeys: List<RankingMetricKey>,
        baseNowMillis: Long,
    ): List<IssueSnapshot> {
        val window = resolveWindow(filters, periodKeys, baseNowMillis)
        return metric.snapshotObjects().mapNotNull { snapshot ->
            val createdBy = snapshot.objectOrNull("user")
                ?.string("login")
                ?.trim()
                ?.lowercase()
            val assignees = snapshot.arrayOrNull("assignees")
                .orEmpty()
                .mapNotNull { item -> (item as? JsonObject)?.string("login")?.trim()?.lowercase() }
                .toSet()

            if (selectedUsers.isNotEmpty() && createdBy !in selectedUsers && assignees.none(selectedUsers::contains)) {
                return@mapNotNull null
            }

            val createdAt = snapshot.string("created_at")?.toInstantMillis()
            val closedAt = snapshot.string("closed_at")?.toInstantMillis()
            if (!window.matchesAny(createdAt, closedAt)) return@mapNotNull null

            IssueSnapshot(
                createdBy = createdBy,
                assignees = assignees,
                createdAt = createdAt,
                closedAt = closedAt,
            )
        }
    }

    private fun extractPullRequests(
        metric: MetricProjectMetric?,
        filters: RankingFilters,
        selectedUsers: Set<String>,
        periodKeys: List<RankingMetricKey>,
        baseNowMillis: Long,
    ): List<PullRequestSnapshot> {
        val window = resolveWindow(filters, periodKeys, baseNowMillis)
        return metric.snapshotObjects().mapNotNull { snapshot ->
            val authorLogin = snapshot.objectOrNull("user")
                ?.string("login")
                ?.trim()
                ?.lowercase()
            if (selectedUsers.isNotEmpty() && authorLogin !in selectedUsers) {
                return@mapNotNull null
            }

            val createdAt = snapshot.string("created_at")?.toInstantMillis()
            val closedAt = snapshot.string("closed_at")?.toInstantMillis()
            if (!window.matchesAny(createdAt, closedAt)) return@mapNotNull null

            PullRequestSnapshot(
                authorLogin = authorLogin,
                createdAt = createdAt,
                closedAt = closedAt,
            )
        }
    }

    private fun resolveWindow(
        filters: RankingFilters,
        periodKeys: List<RankingMetricKey>,
        baseNowMillis: Long,
    ): TimeWindow {
        val periodDays = periodKeys.firstNotNullOfOrNull { metricKey ->
            filters.metric(metricKey)
                .takeIf { filters.isEnabled(metricKey) && metricKey.supportsPeriod }
                ?.periodPreset
                ?.days
        } ?: filters.metric(RankingMetricKey.PerformanceGrade)
            .takeIf { filters.isEnabled(RankingMetricKey.PerformanceGrade) && RankingMetricKey.PerformanceGrade.supportsPeriod }
            ?.periodPreset
            ?.days

        val relativeStart = periodDays?.let { days ->
            baseNowMillis - (days.toLong() * DAY_MILLIS)
        }
        val absoluteStart = filters.dateRange.startMillis
        val absoluteEnd = filters.dateRange.endMillis

        val start = listOfNotNull(relativeStart, absoluteStart).maxOrNull()
        val end = listOfNotNull(absoluteEnd, baseNowMillis).minOrNull()

        return TimeWindow(
            startMillis = start,
            endMillis = end,
        )
    }

    private fun totalCommitsGrade(
        commits: List<CommitSnapshot>,
        selectedUserCount: Int,
    ): Double? {
        if (commits.isEmpty()) return null

        val dates = commits.mapNotNull { it.authoredAt }
        val start = dates.minOrNull() ?: return null
        val end = dates.maxOrNull() ?: return null
        val dayCount = (end - start).toDouble() / DAY_MILLIS.toDouble()
        if (dayCount <= 0.0) return 5.0

        val userCount = if (selectedUserCount > 0) {
            selectedUserCount
        } else {
            commits.mapNotNull { it.authorLogin }.toSet().size
        }
        if (userCount <= 0) return 5.0

        val commitsPerDay = commits.size / dayCount / userCount
        return round2(min(commitsPerDay * 9 + 2, 5.0))
    }

    private fun issueCompletenessGrade(
        issues: List<IssueSnapshot>,
    ): Double? {
        if (issues.isEmpty()) return null
        val closed = issues.count { it.closedAt != null }
        return round2((closed.toDouble() / issues.size) * 3 + 2)
    }

    private fun pullRequestHangTimeGrade(
        pullRequests: List<PullRequestSnapshot>,
    ): Double? {
        if (pullRequests.isEmpty()) return null

        val durations = pullRequests.mapNotNull { pullRequest ->
            val createdAt = pullRequest.createdAt ?: return@mapNotNull null
            val closedAt = pullRequest.closedAt ?: return@mapNotNull null
            (closedAt - createdAt).takeIf { it >= 0 }
        }
        if (durations.isEmpty()) return null

        val averageHangTime = durations.average()
        if (averageHangTime < 5 * 60 * 1000) {
            return round2(averageHangTime / (60 * 1000))
        }

        val grade = ((1 - averageHangTime / (7 * DAY_MILLIS)) * 3 + 2).coerceAtLeast(0.0)
        return round2(grade)
    }

    private fun rapidPullRequestsGrade(
        pullRequests: List<PullRequestSnapshot>,
        thresholdMinutes: Int,
    ): Double? {
        if (pullRequests.isEmpty()) return null

        val thresholdMillis = thresholdMinutes.toLong() * 60L * 1000L
        val rapidCount = pullRequests.count { pullRequest ->
            val createdAt = pullRequest.createdAt ?: return@count false
            val closedAt = pullRequest.closedAt ?: return@count false
            closedAt - createdAt < thresholdMillis
        }
        return round2((1 - rapidCount.toDouble() / pullRequests.size) * 3 + 2)
    }

    private fun codeChurnGrade(
        commits: List<CommitSnapshot>,
    ): Double? {
        if (commits.isEmpty()) return null

        val fileChanges = linkedMapOf<String, Int>()
        commits.forEach { commit ->
            commit.files.forEach { file ->
                val filename = file.filename?.trim().orEmpty()
                if (filename.isBlank()) return@forEach

                val changes = file.changes.takeIf { it > 0 }
                    ?: (file.additions + file.deletions).takeIf { it > 0 }
                    ?: return@forEach

                fileChanges[filename] = (fileChanges[filename] ?: 0) + changes
            }
        }
        if (fileChanges.isEmpty()) return null

        val churnPerCommit = fileChanges.values.sum().toDouble() / commits.size.toDouble()
        return round2((5.0 - ln(1.0 + churnPerCommit).coerceAtMost(4.0) * 1.2).coerceIn(0.0, 5.0))
    }

    private fun codeOwnershipGrade(
        commits: List<CommitSnapshot>,
    ): Double? {
        val userLines = linkedMapOf<String, Int>()
        var totalLines = 0

        commits.forEach { commit ->
            val login = commit.authorLogin ?: return@forEach
            commit.files.forEach { file ->
                val lineDelta = file.additions + file.deletions + file.changes
                if (lineDelta <= 0) return@forEach
                userLines[login] = (userLines[login] ?: 0) + lineDelta
                totalLines += lineDelta
            }
        }

        if (totalLines <= 0 || userLines.size < 2) return null

        val normalized = userLines.values.map { it.toDouble() / totalLines }
        val averageShare = 1 / userLines.size.toDouble()
        val worstCase = (1 - averageShare).pow(2) + (userLines.size - 1) * averageShare.pow(2)
        if (worstCase <= 0.0) return null

        val dispersion = normalized.sumOf { (it - averageShare).pow(2) }
        val gradeComponent = (1 - sqrt(dispersion / worstCase)) * 3
        return round2((gradeComponent + 2).coerceIn(0.0, 5.0))
    }

    private fun dominantWeekDayGrade(
        commits: List<CommitSnapshot>,
        issues: List<IssueSnapshot>,
        pullRequests: List<PullRequestSnapshot>,
        unwantedDay: String,
    ): Double? {
        val values = linkedMapOf(
            "Monday" to 0,
            "Tuesday" to 0,
            "Wednesday" to 0,
            "Thursday" to 0,
            "Friday" to 0,
            "Saturday" to 0,
            "Sunday" to 0,
        )

        commits.forEach { commit ->
            commit.authoredAt?.weekdayKey()?.let { day ->
                values[day] = (values[day] ?: 0) + 1
            }
        }
        issues.forEach { issue ->
            issue.createdAt?.weekdayKey()?.let { day ->
                values[day] = (values[day] ?: 0) + 1
            }
            issue.closedAt?.weekdayKey()?.let { day ->
                values[day] = (values[day] ?: 0) - 1
            }
        }
        pullRequests.forEach { request ->
            request.createdAt?.weekdayKey()?.let { day ->
                values[day] = (values[day] ?: 0) + 1
            }
            request.closedAt?.weekdayKey()?.let { day ->
                values[day] = (values[day] ?: 0) - 1
            }
        }

        if (unwantedDay !in values.keys) return null
        val currentDay = values[unwantedDay] ?: return null

        val averageActions = values.entries
            .filterNot { it.key == unwantedDay }
            .map { it.value }
            .average()

        if (averageActions <= 0.0) {
            return if (currentDay <= 0) 5.0 else 0.0
        }

        val ratio = currentDay / averageActions
        if (ratio < 1.0) return 5.0

        return round2(max(0.0, (-3.0 / 2.0) * ratio + 13.0 / 2.0))
    }

    private fun average(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        return round2(values.average())
    }

    private fun round2(value: Double): Double {
        return ((value * 100.0).roundToInt() / 100.0)
    }

    private fun MetricProjectMetric?.snapshotObjects(): List<JsonObject> {
        if (this == null) return emptyList()

        return data.mapNotNull { snapshot ->
            if (!snapshot.error.isNullOrBlank()) return@mapNotNull null
            snapshot.data as? JsonObject
        }
    }

    private fun JsonObject.objectOrNull(key: String): JsonObject? {
        return this[key] as? JsonObject
    }

    private fun JsonObject.arrayOrNull(key: String): JsonArray? {
        return this[key] as? JsonArray
    }

    private fun JsonObject.string(key: String): String? {
        return this[key]?.jsonPrimitive?.contentOrNull
    }

    private fun JsonObject.number(key: String): Int? {
        return this[key]?.jsonPrimitive?.doubleOrNull?.roundToInt()
    }

    private fun String.toInstantMillis(): Long? {
        return runCatching { kotlinx.datetime.Instant.parse(this).toEpochMilliseconds() }.getOrNull()
    }

    private fun Long.weekdayKey(): String {
        return kotlinx.datetime.Instant.fromEpochMilliseconds(this)
            .toLocalDateTime(TimeZone.UTC)
            .dayOfWeek
            .name
            .replaceFirstChar { it.uppercase() }
            .lowercase()
            .replaceFirstChar { it.uppercase() }
    }

    private data class TimeWindow(
        val startMillis: Long?,
        val endMillis: Long?,
    ) {
        fun matches(millis: Long?): Boolean {
            val value = millis ?: return false
            if (startMillis != null && value < startMillis) return false
            if (endMillis != null && value > endMillis) return false
            return true
        }

        fun matchesAny(first: Long?, second: Long?): Boolean {
            return matches(first) || matches(second)
        }
    }

    private enum class ContributionKey {
        TotalCommits,
        IssueCompleteness,
        PullRequestHangTime,
        RapidPullRequests,
        CodeChurn,
        CodeOwnership,
        DominantWeekDay,
    }

    private data class CommitSnapshot(
        val authorLogin: String?,
        val authoredAt: Long?,
        val files: List<CommitFile>,
    )

    private data class CommitFile(
        val filename: String?,
        val status: String?,
        val additions: Int,
        val deletions: Int,
        val changes: Int,
    )

    private data class IssueSnapshot(
        val createdBy: String?,
        val assignees: Set<String>,
        val createdAt: Long?,
        val closedAt: Long?,
    )

    private data class PullRequestSnapshot(
        val authorLogin: String?,
        val createdAt: Long?,
        val closedAt: Long?,
    )
}
