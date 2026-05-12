package com.spbu.projecttrack.rating.data.repository

import com.spbu.projecttrack.rating.data.model.ProjectStatsUiModel

interface IProjectStatsRepository {
    suspend fun loadProjectStats(
        projectId: String,
        selectedRepositoryId: String? = null,
        selectedStartDate: String? = null,
        selectedEndDate: String? = null,
        selectedRapidThresholdMinutes: Int? = null,
        forceRefresh: Boolean = false,
    ): Result<ProjectStatsUiModel>
}
