package com.spbu.projecttrack.rating.data.repository

import com.spbu.projecttrack.core.time.PlatformTime
import com.spbu.projecttrack.projects.data.api.ProjectsApi
import com.spbu.projecttrack.projects.data.model.Member
import com.spbu.projecttrack.projects.data.model.Project
import com.spbu.projecttrack.projects.data.model.ProjectDetail
import com.spbu.projecttrack.projects.data.model.ProjectDetailResponse
import com.spbu.projecttrack.projects.data.model.User
import com.spbu.projecttrack.rating.data.api.MetricApi
import com.spbu.projecttrack.rating.data.model.MetricProjectDetail
import com.spbu.projecttrack.rating.data.model.MetricProjectMetric
import com.spbu.projecttrack.rating.data.model.MetricProjectResource
import com.spbu.projecttrack.rating.data.model.MetricProjectUser
import com.spbu.projecttrack.rating.data.model.ProjectStatsChartPointUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsChartType
import com.spbu.projecttrack.rating.data.model.ProjectStatsCodeChurnSectionUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsDateRangeUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsDonutSliceUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsFileRowUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsIssueSectionUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsMetricRowUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsMetricSectionUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsOwnershipSectionUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsRepositoryUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsThresholdUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsWeekDaySectionUi
import com.spbu.projecttrack.rating.data.model.StatsDetailCommitFileUi
import com.spbu.projecttrack.rating.data.model.StatsDetailCommitUi
import com.spbu.projecttrack.rating.data.model.StatsDetailDataUi
import com.spbu.projecttrack.rating.data.model.StatsDetailIssueUi
import com.spbu.projecttrack.rating.data.model.StatsDetailParticipantUi
import com.spbu.projecttrack.rating.data.model.StatsDetailPullRequestUi
import com.spbu.projecttrack.rating.data.model.UserStatsUiModel
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
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.roundToInt

class UserStatsRepository(
    private val metricApi: MetricApi,
    private val projectsApi: ProjectsApi,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var cachedKey: String? = null
    private var cachedModel: UserStatsUiModel? = null

    suspend fun loadUserStats(
        userId: String,
        fallbackUserName: String,
        preferredProjectName: String? = null,
        selectedRepositoryId: String? = null,
        selectedStartDate: String? = null,
        selectedEndDate: String? = null,
        selectedRapidThresholdMinutes: Int? = null,
        forceRefresh: Boolean = false,
    ): Result<UserStatsUiModel> {
        val requestKey = listOf(
            userId,
            fallbackUserName,
            preferredProjectName.orEmpty(),
            selectedRepositoryId.orEmpty(),
            selectedStartDate.orEmpty(),
            selectedEndDate.orEmpty(),
            selectedRapidThresholdMinutes?.toString().orEmpty(),
        ).joinToString("|")

        cachedModel?.takeIf { !forceRefresh && cachedKey == requestKey }?.let {
            return Result.success(it)
        }

        return try {
            coroutineScope {
                val allProjects = projectsApi.getAllProjects().getOrNull()?.projects.orEmpty()
                val resolvedProject = resolveMetricUserProject(
                    fallbackUserName = fallbackUserName,
                    preferredProjectName = preferredProjectName,
                    projects = allProjects,
                ) ?: resolveRegistryUserProject(
                    userId = userId,
                    fallbackUserName = fallbackUserName,
                    preferredProjectName = preferredProjectName,
                    projects = allProjects,
                ) ?: return@coroutineScope Result.failure(
                    RuntimeException("Не удалось определить проект пользователя")
                )

                val metricDetail = resolvedProject.metricDetail
                    ?: fetchMetricProjectDetail(
                        projectKey = resolvedProject.projectKey,
                        fallbackProjectId = resolvedProject.project?.id,
                    )
                    ?: return@coroutineScope Result.failure(
                        RuntimeException("Не удалось загрузить метрики пользователя")
                    )

                val resolvedRapidThresholdMinutes = resolveRapidThresholdMinutes(
                    selectedRapidThresholdMinutes = selectedRapidThresholdMinutes,
                    primaryResource = selectResource(
                        resources = metricDetail.resources,
                        selectedRepositoryId = selectedRepositoryId,
                    ),
                )

                val selectedResource = selectResource(
                    resources = metricDetail.resources,
                    selectedRepositoryId = selectedRepositoryId,
                )
                val repositories = buildRepositories(metricDetail.resources)
                val project = resolveProject(
                    projectKey = resolvedProject.projectKey,
                    projectResponse = resolvedProject.projectResponse,
                    metricDetail = metricDetail,
                )
                val teamUsers = buildTeamUsers(
                    projectResponse = resolvedProject.projectResponse,
                    metricUsers = metricDetail.users,
                )
                val selectedUser = resolveSelectedUser(
                    teamUsers = teamUsers,
                    userId = userId,
                    fallbackUserName = fallbackUserName,
                    metricUsers = metricDetail.users,
                )
                val context = buildStatsContext(
                    resource = selectedResource,
                    teamUsers = teamUsers,
                    selectedUser = selectedUser,
                    selectedStartDate = selectedStartDate,
                    selectedEndDate = selectedEndDate,
                    rapidThresholdMinutes = resolvedRapidThresholdMinutes,
                )
                val teamRank = buildTeamRank(
                    teamUsers = teamUsers,
                    selectedUser = selectedUser,
                    studentRatings = metricApi.getStudentRatings().getOrNull().orEmpty(),
                )

                val model = UserStatsUiModel(
                    userId = userId,
                    userName = selectedUser.displayName.ifBlank { fallbackUserName },
                    role = selectedUser.role.ifBlank { "Участник" },
                    projectId = resolvedProject.projectKey,
                    projectTitle = project.name,
                    repositories = repositories,
                    selectedRepositoryId = selectedResource?.id.orEmpty(),
                    visibleRange = buildVisibleRange(
                        selectedStartDate = selectedStartDate,
                        selectedEndDate = selectedEndDate,
                        eventStart = context.visibleStart,
                        eventEnd = context.visibleEnd,
                        project = project,
                    ),
                    teamRank = teamRank,
                    commits = buildCommitsSection(context.selectedSnapshot, context.peerSnapshots),
                    issues = buildIssuesSection(context.selectedSnapshot, context.peerSnapshots),
                    pullRequests = buildPullRequestsSection(context.selectedSnapshot, context.peerSnapshots),
                    rapidPullRequests = buildRapidPullRequestsSection(context.selectedSnapshot, context.peerSnapshots),
                    codeChurn = buildCodeChurnSection(context.selectedSnapshot, context.peerSnapshots),
                    codeOwnership = buildOwnershipSection(
                        selectedSnapshot = context.selectedSnapshot,
                        ownershipContributors = context.ownershipContributors,
                        peerSnapshots = context.peerSnapshots,
                    ),
                    dominantWeekDay = buildWeekDaySection(context.selectedSnapshot, context.peerSnapshots),
                    details = buildDetails(
                        context = context,
                        teamUsers = teamUsers,
                        selectedUser = selectedUser,
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

    private suspend fun resolveMetricUserProject(
        fallbackUserName: String,
        preferredProjectName: String?,
        projects: List<Project>,
    ): ResolvedUserProject? = coroutineScope {
        val userNameKey = personNameKey(fallbackUserName)
        val normalizedProjectName = normalizeText(preferredProjectName)
        val metricProjects = metricApi.getProjects().getOrNull().orEmpty()

        metricProjects.map { project ->
            async {
                val metricDetail = metricApi.getProjectDetail(project.id).getOrNull() ?: return@async null
                val score = buildMetricProjectMatchScore(
                    metricDetail = metricDetail,
                    userNameKey = userNameKey,
                    normalizedProjectName = normalizedProjectName,
                ) ?: return@async null

                val registryProject = findRegistryProject(
                    projects = projects,
                    metricDetail = metricDetail,
                )
                val projectResponse = registryProject?.let { project ->
                    fetchProjectDetail(project)
                }

                ResolvedUserProject(
                    projectKey = metricDetail.id,
                    projectResponse = projectResponse,
                    project = registryProject,
                    metricDetail = metricDetail,
                    matchScore = score,
                )
            }
        }.awaitAll()
            .filterNotNull()
            .maxByOrNull { it.matchScore }
    }

    private suspend fun resolveRegistryUserProject(
        userId: String,
        fallbackUserName: String,
        preferredProjectName: String?,
        projects: List<Project>,
    ): ResolvedUserProject? = coroutineScope {
        val normalizedUserName = personNameKey(fallbackUserName)
        val normalizedProjectName = normalizeText(preferredProjectName)

        projects.map { project ->
            async {
                val projectKey = project.slug?.takeIf { it.isNotBlank() } ?: project.id
                val projectResponse = projectsApi.getProjectById(projectKey).getOrNull() ?: return@async null
                val score = buildProjectMatchScore(
                    project = project,
                    projectResponse = projectResponse,
                    userId = userId,
                    normalizedUserName = normalizedUserName,
                    normalizedProjectName = normalizedProjectName,
                ) ?: return@async null

                ResolvedUserProject(
                    projectKey = projectKey,
                    projectResponse = projectResponse,
                    project = project,
                    metricDetail = null,
                    matchScore = score,
                )
            }
        }.awaitAll()
            .filterNotNull()
            .maxByOrNull { it.matchScore }
    }

    private fun buildMetricProjectMatchScore(
        metricDetail: MetricProjectDetail,
        userNameKey: String,
        normalizedProjectName: String,
    ): Int? {
        var score = 0

        if (userNameKey.isNotBlank() && metricDetail.users.any { personNameKey(it.name) == userNameKey }) {
            score += 80
        }
        if (normalizedProjectName.isNotBlank() && normalizeText(metricDetail.name) == normalizedProjectName) {
            score += 60
        }

        return score.takeIf { it > 0 }
    }

    private fun buildProjectMatchScore(
        project: Project,
        projectResponse: ProjectDetailResponse,
        userId: String,
        normalizedUserName: String,
        normalizedProjectName: String,
    ): Int? {
        val users = projectResponse.users.orEmpty()
        val members = projectResponse.members.orEmpty()
        val usersById = users.associateBy { it.id }

        var score = 0
        if (users.any { it.id == userId } || members.any { it.user?.toString() == userId }) {
            score += 100
        }

        val names = buildList {
            addAll(users.mapNotNull { it.name })
            addAll(
                members.mapNotNull { member ->
                    resolveMemberName(member, usersById)
                }
            )
        }
        if (normalizedUserName.isNotBlank() && names.any { personNameKey(it) == normalizedUserName }) {
            score += 80
        }

        val projectNames = listOfNotNull(project.name, projectResponse.project?.name)
        if (normalizedProjectName.isNotBlank() && projectNames.any { normalizeText(it) == normalizedProjectName }) {
            score += 60
        }

        return score.takeIf { it > 0 }
    }

    private fun resolveProject(
        projectKey: String,
        projectResponse: ProjectDetailResponse?,
        metricDetail: MetricProjectDetail,
    ): ProjectDetail {
        return projectResponse?.project ?: ProjectDetail(
            id = metricDetail.id,
            name = metricDetail.name,
            description = metricDetail.description,
            shortDescription = null,
            dateStart = metricDetail.dateStart,
            dateEnd = metricDetail.dateEnd,
            slug = projectKey,
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

    private suspend fun fetchProjectDetail(project: Project): ProjectDetailResponse? {
        val projectKey = project.slug?.takeIf { it.isNotBlank() } ?: project.id
        return projectsApi.getProjectById(projectKey).getOrNull()
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

    private fun findRegistryProject(
        projects: List<Project>,
        metricDetail: MetricProjectDetail,
    ): Project? {
        return projects.firstOrNull { project ->
            val projectKey = project.slug?.takeIf { it.isNotBlank() } ?: project.id
            projectKey == metricDetail.id || normalizeText(project.name) == normalizeText(metricDetail.name)
        }
    }

    private fun buildTeamUsers(
        projectResponse: ProjectDetailResponse?,
        metricUsers: List<MetricProjectUser>,
    ): List<TeamUserIdentity> {
        val users = projectResponse?.users.orEmpty()
        val usersById = users.associateBy { it.id }
        val metricUsersByName = metricUsers.associateBy { personNameKey(it.name) }

        val memberUsers = projectResponse?.members.orEmpty().mapNotNull { member ->
            val displayName = resolveMemberName(member, usersById)?.trim().orEmpty()
            if (displayName.isBlank()) return@mapNotNull null

            val metricUser = metricUsersByName[personNameKey(displayName)]
            TeamUserIdentity(
                userId = member.user?.toString(),
                displayName = preferDisplayPersonName(displayName, metricUser?.name)
                    ?: displayName,
                role = member.role?.trim().takeUnless { it.isNullOrBlank() }
                    ?: metricUser?.roles?.joinToString(", ").orEmpty().ifBlank { "Участник" },
                login = metricUser?.githubLogin(),
            )
        }

        val projectUsers = users.mapNotNull { user ->
            val displayName = user.name?.trim().orEmpty()
            if (displayName.isBlank()) return@mapNotNull null
            val metricUser = metricUsersByName[personNameKey(displayName)]
            TeamUserIdentity(
                userId = user.id,
                displayName = preferDisplayPersonName(displayName, metricUser?.name)
                    ?: displayName,
                role = metricUser?.roles?.joinToString(", ").orEmpty().ifBlank { "Участник" },
                login = metricUser?.githubLogin(),
            )
        }

        val metricOnlyUsers = metricUsers.mapNotNull { metricUser ->
            val displayName = displayPersonName(metricUser.name).trim()
            if (displayName.isBlank()) return@mapNotNull null

            val matchingUser = users.firstOrNull { user ->
                personNameMatches(user.name, metricUser.name)
            }
            TeamUserIdentity(
                userId = matchingUser?.id,
                displayName = preferDisplayPersonName(matchingUser?.name, displayName)
                    ?: displayName,
                role = metricUser.roles.joinToString(", ").ifBlank { "Участник" },
                login = metricUser.githubLogin(),
            )
        }

        return (memberUsers + projectUsers + metricOnlyUsers)
            .distinctBy { identityKey(it.userId, it.displayName, it.login) }
    }

    private fun resolveSelectedUser(
        teamUsers: List<TeamUserIdentity>,
        userId: String,
        fallbackUserName: String,
        metricUsers: List<MetricProjectUser>,
    ): TeamUserIdentity {
        val normalizedName = personNameKey(fallbackUserName)
        return teamUsers.firstOrNull { it.userId == userId }
            ?: teamUsers.firstOrNull { personNameKey(it.displayName) == normalizedName }
            ?: metricUsers.firstOrNull { personNameKey(it.name) == normalizedName }?.let { metricUser ->
                TeamUserIdentity(
                    userId = userId.ifBlank { null },
                    displayName = displayPersonName(metricUser.name).ifBlank { fallbackUserName },
                    role = metricUser.roles.joinToString(", ").ifBlank { "Участник" },
                    login = metricUser.githubLogin(),
                )
            }
            ?: TeamUserIdentity(
                userId = userId.ifBlank { null },
                displayName = fallbackUserName,
                role = "Участник",
                login = null,
            )
    }

    private fun buildStatsContext(
        resource: MetricProjectResource?,
        teamUsers: List<TeamUserIdentity>,
        selectedUser: TeamUserIdentity,
        selectedStartDate: String?,
        selectedEndDate: String?,
        rapidThresholdMinutes: Int,
    ): UserStatsContext {
        if (resource == null) {
            val emptySnapshot = UserMetricSnapshot.empty(selectedUser.displayName)
            return UserStatsContext(
                selectedSnapshot = emptySnapshot,
                peerSnapshots = emptyList(),
                ownershipContributors = emptyList(),
                visibleStart = null,
                visibleEnd = null,
                commits = emptyList(),
                issues = emptyList(),
                pullRequests = emptyList(),
            )
        }

        val metricsByName = resource.metrics.associateBy { it.name }
        val commitsRaw = extractSnapshots<ProjectCommitSnapshot>(metricsByName[METRIC_COMMITS])
        val issuesRaw = deduplicateIssues(
            extractSnapshots<ProjectIssueSnapshot>(metricsByName[METRIC_ISSUES])
        )
        val pullRequestsRaw = deduplicatePullRequests(
            extractSnapshots<ProjectPullRequestSnapshot>(metricsByName[METRIC_PULL_REQUESTS])
        )

        val allEventDates = buildList {
            addAll(commitsRaw.mapNotNull { parseInstant(it.commit?.author?.date) })
            addAll(issuesRaw.mapNotNull { parseInstant(it.created_at) })
            addAll(issuesRaw.mapNotNull { parseInstant(it.closed_at) })
            addAll(pullRequestsRaw.mapNotNull { parseInstant(it.created_at) })
            addAll(pullRequestsRaw.mapNotNull { pullRequestCompletedAtIso(it)?.let(::parseInstant) })
        }
        val window = resolveWindow(
            selectedStartDate = selectedStartDate,
            selectedEndDate = selectedEndDate,
            eventDates = allEventDates,
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

        val totalProjectLines = commits.sumOf { commit ->
            commit.files.sumOf { file ->
                val additions = file.additions ?: 0
                val deletions = file.deletions ?: 0
                val changes = file.changes ?: 0
                additions + deletions + changes
            }
        }

        val teamSnapshots = teamUsers.map { identity ->
            buildUserMetricSnapshot(
                identity = identity,
                commits = commits,
                issues = issues,
                pullRequests = pullRequests,
                rapidThresholdMinutes = rapidThresholdMinutes,
                totalProjectLines = totalProjectLines,
            )
        }
        val selectedSnapshot = teamSnapshots.firstOrNull { identityKey(it.userId, it.displayName, it.login) == identityKey(selectedUser.userId, selectedUser.displayName, selectedUser.login) }
            ?: buildUserMetricSnapshot(
                identity = selectedUser,
                commits = commits,
                issues = issues,
                pullRequests = pullRequests,
                rapidThresholdMinutes = rapidThresholdMinutes,
                totalProjectLines = totalProjectLines,
            )

        val ownershipContributors = buildOwnershipContributors(
            commits = commits,
            selectedUserLogin = selectedUser.login,
            teamUsers = teamUsers,
        )

        return UserStatsContext(
            selectedSnapshot = selectedSnapshot,
            peerSnapshots = teamSnapshots.filterNot {
                identityKey(it.userId, it.displayName, it.login) == identityKey(selectedSnapshot.userId, selectedSnapshot.displayName, selectedSnapshot.login)
            },
            ownershipContributors = ownershipContributors,
            visibleStart = window?.first ?: allEventDates.minOrNull(),
            visibleEnd = window?.second ?: allEventDates.maxOrNull(),
            commits = commits,
            issues = issues,
            pullRequests = pullRequests,
        )
    }

    private fun buildDetails(
        context: UserStatsContext,
        teamUsers: List<TeamUserIdentity>,
        selectedUser: TeamUserIdentity,
    ): StatsDetailDataUi {
        val nowIso = Instant.fromEpochMilliseconds(PlatformTime.currentTimeMillis()).toString()
        val displayNames = teamUsers.associateBy(
            keySelector = { normalizeLogin(it.login) ?: personNameKey(it.displayName) },
            valueTransform = { it.displayName },
        )
        val participants = teamUsers.mapNotNull { user ->
            val participantId = normalizeLogin(user.login) ?: return@mapNotNull null
            StatsDetailParticipantUi(
                id = participantId,
                name = user.displayName,
                subtitle = user.role.ifBlank { "Участник" },
                isCurrentUser = participantId == normalizeLogin(selectedUser.login),
            )
        }.distinctBy { it.id }

        return StatsDetailDataUi(
            participants = participants,
            defaultParticipantId = normalizeLogin(selectedUser.login),
            commits = context.commits.map { commit ->
                StatsDetailCommitUi(
                    authorId = normalizeLogin(commit.author?.login),
                    authorName = resolveUserDisplayName(commit.author?.login, displayNames),
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
            issues = context.issues.map { issue ->
                val assignees = issue.assignees.mapNotNull { assignee ->
                    val assigneeId = normalizeLogin(assignee.login) ?: return@mapNotNull null
                    Triple(
                        assigneeId,
                        resolveUserDisplayName(assignee.login, displayNames),
                        assignee.avatar_url,
                    )
                }
                StatsDetailIssueUi(
                    creatorId = normalizeLogin(issue.user?.login),
                    creatorName = resolveUserDisplayName(issue.user?.login, displayNames),
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
                    labels = issue.labels.mapNotNull { label ->
                        label.name?.trim()?.takeIf { it.isNotBlank() }
                    },
                    comments = issue.comments,
                    thumbsUpCount = issue.reactions?.plusOne,
                    thumbsDownCount = issue.reactions?.minusOne,
                    url = issue.html_url,
                )
            },
            pullRequests = context.pullRequests.map { pullRequest ->
                val completedAtIso = pullRequestCompletedAtIso(pullRequest)
                val assignees = pullRequest.assignees.mapNotNull { assignee ->
                    val assigneeId = normalizeLogin(assignee.login) ?: return@mapNotNull null
                    assigneeId to resolveUserDisplayName(assignee.login, displayNames)
                }
                StatsDetailPullRequestUi(
                    authorId = normalizeLogin(pullRequest.user?.login),
                    authorName = resolveUserDisplayName(pullRequest.user?.login, displayNames),
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

    private fun normalizeLogin(login: String?): String? {
        return login?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    }

    private fun resolveUserDisplayName(
        login: String?,
        displayNames: Map<String, String>,
    ): String {
        val participantId = normalizeLogin(login)
        return participantId?.let(displayNames::get)
            ?: login?.trim().orEmpty().ifBlank { "Участник" }
    }

    private fun buildUserMetricSnapshot(
        identity: TeamUserIdentity,
        commits: List<ProjectCommitSnapshot>,
        issues: List<ProjectIssueSnapshot>,
        pullRequests: List<ProjectPullRequestSnapshot>,
        rapidThresholdMinutes: Int,
        totalProjectLines: Int,
    ): UserMetricSnapshot {
        val login = identity.login?.trim().orEmpty()
        val normalizedLogin = login.lowercase()

        val userCommits = if (normalizedLogin.isBlank()) {
            emptyList()
        } else {
            commits.filter { commit ->
                commit.author?.login?.trim()?.lowercase() == normalizedLogin
            }
        }
        val userIssues = if (normalizedLogin.isBlank()) {
            emptyList()
        } else {
            issues.filter { issue ->
                issueParticipantLogins(issue).contains(normalizedLogin)
            }
        }
        val userPullRequests = if (normalizedLogin.isBlank()) {
            emptyList()
        } else {
            pullRequests.filter { pullRequest ->
                pullRequest.user?.login?.trim()?.lowercase() == normalizedLogin
            }
        }

        val closedPullRequests = userPullRequests.filter { pullRequestCompletedAtMillis(it) != null }
        val rapidPullRequests = closedPullRequests.filter {
            isRapidPullRequest(it, rapidThresholdMinutes.toLong() * MILLIS_PER_MINUTE)
        }
        val fileStats = buildFileStats(userCommits)
        val weekdays = buildWeekDays(userCommits, userIssues, userPullRequests)
        val linesOwned = userCommits.sumOf { commit ->
            commit.files.sumOf { file ->
                val additions = file.additions ?: 0
                val deletions = file.deletions ?: 0
                val changes = file.changes ?: 0
                additions + deletions + changes
            }
        }

        return UserMetricSnapshot(
            userId = identity.userId,
            displayName = identity.displayName,
            role = identity.role,
            login = identity.login,
            commitCount = userCommits.size,
            issueCount = userIssues.size,
            openIssueCount = userIssues.count { it.closed_at.isNullOrBlank() },
            closedIssueCount = userIssues.count { !it.closed_at.isNullOrBlank() },
            pullRequestCount = userPullRequests.size,
            rapidPullRequestCount = rapidPullRequests.size,
            fileStats = fileStats,
            weekdays = weekdays,
            weekdayTotal = weekdays.sumOf { it.value },
            dominantWeekdayLabel = weekdays.maxByOrNull { it.value }?.label,
            linesOwned = linesOwned,
            commitChart = buildChartPoints(
                dates = userCommits.mapNotNull { parseInstant(it.commit?.author?.date) },
                hintFormatter = { count -> "$count ${pluralize(count, "коммит", "коммита", "коммитов")}" },
            ),
            pullRequestChart = buildChartPoints(
                dates = userPullRequests.mapNotNull { parseInstant(it.created_at) },
                hintFormatter = { count -> "$count ${openedPullRequestsLabel(count)}" },
            ),
            commitsScore = totalCommitsScore(userCommits),
            issueScore = issueCompletenessScore(userIssues),
            pullRequestScore = pullRequestHangScore(userPullRequests),
            rapidPullScore = rapidPullScore(closedPullRequests, rapidThresholdMinutes),
            codeChurnScore = codeChurnScore(fileStats, userCommits.size),
            codeOwnershipScore = if (totalProjectLines > 0 && linesOwned > 0) {
                round2((2 + 3 * (linesOwned.toDouble() / totalProjectLines.toDouble())).coerceIn(0.0, 5.0))
            } else {
                null
            },
            weekDayScore = weekDayScore(weekdays),
        )
    }

    private fun buildTeamRank(
        teamUsers: List<TeamUserIdentity>,
        selectedUser: TeamUserIdentity,
        studentRatings: List<com.spbu.projecttrack.rating.data.model.MetricRankingItem>,
    ): Int? {
        val scoresByKey = linkedMapOf<String, Double?>()
        teamUsers.forEach { user ->
            val rating = studentRatings.firstOrNull { item ->
                item.id == user.userId ||
                    personNameMatches(item.name, user.displayName)
            }
            scoresByKey[identityKey(user.userId, user.displayName, user.login)] = rating?.score
        }

        val selectedKey = identityKey(selectedUser.userId, selectedUser.displayName, selectedUser.login)
        return buildRank(
            currentScore = scoresByKey[selectedKey],
            peerScores = scoresByKey
                .filterKeys { it != selectedKey }
                .values
                .toList(),
        )
    }

    private fun buildCommitsSection(
        snapshot: UserMetricSnapshot,
        peerSnapshots: List<UserMetricSnapshot>,
    ): ProjectStatsMetricSectionUi {
        val rank = buildRank(snapshot.commitsScore, peerSnapshots.map { it.commitsScore })
        return ProjectStatsMetricSectionUi(
            title = "Коммиты",
            score = snapshot.commitsScore,
            primaryValue = snapshot.commitCount.toString(),
            primaryCaption = pluralize(snapshot.commitCount, "коммит", "коммита", "коммитов"),
            rank = rank,
            rankCaption = "место в рейтинге",
            chartTitle = "График коммитов",
            chartType = ProjectStatsChartType.Bars,
            chartPoints = snapshot.commitChart,
            tableTitle = "",
            tableRows = emptyList(),
            tooltipTitle = "${snapshot.commitCount} коммитов",
        )
    }

    private fun buildIssuesSection(
        snapshot: UserMetricSnapshot,
        peerSnapshots: List<UserMetricSnapshot>,
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
            progress = progress,
            remainingText = when {
                snapshot.issueCount == 0 -> "Нет активных Issue"
                snapshot.openIssueCount > 0 -> "Закройте еще ${snapshot.openIssueCount} Issue"
                else -> "Все Issue закрыты"
            },
            rank = rank,
            tableRows = emptyList(),
        )
    }

    private fun buildPullRequestsSection(
        snapshot: UserMetricSnapshot,
        peerSnapshots: List<UserMetricSnapshot>,
    ): ProjectStatsMetricSectionUi {
        val rank = buildRank(snapshot.pullRequestScore, peerSnapshots.map { it.pullRequestScore })
        return ProjectStatsMetricSectionUi(
            title = "Pull Requests",
            score = snapshot.pullRequestScore,
            primaryValue = snapshot.pullRequestCount.toString(),
            primaryCaption = "всего PR",
            rank = rank,
            rankCaption = "место в рейтинге",
            chartTitle = "График Pull Requests",
            chartType = ProjectStatsChartType.Line,
            chartPoints = snapshot.pullRequestChart,
            tableTitle = "",
            tableRows = emptyList(),
            tooltipTitle = "${snapshot.pullRequestCount} Pull Requests",
        )
    }

    private fun buildRapidPullRequestsSection(
        snapshot: UserMetricSnapshot,
        peerSnapshots: List<UserMetricSnapshot>,
    ): ProjectStatsMetricSectionUi {
        val rank = buildRank(snapshot.rapidPullScore, peerSnapshots.map { it.rapidPullScore })
        return ProjectStatsMetricSectionUi(
            title = "Быстрые Pull Requests",
            score = snapshot.rapidPullScore,
            primaryValue = snapshot.rapidPullRequestCount.toString(),
            primaryCaption = "быстрых PR",
            rank = rank,
            rankCaption = "место в рейтинге",
            chartTitle = "",
            chartType = ProjectStatsChartType.Line,
            chartPoints = emptyList(),
            tableTitle = "",
            tableRows = emptyList(),
            tooltipTitle = "",
        )
    }

    private fun buildCodeChurnSection(
        snapshot: UserMetricSnapshot,
        peerSnapshots: List<UserMetricSnapshot>,
    ): ProjectStatsCodeChurnSectionUi {
        val rank = buildRank(snapshot.codeChurnScore, peerSnapshots.map { it.codeChurnScore })
        val fileStats = snapshot.fileStats
        return ProjectStatsCodeChurnSectionUi(
            title = "Изменчивость кода",
            score = snapshot.codeChurnScore,
            changedFilesCount = fileStats.size,
            rank = rank,
            fileRows = fileStats.map {
                ProjectStatsFileRowUi(
                    fileName = it.fileName,
                    value = it.changes.toString(),
                )
            },
            tableRows = emptyList(),
            slices = buildFileChurnSlices(fileStats),
            mostChangedFileName = fileStats.firstOrNull()?.fileName,
        )
    }

    private fun buildFileChurnSlices(
        fileStats: List<ProjectFileStat>,
    ): List<ProjectStatsDonutSliceUi> {
        if (fileStats.isEmpty()) return emptyList()
        val buckets = listOf(
            "1 изменение" to fileStats.count { it.changes == 1 },
            "2-3 изменения" to fileStats.count { it.changes in 2..3 },
            "4-5 изменений" to fileStats.count { it.changes in 4..5 },
            "6-7 изменений" to fileStats.count { it.changes in 6..7 },
            "8-10 изменений" to fileStats.count { it.changes in 8..10 },
            ">10 изменений" to fileStats.count { it.changes > 10 },
        ).filter { it.second > 0 }
        val total = buckets.sumOf { it.second }.takeIf { it > 0 } ?: 1
        return buckets.mapIndexed { index, (label, value) ->
            ProjectStatsDonutSliceUi(
                label = label,
                secondaryLabel = "$value файлов",
                percentLabel = round2(value * 100.0 / total).toString() + "%",
                value = value.toFloat(),
                colorHex = weekdayPalette[index % weekdayPalette.size],
                highlight = value == buckets.maxOf { it.second },
            )
        }
    }

    private fun buildOwnershipSection(
        selectedSnapshot: UserMetricSnapshot,
        ownershipContributors: List<OwnershipContributorStat>,
        peerSnapshots: List<UserMetricSnapshot>,
    ): ProjectStatsOwnershipSectionUi {
        val totalLines = ownershipContributors.sumOf { it.value }
        val rank = buildRank(selectedSnapshot.codeOwnershipScore, peerSnapshots.map { it.codeOwnershipScore })
        return ProjectStatsOwnershipSectionUi(
            title = "Владение кодом",
            score = selectedSnapshot.codeOwnershipScore,
            rank = rank,
            slices = ownershipContributors.mapIndexed { index, item ->
                ProjectStatsDonutSliceUi(
                    label = item.displayName,
                    secondaryLabel = item.displayValue,
                    percentLabel = percentLabel(item.value, totalLines),
                    value = item.value.toFloat(),
                    colorHex = ownershipPalette[index % ownershipPalette.size],
                    highlight = item.isSelected,
                )
            },
        )
    }

    private fun buildWeekDaySection(
        snapshot: UserMetricSnapshot,
        peerSnapshots: List<UserMetricSnapshot>,
    ): ProjectStatsWeekDaySectionUi {
        val rank = buildRank(snapshot.weekDayScore, peerSnapshots.map { it.weekDayScore })
        return ProjectStatsWeekDaySectionUi(
            title = "Доминирующий день недели",
            score = snapshot.weekDayScore,
            headline = snapshot.dominantWeekdayLabel?.uppercase() ?: "НЕТ ДАННЫХ",
            subtitle = snapshot.dominantWeekdayLabel?.let { "самый активный день недели" } ?: "нет данных",
            slices = snapshot.weekdays.mapIndexed { index, day ->
                ProjectStatsDonutSliceUi(
                    label = day.label,
                    secondaryLabel = "${day.value} ${pluralize(day.value, "действие", "действия", "действий")}",
                    percentLabel = percentLabel(day.value, snapshot.weekdayTotal),
                    value = day.value.toFloat(),
                    colorHex = weekdayPalette[index % weekdayPalette.size],
                    highlight = day.isDominant,
                )
            },
        )
    }

    private fun buildRepositories(resources: List<MetricProjectResource>): List<ProjectStatsRepositoryUi> {
        return resources.map { resource ->
            ProjectStatsRepositoryUi(
                id = resource.id,
                title = extractStringParam(resource, "url")?.ifBlank { null }
                    ?: resource.name.ifBlank { "Репозиторий" },
                subtitle = resource.platform?.trim()?.takeIf { it.isNotBlank() } ?: "GitHub",
            )
        }
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

    private fun buildVisibleRange(
        selectedStartDate: String?,
        selectedEndDate: String?,
        eventStart: Instant?,
        eventEnd: Instant?,
        project: ProjectDetail,
    ): ProjectStatsDateRangeUi {
        val startDate = selectedStartDate?.let(::parseIsoDate)
            ?: eventStart?.toLocalDateTime(TimeZone.UTC)?.date
            ?: parseFlexibleDate(project.dateStart)
            ?: fallbackVisibleDate()
        val endDate = selectedEndDate?.let(::parseIsoDate)
            ?: eventEnd?.toLocalDateTime(TimeZone.UTC)?.date
            ?: parseFlexibleDate(project.dateEnd)
            ?: startDate

        return ProjectStatsDateRangeUi(
            startIsoDate = formatIsoDate(startDate),
            endIsoDate = formatIsoDate(endDate),
            startLabel = formatLocalDateLabel(startDate),
            endLabel = formatLocalDateLabel(endDate),
        )
    }

    private fun buildOwnershipContributors(
        commits: List<ProjectCommitSnapshot>,
        selectedUserLogin: String?,
        teamUsers: List<TeamUserIdentity>,
    ): List<OwnershipContributorStat> {
        val displayNames = teamUsers.associateBy(
            keySelector = { it.login?.trim()?.lowercase().orEmpty() },
            valueTransform = { it.displayName },
        )
        val lines = linkedMapOf<String, Int>()
        commits.forEach { commit ->
            val login = commit.author?.login?.trim().orEmpty()
            if (login.isBlank()) return@forEach

            val delta = commit.files.sumOf { file ->
                val additions = file.additions ?: 0
                val deletions = file.deletions ?: 0
                val changes = file.changes ?: 0
                additions + deletions + changes
            }
            if (delta <= 0) return@forEach
            lines[login] = (lines[login] ?: 0) + delta
        }

        return lines.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.lowercase() })
            .map { (login, value) ->
                OwnershipContributorStat(
                    login = login,
                    displayName = displayNames[login.lowercase()] ?: login,
                    value = value,
                    displayValue = "$value строк",
                    isSelected = selectedUserLogin?.equals(login, ignoreCase = true) == true,
                )
            }
    }

    private fun resolveMemberName(
        member: Member,
        usersById: Map<String, User>,
    ): String? {
        val userName = member.user?.toString()?.let(usersById::get)?.name?.trim()
        if (!userName.isNullOrBlank()) return userName
        return member.name.trim().takeIf { it.isNotBlank() }
    }

    private fun issueParticipantLogins(issue: ProjectIssueSnapshot): Set<String> {
        return buildSet {
            issue.user?.login?.trim()?.lowercase()?.takeIf { it.isNotBlank() }?.let(::add)
            issue.assignees.mapNotNullTo(this) { assignee ->
                assignee.login?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
            }
        }
    }

    private fun buildFileStats(commits: List<ProjectCommitSnapshot>): List<ProjectFileStat> {
        val fileChanges = linkedMapOf<String, Int>()
        commits.forEach { commit ->
            commit.files.forEach { file ->
                val fileName = file.filename?.trim().orEmpty()
                if (fileName.isBlank()) return@forEach

                val changes = file.changes ?: ((file.additions ?: 0) + (file.deletions ?: 0))
                if (changes <= 0) return@forEach
                fileChanges[fileName] = (fileChanges[fileName] ?: 0) + changes
            }
        }

        return fileChanges.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.lowercase() })
            .map { entry ->
                ProjectFileStat(
                    fileName = entry.key,
                    changes = entry.value,
                )
            }
    }

    private fun buildWeekDays(
        commits: List<ProjectCommitSnapshot>,
        issues: List<ProjectIssueSnapshot>,
        pullRequests: List<ProjectPullRequestSnapshot>,
    ): List<ProjectWeekdayStat> {
        val labels = listOf(
            "Понедельник",
            "Вторник",
            "Среда",
            "Четверг",
            "Пятница",
            "Суббота",
            "Воскресенье",
        )
        val counts = linkedMapOf<String, Int>().apply {
            labels.forEach { put(it, 0) }
        }

        fun bump(date: String?) {
            val label = weekdayLabel(date) ?: return
            counts[label] = (counts[label] ?: 0) + 1
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

    private fun totalCommitsScore(commits: List<ProjectCommitSnapshot>): Double? {
        if (commits.isEmpty()) return null

        val dates = commits.mapNotNull { parseInstant(it.commit?.author?.date)?.toEpochMilliseconds() }
        val start = dates.minOrNull() ?: return null
        val end = dates.maxOrNull() ?: return null
        val dayCount = (end - start).toDouble() / MILLIS_PER_DAY
        if (dayCount <= 0) return 5.0

        val commitsPerDay = commits.size / dayCount
        return round2((commitsPerDay * 3 + 2).coerceIn(0.0, 5.0))
    }

    private fun issueCompletenessScore(issues: List<ProjectIssueSnapshot>): Double? {
        if (issues.isEmpty()) return null
        val closed = issues.count { !it.closed_at.isNullOrBlank() }
        return round2((closed.toDouble() / issues.size) * 3 + 2)
    }

    private fun pullRequestHangScore(pullRequests: List<ProjectPullRequestSnapshot>): Double? {
        val durations = pullRequests.mapNotNull(::pullRequestLifetimeMillis)
        if (durations.isEmpty()) return null

        val averageHangTime = durations.average()
        return if (averageHangTime < 5 * MILLIS_PER_MINUTE) {
            round2(5.0)
        } else {
            round2(((1 - averageHangTime / (7 * MILLIS_PER_DAY)) * 3 + 2).coerceAtLeast(0.0))
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

    private fun codeChurnScore(
        fileStats: List<ProjectFileStat>,
        commitCount: Int,
    ): Double? {
        if (fileStats.isEmpty() || commitCount <= 0) return null
        val churnPerCommit = fileStats.sumOf { it.changes }.toDouble() / commitCount
        return round2((5.0 - ln(1.0 + churnPerCommit).coerceAtMost(4.0) * 1.2).coerceIn(0.0, 5.0))
    }

    private fun weekDayScore(weekdays: List<ProjectWeekdayStat>): Double? {
        if (weekdays.isEmpty()) return null
        val dominant = weekdays.maxByOrNull { it.value } ?: return null
        val total = weekdays.sumOf { it.value }
        if (total <= 0) return null

        val dominantShare = dominant.value.toDouble() / total.toDouble()
        return round2((2 + 3 * dominantShare).coerceIn(0.0, 5.0))
    }

    private fun buildRank(
        currentScore: Double?,
        peerScores: List<Double?>,
    ): Int? {
        if (currentScore == null) return null
        val ranked = (peerScores + currentScore).filterNotNull().sortedDescending()
        return ranked.indexOfFirst { it == currentScore }.takeIf { it >= 0 }?.plus(1)
    }

    private fun openedPullRequestsLabel(count: Int): String {
        return if (count == 1) "открытый PR" else "открытых PR"
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

    private fun parseRapidThresholdMinutes(resource: MetricProjectResource?): Int? {
        val metric = resource?.metrics?.firstOrNull { it.name == METRIC_RAPID_PULL_REQUESTS } ?: return null
        val threshold = metric.params.firstOrNull { it.name == "rapidPullRequestsThreshold" }?.value ?: return null
        val obj = runCatching { threshold.jsonObject }.getOrNull() ?: return null
        val number = obj["number"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
            ?: obj["value"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
        val unit = obj["unitOfTime"]?.jsonPrimitive?.contentOrNull
        if (number == null || unit.isNullOrBlank()) return null

        return when {
            unit.contains("second", ignoreCase = true) -> (number / 60.0).roundToInt()
            unit.contains("minute", ignoreCase = true) -> number.roundToInt()
            unit.contains("hour", ignoreCase = true) -> (number * 60).roundToInt()
            unit.contains("day", ignoreCase = true) -> (number * MINUTES_PER_DAY).roundToInt()
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

    private fun resolveWindow(
        selectedStartDate: String?,
        selectedEndDate: String?,
        eventDates: List<Instant>,
    ): Pair<Instant, Instant>? {
        val start = selectedStartDate?.let(::startOfDay) ?: eventDates.minOrNull()
        val end = selectedEndDate?.let(::endOfDay) ?: eventDates.maxOrNull()
        if (start == null || end == null) return null
        return if (start <= end) start to end else end to start
    }

    private fun isInWindow(date: Instant?, window: Pair<Instant, Instant>?): Boolean {
        if (date == null || window == null) return true
        val value = date.toEpochMilliseconds()
        return value in window.first.toEpochMilliseconds()..window.second.toEpochMilliseconds()
    }

    private fun weekdayLabel(date: String?): String? {
        val instant = parseInstant(date) ?: return null
        return when (instant.toLocalDateTime(TimeZone.UTC).dayOfWeek.name.lowercase()) {
            "monday" -> "Понедельник"
            "tuesday" -> "Вторник"
            "wednesday" -> "Среда"
            "thursday" -> "Четверг"
            "friday" -> "Пятница"
            "saturday" -> "Суббота"
            "sunday" -> "Воскресенье"
            else -> null
        }
    }

    private fun formatDateLabel(dayBucket: Long): String {
        val instant = Instant.fromEpochMilliseconds(dayBucket * MILLIS_PER_DAY)
        return formatLocalDateLabel(instant.toLocalDateTime(TimeZone.UTC).date)
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

    private fun percentLabel(value: Int, total: Int): String {
        if (total <= 0) return "0%"
        return "${((value.toDouble() / total.toDouble()) * 100.0).roundToInt()}%"
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
        val metric = resource.metrics.firstOrNull { it.params.any { param -> param.name == name } } ?: return null
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

    private fun normalizeText(value: String?): String {
        return normalizeComparableText(value)
    }

    private fun identityKey(userId: String?, displayName: String, login: String?): String {
        return listOf(
            userId.orEmpty(),
            personNameKey(displayName),
            login?.trim()?.lowercase().orEmpty(),
        ).joinToString("|")
    }

    private fun MetricProjectUser.githubLogin(): String? {
        return identifiers.firstOrNull { it.platform.equals("GitHub", ignoreCase = true) }
            ?.value
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private data class ResolvedUserProject(
        val projectKey: String,
        val projectResponse: ProjectDetailResponse?,
        val project: Project?,
        val metricDetail: MetricProjectDetail?,
        val matchScore: Int,
    )

    private data class TeamUserIdentity(
        val userId: String?,
        val displayName: String,
        val role: String,
        val login: String?,
    )

    private data class UserStatsContext(
        val selectedSnapshot: UserMetricSnapshot,
        val peerSnapshots: List<UserMetricSnapshot>,
        val ownershipContributors: List<OwnershipContributorStat>,
        val visibleStart: Instant?,
        val visibleEnd: Instant?,
        val commits: List<ProjectCommitSnapshot>,
        val issues: List<ProjectIssueSnapshot>,
        val pullRequests: List<ProjectPullRequestSnapshot>,
    )

    private data class UserMetricSnapshot(
        val userId: String?,
        val displayName: String,
        val role: String,
        val login: String?,
        val commitCount: Int,
        val issueCount: Int,
        val openIssueCount: Int,
        val closedIssueCount: Int,
        val pullRequestCount: Int,
        val rapidPullRequestCount: Int,
        val fileStats: List<ProjectFileStat>,
        val weekdays: List<ProjectWeekdayStat>,
        val weekdayTotal: Int,
        val dominantWeekdayLabel: String?,
        val linesOwned: Int,
        val commitChart: List<ProjectStatsChartPointUi>,
        val pullRequestChart: List<ProjectStatsChartPointUi>,
        val commitsScore: Double?,
        val issueScore: Double?,
        val pullRequestScore: Double?,
        val rapidPullScore: Double?,
        val codeChurnScore: Double?,
        val codeOwnershipScore: Double?,
        val weekDayScore: Double?,
    ) {
        companion object {
            fun empty(displayName: String): UserMetricSnapshot {
                return UserMetricSnapshot(
                    userId = null,
                    displayName = displayName,
                    role = "Участник",
                    login = null,
                    commitCount = 0,
                    issueCount = 0,
                    openIssueCount = 0,
                    closedIssueCount = 0,
                    pullRequestCount = 0,
                    rapidPullRequestCount = 0,
                    fileStats = emptyList(),
                    weekdays = emptyList(),
                    weekdayTotal = 0,
                    dominantWeekdayLabel = null,
                    linesOwned = 0,
                    commitChart = emptyList(),
                    pullRequestChart = emptyList(),
                    commitsScore = null,
                    issueScore = null,
                    pullRequestScore = null,
                    rapidPullScore = null,
                    codeChurnScore = null,
                    codeOwnershipScore = null,
                    weekDayScore = null,
                )
            }
        }
    }

    private data class OwnershipContributorStat(
        val login: String,
        val displayName: String,
        val value: Int,
        val displayValue: String,
        val isSelected: Boolean,
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
}

private fun round2(value: Double): Double = ((value * 100.0).roundToInt() / 100.0)
