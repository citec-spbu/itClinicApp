package com.spbu.projecttrack.rating.data.model

data class UserStatsUiModel(
    val userId: String,
    val userName: String,
    val role: String,
    val projectId: String,
    val projectTitle: String,
    val repositories: List<ProjectStatsRepositoryUi>,
    val selectedRepositoryId: String,
    val visibleRange: ProjectStatsDateRangeUi,
    val teamRank: Int?,
    val commits: ProjectStatsMetricSectionUi,
    val issues: ProjectStatsIssueSectionUi,
    val pullRequests: ProjectStatsMetricSectionUi,
    val rapidPullRequests: ProjectStatsMetricSectionUi,
    val codeChurn: ProjectStatsCodeChurnSectionUi,
    val codeOwnership: ProjectStatsOwnershipSectionUi,
    val dominantWeekDay: ProjectStatsWeekDaySectionUi,
    val details: StatsDetailDataUi,
    val rapidThreshold: ProjectStatsThresholdUi,
    val showOverallRatingButton: Boolean = true,
)
