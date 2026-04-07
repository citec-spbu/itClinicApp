package com.spbu.projecttrack.projects.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spbu.projecttrack.core.time.PlatformTime
import com.spbu.projecttrack.projects.data.model.*
import com.spbu.projecttrack.projects.data.repository.ProjectsRepository
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProjectDetailUiState {
    data object Loading : ProjectDetailUiState()
    data class Success(
        val project: ProjectDetail,
        val tags: List<Tag>,
        val teams: List<Team>,
        val members: List<Member>,
        val users: List<User>,
        val statusText: String
    ) : ProjectDetailUiState()
    data class Error(val message: String) : ProjectDetailUiState()
}

enum class ProjectStage {
    Completed,
    Hiring,
    Active
}

class ProjectDetailViewModel(
    private val repository: ProjectsRepository,
    private val projectId: String
) : ViewModel() {
    private companion object {
        private const val TAG = "ProjectDetailVM"
    }

    private val _uiState = MutableStateFlow<ProjectDetailUiState>(ProjectDetailUiState.Loading)
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()

    init {
        loadProjectDetail()
    }

    fun loadProjectDetail() {
        viewModelScope.launch {
            _uiState.value = ProjectDetailUiState.Loading

            repository.getProjectById(projectId)
                .onSuccess { response ->
                    val project = response.project
                    if (project != null) {
                        println(buildString {
                            appendLine("[$TAG] ProjectDetail:")
                            appendLine("  id: ${project.id}")
                            appendLine("  name: ${project.name}")
                            appendLine("  description: ${project.description ?: "<null>"}")
                            appendLine("  shortDescription: ${project.shortDescription ?: "<null>"}")
                            appendLine("  dateStart: ${project.dateStart ?: "<null>"}")
                            appendLine("  dateEnd: ${project.dateEnd ?: "<null>"}")
                            appendLine("  slug: ${project.slug ?: "<null>"}")
                            appendLine("  tags: ${project.tags?.joinToString() ?: "<null>"}")
                            appendLine("  team: ${project.team ?: "<null>"}")
                            appendLine("  status: ${project.status ?: "<null>"}")
                            appendLine("  client: ${project.client ?: "<null>"}")
                            appendLine("  contact: ${project.contact ?: "<null>"}")
                            appendLine("  requirements: ${project.requirements?.joinToString(prefix = "[", postfix = "]") ?: "<null>"}")
                            appendLine("  executorRequirements: ${project.executorRequirements?.joinToString(prefix = "[", postfix = "]") ?: "<null>"}")
                        })

                        _uiState.value = ProjectDetailUiState.Success(
                            project = project,
                            tags = response.tags,
                            teams = response.teams ?: emptyList(),
                            members = response.members ?: emptyList(),
                            users = response.users ?: emptyList(),
                            statusText = resolveProjectStatusText(
                                project = project,
                                teams = response.teams ?: emptyList()
                            )
                        )
                    } else {
                        _uiState.value = ProjectDetailUiState.Error("Проект не найден")
                    }
                }
                .onFailure { error ->
                    _uiState.value = ProjectDetailUiState.Error(
                        message = error.message ?: "Неизвестная ошибка"
                    )
                }
        }
    }

    fun retry() {
        loadProjectDetail()
    }
}

private fun resolveProjectStatusText(project: ProjectDetail, teams: List<Team>): String {
    val computedStage = computeProjectStage(project, teams)
    if (computedStage != null) {
        return when (computedStage) {
            ProjectStage.Completed -> "Проект завершен"
            ProjectStage.Hiring -> "На проект идет набор"
            ProjectStage.Active -> "Назначена команда"
        }
    }

    return mapStatusToDisplay(project.status)
}

private fun computeProjectStage(project: ProjectDetail, teams: List<Team>): ProjectStage? {
    val dateEnd = project.dateEnd?.take(10)?.trim().orEmpty()
    val parsedEndDate = parseLocalDate(dateEnd) ?: return null
    val today = Instant
        .fromEpochMilliseconds(PlatformTime.currentTimeMillis())
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
    if (parsedEndDate < today) return ProjectStage.Completed

    val teamLimit = project.teamLimit ?: return null
    val availableSlots = teamLimit - teams.size
    return if (availableSlots > 0) ProjectStage.Hiring else ProjectStage.Active
}

private fun parseLocalDate(value: String): LocalDate? {
    if (value.isBlank()) return null
    return runCatching { LocalDate.parse(value) }.getOrNull()
}

private fun mapStatusToDisplay(status: String?): String {
    return when (status?.lowercase()) {
        "assigned", "team_assigned" -> "Назначена команда"
        "in_progress", "active" -> "В процессе"
        "completed", "done" -> "Проект завершен"
        "new", "open" -> "Открыт для записи"
        "pending" -> "Ожидание"
        "cancelled" -> "Отменен"
        else -> status ?: "Не указан"
    }
}
