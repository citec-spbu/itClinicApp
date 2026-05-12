package com.spbu.projecttrack.rating.data.repository

import com.spbu.projecttrack.core.time.PlatformTime
import com.spbu.projecttrack.projects.data.api.ProjectsApi
import com.spbu.projecttrack.projects.data.model.Member
import com.spbu.projecttrack.projects.data.model.Project
import com.spbu.projecttrack.projects.data.model.ProjectDetailResponse
import com.spbu.projecttrack.projects.data.model.Tag
import com.spbu.projecttrack.projects.data.model.User
import com.spbu.projecttrack.projects.presentation.util.extractGithubUrl
import com.spbu.projecttrack.rating.data.api.MetricApi
import com.spbu.projecttrack.rating.data.model.MetricProjectDetail
import com.spbu.projecttrack.rating.data.model.MetricProjectInList
import com.spbu.projecttrack.rating.data.model.MetricRankingItem
import com.spbu.projecttrack.rating.data.model.RankingData
import com.spbu.projecttrack.rating.data.model.RankingFilters
import com.spbu.projecttrack.rating.data.model.RankingItem
import com.spbu.projecttrack.rating.data.model.RatingSyncIdentifier
import com.spbu.projecttrack.rating.data.model.RatingSyncMember
import com.spbu.projecttrack.rating.data.model.RatingSyncProject
import com.spbu.projecttrack.user.data.api.UserProfileApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

class RankingRepository(
    private val api: MetricApi,
    private val projectsApi: ProjectsApi,
    private val userProfileApi: UserProfileApi,
) : IRankingRepository {
    private var hasSynced = false
    private var cachedProjectCatalog: ProjectCatalog? = null
    private var cachedSource: RankingSource? = null

    override suspend fun loadRatings(
        filters: RankingFilters,
        forceRefresh: Boolean,
    ): Result<RankingData> {
        return runCatching {
            val source = getSource(forceRefresh).getOrThrow()
            // buildRankingData is pure CPU work — run it on Default to avoid blocking
            // the Main thread (which would freeze animations on cached re-entry).
            withContext(Dispatchers.Default) {
                buildRankingData(source, filters)
            }
        }
    }

    private suspend fun getSource(forceRefresh: Boolean): Result<RankingSource> {
        cachedSource?.takeIf { !forceRefresh }?.let { return Result.success(it) }

        val projectCatalog = getProjectCatalog(forceRefresh).getOrNull()
        val profile = userProfileApi.getProfile().getOrNull()

        if (projectCatalog != null) {
            syncProjects(projectCatalog)
        }

        val metricProjects = api.getProjects().getOrElse {
            throw it
        }
        val studentRatings = api.getStudentRatings().getOrNull().orEmpty()
        val metricDetails = loadMetricProjectDetails(metricProjects)

        val studentProjectNames = buildMap {
            putAll(projectCatalog?.let(::buildRegistryStudentProjectNames).orEmpty())
            putAll(buildMetricStudentProjectNames(metricDetails.values))
        }
        val studentDisplayNames = buildMap {
            putAll(projectCatalog?.let(::buildStudentDisplayNames).orEmpty())
            putAll(buildMetricStudentDisplayNames(metricDetails.values))
        }

        return Result.success(
            RankingSource(
                projectCatalog = projectCatalog,
                metricProjects = metricProjects,
                metricDetailsById = metricDetails,
                currentProject = profile?.projects.orEmpty().firstOrNull(),
                currentUserName = profile?.user?.fullName?.displayName()?.trim()?.takeIf { it.isNotBlank() },
                currentProjectName = profile?.projects.orEmpty().firstOrNull()?.name,
                studentProjectNames = studentProjectNames,
                studentDisplayNames = studentDisplayNames,
                studentRatings = studentRatings,
            ).also { cachedSource = it }
        )
    }

    private fun buildRankingData(
        source: RankingSource,
        filters: RankingFilters,
    ): RankingData {
        val nowMillis = PlatformTime.currentTimeMillis()
        val movementBaseMillis = nowMillis - WEEK_MILLIS

        val projectEntries = source.metricProjects.map { project ->
            val detail = source.metricDetailsById[project.id]
            val calculatedScore = detail?.let {
                RankingScoreEngine.calculateProjectScore(it, filters, nowMillis)
            }
            ProjectRankingEntry(
                project = project,
                detail = detail,
                // Fall back to the API grade only when metrics never loaded.
                // If metrics exist but produce no score, show "—" instead of stale API data.
                score = calculatedScore ?: if (detail == null) fallbackProjectScore(project) else null,
            )
        }
        val projectMovement = buildMovement(
            currentScores = projectEntries.map { entry ->
                ScoreRecord(
                    key = entry.project.id,
                    title = entry.project.name,
                    score = entry.score,
                )
            },
            previousScores = projectEntries.associate { entry ->
                entry.project.id to entry.detail?.let { detail ->
                    RankingScoreEngine.calculateProjectScore(detail, filters, movementBaseMillis)
                }
            },
        )
        val projectItems = projectEntries.sortedProjectEntries()
            .map { entry ->
                val movement = projectMovement[entry.project.id]
                entry.toRankingItem(
                    source = source,
                    movement = movement,
                )
            }

        val studentEntries = buildStudentEntries(
            source = source,
            filters = filters,
            baseNowMillis = nowMillis,
        )
        val previousStudentEntries = buildStudentEntries(
            source = source,
            filters = filters,
            baseNowMillis = movementBaseMillis,
        )
        val studentMovement = buildMovement(
            currentScores = studentEntries.map { entry ->
                ScoreRecord(
                    key = entry.key,
                    title = entry.title,
                    score = entry.score,
                )
            },
            previousScores = previousStudentEntries.associate { entry -> entry.key to entry.score },
        )
        val studentItems = studentEntries.sortedStudentEntries()
            .map { entry ->
                val movement = studentMovement[entry.key]
                entry.toRankingItem(
                    source = source,
                    movement = movement,
                )
            }

        return RankingData(
            projects = projectItems,
            students = studentItems,
            currentUserName = source.currentUserName,
            currentUserProjectName = source.currentProjectName,
        )
    }

    private suspend fun getProjectCatalog(forceRefresh: Boolean): Result<ProjectCatalog> {
        cachedProjectCatalog?.takeIf { !forceRefresh }?.let { return Result.success(it) }

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

        return Result.success(
            ProjectCatalog(
                projects = allProjects,
                tagsById = tagsById,
                detailsByProjectId = detailsByProjectId,
            ).also { cachedProjectCatalog = it }
        )
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
                id = project.slug?.takeIf { it.isNotBlank() } ?: project.id,
                name = project.name,
                description = detailProject?.description ?: project.description,
                dateStart = detailProject?.dateStart ?: project.dateStart,
                dateEnd = detailProject?.dateEnd ?: project.dateEnd,
                githubUrl = githubUrl,
                members = members,
            )
        }

        val result = api.syncProjects(syncProjects)
        if (result.isSuccess) {
            hasSynced = true
        }
        return result
    }

    private fun buildRegistryStudentProjectNames(
        projectCatalog: ProjectCatalog,
    ): Map<String, String> {
        val studentProjects = mutableMapOf<String, String>()

        projectCatalog.projects.forEach { project ->
            val detail = projectCatalog.detailsByProjectId[project.id] ?: return@forEach
            val projectName = detail.project?.name?.trim().takeIf { !it.isNullOrBlank() } ?: project.name
            val usersById = detail.users.orEmpty().associateBy { it.id }

            detail.users.orEmpty().forEach { user ->
                studentProjects.registerStudentProject(
                    studentName = user.name,
                    projectName = projectName,
                )
            }
            detail.members.orEmpty().forEach { member ->
                val resolvedName = resolveMemberName(member, usersById)
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: return@forEach

                studentProjects.registerStudentProject(
                    studentName = resolvedName,
                    projectName = projectName,
                )
            }
        }

        return studentProjects
    }

    private fun buildStudentDisplayNames(
        projectCatalog: ProjectCatalog,
    ): Map<String, String> {
        val studentNames = mutableMapOf<String, String>()

        projectCatalog.projects.forEach { project ->
            val detail = projectCatalog.detailsByProjectId[project.id] ?: return@forEach
            val usersById = detail.users.orEmpty().associateBy { it.id }

            detail.users.orEmpty().forEach { user ->
                studentNames.registerStudentDisplayName(user.name)
            }
            detail.members.orEmpty().forEach { member ->
                studentNames.registerStudentDisplayName(resolveMemberName(member, usersById))
            }
        }

        return studentNames
    }

    private fun buildMetricStudentProjectNames(
        details: Collection<MetricProjectDetail>,
    ): Map<String, String> {
        val studentProjects = mutableMapOf<String, String>()

        details.forEach { detail ->
            val projectName = detail.name.trim().takeIf { it.isNotBlank() } ?: return@forEach
            detail.users.forEach { user ->
                studentProjects.registerStudentProject(
                    studentName = user.name,
                    projectName = projectName,
                )
            }
        }

        return studentProjects
    }

    private fun buildMetricStudentDisplayNames(
        details: Collection<MetricProjectDetail>,
    ): Map<String, String> {
        val studentNames = mutableMapOf<String, String>()
        details.forEach { detail ->
            detail.users.forEach { user ->
                studentNames.registerStudentDisplayName(user.name)
            }
        }
        return studentNames
    }

    private suspend fun fetchProjectDetail(project: Project): ProjectDetailResponse? {
        val projectKey = project.slug?.takeIf { it.isNotBlank() } ?: project.id
        return projectsApi.getProjectById(projectKey).getOrNull()
    }

    private fun buildMembers(
        detail: ProjectDetailResponse?,
        githubUrl: String?,
    ): List<RatingSyncMember> {
        if (detail == null) return emptyList()
        val members = detail.members.orEmpty()
        if (members.isEmpty()) return emptyList()

        val usersById = detail.users.orEmpty().associateBy { it.id }
        val repoOwner = extractGithubOwner(githubUrl)
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
            )

            RatingSyncMember(
                name = resolvedName,
                roles = roles,
                identifiers = listOfNotNull(identifier),
            )
        }.distinctBy { it.name }
    }

    private fun resolveMemberName(
        member: Member,
        usersById: Map<String, User>,
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
    ): RatingSyncIdentifier? {
        val user = member.user?.toString()
            ?.let { userId -> usersById[userId] }
        val userName = user?.name?.trim()
        val explicitGithubLogin = user?.githubLogin
            ?.trim()
            ?.takeIf { isValidGithubLogin(it) }

        val githubLogin = listOfNotNull(explicitGithubLogin)
            .plus(listOf(member.name, resolvedName, userName).mapNotNull(::extractGithubLogin))
            .firstOrNull()
            ?: repoOwner?.takeIf {
                shouldUseRepoOwnerAsIdentifier(
                    repoOwner = it,
                    candidates = listOfNotNull(resolvedName, userName, explicitGithubLogin),
                )
            }

        return githubLogin?.let { login ->
            RatingSyncIdentifier(
                platform = "GitHub",
                value = login,
            )
        }
    }

    private fun extractGithubOwner(githubUrl: String?): String? {
        if (githubUrl.isNullOrBlank()) return null
        val regex = Regex(
            pattern = "github\\.com/([A-Za-z0-9](?:[A-Za-z0-9-]{0,38}))/",
            option = RegexOption.IGNORE_CASE,
        )
        return regex.find(githubUrl)?.groupValues?.getOrNull(1)
    }

    private fun extractGithubLogin(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val value = raw.trim()

        val urlRegex = Regex(
            pattern = "github\\.com/([A-Za-z0-9](?:[A-Za-z0-9-]{0,38}))",
            option = RegexOption.IGNORE_CASE,
        )
        val fromUrl = urlRegex.find(value)?.groupValues?.getOrNull(1)
        if (isValidGithubLogin(fromUrl)) return fromUrl

        val mentionRegex = Regex(pattern = "@([A-Za-z0-9](?:[A-Za-z0-9-]{0,38}))")
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

    private fun shouldUseRepoOwnerAsIdentifier(
        repoOwner: String,
        candidates: List<String>,
    ): Boolean {
        val ownerKey = githubKey(repoOwner) ?: return false
        return candidates.any { candidate ->
            val candidateKey = githubKey(candidate) ?: return@any false
            candidateKey == ownerKey ||
                candidateKey.startsWith(ownerKey) ||
                ownerKey.startsWith(candidateKey)
        }
    }

    private fun githubKey(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val normalized = raw.lowercase()
            .filter { it.isLetterOrDigit() }
        return normalized.takeIf { it.isNotBlank() }
    }

    private suspend fun loadMetricProjectDetails(
        metricProjects: List<MetricProjectInList>,
    ): Map<String, MetricProjectDetail> = coroutineScope {
        metricProjects.map { item ->
            async {
                item.id to api.getProjectDetail(item.id).getOrNull()
            }
        }.awaitAll()
            .mapNotNull { (id, detail) -> detail?.let { id to it } }
            .toMap()
    }

    private fun buildStudentEntries(
        source: RankingSource,
        filters: RankingFilters,
        baseNowMillis: Long,
    ): List<StudentRankingEntry> {
        val buckets = linkedMapOf<String, StudentAggregation>()

        source.metricDetailsById.values.forEach { detail ->
            val projectName = detail.name.trim().takeIf { it.isNotBlank() } ?: return@forEach
            val usersByIdentity = detail.users
                .groupBy { personNameKey(it.name) }
                .filterKeys { it.isNotBlank() }

            usersByIdentity.forEach { (identityKey, users) ->
                val mergedLogins = users.flatMap { user ->
                    user.identifiers.mapNotNull { identifier ->
                        identifier.value.trim()
                            .takeIf { identifier.platform.equals("GitHub", ignoreCase = true) && it.isNotBlank() }
                            ?.lowercase()
                    }
                }.toSet()

                val score = if (mergedLogins.isEmpty()) {
                    null
                } else {
                    RankingScoreEngine.calculateUserScore(
                        project = detail,
                        filters = filters,
                        selectedUsers = mergedLogins,
                        baseNowMillis = baseNowMillis,
                    )
                }

                val bucket = buckets.getOrPut(identityKey) { StudentAggregation(identityKey) }
                bucket.displayName = preferDisplayPersonName(
                    bucket.displayName,
                    source.studentDisplayNames[identityKey] ?: users.firstOrNull()?.name,
                )
                bucket.projectNames += projectName
                if (score != null) {
                    bucket.scores += score
                }
            }
        }

        return buckets.values.map { bucket ->
            val title = bucket.displayName?.takeIf { it.isNotBlank() } ?: bucket.identityKey
            val projectName = when {
                source.currentUserName != null && personNameMatches(title, source.currentUserName) -> {
                    source.currentProjectName ?: source.studentProjectNames[bucket.identityKey]
                }

                bucket.projectNames.size == 1 -> bucket.projectNames.firstOrNull()
                else -> source.studentProjectNames[bucket.identityKey] ?: bucket.projectNames.firstOrNull()
            }
            val fallbackScore = resolveStudentFallbackScore(
                ratings = source.studentRatings,
                identityKey = bucket.identityKey,
                displayName = title,
            )

            StudentRankingEntry(
                key = bucket.identityKey,
                title = title,
                projectName = projectName,
                score = bucket.scores.takeIf { it.isNotEmpty() }?.average() ?: fallbackScore,
                markerLabel = if (source.currentUserName != null && personNameMatches(title, source.currentUserName)) {
                    "Вы"
                } else {
                    null
                },
            )
        }
    }

    private fun buildMovement(
        currentScores: List<ScoreRecord>,
        previousScores: Map<String, Double?>,
    ): Map<String, RankingMovement> {
        val currentPositions = currentScores.sortedScoreRecords()
            .mapIndexed { index, item -> item.key to (index + 1) }
            .toMap()

        val titlesByKey = currentScores.associate { it.key to it.title }
        val historicalScores = previousScores.mapNotNull { (key, score) ->
            score?.let {
                ScoreRecord(
                    key = key,
                    title = titlesByKey[key] ?: key,
                    score = it,
                )
            }
        }.sortedScoreRecords()

        val previousPositions = historicalScores.mapIndexed { index, item ->
            item.key to (index + 1)
        }.toMap()

        return currentPositions.mapNotNull { (id, currentPosition) ->
            val previousPosition = previousPositions[id] ?: return@mapNotNull null
            id to RankingMovement(
                previousPosition = previousPosition,
                positionDelta = previousPosition - currentPosition,
            )
        }.toMap()
    }

    private fun ProjectRankingEntry.toRankingItem(
        source: RankingSource,
        movement: RankingMovement?,
    ): RankingItem {
        val projectCatalog = source.projectCatalog
        val projectId = project.id
        val projectName = project.name
        val catalogProject = projectCatalog?.projectsById?.get(projectId)
        val detailProject = projectCatalog?.detailsByProjectId?.get(projectId)?.project
        val description = detailProject?.shortDescription
            ?: detailProject?.description
            ?: catalogProject?.shortDescription
            ?: catalogProject?.description
            ?: detail?.description

        val tagIds = detailProject?.tags ?: catalogProject?.tags.orEmpty()
        val isCurrentProject = source.currentProject != null && (
            source.currentProject.id == projectId || normalizedEquals(source.currentProject.name, projectName)
            )

        return RankingItem(
            key = projectId,
            title = projectName,
            score = score,
            scoreText = formatScore(score),
            description = description?.trim()?.takeIf { it.isNotBlank() },
            tags = tagIds
                .mapNotNull { tagId -> projectCatalog?.tagsById?.get(tagId)?.name }
                .distinct(),
            markerLabel = if (isCurrentProject) "Текущий" else null,
            previousPosition = movement?.previousPosition,
            positionDelta = movement?.positionDelta,
            historyAvailable = movement != null,
        )
    }

    private fun StudentRankingEntry.toRankingItem(
        source: RankingSource,
        movement: RankingMovement?,
    ): RankingItem {
        val title = source.studentDisplayNames[key]
            ?: displayPersonName(title).takeIf { it.isNotBlank() }
            ?: title

        return RankingItem(
            key = key,
            title = title,
            score = score,
            scoreText = formatScore(score),
            projectName = projectName,
            markerLabel = markerLabel,
            previousPosition = movement?.previousPosition,
            positionDelta = movement?.positionDelta,
            historyAvailable = movement != null,
        )
    }

    private fun resolveStudentFallbackScore(
        ratings: List<MetricRankingItem>,
        identityKey: String,
        displayName: String,
    ): Double? {
        return ratings.firstOrNull { item ->
            personNameMatches(item.name, displayName) ||
                personNameKey(item.name) == identityKey
        }?.score
    }

    private fun fallbackProjectScore(
        project: MetricProjectInList,
    ): Double? {
        return project.grade.toScoreOrNull()
    }

    private fun formatScore(value: Double?): String {
        if (value == null) return "—"
        val rounded = (value * 100).roundToInt()
        val intPart = rounded / 100
        val fracPart = abs(rounded % 100)
        return "${intPart}.${fracPart.toString().padStart(2, '0')}"
    }

    private fun normalizeName(value: String?): String {
        return normalizeComparableText(value)
    }

    private fun normalizedEquals(first: String?, second: String?): Boolean {
        val left = normalizeName(first)
        val right = normalizeName(second)
        return left.isNotBlank() && left == right
    }

    private fun MutableMap<String, String>.registerStudentProject(
        studentName: String?,
        projectName: String,
    ) {
        val key = personNameKey(studentName)
        if (key.isBlank()) return
        if (!containsKey(key)) {
            put(key, projectName.trim())
        }
    }

    private fun MutableMap<String, String>.registerStudentDisplayName(
        studentName: String?,
    ) {
        val key = personNameKey(studentName)
        if (key.isBlank()) return

        val preferredName = preferDisplayPersonName(this[key], studentName)
            ?.takeIf { it.isNotBlank() }
            ?: return

        this[key] = preferredName
    }

    private fun String?.toScoreOrNull(): Double? {
        val value = this?.trim()
        if (value.isNullOrEmpty()) return null
        if (value.equals("N/A", ignoreCase = true)) return null
        return value.toDoubleOrNull()
    }

    private companion object {
        const val WEEK_MILLIS = 7L * 24L * 60L * 60L * 1000L
    }
}

private data class ProjectCatalog(
    val projects: List<Project>,
    val tagsById: Map<Int, Tag>,
    val detailsByProjectId: Map<String, ProjectDetailResponse?>,
) {
    val projectsById: Map<String, Project> = projects.associateBy { it.id }
}

private data class RankingSource(
    val projectCatalog: ProjectCatalog?,
    val metricProjects: List<MetricProjectInList>,
    val metricDetailsById: Map<String, MetricProjectDetail>,
    val currentProject: Project?,
    val currentUserName: String?,
    val currentProjectName: String?,
    val studentProjectNames: Map<String, String>,
    val studentDisplayNames: Map<String, String>,
    val studentRatings: List<MetricRankingItem>,
)

private data class ProjectRankingEntry(
    val project: MetricProjectInList,
    val detail: MetricProjectDetail?,
    val score: Double?,
)

private data class StudentRankingEntry(
    val key: String,
    val title: String,
    val projectName: String?,
    val score: Double?,
    val markerLabel: String?,
)

private data class StudentAggregation(
    val identityKey: String,
    var displayName: String? = null,
    val projectNames: MutableSet<String> = linkedSetOf(),
    val scores: MutableList<Double> = mutableListOf(),
)

private data class ScoreRecord(
    val key: String,
    val title: String,
    val score: Double?,
)

private data class RankingMovement(
    val previousPosition: Int,
    val positionDelta: Int,
)

private fun List<ProjectRankingEntry>.sortedProjectEntries(): List<ProjectRankingEntry> {
    return sortedWith(
        compareBy<ProjectRankingEntry> { it.score == null }
            .thenByDescending { it.score ?: Double.NEGATIVE_INFINITY }
            .thenBy { it.project.name.lowercase() }
    )
}

private fun List<StudentRankingEntry>.sortedStudentEntries(): List<StudentRankingEntry> {
    return sortedWith(
        compareBy<StudentRankingEntry> { it.score == null }
            .thenByDescending { it.score ?: Double.NEGATIVE_INFINITY }
            .thenBy { it.title.lowercase() }
    )
}

private fun List<ScoreRecord>.sortedScoreRecords(): List<ScoreRecord> {
    return sortedWith(
        compareBy<ScoreRecord> { it.score == null }
            .thenByDescending { it.score ?: Double.NEGATIVE_INFINITY }
            .thenBy { it.title.lowercase() }
    )
}
