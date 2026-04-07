package com.spbu.projecttrack.rating.data.repository

import com.spbu.projecttrack.projects.data.api.ProjectsApi
import com.spbu.projecttrack.projects.data.model.Member
import com.spbu.projecttrack.projects.data.model.Project
import com.spbu.projecttrack.projects.data.model.ProjectDetailResponse
import com.spbu.projecttrack.projects.data.model.Tag
import com.spbu.projecttrack.projects.data.model.User
import com.spbu.projecttrack.projects.presentation.util.extractGithubUrl
import com.spbu.projecttrack.rating.data.api.MetricApi
import com.spbu.projecttrack.rating.data.model.MetricRankingItem
import com.spbu.projecttrack.rating.data.model.RankingData
import com.spbu.projecttrack.rating.data.model.RankingItem
import com.spbu.projecttrack.rating.data.model.RatingSyncIdentifier
import com.spbu.projecttrack.rating.data.model.RatingSyncMember
import com.spbu.projecttrack.rating.data.model.RatingSyncProject
import com.spbu.projecttrack.user.data.api.UserProfileApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.abs
import kotlin.math.roundToInt

class RankingRepository(
    private val api: MetricApi,
    private val projectsApi: ProjectsApi,
    private val userProfileApi: UserProfileApi
) {
    private var hasSynced = false
    private var cachedProjectCatalog: ProjectCatalog? = null

    suspend fun loadRatings(): Result<RankingData> {
        val projectCatalog = getProjectCatalog().getOrNull()
        val profile = userProfileApi.getProfile().getOrNull()

        if (projectCatalog != null) {
            syncProjects(projectCatalog)
        }

        val projectsResult = api.getProjectRatings()
        if (projectsResult.isFailure) {
            return Result.failure(
                projectsResult.exceptionOrNull() ?: RuntimeException("Failed to load project ratings")
            )
        }

        val studentsResult = api.getStudentRatings()
        if (studentsResult.isFailure) {
            return Result.failure(
                studentsResult.exceptionOrNull() ?: RuntimeException("Failed to load student ratings")
            )
        }

        val metricProjectItems = projectsResult.getOrNull().orEmpty()
        val currentProject = profile?.projects.orEmpty().firstOrNull()
        val currentUserName = profile?.user?.fullName
            ?.displayName()
            ?.takeIf { it.isNotBlank() }
        val studentProjectNames = projectCatalog?.let(::buildStudentProjectNames).orEmpty()

        val projectItems = metricProjectItems.map { item ->
            item.toProjectRankingItem(
                projectCatalog = projectCatalog,
                currentProject = currentProject
            )
        }
        val studentItems = studentsResult.getOrNull().orEmpty().map { item ->
            item.toStudentRankingItem(
                studentProjectNames = studentProjectNames,
                currentUserName = currentUserName,
                currentProjectName = currentProject?.name
            )
        }
        val weeklyMovement = loadProjectWeeklyMovement(metricProjectItems)

        val projectItemsWithMovement = projectItems.map { item ->
            val movement = weeklyMovement[item.key]
            item.copy(
                previousPosition = movement?.previousPosition,
                positionDelta = movement?.positionDelta,
                historyAvailable = true
            )
        }

        return Result.success(
            RankingData(
                projects = projectItemsWithMovement,
                students = studentItems
            )
        )
    }

    private suspend fun getProjectCatalog(): Result<ProjectCatalog> {
        cachedProjectCatalog?.let { return Result.success(it) }

        val allProjectsResult = projectsApi.getAllProjects()
        if (allProjectsResult.isFailure) {
            return Result.failure(
                allProjectsResult.exceptionOrNull() ?: RuntimeException("Не удалось получить проекты")
            )
        }

        val projectsResponse = allProjectsResult.getOrThrow()
        val allProjects = projectsResponse.projects.distinctBy { it.id }
        val tagsById = projectsResponse.tags.associateBy(Tag::id)
        val detailsByProjectId = allProjects.associate { project ->
            project.id to fetchProjectDetail(project)
        }

        val catalog = ProjectCatalog(
            projects = allProjects,
            tagsById = tagsById,
            detailsByProjectId = detailsByProjectId
        )
        cachedProjectCatalog = catalog
        return Result.success(catalog)
    }

    private suspend fun syncProjects(projectCatalog: ProjectCatalog): Result<Unit> {
        if (hasSynced) return Result.success(Unit)
        if (projectCatalog.projects.isEmpty()) return Result.success(Unit)

        val syncProjects = projectCatalog.projects.map { project ->
            val detail = projectCatalog.detailsByProjectId[project.id]
            val detailProject = detail?.project
            val githubUrl = extractGithubUrl(detailProject) ?: extractGithubUrl(project)
            val members = buildMembers(detail, githubUrl)

            RatingSyncProject(
                id = project.id,
                name = project.name,
                description = detailProject?.description ?: project.description,
                dateStart = detailProject?.dateStart ?: project.dateStart,
                dateEnd = detailProject?.dateEnd ?: project.dateEnd,
                githubUrl = githubUrl,
                members = members
            )
        }

        val result = api.syncProjects(syncProjects)
        if (result.isSuccess) {
            hasSynced = true
        }
        return result
    }

    private fun buildStudentProjectNames(
        projectCatalog: ProjectCatalog
    ): Map<String, String> {
        val studentProjects = mutableMapOf<String, String>()

        projectCatalog.projects.forEach { project ->
            val detail = projectCatalog.detailsByProjectId[project.id] ?: return@forEach
            val projectName = detail.project?.name?.trim().takeIf { !it.isNullOrBlank() } ?: project.name
            val usersById = detail.users.orEmpty().associateBy { it.id }

            detail.members.orEmpty().forEach { member ->
                val resolvedName = resolveMemberName(member, usersById)
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: return@forEach

                val normalizedName = normalizeName(resolvedName)
                if (normalizedName !in studentProjects) {
                    studentProjects[normalizedName] = projectName
                }
            }
        }

        return studentProjects
    }

    private suspend fun fetchProjectDetail(project: Project): ProjectDetailResponse? {
        val projectKey = project.slug?.takeIf { it.isNotBlank() } ?: project.id
        val detailResult = projectsApi.getProjectById(projectKey)
        return detailResult.getOrNull()
    }

    private fun buildMembers(
        detail: ProjectDetailResponse?,
        githubUrl: String?
    ): List<RatingSyncMember> {
        if (detail == null) return emptyList()
        val members = detail.members.orEmpty()
        if (members.isEmpty()) return emptyList()

        val usersById = detail.users.orEmpty()
            .associateBy { it.id }
        val repoOwner = extractGithubOwner(githubUrl)
        val shouldUseOwnerFallback = members.size == 1

        return members.mapNotNull { member ->
            val resolvedName = resolveMemberName(member, usersById)
            if (resolvedName.isNullOrBlank()) return@mapNotNull null

            val roles = buildList {
                member.role?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
                if (member.isAdministrator == true) {
                    add("Администратор")
                }
            }.distinct()

            val identifier = resolveGithubIdentifier(
                member = member,
                resolvedName = resolvedName,
                usersById = usersById,
                repoOwner = repoOwner,
                useOwnerFallback = shouldUseOwnerFallback
            )

            RatingSyncMember(
                name = resolvedName,
                roles = roles,
                identifiers = listOfNotNull(identifier)
            )
        }.distinctBy { it.name }
    }

    private fun resolveMemberName(
        member: Member,
        usersById: Map<String, User>
    ): String? {
        val name = member.name.trim()
        if (name.isNotBlank()) return name

        val userId = member.user?.toString() ?: return null
        return usersById[userId]?.name?.trim()
    }

    private fun resolveGithubIdentifier(
        member: Member,
        resolvedName: String,
        usersById: Map<String, User>,
        repoOwner: String?,
        useOwnerFallback: Boolean
    ): RatingSyncIdentifier? {
        val userName = member.user?.toString()
            ?.let { userId -> usersById[userId]?.name }
            ?.trim()

        val githubLogin = listOf(member.name, resolvedName, userName)
            .mapNotNull(::extractGithubLogin)
            .firstOrNull()
            ?: if (useOwnerFallback) repoOwner else null

        return githubLogin?.let { login ->
            RatingSyncIdentifier(
                platform = "GitHub",
                value = login
            )
        }
    }

    private fun extractGithubOwner(githubUrl: String?): String? {
        if (githubUrl.isNullOrBlank()) return null
        val regex = Regex(
            pattern = "github\\.com/([A-Za-z0-9](?:[A-Za-z0-9-]{0,38}))/",
            option = RegexOption.IGNORE_CASE
        )
        return regex.find(githubUrl)?.groupValues?.getOrNull(1)
    }

    private fun extractGithubLogin(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val value = raw.trim()

        val urlRegex = Regex(
            pattern = "github\\.com/([A-Za-z0-9](?:[A-Za-z0-9-]{0,38}))",
            option = RegexOption.IGNORE_CASE
        )
        val fromUrl = urlRegex.find(value)?.groupValues?.getOrNull(1)
        if (isValidGithubLogin(fromUrl)) return fromUrl

        val mentionRegex = Regex(
            pattern = "@([A-Za-z0-9](?:[A-Za-z0-9-]{0,38}))"
        )
        val fromMention = mentionRegex.find(value)?.groupValues?.getOrNull(1)
        if (isValidGithubLogin(fromMention)) return fromMention

        val dashCandidate = sequenceOf(" - ", " — ")
            .firstNotNullOfOrNull { delimiter ->
                value.substringBefore(delimiter, "").trim().takeIf { it.isNotBlank() }
            }
        if (isValidGithubLogin(dashCandidate)) return dashCandidate

        if (!value.contains(' ') && isValidGithubLogin(value)) {
            return value
        }

        return null
    }

    private fun isValidGithubLogin(candidate: String?): Boolean {
        if (candidate.isNullOrBlank()) return false
        if (candidate.length > 39) return false
        if (candidate.startsWith('-') || candidate.endsWith('-')) return false
        return candidate.matches(Regex("^[A-Za-z0-9-]+$"))
    }

    private fun MetricRankingItem.toProjectRankingItem(
        projectCatalog: ProjectCatalog?,
        currentProject: Project?
    ): RankingItem {
        val project = projectCatalog?.projectsById?.get(id)
        val detailProject = projectCatalog?.detailsByProjectId?.get(id)?.project
        val description = detailProject?.shortDescription
            ?: detailProject?.description
            ?: project?.shortDescription
            ?: project?.description
        val tagIds = detailProject?.tags ?: project?.tags.orEmpty()
        val isCurrentProject = currentProject != null && (
            currentProject.id == id ||
                normalizedEquals(currentProject.name, name)
            )

        return RankingItem(
            key = id,
            title = name,
            score = score,
            scoreText = formatScore(score),
            description = description?.trim()?.takeIf { it.isNotBlank() },
            tags = tagIds
                .mapNotNull { tagId -> projectCatalog?.tagsById?.get(tagId)?.name }
                .distinct(),
            markerLabel = if (isCurrentProject) "Текущий" else null
        )
    }

    private fun MetricRankingItem.toStudentRankingItem(
        studentProjectNames: Map<String, String>,
        currentUserName: String?,
        currentProjectName: String?
    ): RankingItem {
        val normalizedName = normalizeName(name)
        val isCurrentUser = currentUserName != null &&
            normalizedName.isNotBlank() &&
            normalizedName == normalizeName(currentUserName)

        return RankingItem(
            key = id,
            title = name,
            score = score,
            scoreText = formatScore(score),
            projectName = if (isCurrentUser) {
                currentProjectName ?: studentProjectNames[normalizedName]
            } else {
                studentProjectNames[normalizedName]
            },
            markerLabel = if (isCurrentUser) "Вы" else null
        )
    }

    private suspend fun loadProjectWeeklyMovement(
        projectRatings: List<MetricRankingItem>
    ): Map<String, ProjectRankingMovement> = coroutineScope {
        if (projectRatings.isEmpty()) {
            return@coroutineScope emptyMap()
        }

        val details = projectRatings.map { item ->
            async {
                api.getProjectDetail(item.id).getOrNull()
            }
        }.awaitAll()

        ProjectRankingHistoryHack.buildWeeklyMovement(
            currentRatings = projectRatings,
            details = details
        )
    }

    private fun formatScore(value: Double?): String {
        if (value == null) return "N/A"
        val rounded = (value * 100).roundToInt()
        val intPart = rounded / 100
        val fracPart = abs(rounded % 100)
        return "${intPart}.${fracPart.toString().padStart(2, '0')}"
    }

    private fun normalizeName(value: String?): String {
        return value
            ?.trim()
            ?.lowercase()
            ?.replace(Regex("\\s+"), " ")
            .orEmpty()
    }

    private fun normalizedEquals(first: String?, second: String?): Boolean {
        val left = normalizeName(first)
        val right = normalizeName(second)
        return left.isNotBlank() && left == right
    }
}

private data class ProjectCatalog(
    val projects: List<Project>,
    val tagsById: Map<Int, Tag>,
    val detailsByProjectId: Map<String, ProjectDetailResponse?>
) {
    val projectsById: Map<String, Project> = projects.associateBy { it.id }
}
