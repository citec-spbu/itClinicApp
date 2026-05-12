package com.spbu.projecttrack.rating.data.repository

import com.spbu.projecttrack.rating.data.model.MetricProjectDetail
import com.spbu.projecttrack.rating.data.model.MetricProjectMetric
import com.spbu.projecttrack.rating.data.model.MetricProjectResource
import com.spbu.projecttrack.rating.data.model.MetricProjectSnapshot
import com.spbu.projecttrack.rating.data.model.RankingFilters
import com.spbu.projecttrack.rating.data.model.RankingMetricFilter
import com.spbu.projecttrack.rating.data.model.RankingMetricKey
import com.spbu.projecttrack.rating.data.model.RankingWeekDay
import com.spbu.projecttrack.rating.data.model.rankingDefaultMetricFilters
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RankingScoreEngineTest {

    // Fixed "now" — 2025-01-01T00:00:00Z
    private val NOW = 1_735_689_600_000L

    // ─────────────────────────────────────────────
    // Builder helpers
    // ─────────────────────────────────────────────

    private fun commitJson(
        login: String = "alice",
        date: String = "2024-06-15T10:00:00Z",
        files: List<JsonObject> = emptyList(),
    ): JsonObject = buildJsonObject {
        put("author", buildJsonObject { put("login", login) })
        put("commit", buildJsonObject {
            put("author", buildJsonObject { put("date", date) })
        })
        put("files", JsonArray(files))
    }

    private fun fileJson(
        filename: String = "Main.kt",
        additions: Int = 0,
        deletions: Int = 0,
        changes: Int = 0,
    ): JsonObject = buildJsonObject {
        put("filename", filename)
        put("status", "modified")
        put("additions", additions)
        put("deletions", deletions)
        put("changes", changes)
    }

    private fun issueJson(
        login: String = "alice",
        createdAt: String = "2024-06-01T10:00:00Z",
        closedAt: String? = null,
    ): JsonObject = buildJsonObject {
        put("user", buildJsonObject { put("login", login) })
        put("assignees", JsonArray(emptyList()))
        put("created_at", createdAt)
        if (closedAt != null) put("closed_at", closedAt)
    }

    private fun prJson(
        login: String = "alice",
        createdAt: String,
        closedAt: String? = null,
    ): JsonObject = buildJsonObject {
        put("user", buildJsonObject { put("login", login) })
        put("created_at", createdAt)
        if (closedAt != null) put("closed_at", closedAt)
    }

    private fun makeProject(
        commits: List<JsonObject> = emptyList(),
        issues: List<JsonObject> = emptyList(),
        prs: List<JsonObject> = emptyList(),
    ): MetricProjectDetail {
        val metrics = buildList {
            if (commits.isNotEmpty()) add(
                MetricProjectMetric(
                    id = "c", name = "Commits",
                    data = commits.map { MetricProjectSnapshot(data = it) },
                )
            )
            if (issues.isNotEmpty()) add(
                MetricProjectMetric(
                    id = "i", name = "Issues",
                    data = issues.map { MetricProjectSnapshot(data = it) },
                )
            )
            if (prs.isNotEmpty()) add(
                MetricProjectMetric(
                    id = "p", name = "Pull Requests",
                    data = prs.map { MetricProjectSnapshot(data = it) },
                )
            )
        }
        return MetricProjectDetail(
            id = "test",
            name = "Test Project",
            resources = listOf(MetricProjectResource(id = "r1", name = "repo", metrics = metrics)),
        )
    }

    /** Filters with only the specified metrics enabled. */
    private fun filtersWithOnly(vararg keys: RankingMetricKey): RankingFilters =
        RankingFilters(
            metrics = rankingDefaultMetricFilters().toMutableMap().apply {
                keys.forEach { key -> this[key] = RankingMetricFilter(enabled = true) }
            }
        )

    private fun projectScore(
        project: MetricProjectDetail,
        filters: RankingFilters = RankingFilters(),
    ): Double? = RankingScoreEngine.calculateProjectScore(project, filters, NOW)

    private fun userScore(
        project: MetricProjectDetail,
        users: Set<String>,
        filters: RankingFilters = RankingFilters(),
    ): Double? = RankingScoreEngine.calculateUserScore(project, filters, users, NOW)

    // ─────────────────────────────────────────────
    // Base cases
    // ─────────────────────────────────────────────

    @Test
    fun projectWithNoResourcesReturnsNull() {
        val project = MetricProjectDetail(id = "empty", name = "Empty", resources = emptyList())
        assertNull(projectScore(project))
    }

    @Test
    fun projectWithEmptyMetricsReturnsNull() {
        val project = makeProject() // no commits, issues, or PRs
        assertNull(projectScore(project))
    }

    @Test
    fun snapshotsWithErrorsAreSkipped() {
        val metrics = listOf(
            MetricProjectMetric(
                id = "i", name = "Issues",
                data = listOf(
                    MetricProjectSnapshot(error = "fetch failed", data = null),
                    MetricProjectSnapshot(data = issueJson(closedAt = "2024-06-02T10:00:00Z")),
                ),
            )
        )
        val project = MetricProjectDetail(
            id = "test", name = "Test",
            resources = listOf(MetricProjectResource(id = "r", name = "repo", metrics = metrics)),
        )
        // After discarding the error snapshot, 1 closed issue remains → 5.0
        assertEquals(5.0, projectScore(project, filtersWithOnly(RankingMetricKey.IssueCompleteness)))
    }

    // ─────────────────────────────────────────────
    // issueCompletenessGrade
    // Formula: (closed / total) * 3 + 2
    // ─────────────────────────────────────────────

    @Test
    fun issueCompleteness_allClosedReturns5() {
        val project = makeProject(
            issues = listOf(
                issueJson(closedAt = "2024-06-02T10:00:00Z"),
                issueJson(closedAt = "2024-06-03T10:00:00Z"),
            )
        )
        assertEquals(5.0, projectScore(project, filtersWithOnly(RankingMetricKey.IssueCompleteness)))
    }

    @Test
    fun issueCompleteness_noneClosedReturns2() {
        val project = makeProject(
            issues = listOf(issueJson(), issueJson())
        )
        assertEquals(2.0, projectScore(project, filtersWithOnly(RankingMetricKey.IssueCompleteness)))
    }

    @Test
    fun issueCompleteness_halfClosedReturns3_5() {
        val project = makeProject(
            issues = listOf(
                issueJson(closedAt = "2024-06-02T10:00:00Z"),
                issueJson(), // open
            )
        )
        assertEquals(3.5, projectScore(project, filtersWithOnly(RankingMetricKey.IssueCompleteness)))
    }

    // ─────────────────────────────────────────────
    // rapidPullRequestsGrade
    // Formula: (1 - rapidCount / total) * 3 + 2
    // Default threshold = 150 minutes
    // ─────────────────────────────────────────────

    @Test
    fun rapidPRs_allRapidReturns2() {
        // Closed within 1 minute → rapid
        val project = makeProject(
            prs = listOf(
                prJson(createdAt = "2024-06-01T10:00:00Z", closedAt = "2024-06-01T10:01:00Z"),
                prJson(createdAt = "2024-06-01T11:00:00Z", closedAt = "2024-06-01T11:01:00Z"),
            )
        )
        assertEquals(2.0, projectScore(project, filtersWithOnly(RankingMetricKey.RapidPullRequests)))
    }

    @Test
    fun rapidPRs_noneRapidReturns5() {
        // Closed after 24 hours → slow
        val project = makeProject(
            prs = listOf(
                prJson(createdAt = "2024-06-01T10:00:00Z", closedAt = "2024-06-02T10:00:00Z"),
                prJson(createdAt = "2024-06-02T10:00:00Z", closedAt = "2024-06-03T10:00:00Z"),
            )
        )
        assertEquals(5.0, projectScore(project, filtersWithOnly(RankingMetricKey.RapidPullRequests)))
    }

    @Test
    fun rapidPRs_halfRapidReturns3_5() {
        val project = makeProject(
            prs = listOf(
                prJson(createdAt = "2024-06-01T10:00:00Z", closedAt = "2024-06-01T10:01:00Z"), // rapid
                prJson(createdAt = "2024-06-01T10:00:00Z", closedAt = "2024-06-02T10:00:00Z"), // slow
            )
        )
        assertEquals(3.5, projectScore(project, filtersWithOnly(RankingMetricKey.RapidPullRequests)))
    }

    // ─────────────────────────────────────────────
    // pullRequestHangTimeGrade
    // Formula:
    //   if avg < 5 min → avg_in_minutes
    //   else → (1 - avg / 7days) * 3 + 2, min 0
    // ─────────────────────────────────────────────

    @Test
    fun prHangTime_3_5daysReturns3_5() {
        // (1 - 3.5/7) * 3 + 2 = 0.5 * 3 + 2 = 3.5
        val project = makeProject(
            prs = listOf(
                prJson(createdAt = "2024-06-01T00:00:00Z", closedAt = "2024-06-04T12:00:00Z")
            )
        )
        assertEquals(3.5, projectScore(project, filtersWithOnly(RankingMetricKey.PullRequestHangTime)))
    }

    @Test
    fun prHangTime_2minutesReturnsScore2() {
        // avg < 5 min → grade = 2_000_000 / 60_000 = 2.0 minutes → round2 = 2.0
        val project = makeProject(
            prs = listOf(
                prJson(createdAt = "2024-06-01T10:00:00Z", closedAt = "2024-06-01T10:02:00Z")
            )
        )
        assertEquals(2.0, projectScore(project, filtersWithOnly(RankingMetricKey.PullRequestHangTime)))
    }

    @Test
    fun prHangTime_over7daysReturns0() {
        // 14 days → (1 - 2) * 3 + 2 = -1 → coerceAtLeast(0.0) = 0.0
        val project = makeProject(
            prs = listOf(
                prJson(createdAt = "2024-06-01T00:00:00Z", closedAt = "2024-06-15T00:00:00Z")
            )
        )
        assertEquals(0.0, projectScore(project, filtersWithOnly(RankingMetricKey.PullRequestHangTime)))
    }

    @Test
    fun prHangTime_noClosedPRsReturnsNull() {
        // No PR with both dates → null
        val project = makeProject(
            prs = listOf(prJson(createdAt = "2024-06-01T10:00:00Z")) // closedAt absent
        )
        assertNull(projectScore(project, filtersWithOnly(RankingMetricKey.PullRequestHangTime)))
    }

    // ─────────────────────────────────────────────
    // totalCommitsGrade
    // Formula: min(commitsPerDay * 9 + 2, 5.0)
    // ─────────────────────────────────────────────

    @Test
    fun totalCommits_allOnSameDayReturns5() {
        // dayCount = 0 → returns 5.0 directly
        val project = makeProject(
            commits = listOf(
                commitJson(date = "2024-06-01T10:00:00Z"),
                commitJson(date = "2024-06-01T11:00:00Z"),
            )
        )
        assertEquals(5.0, projectScore(project, filtersWithOnly(RankingMetricKey.TotalCommits)))
    }

    @Test
    fun totalCommits_regularActivityProducesScoreBetween2And5() {
        // 5 commits over 7 days, 1 user → commitsPerDay ≈ 0.71
        // grade ≈ min(0.71 * 9 + 2, 5.0) = min(6.4, 5.0) = 5.0
        val dates = listOf(
            "2024-06-01T10:00:00Z",
            "2024-06-02T10:00:00Z",
            "2024-06-03T10:00:00Z",
            "2024-06-05T10:00:00Z",
            "2024-06-07T10:00:00Z",
        )
        val project = makeProject(commits = dates.map { commitJson(date = it) })
        val result = projectScore(project, filtersWithOnly(RankingMetricKey.TotalCommits))
        assertNotNull(result)
        assertTrue(result in 2.0..5.0, "Expected score in 2..5, got $result")
    }

    // ─────────────────────────────────────────────
    // codeChurnGrade
    // Formula: 5.0 - ln(1 + churnPerCommit).coerceAtMost(4.0) * 1.2
    // ─────────────────────────────────────────────

    @Test
    fun codeChurn_noFileChangesReturnsNull() {
        // Commit with no file changes → null
        val project = makeProject(commits = listOf(commitJson()))
        assertNull(projectScore(project, filtersWithOnly(RankingMetricKey.CodeChurn)))
    }

    @Test
    fun codeChurn_lowChurnProducesHighGrade() {
        // 1 commit, 2 changes → grade ≈ 5.0 - ln(3) * 1.2 ≈ 3.68
        val project = makeProject(
            commits = listOf(commitJson(files = listOf(fileJson(changes = 2))))
        )
        val result = projectScore(project, filtersWithOnly(RankingMetricKey.CodeChurn))
        assertNotNull(result)
        assertTrue(result > 3.0, "Expected score > 3 for low churn, got $result")
    }

    @Test
    fun codeChurn_highChurnProducesLowGrade() {
        // 1 commit, 1000 changes → ln(1001) ≈ 6.9 → coerceAtMost(4.0) → 5.0 - 4.8 = 0.2
        val project = makeProject(
            commits = listOf(commitJson(files = listOf(fileJson(changes = 1000))))
        )
        val result = projectScore(project, filtersWithOnly(RankingMetricKey.CodeChurn))
        assertNotNull(result)
        assertTrue(result < 1.0, "Expected score < 1 for high churn, got $result")
    }

    // ─────────────────────────────────────────────
    // codeOwnershipGrade
    // null if < 2 authors, 5.0 for equal distribution
    // ─────────────────────────────────────────────

    @Test
    fun codeOwnership_singleAuthorReturnsNull() {
        val file = fileJson(changes = 10)
        val project = makeProject(
            commits = listOf(
                commitJson(login = "alice", files = listOf(file)),
                commitJson(login = "alice", files = listOf(file)),
            )
        )
        assertNull(projectScore(project, filtersWithOnly(RankingMetricKey.CodeOwnership)))
    }

    @Test
    fun codeOwnership_equalDistributionReturns5() {
        // alice and bob contribute equally → minimum variance → 5.0
        val file = fileJson(changes = 10)
        val project = makeProject(
            commits = listOf(
                commitJson(login = "alice", files = listOf(file)),
                commitJson(login = "bob", files = listOf(file)),
            )
        )
        assertEquals(5.0, projectScore(project, filtersWithOnly(RankingMetricKey.CodeOwnership)))
    }

    @Test
    fun codeOwnership_dominantAuthorProducesLowerGrade() {
        // alice 90%, bob 10% → high variance → score < 5
        val bigFile = fileJson(changes = 90)
        val smallFile = fileJson(filename = "Other.kt", changes = 10)
        val project = makeProject(
            commits = listOf(
                commitJson(login = "alice", files = listOf(bigFile)),
                commitJson(login = "bob", files = listOf(smallFile)),
            )
        )
        val result = projectScore(project, filtersWithOnly(RankingMetricKey.CodeOwnership))
        assertNotNull(result)
        assertTrue(result < 5.0, "Expected score < 5 when one author dominates, got $result")
    }

    // ─────────────────────────────────────────────
    // dominantWeekDayGrade
    // ─────────────────────────────────────────────

    @Test
    fun dominantWeekDay_noActivityOnUnwantedDayReturns5() {
        // 2024-06-03 = Monday, unwanted day = Thursday → ratio < 1 → 5.0
        val project = makeProject(
            commits = listOf(
                commitJson(date = "2024-06-03T10:00:00Z"), // Monday
                commitJson(date = "2024-06-03T11:00:00Z"), // Monday
            )
        )
        assertEquals(5.0, projectScore(project, filtersWithOnly(RankingMetricKey.DominantWeekDay)))
    }

    @Test
    fun dominantWeekDay_allActivityOnUnwantedDayReturns0() {
        // 2024-06-06 = Thursday (unwanted by default),
        // all activity on Thursday → averageOtherDays = 0 → currentDay > 0 → 0.0
        val project = makeProject(
            commits = listOf(
                commitJson(date = "2024-06-06T10:00:00Z"), // Thursday
                commitJson(date = "2024-06-06T11:00:00Z"), // Thursday
            )
        )
        assertEquals(0.0, projectScore(project, filtersWithOnly(RankingMetricKey.DominantWeekDay)))
    }

    // ─────────────────────────────────────────────
    // calculateUserScore
    // ─────────────────────────────────────────────

    @Test
    fun userScore_emptyUsersSetReturnsNull() {
        val project = makeProject(
            issues = listOf(issueJson(closedAt = "2024-06-02T10:00:00Z"))
        )
        assertNull(userScore(project, emptySet(), filtersWithOnly(RankingMetricKey.IssueCompleteness)))
    }

    @Test
    fun userScore_filtersIssuesByAuthor() {
        val project = makeProject(
            issues = listOf(
                issueJson(login = "alice", closedAt = "2024-06-02T10:00:00Z"), // alice, closed
                issueJson(login = "bob"),                                        // bob, open
            )
        )
        val filters = filtersWithOnly(RankingMetricKey.IssueCompleteness)

        // Only alice → all closed → 5.0
        assertEquals(5.0, userScore(project, setOf("alice"), filters))

        // Only bob → nothing closed → 2.0
        assertEquals(2.0, userScore(project, setOf("bob"), filters))
    }

    @Test
    fun userScore_loginMatchingIsCaseInsensitive() {
        val project = makeProject(
            issues = listOf(issueJson(login = "Alice", closedAt = "2024-06-02T10:00:00Z"))
        )
        // Passing "alice" in lowercase must match "Alice"
        assertEquals(
            5.0,
            userScore(project, setOf("alice"), filtersWithOnly(RankingMetricKey.IssueCompleteness))
        )
    }

    @Test
    fun userScore_userWithNoActivityReturnsNull() {
        val project = makeProject(
            issues = listOf(issueJson(login = "alice", closedAt = "2024-06-02T10:00:00Z"))
        )
        // charlie is not mentioned anywhere → no data → null
        assertNull(
            userScore(project, setOf("charlie"), filtersWithOnly(RankingMetricKey.IssueCompleteness))
        )
    }

    // ─────────────────────────────────────────────
    // Metric selection
    // ─────────────────────────────────────────────

    @Test
    fun noEnabledMetrics_usesAllAvailableData() {
        // With empty filters the engine computes from all available metrics
        val project = makeProject(
            issues = listOf(issueJson(closedAt = "2024-06-02T10:00:00Z"))
        )
        // Result is not null — data exists and all metrics are allowed
        assertNotNull(projectScore(project, RankingFilters()))
    }

    @Test
    fun enabledMetricWithNoDataReturnsNull() {
        // Commits enabled but no commit data exists — only issues
        val project = makeProject(
            issues = listOf(issueJson(closedAt = "2024-06-02T10:00:00Z"))
        )
        assertNull(projectScore(project, filtersWithOnly(RankingMetricKey.TotalCommits)))
    }

    @Test
    fun scoreIsBoundedBetween0And5() {
        // Verify that no computed score falls outside [0, 5]
        val project = makeProject(
            commits = listOf(commitJson(files = listOf(fileJson(changes = 9999)))),
            issues = listOf(issueJson()),
            prs = listOf(prJson(createdAt = "2024-01-01T00:00:00Z", closedAt = "2024-06-01T00:00:00Z")),
        )
        val result = projectScore(project)
        if (result != null) {
            assertTrue(result >= 0.0, "Score must not be negative: $result")
            assertTrue(result <= 5.0, "Score must not exceed 5.0: $result")
        }
    }
}
