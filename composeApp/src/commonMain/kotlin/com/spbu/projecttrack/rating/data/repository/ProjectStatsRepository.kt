package com.spbu.projecttrack.rating.data.repository

import com.spbu.projecttrack.core.settings.localizePluralRuntime
import com.spbu.projecttrack.core.settings.localizeRuntime
import com.spbu.projecttrack.core.time.PlatformTime
import com.spbu.projecttrack.projects.data.api.ProjectsApi
import com.spbu.projecttrack.projects.data.model.Member
import com.spbu.projecttrack.projects.data.model.Project
import com.spbu.projecttrack.projects.data.model.ProjectDetail
import com.spbu.projecttrack.projects.data.model.ProjectDetailResponse
import com.spbu.projecttrack.projects.data.model.User
import com.spbu.projecttrack.rating.data.api.MetricApi
import com.spbu.projecttrack.rating.common.formatDurationMillisLabel
import com.spbu.projecttrack.rating.data.model.MetricProjectDetail
import com.spbu.projecttrack.rating.data.model.MetricProjectMetric
import com.spbu.projecttrack.rating.data.model.MetricProjectResource
import com.spbu.projecttrack.rating.data.model.MetricProjectSnapshot
import com.spbu.projecttrack.rating.data.model.MetricProjectUser
import com.spbu.projecttrack.rating.data.model.ProjectStatsChartPointUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsChartType
import com.spbu.projecttrack.rating.data.model.ProjectStatsCodeChurnSectionUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsDateRangeUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsDonutSliceUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsFileRowUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsIssueSectionUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsMemberUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsMetricRowUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsMetricSectionUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsOwnershipSectionUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsRepositoryUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsThresholdUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsUiModel
import com.spbu.projecttrack.rating.data.model.ProjectStatsWeekDaySectionUi
import com.spbu.projecttrack.rating.data.model.StatsDetailCommitFileUi
import com.spbu.projecttrack.rating.data.model.StatsDetailCommitUi
import com.spbu.projecttrack.rating.data.model.StatsDetailDataUi
import com.spbu.projecttrack.rating.data.model.StatsDetailIssueUi
import com.spbu.projecttrack.rating.data.model.StatsDetailParticipantUi
import com.spbu.projecttrack.rating.data.model.StatsDetailPullRequestUi
import com.spbu.projecttrack.user.data.api.UserProfileApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

class ProjectStatsRepository(
    private val metricApi: MetricApi,
    private val projectsApi: ProjectsApi,
    private val userProfileApi: UserProfileApi,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var cachedKey: String? = null
    private var cachedModel: ProjectStatsUiModel? = null

    suspend fun loadProjectStats(
        projectId: String,
        selectedRepositoryId: String? = null,
        selectedStartDate: String? = null,
        selectedEndDate: String? = null,
        selectedRapidThresholdMinutes: Int? = null,
        forceRefresh: Boolean = false,
    ): Result<ProjectStatsUiModel> {
        val requestKey = buildRequestKey(
            projectId = projectId,
            repositoryId = selectedRepositoryId,
            startDate = selectedStartDate,
            endDate = selectedEndDate,
            rapidThresholdMinutes = selectedRapidThresholdMinutes,
        )

        cachedModel?.takeIf { !forceRefresh && cachedKey == requestKey }?.let {
            return Result.success(it)
        }

        return try {
            coroutineScope {
                val projectDeferred = async { projectsApi.getProjectById(projectId) }
                val metricDeferred = async { metricApi.getProjectDetail(projectId) }
                val profileDeferred = async { userProfileApi.getProfile() }
                val projectsDeferred = async { projectsApi.getAllProjects() }

                val projectResponse = projectDeferred.await().getOrNull()
                val metricDetail = metricDeferred.await().getOrNull()
                val profile = profileDeferred.await().getOrNull()
                val allProjects = projectsDeferred.await().getOrNull()

                val resolvedProject = resolveProject(projectId, projectResponse, metricDetail)
                val resolvedMetricDetail = resolveMetricDetail(
                    resolvedProject,
                    metricDetail ?: fetchMetricProjectDetail(
                        projectKey = projectId,
                        fallbackProjectId = resolvedProject.id,
                    ),
                )
                val primaryResource = selectResource(
                    resources = resolvedMetricDetail.resources,
                    selectedRepositoryId = selectedRepositoryId,
                )
                val resolvedRapidThresholdMinutes = resolveRapidThresholdMinutes(
                    selectedRapidThresholdMinutes = selectedRapidThresholdMinutes,
                    primaryResource = primaryResource,
                )

                val currentUserName = profile?.user?.fullName?.displayName()
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                val currentUserLogin = resolveCurrentUserLogin(
                    metricUsers = resolvedMetricDetail.users,
                    currentUserName = currentUserName,
                )
                val userNameLookup = buildUserNameLookup(
                    projectUsers = projectResponse?.users.orEmpty(),
                    metricUsers = resolvedMetricDetail.users,
                )

                val currentSnapshot = buildResourceSnapshot(
                    resource = primaryResource,
                    selectedStartDate = selectedStartDate,
                    selectedEndDate = selectedEndDate,
                    rapidThresholdMinutes = resolvedRapidThresholdMinutes,
                    currentUserLogin = currentUserLogin,
                    userNameLookup = userNameLookup,
                )

                val peerSnapshots = buildPeerSnapshots(
                    allProjects = allProjects?.projects.orEmpty(),
                    currentProject = resolvedProject,
                    selectedStartDate = selectedStartDate,
                    selectedEndDate = selectedEndDate,
                    rapidThresholdMinutes = resolvedRapidThresholdMinutes,
                )
                val members = buildMembers(
                    members = projectResponse?.members.orEmpty(),
                    users = projectResponse?.users.orEmpty(),
                    metricUsers = resolvedMetricDetail.users,
                    currentUserName = currentUserName,
                    currentUserLogin = currentUserLogin,
                )

                val model = ProjectStatsUiModel(
                    projectId = resolvedProject.id,
                    title = resolvedProject.name,
                    customer = resolvedProject.client
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: resolvedProject.contact
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: "—",
                    members = members,
                    repositories = buildRepositories(resolvedMetricDetail.resources),
                    selectedRepositoryId = primaryResource?.id.orEmpty(),
                    visibleRange = buildVisibleRange(
                        selectedStartDate = selectedStartDate,
                        selectedEndDate = selectedEndDate,
                        snapshot = currentSnapshot,
                        project = resolvedProject,
                    ),
                    commits = buildCommitsSection(currentSnapshot, peerSnapshots),
                    issues = buildIssuesSection(currentSnapshot, peerSnapshots),
                    pullRequests = buildPullRequestsSection(currentSnapshot, peerSnapshots),
                    rapidPullRequests = buildRapidPullRequestsSection(
                        currentSnapshot = currentSnapshot,
                        peerSnapshots = peerSnapshots,
                    ),
                    codeChurn = buildCodeChurnSection(currentSnapshot, peerSnapshots),
                    codeOwnership = buildCodeOwnershipSection(currentSnapshot, peerSnapshots),
                    dominantWeekDay = buildWeekDaySection(currentSnapshot, peerSnapshots),
                    details = buildDetails(
                        snapshot = currentSnapshot,
                        members = members,
                        userNameLookup = userNameLookup,
                    ),
                    rapidThreshold = buildRapidThreshold(resolvedRapidThresholdMinutes),
                    showOverallRatingButton = true,
                )

                cachedKey = requestKey
                cachedModel = model
                Result.success(model)
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private suspend fun buildPeerSnapshots(
        allProjects: List<Project>,
        currentProject: ProjectDetail,
        selectedStartDate: String?,
        selectedEndDate: String?,
        rapidThresholdMinutes: Int,
    ): List<ProjectStatsResourceSnapshot> {
        val currentId = currentProject.id.trim()
        val currentName = currentProject.name.trim()
        val currentSlug = currentProject.slug?.trim().orEmpty()

        val peers = allProjects.filterNot { project ->
            val projectSlug = project.slug?.trim().orEmpty()
            project.id.trim() == currentId ||
                project.name.trim() == currentName ||
                (currentSlug.isNotBlank() && projectSlug == currentSlug)
        }

        if (peers.isEmpty()) return emptyList()

        return coroutineScope {
            peers.map { project ->
                async {
                    val peerId = project.slug?.trim().takeUnless { it.isNullOrBlank() } ?: project.id
                    val peerDetail = fetchMetricProjectDetail(
                        projectKey = peerId,
                        fallbackProjectId = project.id,
                    ) ?: return@async null
                    val primaryResource = selectResource(peerDetail.resources, selectedRepositoryId = null)
                    buildResourceSnapshot(
                        resource = primaryResource,
                        selectedStartDate = selectedStartDate,
                        selectedEndDate = selectedEndDate,
                        rapidThresholdMinutes = rapidThresholdMinutes,
                        currentUserLogin = null,
                        userNameLookup = emptyMap(),
                    )
                }
            }.awaitAll().filterNotNull()
        }
    }

    private fun buildCommitsSection(
        snapshot: ProjectStatsResourceSnapshot,
        peerSnapshots: List<ProjectStatsResourceSnapshot>,
    ): ProjectStatsMetricSectionUi {
        val rank = buildRank(snapshot.commitsScore, peerSnapshots.map { it.commitsScore })
        return ProjectStatsMetricSectionUi(
            title = localizeRuntime("Коммиты", "Commits"),
            score = snapshot.commitsScore,
            primaryValue = snapshot.commitCount.toString(),
            primaryCaption = commitsLabel(snapshot.commitCount),
            rank = rank,
            rankCaption = localizeRuntime("место в рейтинге", "rank"),
            chartTitle = localizeRuntime("График коммитов", "Commits chart"),
            chartType = ProjectStatsChartType.Bars,
            chartPoints = snapshot.commitChart,
            tableTitle = localizeRuntime("Количество коммитов", "Commit count"),
            tableRows = snapshot.commitContributors.map {
                ProjectStatsMetricRowUi(
                    name = it.name,
                    value = it.value.toString(),
                    highlight = it.isCurrentUser,
                )
            },
            tooltipTitle = "${snapshot.commitCount} ${commitsLabel(snapshot.commitCount)}",
        )
    }

    private fun buildIssuesSection(
        snapshot: ProjectStatsResourceSnapshot,
        peerSnapshots: List<ProjectStatsResourceSnapshot>,
    ): ProjectStatsIssueSectionUi {
        val rank = buildRank(snapshot.issueScore, peerSnapshots.map { it.issueScore })
        val progress = if (snapshot.issueCount == 0) {
            0f
        } else {
            snapshot.closedIssueCount.toFloat() / snapshot.issueCount.toFloat()
        }

        return ProjectStatsIssueSectionUi(
            title = "Issue",
            score = snapshot.issueScore,
            openIssues = snapshot.openIssueCount,
            closedIssues = snapshot.closedIssueCount,
            progress = progress.coerceIn(0f, 1f),
            remainingText = if (snapshot.issueCount == 0) {
                localizeRuntime("Подробной информации по Issue нет", "Detailed issue data is unavailable")
            } else if (snapshot.openIssueCount > 0) {
                localizeRuntime(
                    "Закройте еще ${snapshot.openIssueCount} Issue",
                    "Close ${snapshot.openIssueCount} more issues",
                )
            } else {
                localizeRuntime("Все Issue закрыты", "All issues are closed")
            },
            rank = rank,
            tableRows = snapshot.issueContributors.map {
                ProjectStatsMetricRowUi(
                    name = it.name,
                    value = it.displayValue ?: it.value.toString(),
                    highlight = it.isCurrentUser,
                )
            },
        )
    }

    private fun buildPullRequestsSection(
        snapshot: ProjectStatsResourceSnapshot,
        peerSnapshots: List<ProjectStatsResourceSnapshot>,
    ): ProjectStatsMetricSectionUi {
        val rank = buildRank(snapshot.pullRequestScore, peerSnapshots.map { it.pullRequestScore })
        return ProjectStatsMetricSectionUi(
            title = "Pull Requests",
            score = snapshot.pullRequestScore,
            primaryValue = snapshot.pullRequestCount.toString(),
            primaryCaption = localizeRuntime("всего Pull Request", "total PRs"),
            supplementaryValue = formatAveragePullRequestLifetime(
                pullRequests = snapshot.pullRequests,
            ),
            supplementaryCaption = localizeRuntime(
                "среднее время жизни Pull Request",
                "average PR lifetime",
            ),
            rank = rank,
            rankCaption = localizeRuntime("место в рейтинге", "rank"),
            chartTitle = localizeRuntime("График Pull Requests", "Pull requests chart"),
            chartType = ProjectStatsChartType.Line,
            chartPoints = snapshot.pullRequestChart,
            tableTitle = localizeRuntime("Количество Pull Request", "Pull request count"),
            tableRows = snapshot.pullRequestContributors.map {
                ProjectStatsMetricRowUi(
                    name = it.name,
                    value = it.value.toString(),
                    highlight = it.isCurrentUser,
                )
            },
            tooltipTitle = "${snapshot.pullRequestCount} Pull Requests",
        )
    }

    private fun buildRapidPullRequestsSection(
        currentSnapshot: ProjectStatsResourceSnapshot,
        peerSnapshots: List<ProjectStatsResourceSnapshot>,
    ): ProjectStatsMetricSectionUi {
        val rank = buildRank(currentSnapshot.rapidPullScore, peerSnapshots.map { it.rapidPullScore })
        return ProjectStatsMetricSectionUi(
            title = localizeRuntime("Быстрые Pull Requests", "Rapid Pull Requests"),
            score = currentSnapshot.rapidPullScore,
            primaryValue = currentSnapshot.rapidPullRequestCount.toString(),
            primaryCaption = localizeRuntime("быстрых PR", "rapid PRs"),
            rank = rank,
            rankCaption = localizeRuntime("место в рейтинге", "rank"),
            chartTitle = localizeRuntime("График быстрых PR", "Rapid PR chart"),
            chartType = ProjectStatsChartType.Bars,
            chartPoints = currentSnapshot.rapidPullRequestChart,
            tableTitle = localizeRuntime("Количество быстрых Pull Request", "Rapid pull request count"),
            tableRows = currentSnapshot.pullRequestContributors.map {
                ProjectStatsMetricRowUi(
                    name = it.name,
                    value = it.value.toString(),
                    highlight = it.isCurrentUser,
                )
            },
            tooltipTitle = "${currentSnapshot.rapidPullRequestCount} ${localizeRuntime("быстрых PR", "rapid PRs")}",
        )
    }

    private fun buildCodeChurnSection(
        snapshot: ProjectStatsResourceSnapshot,
        peerSnapshots: List<ProjectStatsResourceSnapshot>,
    ): ProjectStatsCodeChurnSectionUi {
        val rank = buildRank(snapshot.codeChurnScore, peerSnapshots.map { it.codeChurnScore })
        val fileStats = snapshot.fileStats
        val churnSlices = buildFileChurnSlices(fileStats)
        return ProjectStatsCodeChurnSectionUi(
            title = localizeRuntime("Изменчивость кода", "Code churn"),
            score = snapshot.codeChurnScore,
            changedFilesCount = fileStats.size,
            rank = rank,
            fileRows = fileStats.map {
                ProjectStatsFileRowUi(
                    fileName = it.fileName,
                    value = it.changes.toString(),
                )
            },
            tableRows = snapshot.commitContributors.map {
                ProjectStatsMetricRowUi(
                    name = it.name,
                    value = it.value.toString(),
                    highlight = it.isCurrentUser,
                )
            },
            slices = churnSlices,
            mostChangedFileName = fileStats.firstOrNull()?.fileName,
        )
    }

    private fun buildCodeOwnershipSection(
        snapshot: ProjectStatsResourceSnapshot,
        peerSnapshots: List<ProjectStatsResourceSnapshot>,
    ): ProjectStatsOwnershipSectionUi {
        val rank = buildRank(snapshot.codeOwnershipScore, peerSnapshots.map { it.codeOwnershipScore })
        return ProjectStatsOwnershipSectionUi(
            title = localizeRuntime("Владение кодом", "Code ownership"),
            score = snapshot.codeOwnershipScore,
            rank = rank,
            slices = snapshot.codeOwnershipContributors.mapIndexed { index, item ->
                ProjectStatsDonutSliceUi(
                    label = item.name,
                    secondaryLabel = item.displayValue ?: item.value.toString(),
                    percentLabel = percentLabel(item.value, snapshot.totalLines),
                    value = item.value.toFloat(),
                    colorHex = ownershipPalette[index % ownershipPalette.size],
                    highlight = item.isCurrentUser,
                )
            },
        )
    }

    private fun buildWeekDaySection(
        snapshot: ProjectStatsResourceSnapshot,
        peerSnapshots: List<ProjectStatsResourceSnapshot>,
    ): ProjectStatsWeekDaySectionUi {
        val rank = buildRank(snapshot.weekDayScore, peerSnapshots.map { it.weekDayScore })
        return ProjectStatsWeekDaySectionUi(
            title = localizeRuntime("Доминирующий день недели", "Dominant weekday"),
            score = snapshot.weekDayScore,
            headline = snapshot.dominantWeekdayLabel?.uppercase() ?: localizeRuntime("НЕТ ДАННЫХ", "NO DATA"),
            subtitle = snapshot.dominantWeekdayLabel?.let {
                localizeRuntime("самый активный день недели", "most active day of the week")
            } ?: localizeRuntime("нет данных", "no data"),
            slices = snapshot.weekdays.mapIndexed { index, day ->
                ProjectStatsDonutSliceUi(
                    label = day.label,
                    secondaryLabel = "${day.value} ${actionsLabel(day.value)}",
                    percentLabel = percentLabel(day.value, snapshot.weekdayTotal),
                    value = day.value.toFloat(),
                    colorHex = weekdayPalette[index % weekdayPalette.size],
                    highlight = day.isDominant,
                )
            },
        )
    }

    private fun buildResourceSnapshot(
        resource: MetricProjectResource?,
        selectedStartDate: String?,
        selectedEndDate: String?,
        rapidThresholdMinutes: Int,
        currentUserLogin: String?,
        userNameLookup: Map<String, String>,
    ): ProjectStatsResourceSnapshot {
        if (resource == null) {
            return ProjectStatsResourceSnapshot.empty(rapidThresholdMinutes)
        }

        val metricsByName = resource.metrics.associateBy { it.name }
        val commitsRaw = deduplicateCommits(
            extractSnapshots<ProjectCommitSnapshot>(metricsByName[METRIC_COMMITS])
        )
        val issuesRaw = deduplicateIssues(
            extractSnapshots<ProjectIssueSnapshot>(metricsByName[METRIC_ISSUES])
        )
        val pullRequestsRaw = deduplicatePullRequests(
            extractSnapshots<ProjectPullRequestSnapshot>(metricsByName[METRIC_PULL_REQUESTS])
        )
        val eventDates = buildList {
            addAll(commitsRaw.mapNotNull { parseInstant(it.commit?.author?.date) })
            addAll(issuesRaw.mapNotNull { parseInstant(it.created_at) })
            addAll(issuesRaw.mapNotNull { parseInstant(it.closed_at) })
            addAll(pullRequestsRaw.mapNotNull { parseInstant(it.created_at) })
            addAll(pullRequestsRaw.mapNotNull { pullRequestCompletedAtIso(it)?.let(::parseInstant) })
        }

        val window = resolveWindow(
            selectedStartDate = selectedStartDate,
            selectedEndDate = selectedEndDate,
            eventDates = eventDates,
        )

        val commits = commitsRaw.filter { commit ->
            isInWindow(parseInstant(commit.commit?.author?.date), window)
        }
        val issues = issuesRaw.filter { issue ->
            isInWindow(parseInstant(issue.created_at), window) ||
                isInWindow(parseInstant(issue.closed_at), window)
        }
        val pullRequests = pullRequestsRaw.filter { pullRequest ->
            isInWindow(parseInstant(pullRequest.created_at), window) ||
                isInWindow(pullRequestCompletedAtIso(pullRequest)?.let(::parseInstant), window)
        }
        val closedPullRequests = pullRequests.filter { pullRequest ->
            pullRequestCompletedAtMillis(pullRequest) != null
        }
        val rapidPullRequests = closedPullRequests.filter { pullRequest ->
            isRapidPullRequest(pullRequest, rapidThresholdMinutes.toLong() * MILLIS_PER_MINUTE)
        }

        val commitCount = commits.size
        val issueCount = issues.size
        val openIssueCount = issues.count { it.closed_at.isNullOrBlank() }
        val closedIssueCount = issueCount - openIssueCount
        val pullRequestCount = pullRequests.size
        val rapidPullRequestCount = rapidPullRequests.size

        val commitChart = buildChartPoints(
            dates = commits.mapNotNull { parseInstant(it.commit?.author?.date) },
            hintFormatter = { count ->
                "$count ${commitsLabel(count)}"
            },
        )
        val pullRequestChart = buildChartPoints(
            dates = pullRequests.mapNotNull { parseInstant(it.created_at) },
            hintFormatter = { count -> "$count ${openedPullRequestsLabel(count)}" },
        )
        val rapidPullRequestChart = buildChartPoints(
            dates = rapidPullRequests.mapNotNull { pullRequestCompletedAtIso(it)?.let(::parseInstant) },
            hintFormatter = { count -> "$count ${localizeRuntime("быстрых PR", "rapid PRs")}" },
        )

        val contributorNameResolver = { login: String ->
            userNameLookup[login.trim().lowercase()] ?: login
        }

        val commitContributors = buildContributors(
            logins = commits.mapNotNull { it.author?.login },
            currentUserLogin = currentUserLogin,
            displayNameResolver = contributorNameResolver,
        )
        val issueContributors = buildIssueContributors(
            issues = issues,
            currentUserLogin = currentUserLogin,
            displayNameResolver = contributorNameResolver,
        )
        val pullRequestContributors = buildContributors(
            logins = pullRequests.mapNotNull { it.user?.login },
            currentUserLogin = currentUserLogin,
            displayNameResolver = contributorNameResolver,
        )
        val fileStats = buildFileStats(commits)
        val codeOwnershipContributors = buildCodeOwnershipContributors(
            commits = commits,
            currentUserLogin = currentUserLogin,
            displayNameResolver = contributorNameResolver,
        )
        val weekdays = buildWeekDays(commits, issues, pullRequests)

        return ProjectStatsResourceSnapshot(
            resourceId = resource.id,
            resourceName = resource.name,
            platform = resource.platform,
            url = extractStringParam(resource, "url") ?: extractStringParam(resource, "apiEndpoint"),
            commitCount = commitCount,
            issueCount = issueCount,
            openIssueCount = openIssueCount,
            closedIssueCount = closedIssueCount,
            pullRequestCount = pullRequestCount,
            rapidPullRequestCount = rapidPullRequestCount,
            commitContributors = commitContributors,
            issueContributors = issueContributors,
            pullRequestContributors = pullRequestContributors,
            codeOwnershipContributors = codeOwnershipContributors,
            fileStats = fileStats,
            weekdays = weekdays,
            weekdayTotal = weekdays.sumOf { it.value },
            dominantWeekdayLabel = weekdays.maxByOrNull { it.value }?.label,
            totalLines = codeOwnershipContributors.sumOf { it.value },
            commitsScore = totalCommitsScore(commits),
            issueScore = issueCompletenessScore(issues),
            pullRequestScore = pullRequestHangScore(pullRequests),
            rapidPullScore = rapidPullScore(closedPullRequests, rapidThresholdMinutes),
            codeOwnershipScore = codeOwnershipScore(commits),
            weekDayScore = weekDayScore(weekdays),
            codeChurnScore = codeChurnScore(fileStats, commitCount),
            commitChart = commitChart,
            pullRequestChart = pullRequestChart,
            rapidPullRequestChart = rapidPullRequestChart,
            visibleStart = window?.first ?: eventDates.minOrNull(),
            visibleEnd = window?.second ?: eventDates.maxOrNull(),
            rapidThresholdMinutes = rapidThresholdMinutes,
            commits = commits,
            issues = issues,
            pullRequests = pullRequests,
        )
    }

    private fun buildDetails(
        snapshot: ProjectStatsResourceSnapshot,
        members: List<ProjectStatsMemberUi>,
        userNameLookup: Map<String, String>,
    ): StatsDetailDataUi {
        val nowIso = Instant.fromEpochMilliseconds(PlatformTime.currentTimeMillis()).toString()
        return StatsDetailDataUi(
            participants = buildDetailParticipants(
                members = members,
                userNameLookup = userNameLookup,
                commits = snapshot.commits,
                issues = snapshot.issues,
                pullRequests = snapshot.pullRequests,
            ),
            commits = snapshot.commits.map { commit ->
                StatsDetailCommitUi(
                    authorId = normalizeLogin(commit.author?.login),
                    authorName = resolveUserDisplayName(commit.author?.login, userNameLookup),
                    authorAvatarUrl = commit.author?.avatar_url,
                    message = commit.commit?.message?.trim().takeUnless { it.isNullOrBlank() }
                        ?: commit.sha?.take(7)?.let { "Commit $it" }
                        ?: "Commit",
                    committedAtIso = commit.commit?.author?.date,
                    committedAtLabel = formatDateTimeLabel(commit.commit?.author?.date),
                    url = commit.html_url,
                    sha = commit.sha,
                    additions = commit.files.sumOf { it.additions ?: 0 },
                    deletions = commit.files.sumOf { it.deletions ?: 0 },
                    changes = commit.files.sumOf { it.changes ?: 0 },
                    files = commit.files.map { file ->
                        StatsDetailCommitFileUi(
                            fileName = file.filename?.trim().orEmpty(),
                            additions = file.additions ?: 0,
                            deletions = file.deletions ?: 0,
                            changes = file.changes ?: 0,
                            status = file.status,
                        )
                    }.filter { it.fileName.isNotBlank() }
                )
            },
            issues = snapshot.issues.map { issue ->
                val assignees = issue.assignees.mapNotNull { assignee ->
                    val assigneeId = normalizeLogin(assignee.login) ?: return@mapNotNull null
                    Triple(
                        assigneeId,
                        resolveUserDisplayName(assignee.login, userNameLookup),
                        assignee.avatar_url,
                    )
                }
                StatsDetailIssueUi(
                    creatorId = normalizeLogin(issue.user?.login),
                    creatorName = resolveUserDisplayName(issue.user?.login, userNameLookup),
                    creatorAvatarUrl = issue.user?.avatar_url,
                    assigneeIds = assignees.map { it.first },
                    assigneeNames = assignees.map { it.second },
                    assigneeAvatarUrls = assignees.map { it.third },
                    createdAtIso = issue.created_at,
                    createdAtLabel = formatDateTimeLabel(issue.created_at),
                    closedAtIso = issue.closed_at,
                    closedAtLabel = formatDateTimeLabel(issue.closed_at),
                    title = issue.title?.trim().takeUnless { it.isNullOrBlank() }
                        ?: issue.number?.let { "Issue #$it" }
                        ?: "Issue",
                    number = issue.number,
                    state = issue.state,
                    labels = issue.labels.mapNotNull { it.name?.trim()?.takeIf(String::isNotBlank) },
                    comments = issue.comments,
                    thumbsUpCount = issue.reactions?.plusOne,
                    thumbsDownCount = issue.reactions?.minusOne,
                    url = issue.html_url,
                )
            },
            pullRequests = snapshot.pullRequests.map { pullRequest ->
                val completedAtIso = pullRequestCompletedAtIso(pullRequest)
                val assignees = pullRequest.assignees.mapNotNull { assignee ->
                    val assigneeId = normalizeLogin(assignee.login) ?: return@mapNotNull null
                    assigneeId to resolveUserDisplayName(assignee.login, userNameLookup)
                }
                StatsDetailPullRequestUi(
                    authorId = normalizeLogin(pullRequest.user?.login),
                    authorName = resolveUserDisplayName(pullRequest.user?.login, userNameLookup),
                    assigneeIds = assignees.map { it.first },
                    assigneeNames = assignees.map { it.second },
                    createdAtIso = pullRequest.created_at,
                    createdAtLabel = formatDateTimeLabel(pullRequest.created_at),
                    closedAtIso = completedAtIso,
                    closedAtLabel = completedAtIso?.let(::formatDateTimeLabel),
                    effectiveEndAtIso = completedAtIso ?: nowIso,
                    title = pullRequest.title?.trim().takeUnless { it.isNullOrBlank() }
                        ?: pullRequest.number?.let { "Pull Request #$it" }
                        ?: "Pull Request",
                    number = pullRequest.number,
                    state = pullRequest.state,
                    comments = pullRequest.comments,
                    commitsCount = pullRequest.commits,
                    additions = pullRequest.additions,
                    deletions = pullRequest.deletions,
                    changedFiles = pullRequest.changed_files,
                    url = pullRequest.html_url,
                )
            },
        )
    }

    private fun buildDetailParticipants(
        members: List<ProjectStatsMemberUi>,
        userNameLookup: Map<String, String>,
        commits: List<ProjectCommitSnapshot>,
        issues: List<ProjectIssueSnapshot>,
        pullRequests: List<ProjectPullRequestSnapshot>,
    ): List<StatsDetailParticipantUi> {
        val loginByName = userNameLookup.entries.associateBy(
            keySelector = { personNameKey(it.value) },
            valueTransform = { it.key },
        )
        val participants = linkedMapOf<String, StatsDetailParticipantUi>()

        members.forEach { member ->
            val participantId = loginByName[personNameKey(member.name)] ?: return@forEach
            participants[participantId] = StatsDetailParticipantUi(
                id = participantId,
                name = member.name,
                subtitle = member.role,
                isCurrentUser = member.isCurrentUser,
            )
        }

        fun ensureParticipant(login: String?) {
            val participantId = normalizeLogin(login) ?: return
            if (participants.containsKey(participantId)) return
            val name = resolveUserDisplayName(login, userNameLookup)
            participants[participantId] = StatsDetailParticipantUi(
                id = participantId,
                name = name,
                subtitle = participantLabel(),
            )
        }

        commits.forEach { ensureParticipant(it.author?.login) }
        issues.forEach { issue ->
            ensureParticipant(issue.user?.login)
            issue.assignees.forEach { ensureParticipant(it.login) }
        }
        pullRequests.forEach { pullRequest ->
            ensureParticipant(pullRequest.user?.login)
            pullRequest.assignees.forEach { ensureParticipant(it.login) }
        }

        return participants.values.sortedWith(
            compareByDescending<StatsDetailParticipantUi> { it.isCurrentUser }
                .thenBy { it.name.lowercase() }
        )
    }

    private fun normalizeLogin(login: String?): String? {
        return login?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    }

    private fun resolveUserDisplayName(
        login: String?,
        userNameLookup: Map<String, String>,
    ): String {
        val normalized = normalizeLogin(login)
        return normalized?.let { userNameLookup[it] } ?: login?.trim().orEmpty().ifBlank { participantLabel() }
    }

    private fun buildContributors(
        logins: List<String>,
        currentUserLogin: String?,
        displayNameResolver: (String) -> String,
    ): List<ProjectStatsContributorStat> {
        val counts = linkedMapOf<String, Int>()
        logins.forEach { login ->
            val normalized = login.trim()
            if (normalized.isBlank()) return@forEach
            counts[normalized] = (counts[normalized] ?: 0) + 1
        }

        return counts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.lowercase() })
            .map { (login, count) ->
                ProjectStatsContributorStat(
                    login = login,
                    name = displayNameResolver(login),
                    value = count,
                    displayValue = count.toString(),
                    isCurrentUser = currentUserLogin?.equals(login, ignoreCase = true) == true,
                )
            }
    }

    private fun buildIssueContributors(
        issues: List<ProjectIssueSnapshot>,
        currentUserLogin: String?,
        displayNameResolver: (String) -> String,
    ): List<ProjectStatsContributorStat> {
        data class IssueStateCounts(
            var open: Int = 0,
            var closed: Int = 0,
        )

        val counts = linkedMapOf<String, IssueStateCounts>()

        fun increment(normalizedLogin: String, isClosed: Boolean) {
            if (normalizedLogin.isBlank()) return
            val entry = counts.getOrPut(normalizedLogin) { IssueStateCounts() }
            if (isClosed) {
                entry.closed += 1
            } else {
                entry.open += 1
            }
        }

        issues.forEach { issue ->
            val isClosed = !issue.closed_at.isNullOrBlank()
            val participants = buildSet {
                issue.user?.login?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
                issue.assignees.forEach { assignee ->
                    assignee.login?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
                }
            }
            participants.forEach { login ->
                increment(login, isClosed)
            }
        }

        return counts.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, IssueStateCounts>> { it.value.open + it.value.closed }
                    .thenBy { it.key.lowercase() }
            )
            .map { (login, count) ->
                ProjectStatsContributorStat(
                    login = login,
                    name = displayNameResolver(login),
                    value = count.open + count.closed,
                    displayValue = "${count.open}/${count.closed}",
                    isCurrentUser = currentUserLogin?.equals(login, ignoreCase = true) == true,
                )
            }
    }

    private fun deduplicateCommits(
        commits: List<ProjectCommitSnapshot>,
    ): List<ProjectCommitSnapshot> {
        // Prefer the entry with more file details (non-empty files list)
        return commits
            .groupBy { it.sha?.trim()?.lowercase() ?: it.commit?.message?.trim() ?: "" }
            .values
            .map { duplicates ->
                duplicates.maxByOrNull { it.files.size } ?: duplicates.first()
            }
    }

    private fun deduplicatePullRequests(
        pullRequests: List<ProjectPullRequestSnapshot>,
    ): List<ProjectPullRequestSnapshot> {
        return pullRequests
            .groupBy(::pullRequestIdentityKey)
            .values
            .map { duplicates ->
                duplicates.reduce(::preferPullRequestSnapshot)
            }
    }

    private fun pullRequestIdentityKey(
        pullRequest: ProjectPullRequestSnapshot,
    ): String {
        val number = pullRequest.number
        if (number != null) return "number:$number"

        val url = pullRequest.html_url?.trim()?.lowercase()
        if (!url.isNullOrBlank()) return "url:$url"

        val author = pullRequest.user?.login?.trim()?.lowercase().orEmpty()
        val title = pullRequest.title?.trim()?.lowercase().orEmpty()
        val createdAt = pullRequest.created_at?.trim().orEmpty()
        return "fallback:$author|$title|$createdAt"
    }

    private fun preferPullRequestSnapshot(
        current: ProjectPullRequestSnapshot,
        candidate: ProjectPullRequestSnapshot,
    ): ProjectPullRequestSnapshot {
        val currentScore = pullRequestSnapshotScore(current)
        val candidateScore = pullRequestSnapshotScore(candidate)
        return when {
            candidateScore > currentScore -> candidate
            candidateScore < currentScore -> current
            else -> {
                val currentClosed = parseInstant(current.closed_at)?.toEpochMilliseconds() ?: Long.MIN_VALUE
                val candidateClosed = parseInstant(candidate.closed_at)?.toEpochMilliseconds() ?: Long.MIN_VALUE
                if (candidateClosed >= currentClosed) candidate else current
            }
        }
    }

    private fun pullRequestSnapshotScore(
        pullRequest: ProjectPullRequestSnapshot,
    ): Int {
        return listOf(
            pullRequest.closed_at,
            pullRequest.title,
            pullRequest.state,
            pullRequest.html_url,
        ).count { !it.isNullOrBlank() } +
            listOf(
                pullRequest.number,
                pullRequest.comments,
                pullRequest.commits,
                pullRequest.additions,
                pullRequest.deletions,
                pullRequest.changed_files,
            ).count { it != null }
    }

    private fun deduplicateIssues(
        issues: List<ProjectIssueSnapshot>,
    ): List<ProjectIssueSnapshot> {
        return issues
            .groupBy(::issueIdentityKey)
            .values
            .map { duplicates ->
                duplicates.reduce(::preferIssueSnapshot)
            }
    }

    private fun issueIdentityKey(
        issue: ProjectIssueSnapshot,
    ): String {
        val number = issue.number
        if (number != null) return "number:$number"

        val url = issue.html_url?.trim()?.lowercase()
        if (!url.isNullOrBlank()) return "url:$url"

        val author = issue.user?.login?.trim()?.lowercase().orEmpty()
        val title = issue.title?.trim()?.lowercase().orEmpty()
        val createdAt = issue.created_at?.trim().orEmpty()
        return "fallback:$author|$title|$createdAt"
    }

    private fun preferIssueSnapshot(
        current: ProjectIssueSnapshot,
        candidate: ProjectIssueSnapshot,
    ): ProjectIssueSnapshot {
        val currentScore = issueSnapshotScore(current)
        val candidateScore = issueSnapshotScore(candidate)
        return when {
            candidateScore > currentScore -> candidate
            candidateScore < currentScore -> current
            else -> {
                val currentClosed = parseInstant(current.closed_at)?.toEpochMilliseconds() ?: Long.MIN_VALUE
                val candidateClosed = parseInstant(candidate.closed_at)?.toEpochMilliseconds() ?: Long.MIN_VALUE
                if (candidateClosed >= currentClosed) candidate else current
            }
        }
    }

    private fun issueSnapshotScore(
        issue: ProjectIssueSnapshot,
    ): Int {
        return listOf(
            issue.created_at,
            issue.closed_at,
            issue.title,
            issue.state,
            issue.html_url,
        ).count { !it.isNullOrBlank() } +
            listOf(
                issue.number,
                issue.comments,
                issue.reactions?.plusOne,
                issue.reactions?.minusOne,
            ).count { it != null } +
            if (issue.labels.isNotEmpty()) 1 else 0 +
            if (issue.assignees.isNotEmpty()) 1 else 0
    }

    private fun buildCodeOwnershipContributors(
        commits: List<ProjectCommitSnapshot>,
        currentUserLogin: String?,
        displayNameResolver: (String) -> String,
    ): List<ProjectStatsContributorStat> {
        val lines = linkedMapOf<String, Int>()
        commits.forEach { commit ->
            val login = commit.author?.login?.trim().orEmpty()
            if (login.isBlank()) return@forEach

            val delta = commit.files.sumOf { file ->
                val additions = file.additions ?: 0
                val deletions = file.deletions ?: 0
                // changes = additions + deletions в GitHub API — не суммируем, иначе двойной счёт
                additions + deletions
            }

            if (delta <= 0) return@forEach
            lines[login] = (lines[login] ?: 0) + delta
        }

        return lines.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.lowercase() })
            .map { (login, value) ->
                ProjectStatsContributorStat(
                    login = login,
                    name = displayNameResolver(login),
                    value = value,
                    displayValue = "$value ${linesLabel(value)}",
                    isCurrentUser = currentUserLogin?.equals(login, ignoreCase = true) == true,
                )
            }
    }

    private fun buildFileStats(commits: List<ProjectCommitSnapshot>): List<ProjectFileStat> {
        val fileChanges = linkedMapOf<String, Int>()
        commits.forEach { commit ->
            commit.files.forEach { file ->
                val filename = file.filename?.trim().orEmpty()
                if (filename.isBlank()) return@forEach

                val changes = file.changes ?: ((file.additions ?: 0) + (file.deletions ?: 0))
                if (changes <= 0) return@forEach

                fileChanges[filename] = (fileChanges[filename] ?: 0) + changes
            }
        }

        return fileChanges.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.lowercase() })
            .map { (filename, changes) ->
                ProjectFileStat(fileName = filename, changes = changes)
            }
    }

    private fun buildWeekDays(
        commits: List<ProjectCommitSnapshot>,
        issues: List<ProjectIssueSnapshot>,
        pullRequests: List<ProjectPullRequestSnapshot>,
    ): List<ProjectWeekdayStat> {
        val labels = listOf(
            weekdayName("monday"),
            weekdayName("tuesday"),
            weekdayName("wednesday"),
            weekdayName("thursday"),
            weekdayName("friday"),
            weekdayName("saturday"),
            weekdayName("sunday"),
        )
        val counts = linkedMapOf<String, Int>().apply {
            labels.forEach { put(it, 0) }
        }

        fun bump(date: String?, delta: Int = 1) {
            val label = weekdayLabel(date) ?: return
            counts[label] = (counts[label] ?: 0) + delta
        }

        commits.forEach { bump(it.commit?.author?.date) }
        issues.forEach {
            bump(it.created_at)
            if (!it.closed_at.isNullOrBlank()) {
                bump(it.closed_at)
            }
        }
        pullRequests.forEach {
            bump(it.created_at)
            if (!it.closed_at.isNullOrBlank()) {
                bump(it.closed_at)
            }
        }

        val dominant = counts.maxByOrNull { it.value }?.key
        return counts.entries.map { (label, value) ->
            ProjectWeekdayStat(
                label = label,
                value = value,
                isDominant = label == dominant,
            )
        }
    }

    private fun buildChartPoints(
        dates: List<Instant>,
        hintFormatter: (Int) -> String,
    ): List<ProjectStatsChartPointUi> {
        if (dates.isEmpty()) return emptyList()

        return dates
            .groupingBy { it.toEpochMilliseconds() / MILLIS_PER_DAY }
            .eachCount()
            .entries
            .sortedBy { it.key }
            .map { entry ->
                ProjectStatsChartPointUi(
                    label = formatDateLabel(entry.key),
                    value = entry.value.toFloat(),
                    valueLabel = entry.value.toString(),
                    hint = hintFormatter(entry.value),
                )
            }
    }

    private fun buildVisibleRange(
        selectedStartDate: String?,
        selectedEndDate: String?,
        snapshot: ProjectStatsResourceSnapshot,
        project: ProjectDetail,
    ): ProjectStatsDateRangeUi {
        val startDate = selectedStartDate?.let(::parseIsoDate)
            ?: snapshot.visibleStart?.toLocalDateTime(TimeZone.UTC)?.date
            ?: parseFlexibleDate(project.dateStart)
            ?: fallbackVisibleDate()
        val endDate = selectedEndDate?.let(::parseIsoDate)
            ?: snapshot.visibleEnd?.toLocalDateTime(TimeZone.UTC)?.date
            ?: parseFlexibleDate(project.dateEnd)
            ?: startDate

        return ProjectStatsDateRangeUi(
            startIsoDate = formatIsoDate(startDate),
            endIsoDate = formatIsoDate(endDate),
            startLabel = formatLocalDateLabel(startDate),
            endLabel = formatLocalDateLabel(endDate),
        )
    }

    private fun buildMembers(
        members: List<Member>,
        users: List<User>,
        metricUsers: List<MetricProjectUser>,
        currentUserName: String?,
        currentUserLogin: String?,
    ): List<ProjectStatsMemberUi> {
        val usersById = users.associateBy { it.id }
        val metricFallback = metricUsers.map { metricUser ->
            ProjectStatsMemberUi(
                userId = users.firstOrNull { user ->
                    personNameMatches(user.name, metricUser.name)
                }?.id,
                login = metricUser.githubLogin(),
                name = metricUser.name.trim(),
                role = metricUser.roles.joinToString(", ").trim().ifBlank { participantLabel() },
                isCurrentUser = isCurrentUser(
                    displayName = metricUser.name,
                    currentUserName = currentUserName,
                    currentUserLogin = currentUserLogin,
                    metricUser = metricUser,
                ),
            )
        }

        val projectMembers = members.map { member ->
            val resolvedName = member.user
                ?.toString()
                ?.let(usersById::get)
                ?.name
                ?.trim()
                .takeUnless { it.isNullOrBlank() }
                ?: member.name.trim()

            val role = member.role?.trim().takeUnless { it.isNullOrBlank() }
                ?: metricUsers.firstOrNull { metricUser ->
                    metricUser.name.trim().equals(resolvedName, ignoreCase = true)
                }?.roles?.joinToString(", ").orEmpty().ifBlank { "Участник" }
            val metricUser = metricUsers.firstOrNull { candidate ->
                candidate.name.trim().equals(resolvedName, ignoreCase = true)
            }

            ProjectStatsMemberUi(
                userId = member.user?.toString(),
                login = metricUser?.githubLogin(),
                name = resolvedName,
                role = role,
                isCurrentUser = isCurrentUser(
                    displayName = resolvedName,
                    currentUserName = currentUserName,
                    currentUserLogin = currentUserLogin,
                    metricUser = metricUser,
                ),
            )
        }

        val combined = (projectMembers + metricFallback)
            .distinctBy { it.name.trim().lowercase() }

        return if (combined.isNotEmpty()) combined else metricFallback
    }

    private fun formatAveragePullRequestLifetime(
        pullRequests: List<ProjectPullRequestSnapshot>,
    ): String? {
        val durations = pullRequests.mapNotNull(::pullRequestLifetimeMillis)
        if (durations.isEmpty()) return null
        return formatDurationMillisLabel(durations.average())
    }

    private fun openedPullRequestsLabel(count: Int): String {
        return localizeRuntime(
            if (count == 1) "открытый PR" else "открытых PR",
            if (count == 1) "open PR" else "open PRs",
        )
    }

    private fun buildRepositories(resources: List<MetricProjectResource>): List<ProjectStatsRepositoryUi> {
        return resources.map { resource ->
            val repositoryUrl = extractStringParam(resource, "url")
                ?: extractStringParam(resource, "apiEndpoint")
            ProjectStatsRepositoryUi(
                id = resource.id,
                title = repositoryUrl?.trim()?.takeIf { it.isNotBlank() }
                    ?: resource.name.ifBlank { repositoryLabel() },
                subtitle = resource.platform?.trim()?.takeIf { it.isNotBlank() }
                    ?: resource.project?.trim()?.takeIf { it.isNotBlank() }
                    ?: "GitHub",
            )
        }
    }

    private fun buildRequestKey(
        projectId: String,
        repositoryId: String?,
        startDate: String?,
        endDate: String?,
        rapidThresholdMinutes: Int?,
    ): String {
        return listOf(
            projectId,
            repositoryId.orEmpty(),
            startDate.orEmpty(),
            endDate.orEmpty(),
            rapidThresholdMinutes?.toString().orEmpty(),
        ).joinToString("|")
    }

    private fun resolveProject(
        projectId: String,
        projectResponse: ProjectDetailResponse?,
        metricDetail: MetricProjectDetail?,
    ): ProjectDetail {
        return projectResponse?.project ?: ProjectDetail(
            id = metricDetail?.id ?: projectId,
            name = metricDetail?.name ?: projectId,
            description = metricDetail?.description,
            shortDescription = null,
            dateStart = metricDetail?.dateStart,
            dateEnd = metricDetail?.dateEnd,
            slug = projectId,
            tags = emptyList(),
            team = null,
            status = null,
            teamLimit = null,
            client = null,
            contact = null,
            requirements = null,
            executorRequirements = null,
        )
    }

    private fun resolveMetricDetail(
        project: ProjectDetail,
        metricDetail: MetricProjectDetail?,
    ): MetricProjectDetail {
        return metricDetail ?: MetricProjectDetail(
            id = project.id,
            name = project.name,
            description = project.description,
            dateStart = project.dateStart,
            dateEnd = project.dateEnd,
            users = emptyList(),
            resources = emptyList(),
        )
    }

    private suspend fun fetchMetricProjectDetail(
        projectKey: String,
        fallbackProjectId: String?,
    ): MetricProjectDetail? {
        metricApi.getProjectDetail(projectKey).getOrNull()?.let { return it }

        val fallbackId = fallbackProjectId
            ?.trim()
            ?.takeIf { it.isNotBlank() && it != projectKey }
            ?: return null

        return metricApi.getProjectDetail(fallbackId).getOrNull()
    }

    private fun resolveRapidThresholdMinutes(
        selectedRapidThresholdMinutes: Int?,
        primaryResource: MetricProjectResource?,
    ): Int {
        return selectedRapidThresholdMinutes
            ?.takeIf { it > 0 }
            ?: parseRapidThresholdMinutes(primaryResource)
            ?: DEFAULT_RAPID_THRESHOLD_MINUTES
    }

    private fun buildRapidThreshold(totalMinutes: Int): ProjectStatsThresholdUi {
        val safeMinutes = totalMinutes.coerceAtLeast(1)
        val days = safeMinutes / MINUTES_PER_DAY
        val hours = (safeMinutes % MINUTES_PER_DAY) / 60
        val minutes = safeMinutes % 60
        return ProjectStatsThresholdUi(
            totalMinutes = safeMinutes,
            days = days,
            hours = hours,
            minutes = minutes,
        )
    }

    private fun selectResource(
        resources: List<MetricProjectResource>,
        selectedRepositoryId: String?,
    ): MetricProjectResource? {
        if (resources.isEmpty()) return null

        val selectedId = selectedRepositoryId?.trim().orEmpty()
        if (selectedId.isNotBlank()) {
            resources.firstOrNull { it.id == selectedId }?.let { return it }
        }

        return resources.firstOrNull { it.platform?.isNotBlank() == true } ?: resources.first()
    }

    private fun resolveWindow(
        selectedStartDate: String?,
        selectedEndDate: String?,
        eventDates: List<Instant>,
    ): Pair<Instant, Instant>? {
        val start = selectedStartDate?.let(::startOfDay)
            ?: eventDates.minOrNull()
        val end = selectedEndDate?.let(::endOfDay)
            ?: eventDates.maxOrNull()

        if (start == null || end == null) return null
        return if (start <= end) start to end else end to start
    }

    private fun isInWindow(date: Instant?, window: Pair<Instant, Instant>?): Boolean {
        if (date == null || window == null) return true
        val (start, end) = window
        val value = date.toEpochMilliseconds()
        return value in start.toEpochMilliseconds()..end.toEpochMilliseconds()
    }

    private fun totalCommitsScore(commits: List<ProjectCommitSnapshot>): Double? {
        if (commits.isEmpty()) return null

        val dates = commits.mapNotNull { parseInstant(it.commit?.author?.date)?.toEpochMilliseconds() }
        val start = dates.minOrNull() ?: return null
        val end = dates.maxOrNull() ?: return null
        val dayCount = (end - start).toDouble() / MILLIS_PER_DAY
        if (dayCount <= 0) return 5.0

        val userCount = commits.mapNotNull { it.author?.login?.trim()?.lowercase() }.distinct().size
        if (userCount <= 0) return 5.0

        val commitsPerDay = commits.size / dayCount / userCount
        return round2((commitsPerDay * 9) + 2).coerceAtMost(5.0)
    }

    private fun issueCompletenessScore(issues: List<ProjectIssueSnapshot>): Double? {
        if (issues.isEmpty()) return null
        val closed = issues.count { !it.closed_at.isNullOrBlank() }
        return round2((closed.toDouble() / issues.size) * 3 + 2)
    }

    private fun pullRequestHangScore(pullRequests: List<ProjectPullRequestSnapshot>): Double? {
        val durations = pullRequests.mapNotNull(::pullRequestLifetimeMillis)

        if (durations.isEmpty()) return null

        val averageHangTime = durations.sum() / durations.size
        return if (averageHangTime < 5 * MILLIS_PER_MINUTE) {
            round2(averageHangTime.toDouble() / MILLIS_PER_MINUTE)
        } else {
            round2(((1 - averageHangTime.toDouble() / (7 * MILLIS_PER_DAY)) * 3 + 2).coerceAtLeast(0.0))
        }
    }

    private fun rapidPullScore(
        pullRequests: List<ProjectPullRequestSnapshot>,
        thresholdMinutes: Int,
    ): Double? {
        if (pullRequests.isEmpty()) return null

        val thresholdMs = thresholdMinutes * MILLIS_PER_MINUTE
        val rapidCount = pullRequests.count { isRapidPullRequest(it, thresholdMs) }
        return round2((1 - rapidCount.toDouble() / pullRequests.size) * 3 + 2)
    }

    private fun codeOwnershipScore(commits: List<ProjectCommitSnapshot>): Double? {
        val userLines = linkedMapOf<String, Int>()
        var totalLines = 0

        commits.forEach { commit ->
            val login = commit.author?.login?.trim().orEmpty()
            if (login.isBlank()) return@forEach

            commit.files.forEach { file ->
                val additions = file.additions ?: 0
                val deletions = file.deletions ?: 0
                // changes = additions + deletions в GitHub API — не суммируем
                val lineDelta = additions + deletions
                if (lineDelta <= 0) return@forEach

                userLines[login] = (userLines[login] ?: 0) + lineDelta
                totalLines += lineDelta
            }
        }

        if (totalLines <= 0 || userLines.size < 2) return null

        val normalized = userLines.values.map { it.toDouble() / totalLines }
        val averageShare = 1 / userLines.size.toDouble()
        val worstCase = (1 - averageShare) * (1 - averageShare) + (userLines.size - 1) * (averageShare * averageShare)
        if (worstCase <= 0) return null

        val dispersion = normalized.sumOf { (it - averageShare) * (it - averageShare) }
        val gradeComponent = (1 - sqrt(dispersion / worstCase)) * 3
        return round2((gradeComponent + 2).coerceIn(0.0, 5.0))
    }

    private fun weekDayScore(weekdays: List<ProjectWeekdayStat>): Double? {
        if (weekdays.isEmpty()) return null
        val dominant = weekdays.maxByOrNull { it.value } ?: return null
        val total = weekdays.sumOf { it.value }
        if (total <= 0) return null

        val dominantShare = dominant.value.toDouble() / total.toDouble()
        return round2((2 + 3 * dominantShare).coerceIn(0.0, 5.0))
    }

    private fun codeChurnScore(
        fileStats: List<ProjectFileStat>,
        commitCount: Int,
    ): Double? {
        if (fileStats.isEmpty() || commitCount <= 0) return null
        val churnPerCommit = fileStats.sumOf { it.changes }.toDouble() / commitCount
        return round2((5.0 - ln(1.0 + churnPerCommit).coerceAtMost(4.0) * 1.2).coerceIn(0.0, 5.0))
    }

    private fun buildRank(currentScore: Double?, peerScores: List<Double?>): Int? {
        if (currentScore == null) return null
        val ranked = (peerScores + currentScore).filterNotNull().sortedDescending()
        return ranked.indexOfFirst { it == currentScore }.takeIf { it >= 0 }?.plus(1)
    }

    private fun parseRapidThresholdMinutes(resource: MetricProjectResource?): Int? {
        val metric = resource?.metrics?.firstOrNull { it.name == "Rapid Pull Requests" } ?: return null
        val params = metric.params
        val threshold = params.firstOrNull { it.name == "rapidPullRequestsThreshold" }?.value ?: return null
        val obj = runCatching { threshold.jsonObject }.getOrNull() ?: return null
        val number = obj["number"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
            ?: obj["value"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
        val unit = obj["unitOfTime"]?.jsonPrimitive?.contentOrNull
        if (number == null || unit.isNullOrBlank()) return null

        return when {
            unit.contains("second", ignoreCase = true) -> (number / 60.0).roundToInt()
            unit.contains("minute", ignoreCase = true) -> number.roundToInt()
            unit.contains("hour", ignoreCase = true) -> (number * 60).roundToInt()
            unit.contains("day", ignoreCase = true) -> (number * 60 * 24).roundToInt()
            else -> null
        }
    }

    private fun isRapidPullRequest(
        pullRequest: ProjectPullRequestSnapshot,
        thresholdMs: Long,
    ): Boolean {
        val created = parseInstant(pullRequest.created_at)?.toEpochMilliseconds() ?: return false
        val closed = pullRequestCompletedAtMillis(pullRequest) ?: return false
        return closed - created < thresholdMs
    }

    private fun pullRequestLifetimeMillis(
        pullRequest: ProjectPullRequestSnapshot,
        nowMillis: Long = PlatformTime.currentTimeMillis(),
    ): Long? {
        val created = parseInstant(pullRequest.created_at)?.toEpochMilliseconds() ?: return null
        val end = pullRequestCompletedAtMillis(pullRequest)
            ?: nowMillis.takeIf { pullRequest.state.equals("open", ignoreCase = true) }
            ?: return null
        if (end <= created) return null
        return end - created
    }

    private fun pullRequestCompletedAtIso(
        pullRequest: ProjectPullRequestSnapshot,
    ): String? {
        val candidates = if (pullRequest.state.equals("open", ignoreCase = true)) {
            listOf(pullRequest.merged_at, pullRequest.pull_request?.merged_at, pullRequest.closed_at)
        } else {
            listOf(
                pullRequest.merged_at,
                pullRequest.pull_request?.merged_at,
                pullRequest.closed_at,
                pullRequest.updated_at,
            )
        }
        return candidates.firstOrNull { parseInstant(it) != null }
    }

    private fun pullRequestCompletedAtMillis(
        pullRequest: ProjectPullRequestSnapshot,
    ): Long? {
        return pullRequestCompletedAtIso(pullRequest)
            ?.let(::parseInstant)
            ?.toEpochMilliseconds()
    }

    private fun weekdayLabel(date: String?): String? {
        val instant = parseInstant(date) ?: return null
        return when (instant.toLocalDateTime(TimeZone.UTC).dayOfWeek.name.lowercase()) {
            "monday" -> weekdayName("monday")
            "tuesday" -> weekdayName("tuesday")
            "wednesday" -> weekdayName("wednesday")
            "thursday" -> weekdayName("thursday")
            "friday" -> weekdayName("friday")
            "saturday" -> weekdayName("saturday")
            "sunday" -> weekdayName("sunday")
            else -> null
        }
    }

    private fun formatDateLabel(instant: Instant): String {
        return formatLocalDateLabel(instant.toLocalDateTime(TimeZone.UTC).date)
    }

    private fun formatDateLabel(dayBucket: Long): String {
        val instant = Instant.fromEpochMilliseconds(dayBucket * MILLIS_PER_DAY)
        return formatDateLabel(instant)
    }

    private fun formatDateTimeLabel(value: String?): String {
        val instant = parseInstant(value) ?: return "—"
        val dateTime = instant.toLocalDateTime(TimeZone.UTC)
        val day = dateTime.date.dayOfMonth.toString().padStart(2, '0')
        val month = dateTime.date.monthNumber.toString().padStart(2, '0')
        val year = (dateTime.date.year % 100).toString().padStart(2, '0')
        val hour = dateTime.hour.toString().padStart(2, '0')
        val minute = dateTime.minute.toString().padStart(2, '0')
        return "$day.$month.$year $hour:$minute"
    }

    private fun formatLocalDateLabel(date: LocalDate): String {
        val day = date.dayOfMonth.toString().padStart(2, '0')
        val month = date.monthNumber.toString().padStart(2, '0')
        val year = (date.year % 100).toString().padStart(2, '0')
        return "$day.$month.$year"
    }

    private fun formatIsoDate(date: LocalDate): String {
        val month = date.monthNumber.toString().padStart(2, '0')
        val day = date.dayOfMonth.toString().padStart(2, '0')
        return "${date.year}-$month-$day"
    }

    private fun percentLabel(value: Int, total: Int): String {
        if (total <= 0) return "0%"
        val percent = (value.toDouble() / total.toDouble()) * 100.0
        return "${percent.roundToInt()}%"
    }

    private fun parseInstant(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        return runCatching { Instant.parse(value) }.getOrNull()
    }

    private fun parseIsoDate(value: String): LocalDate? {
        return runCatching { LocalDate.parse(value) }.getOrNull()
    }

    private fun parseFlexibleDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        return parseIsoDate(value.take(10))
            ?: parseInstant(value)?.toLocalDateTime(TimeZone.UTC)?.date
    }

    private fun startOfDay(isoDate: String): Instant? {
        val date = parseIsoDate(isoDate) ?: return null
        return date.atStartOfDayIn(TimeZone.UTC)
    }

    private fun endOfDay(isoDate: String): Instant? {
        val start = startOfDay(isoDate) ?: return null
        return Instant.fromEpochMilliseconds(start.toEpochMilliseconds() + MILLIS_PER_DAY - 1L)
    }

    private fun fallbackVisibleDate(): LocalDate {
        return LocalDate(2000, 1, 1)
    }

    private fun pluralize(
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

    private fun extractStringParam(resource: MetricProjectResource, name: String): String? {
        val metric = resource.metrics.firstOrNull { metric ->
            metric.params.any { it.name == name }
        } ?: return null

        return metric.params.firstOrNull { it.name == name }
            ?.value
            ?.jsonPrimitive
            ?.contentOrNull
            ?.takeIf { it.isNotBlank() }
    }

    private inline fun <reified T> extractSnapshots(metric: MetricProjectMetric?): List<T> {
        return metric?.data.orEmpty().mapNotNull { snapshot ->
            val data = snapshot.data ?: return@mapNotNull null
            runCatching { json.decodeFromJsonElement<T>(data) }.getOrNull()
        }
    }

    private fun buildUserNameLookup(
        projectUsers: List<User>,
        metricUsers: List<MetricProjectUser>,
    ): Map<String, String> {
        val lookup = linkedMapOf<String, String>()

        metricUsers.forEach { user ->
            val displayName = user.name.trim()
            val login = user.identifiers.firstOrNull {
                it.platform.equals("GitHub", ignoreCase = true)
            }?.value?.trim()

            if (!login.isNullOrBlank()) {
                lookup[login.lowercase()] = displayName
            }
        }

        projectUsers.forEach { user ->
            val name = user.name?.trim().orEmpty()
            if (name.isBlank()) return@forEach
            lookup[user.id.trim().lowercase()] = name
        }

        return lookup
    }

    private fun resolveCurrentUserLogin(
        metricUsers: List<MetricProjectUser>,
        currentUserName: String?,
    ): String? {
        if (currentUserName.isNullOrBlank()) return null

        return metricUsers.firstOrNull { user ->
            user.name.trim().equals(currentUserName, ignoreCase = true)
        }
            ?.identifiers
            ?.firstOrNull { it.platform.equals("GitHub", ignoreCase = true) }
            ?.value
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun MetricProjectUser.githubLogin(): String? {
        return identifiers.firstOrNull { it.platform.equals("GitHub", ignoreCase = true) }
            ?.value
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun isCurrentUser(
        displayName: String,
        currentUserName: String?,
        currentUserLogin: String?,
        metricUser: MetricProjectUser? = null,
    ): Boolean {
        if (!currentUserName.isNullOrBlank() && displayName.equals(currentUserName, ignoreCase = true)) {
            return true
        }

        if (metricUser != null && !currentUserLogin.isNullOrBlank()) {
            val login = metricUser.identifiers.firstOrNull {
                it.platform.equals("GitHub", ignoreCase = true)
            }?.value
            if (!login.isNullOrBlank() && login.equals(currentUserLogin, ignoreCase = true)) {
                return true
            }
        }

        return false
    }

    private data class ProjectStatsContributorStat(
        val login: String,
        val name: String,
        val value: Int,
        val displayValue: String? = null,
        val isCurrentUser: Boolean = false,
    )

    private data class ProjectFileStat(
        val fileName: String,
        val changes: Int,
    )

    private data class ProjectWeekdayStat(
        val label: String,
        val value: Int,
        val isDominant: Boolean,
    )

    private data class ProjectStatsResourceSnapshot(
        val resourceId: String,
        val resourceName: String,
        val platform: String?,
        val url: String?,
        val commitCount: Int,
        val issueCount: Int,
        val openIssueCount: Int,
        val closedIssueCount: Int,
        val pullRequestCount: Int,
        val rapidPullRequestCount: Int,
        val commitContributors: List<ProjectStatsContributorStat>,
        val issueContributors: List<ProjectStatsContributorStat>,
        val pullRequestContributors: List<ProjectStatsContributorStat>,
        val codeOwnershipContributors: List<ProjectStatsContributorStat>,
        val fileStats: List<ProjectFileStat>,
        val weekdays: List<ProjectWeekdayStat>,
        val weekdayTotal: Int,
        val dominantWeekdayLabel: String?,
        val totalLines: Int,
        val commitsScore: Double?,
        val issueScore: Double?,
        val pullRequestScore: Double?,
        val rapidPullScore: Double?,
        val codeOwnershipScore: Double?,
        val weekDayScore: Double?,
        val codeChurnScore: Double?,
        val commitChart: List<ProjectStatsChartPointUi>,
        val pullRequestChart: List<ProjectStatsChartPointUi>,
        val rapidPullRequestChart: List<ProjectStatsChartPointUi>,
        val visibleStart: Instant?,
        val visibleEnd: Instant?,
        val rapidThresholdMinutes: Int,
        val commits: List<ProjectCommitSnapshot>,
        val issues: List<ProjectIssueSnapshot>,
        val pullRequests: List<ProjectPullRequestSnapshot>,
    ) {
        companion object {
            fun empty(rapidThresholdMinutes: Int): ProjectStatsResourceSnapshot {
                return ProjectStatsResourceSnapshot(
                    resourceId = "",
                    resourceName = "",
                    platform = null,
                    url = null,
                    commitCount = 0,
                    issueCount = 0,
                    openIssueCount = 0,
                    closedIssueCount = 0,
                    pullRequestCount = 0,
                    rapidPullRequestCount = 0,
                    commitContributors = emptyList(),
                    issueContributors = emptyList(),
                    pullRequestContributors = emptyList(),
                    codeOwnershipContributors = emptyList(),
                    fileStats = emptyList(),
                    weekdays = emptyList(),
                    weekdayTotal = 0,
                    dominantWeekdayLabel = null,
                    totalLines = 0,
                    commitsScore = null,
                    issueScore = null,
                    pullRequestScore = null,
                    rapidPullScore = null,
                    codeOwnershipScore = null,
                    weekDayScore = null,
                    codeChurnScore = null,
                    commitChart = emptyList(),
                    pullRequestChart = emptyList(),
                    rapidPullRequestChart = emptyList(),
                    visibleStart = null,
                    visibleEnd = null,
                    rapidThresholdMinutes = rapidThresholdMinutes,
                    commits = emptyList(),
                    issues = emptyList(),
                    pullRequests = emptyList(),
                )
            }
        }
    }

    @Serializable
    private data class ProjectCommitSnapshot(
        val author: ProjectCommitAuthorSnapshot? = null,
        val commit: ProjectCommitDataSnapshot? = null,
        val files: List<ProjectCommitFileSnapshot> = emptyList(),
        val html_url: String? = null,
        val sha: String? = null,
    )

    @Serializable
    private data class ProjectCommitAuthorSnapshot(
        val login: String? = null,
        val avatar_url: String? = null,
    )

    @Serializable
    private data class ProjectCommitDataSnapshot(
        val author: ProjectCommitCommitAuthorSnapshot? = null,
        val message: String? = null,
    )

    @Serializable
    private data class ProjectCommitCommitAuthorSnapshot(
        val date: String? = null,
    )

    @Serializable
    private data class ProjectCommitFileSnapshot(
        val filename: String? = null,
        val additions: Int? = null,
        val deletions: Int? = null,
        val changes: Int? = null,
        val status: String? = null,
    )

    @Serializable
    private data class ProjectIssueSnapshot(
        val user: ProjectSnapshotUser? = null,
        val assignees: List<ProjectSnapshotUser> = emptyList(),
        val created_at: String? = null,
        val closed_at: String? = null,
        val title: String? = null,
        val number: Int? = null,
        val state: String? = null,
        val comments: Int? = null,
        val reactions: ProjectIssueReactionsSnapshot? = null,
        val html_url: String? = null,
        val labels: List<ProjectSnapshotLabel> = emptyList(),
    )

    @Serializable
    private data class ProjectIssueReactionsSnapshot(
        @SerialName("+1")
        val plusOne: Int? = null,
        @SerialName("-1")
        val minusOne: Int? = null,
    )

    @Serializable
    private data class ProjectPullRequestSnapshot(
        val user: ProjectSnapshotUser? = null,
        val assignees: List<ProjectSnapshotUser> = emptyList(),
        val created_at: String? = null,
        val closed_at: String? = null,
        val merged_at: String? = null,
        val pull_request: ProjectPullRequestMeta? = null,
        val updated_at: String? = null,
        val title: String? = null,
        val number: Int? = null,
        val state: String? = null,
        val comments: Int? = null,
        val commits: Int? = null,
        val additions: Int? = null,
        val deletions: Int? = null,
        val changed_files: Int? = null,
        val html_url: String? = null,
    )

    @Serializable
    private data class ProjectPullRequestMeta(
        val merged_at: String? = null,
    )

    @Serializable
    private data class ProjectSnapshotUser(
        val login: String? = null,
        val avatar_url: String? = null,
    )

    @Serializable
    private data class ProjectSnapshotLabel(
        val name: String? = null,
    )

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
        const val MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND
        const val MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE
        const val MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR
        const val MINUTES_PER_DAY = 24 * 60
        const val DEFAULT_RAPID_THRESHOLD_MINUTES = 5
        const val METRIC_COMMITS = "Commits"
        const val METRIC_ISSUES = "Issues"
        const val METRIC_PULL_REQUESTS = "Pull Requests"
        const val METRIC_RAPID_PULL_REQUESTS = "Rapid Pull Requests"

        val ownershipPalette = listOf(
            0xFFC21807L,
            0xFF0B73D9L,
            0xFF11B78BL,
            0xFF6821D8L,
            0xFFC69207L,
            0xFF0BA7D9L,
            0xFF2A18D8L,
        )

        val weekdayPalette = listOf(
            0xFFC21807L,
            0xFFC69207L,
            0xFF0BA7D9L,
            0xFF6821D8L,
            0xFFC2C807L,
            0xFF11B78BL,
            0xFF2A18D8L,
        )
    }

    private fun buildFileChurnSlices(
        fileStats: List<ProjectFileStat>,
    ): List<ProjectStatsDonutSliceUi> {
        if (fileStats.isEmpty()) return emptyList()
        val buckets = listOf(
            localizeRuntime("1 изменение", "1 change") to fileStats.count { it.changes == 1 },
            localizeRuntime("2-3 изменения", "2-3 changes") to fileStats.count { it.changes in 2..3 },
            localizeRuntime("4-5 изменений", "4-5 changes") to fileStats.count { it.changes in 4..5 },
            localizeRuntime("6-7 изменений", "6-7 changes") to fileStats.count { it.changes in 6..7 },
            localizeRuntime("8-10 изменений", "8-10 changes") to fileStats.count { it.changes in 8..10 },
            localizeRuntime(">10 изменений", ">10 changes") to fileStats.count { it.changes > 10 },
        ).filter { it.second > 0 }
        val total = buckets.sumOf { it.second }.takeIf { it > 0 } ?: 1
        return buckets.mapIndexed { index, (label, value) ->
            ProjectStatsDonutSliceUi(
                label = label,
                secondaryLabel = "$value ${filesLabel(value)}",
                percentLabel = round2(value * 100.0 / total).toString() + "%",
                value = value.toFloat(),
                colorHex = weekdayPalette[index % weekdayPalette.size],
                highlight = value == buckets.maxOf { it.second },
            )
        }
    }

    private fun commitsLabel(count: Int): String =
        localizePluralRuntime(count, "коммит", "коммита", "коммитов", "commit", "commits")

    private fun actionsLabel(count: Int): String =
        localizePluralRuntime(count, "действие", "действия", "действий", "action", "actions")

    private fun filesLabel(count: Int): String =
        localizePluralRuntime(count, "файл", "файла", "файлов", "file", "files")

    private fun linesLabel(count: Int): String =
        localizePluralRuntime(count, "строка", "строки", "строк", "line", "lines")

    private fun participantLabel(): String = localizeRuntime("Участник", "Participant")

    private fun repositoryLabel(): String = localizeRuntime("Репозиторий", "Repository")

    private fun weekdayName(day: String): String = weekdayNameOrNull(day) ?: day.replaceFirstChar(Char::uppercaseChar)

    private fun weekdayNameOrNull(day: String): String? {
        return when (day) {
            "monday" -> localizeRuntime("Понедельник", "Monday")
            "tuesday" -> localizeRuntime("Вторник", "Tuesday")
            "wednesday" -> localizeRuntime("Среда", "Wednesday")
            "thursday" -> localizeRuntime("Четверг", "Thursday")
            "friday" -> localizeRuntime("Пятница", "Friday")
            "saturday" -> localizeRuntime("Суббота", "Saturday")
            "sunday" -> localizeRuntime("Воскресенье", "Sunday")
            else -> null
        }
    }
}

private fun round2(value: Double): Double = ((value * 100.0).roundToInt() / 100.0)
