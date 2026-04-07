package com.spbu.projecttrack.rating.data.repository

import com.spbu.projecttrack.core.time.PlatformTime
import com.spbu.projecttrack.rating.data.model.MetricProjectDetail
import com.spbu.projecttrack.rating.data.model.MetricProjectMetric
import com.spbu.projecttrack.rating.data.model.MetricProjectMetricParam
import com.spbu.projecttrack.rating.data.model.MetricProjectResource
import com.spbu.projecttrack.rating.data.model.MetricRankingItem
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal data class ProjectRankingMovement(
    val previousPosition: Int,
    val positionDelta: Int
)

internal object ProjectRankingHistoryHack {
    private const val WeekMillis = 7L * 24 * 60 * 60 * 1000
    private val WeekDays = listOf(
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday",
        "Sunday"
    )

    private const val MetricCommits = "Commits"
    private const val MetricIssues = "Issues"
    private const val MetricPullRequests = "Pull Requests"
    private const val MetricTotalCommits = "Total Commits"
    private const val MetricIssueCompleteness = "Issue Completeness"
    private const val MetricPullRequestHangTime = "Pull Request Hang Time"
    private const val MetricRapidPullRequests = "Rapid Pull Requests"
    private const val MetricCodeOwnership = "Code Ownership"
    private const val MetricDominantWeekDay = "Dominant Week Day"

    private const val ParamIsGraded = "isGraded"
    private const val ParamGradeWeight = "gradeWeight"
    private const val ParamRapidPullRequestsThreshold = "rapidPullRequestsThreshold"
    private const val ParamUnwantedWeekDay = "unwantedWeekDay"

    fun buildWeeklyMovement(
        currentRatings: List<MetricRankingItem>,
        details: List<MetricProjectDetail?>,
        nowMillis: Long = PlatformTime.currentTimeMillis()
    ): Map<String, ProjectRankingMovement> {
        if (currentRatings.isEmpty()) return emptyMap()

        val detailById = details.filterNotNull().associateBy { it.id }
        val cutoffMillis = nowMillis - WeekMillis
        val currentPositions = currentRatings.mapIndexed { index, item ->
            item.id to (index + 1)
        }.toMap()

        val historicalScores = currentRatings.mapNotNull { item ->
            val detail = detailById[item.id] ?: return@mapNotNull null
            val score = calculateProjectScore(detail, cutoffMillis) ?: return@mapNotNull null
            HistoricalProjectScore(
                id = item.id,
                name = item.name,
                score = score
            )
        }

        val previousPositions = historicalScores
            .sortedWith(
                compareByDescending<HistoricalProjectScore> { it.score }
                    .thenBy { it.name.lowercase() }
            )
            .mapIndexed { index, item ->
                item.id to (index + 1)
            }
            .toMap()

        return currentPositions.mapNotNull { (id, currentPosition) ->
            val previousPosition = previousPositions[id] ?: return@mapNotNull null
            id to ProjectRankingMovement(
                previousPosition = previousPosition,
                positionDelta = previousPosition - currentPosition
            )
        }.toMap()
    }

    private fun calculateProjectScore(
        project: MetricProjectDetail,
        cutoffMillis: Long
    ): Double? {
        val resourceScores = project.resources.mapNotNull { resource ->
            calculateResourceScore(resource, cutoffMillis)
        }

        return average(resourceScores)
    }

    private fun calculateResourceScore(
        resource: MetricProjectResource,
        cutoffMillis: Long
    ): Double? {
        if (resource.metrics.isEmpty()) return null

        val metricsByName = resource.metrics.associateBy { it.name }
        val commits = extractCommits(metricsByName[MetricCommits], cutoffMillis)
        val issues = extractIssues(metricsByName[MetricIssues], cutoffMillis)
        val pullRequests = extractPullRequests(metricsByName[MetricPullRequests], cutoffMillis)

        val grades = buildList {
            resource.metrics.forEach { metric ->
                if (!isGraded(metric.params)) return@forEach

                val grade = when (metric.name) {
                    MetricTotalCommits -> getTotalCommitsGrade(commits)
                    MetricIssueCompleteness -> getIssueCompletenessGrade(issues)
                    MetricPullRequestHangTime -> getPullRequestHangTimeGrade(pullRequests)
                    MetricRapidPullRequests -> getRapidPullRequestsGrade(
                        pullRequests = pullRequests,
                        threshold = getDurationParam(
                            params = metric.params,
                            name = ParamRapidPullRequestsThreshold,
                            fallback = DurationValue(
                                number = 5.0,
                                unitOfTime = "minutes"
                            )
                        )
                    )
                    MetricCodeOwnership -> getCodeOwnershipGrade(commits)
                    MetricDominantWeekDay -> getDominantWeekDayGrade(
                        commits = commits,
                        issues = issues,
                        pullRequests = pullRequests,
                        unwantedDay = getStringParam(metric.params, ParamUnwantedWeekDay)
                            ?: "Not Specified"
                    )
                    else -> null
                }

                if (grade != null && grade.isFinite()) {
                    add(WeightedGrade(grade = grade, weight = getGradeWeight(metric.params)))
                }
            }
        }

        if (grades.isEmpty()) return null

        val totalWeight = grades.sumOf { it.weight }
        if (totalWeight <= 0.0) return null

        val weightedSum = grades.sumOf { it.grade * it.weight }
        return (weightedSum / totalWeight).roundToTwoDecimals()
    }

    private fun extractCommits(
        metric: MetricProjectMetric?,
        cutoffMillis: Long
    ): List<CommitSnapshotData> {
        return metric.filteredSnapshotObjects(cutoffMillis).map { snapshot ->
            CommitSnapshotData(
                authorLogin = snapshot.objectOrNull("author")?.string("login"),
                authoredAt = snapshot.objectOrNull("commit")
                    ?.objectOrNull("author")
                    ?.string("date")
                    ?.toInstantMillis(),
                files = snapshot.arrayOrNull("files").orEmpty().mapNotNull { file ->
                    val fileObject = file as? JsonObject ?: return@mapNotNull null
                    CommitFile(
                        additions = fileObject.number("additions") ?: 0,
                        deletions = fileObject.number("deletions") ?: 0,
                        changes = fileObject.number("changes") ?: 0
                    )
                }
            )
        }
    }

    private fun extractIssues(
        metric: MetricProjectMetric?,
        cutoffMillis: Long
    ): List<IssueSnapshotData> {
        return metric.filteredSnapshotObjects(cutoffMillis).map { snapshot ->
            IssueSnapshotData(
                createdAt = snapshot.string("created_at")?.toInstantMillis(),
                closedAt = snapshot.string("closed_at")?.toInstantMillis()
            )
        }
    }

    private fun extractPullRequests(
        metric: MetricProjectMetric?,
        cutoffMillis: Long
    ): List<PullRequestSnapshotData> {
        return metric.filteredSnapshotObjects(cutoffMillis).map { snapshot ->
            PullRequestSnapshotData(
                createdAt = snapshot.string("created_at")?.toInstantMillis(),
                closedAt = snapshot.string("closed_at")?.toInstantMillis()
            )
        }
    }

    private fun MetricProjectMetric?.filteredSnapshotObjects(cutoffMillis: Long): List<JsonObject> {
        if (this == null) return emptyList()

        return data.mapNotNull { snapshot ->
            val timestamp = snapshot.timestamp ?: return@mapNotNull null
            if (!snapshot.error.isNullOrBlank()) return@mapNotNull null
            if (timestamp > cutoffMillis.toDouble()) return@mapNotNull null
            snapshot.data as? JsonObject
        }
    }

    private fun getTotalCommitsGrade(commits: List<CommitSnapshotData>): Double? {
        if (commits.isEmpty()) return null

        val commitDates = commits.mapNotNull { it.authoredAt }
        val start = commitDates.minOrNull() ?: return null
        val end = commitDates.maxOrNull() ?: return null

        val dayCount = (end - start).toDouble() / (1000 * 60 * 60 * 24)
        if (dayCount <= 0.0) return 5.0

        val userCount = commits.mapNotNull { it.authorLogin?.trim()?.lowercase() }.toSet().size
        if (userCount <= 0) return 5.0

        val commitsPerDay = commits.size / dayCount / userCount
        return min(commitsPerDay * 3 * 3 + 2, 5.0).roundToTwoDecimals()
    }

    private fun getIssueCompletenessGrade(issues: List<IssueSnapshotData>): Double? {
        if (issues.isEmpty()) return null
        val closed = issues.count { it.closedAt != null }
        return ((closed.toDouble() / issues.size) * 3 + 2).roundToTwoDecimals()
    }

    private fun getPullRequestHangTimeGrade(
        pullRequests: List<PullRequestSnapshotData>
    ): Double? {
        if (pullRequests.isEmpty()) return null

        val durations = pullRequests.mapNotNull { pullRequest ->
            val createdAt = pullRequest.createdAt ?: return@mapNotNull null
            val closedAt = pullRequest.closedAt ?: return@mapNotNull null
            (closedAt - createdAt).takeIf { it >= 0 }
        }
        if (durations.isEmpty()) return null

        val averageHangTime = durations.average()
        if (averageHangTime < 1000 * 60 * 5) {
            return (averageHangTime / (1000 * 60)).roundToTwoDecimals()
        }

        val grade = ((1 - averageHangTime / (1000 * 60 * 60 * 24 * 7)) * 3 + 2)
            .roundToTwoDecimals()
        return max(grade, 0.0)
    }

    private fun getRapidPullRequestsGrade(
        pullRequests: List<PullRequestSnapshotData>,
        threshold: DurationValue
    ): Double? {
        if (pullRequests.isEmpty()) return null

        val thresholdMillis = durationToMilliseconds(threshold)
        val rapidPullRequests = pullRequests.count { pullRequest ->
            val createdAt = pullRequest.createdAt ?: return@count false
            val closedAt = pullRequest.closedAt ?: return@count false
            closedAt - createdAt < thresholdMillis
        }

        return ((1 - rapidPullRequests.toDouble() / pullRequests.size) * 3 + 2)
            .roundToTwoDecimals()
    }

    private fun getCodeOwnershipGrade(commits: List<CommitSnapshotData>): Double? {
        val userLines = mutableMapOf<String, Double>()
        var totalLines = 0.0

        commits.forEach { commit ->
            val login = commit.authorLogin?.trim().takeIf { !it.isNullOrBlank() } ?: return@forEach
            commit.files.forEach { file ->
                val lineDelta = file.additions + file.deletions + file.changes
                if (lineDelta <= 0) return@forEach

                userLines[login] = (userLines[login] ?: 0.0) + lineDelta
                totalLines += lineDelta
            }
        }

        if (totalLines <= 0.0) return null

        val contributors = userLines.entries
            .map { entry ->
                ContributorShare(
                    user = entry.key,
                    lines = entry.value / totalLines
                )
            }
            .filter { it.lines > 0.0 }

        if (contributors.size < 2) return null

        val averageShare = 1.0 / contributors.size
        val worstCase = (1 - averageShare).pow(2) +
            (contributors.size - 1) * averageShare.pow(2)
        if (worstCase <= 0.0) return null

        val dispersion = contributors.sumOf { contributor ->
            (contributor.lines - averageShare).pow(2)
        }
        val gradeComponent = (1 - sqrt(dispersion / worstCase)) * 3
        val grade = min(3.0, max(0.0, gradeComponent)) + 2
        return grade.roundToTwoDecimals()
    }

    private fun getDominantWeekDayGrade(
        commits: List<CommitSnapshotData>,
        issues: List<IssueSnapshotData>,
        pullRequests: List<PullRequestSnapshotData>,
        unwantedDay: String
    ): Double? {
        val values = WeekDays.associateWith { 0.0 }.toMutableMap()

        commits.forEach { commit ->
            val index = commit.authoredAt?.toBackendWeekDayIndex() ?: return@forEach
            values[WeekDays[index]] = (values[WeekDays[index]] ?: 0.0) + 1
        }

        issues.forEach { issue ->
            val createdIndex = issue.createdAt?.toBackendWeekDayIndex()
            if (createdIndex != null) {
                values[WeekDays[createdIndex]] = (values[WeekDays[createdIndex]] ?: 0.0) + 1
            }

            val closedIndex = issue.closedAt?.toBackendWeekDayIndex()
            if (closedIndex != null) {
                values[WeekDays[closedIndex]] = (values[WeekDays[closedIndex]] ?: 0.0) - 1
            }
        }

        pullRequests.forEach { pullRequest ->
            val createdIndex = pullRequest.createdAt?.toBackendWeekDayIndex()
            if (createdIndex != null) {
                values[WeekDays[createdIndex]] = (values[WeekDays[createdIndex]] ?: 0.0) + 1
            }

            val closedIndex = pullRequest.closedAt?.toBackendWeekDayIndex()
            if (closedIndex != null) {
                values[WeekDays[closedIndex]] = (values[WeekDays[closedIndex]] ?: 0.0) - 1
            }
        }

        return calculateDominantWeekDayGrade(values, unwantedDay)
    }

    private fun calculateDominantWeekDayGrade(
        values: Map<String, Double>,
        unwantedDay: String
    ): Double? {
        if (unwantedDay == "Not Specified") return null
        if (unwantedDay !in WeekDays) return null

        val currentDay = values[unwantedDay] ?: return null
        val averageActions = WeekDays
            .filterNot { it == unwantedDay }
            .map { values[it] ?: 0.0 }
            .average()

        if (averageActions <= 0.0) {
            return if (currentDay <= 0.0) 5.0 else 0.0
        }

        val ratio = currentDay / averageActions
        if (ratio < 1.0) return 5.0

        return max(0.0, (-3.0 / 2.0) * ratio + 13.0 / 2.0).roundToTwoDecimals()
    }

    private fun average(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        return values.average().roundToTwoDecimals()
    }

    private fun isGraded(params: List<MetricProjectMetricParam>): Boolean {
        return params.any { param ->
            param.name == ParamIsGraded &&
                param.type == "boolean" &&
                param.value?.jsonPrimitive?.booleanOrNull == true
        }
    }

    private fun getGradeWeight(params: List<MetricProjectMetricParam>): Double {
        val raw = params.firstOrNull { it.name == ParamGradeWeight }?.value
        return raw.parseNumber() ?: 1.0
    }

    private fun getStringParam(
        params: List<MetricProjectMetricParam>,
        name: String
    ): String? {
        return params.firstOrNull { it.name == name }
            ?.value
            ?.jsonPrimitive
            ?.contentOrNull
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun getDurationParam(
        params: List<MetricProjectMetricParam>,
        name: String,
        fallback: DurationValue
    ): DurationValue {
        val rawValue = params.firstOrNull {
            it.name == name && it.type == "duration"
        }?.value as? JsonObject ?: return fallback

        val number = rawValue["number"].parseNumber() ?: return fallback
        val unit = rawValue["unitOfTime"]?.jsonPrimitive?.contentOrNull ?: return fallback
        return DurationValue(number = number, unitOfTime = unit)
    }

    private fun durationToMilliseconds(duration: DurationValue): Double {
        val base = 1000.0
        return when (duration.unitOfTime) {
            "seconds" -> duration.number * base
            "minutes" -> duration.number * base * 60
            "hours" -> duration.number * base * 60 * 60
            "days" -> duration.number * base * 60 * 60 * 24
            "weeks" -> duration.number * base * 60 * 60 * 24 * 7
            "months" -> duration.number * base * 60 * 60 * 24 * 30
            "years" -> duration.number * base * 60 * 60 * 24 * 365
            else -> duration.number
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
        return this[key].parseNumber()?.toInt()
    }

    private fun JsonElement?.parseNumber(): Double? {
        val element = this ?: return null
        return when (element) {
            is JsonObject -> null
            is JsonArray -> null
            else -> element.jsonPrimitive.doubleOrNull
                ?: element.jsonPrimitive.intOrNull?.toDouble()
                ?: element.jsonPrimitive.contentOrNull?.toDoubleOrNull()
        }
    }

    private fun String.toInstantMillis(): Long? {
        return runCatching { Instant.parse(this).toEpochMilliseconds() }.getOrNull()
    }

    private fun Long.toBackendWeekDayIndex(): Int? {
        return runCatching {
            val dayOfWeek = Instant.fromEpochMilliseconds(this)
                .toLocalDateTime(TimeZone.UTC)
                .dayOfWeek
            (dayOfWeek.ordinal + 1) % 7
        }.getOrNull()
    }

    private fun Double.roundToTwoDecimals(): Double {
        return (this * 100).roundToInt() / 100.0
    }
}

private data class HistoricalProjectScore(
    val id: String,
    val name: String,
    val score: Double
)

private data class WeightedGrade(
    val grade: Double,
    val weight: Double
)

private data class DurationValue(
    val number: Double,
    val unitOfTime: String
)

private data class CommitFile(
    val additions: Int,
    val deletions: Int,
    val changes: Int
)

private data class CommitSnapshotData(
    val authorLogin: String?,
    val authoredAt: Long?,
    val files: List<CommitFile>
)

private data class IssueSnapshotData(
    val createdAt: Long?,
    val closedAt: Long?
)

private data class PullRequestSnapshotData(
    val createdAt: Long?,
    val closedAt: Long?
)

private data class ContributorShare(
    val user: String,
    val lines: Double
)
