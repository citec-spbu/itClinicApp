package com.spbu.projecttrack.rating.data.model

data class ProjectStatsUiModel(
    val projectId: String,
    val title: String,
    val customer: String,
    val members: List<ProjectStatsMemberUi>,
    val repositories: List<ProjectStatsRepositoryUi>,
    val selectedRepositoryId: String,
    val visibleRange: ProjectStatsDateRangeUi,
    val commits: ProjectStatsMetricSectionUi,
    val issues: ProjectStatsIssueSectionUi,
    val pullRequests: ProjectStatsMetricSectionUi,
    val rapidPullRequests: ProjectStatsMetricSectionUi,
    val codeChurn: ProjectStatsCodeChurnSectionUi,
    val codeOwnership: ProjectStatsOwnershipSectionUi,
    val dominantWeekDay: ProjectStatsWeekDaySectionUi,
    val details: StatsDetailDataUi,
    val rapidThreshold: ProjectStatsThresholdUi,
    val showOverallRatingButton: Boolean = false
)

data class ProjectStatsMemberUi(
    val userId: String? = null,
    val login: String? = null,
    val name: String,
    val role: String,
    val isCurrentUser: Boolean = false
)

data class ProjectStatsRepositoryUi(
    val id: String,
    val title: String,
    val subtitle: String? = null
)

data class ProjectStatsDateRangeUi(
    val startIsoDate: String,
    val endIsoDate: String,
    val startLabel: String,
    val endLabel: String
)

data class ProjectStatsThresholdUi(
    val totalMinutes: Int,
    val days: Int,
    val hours: Int,
    val minutes: Int
)

data class ProjectStatsMetricSectionUi(
    val title: String,
    val score: Double?,
    val primaryValue: String,
    val primaryCaption: String,
    val supplementaryValue: String? = null,
    val supplementaryCaption: String? = null,
    val rank: Int?,
    val rankCaption: String,
    val chartTitle: String,
    val chartType: ProjectStatsChartType,
    val chartPoints: List<ProjectStatsChartPointUi>,
    val tableTitle: String,
    val tableRows: List<ProjectStatsMetricRowUi>,
    val tooltipTitle: String
)

data class ProjectStatsIssueSectionUi(
    val title: String,
    val score: Double?,
    val openIssues: Int,
    val closedIssues: Int,
    val progress: Float,
    val remainingText: String,
    val rank: Int?,
    val tableRows: List<ProjectStatsMetricRowUi>
)

data class ProjectStatsCodeChurnSectionUi(
    val title: String,
    val score: Double?,
    val changedFilesCount: Int,
    val rank: Int?,
    val fileRows: List<ProjectStatsFileRowUi>,
    val tableRows: List<ProjectStatsMetricRowUi>
)

data class ProjectStatsOwnershipSectionUi(
    val title: String,
    val score: Double?,
    val rank: Int?,
    val slices: List<ProjectStatsDonutSliceUi>
)

data class ProjectStatsWeekDaySectionUi(
    val title: String,
    val score: Double?,
    val headline: String,
    val subtitle: String,
    val slices: List<ProjectStatsDonutSliceUi>
)

data class ProjectStatsChartPointUi(
    val label: String,
    val value: Float,
    val valueLabel: String,
    val hint: String
)

data class ProjectStatsMetricRowUi(
    val name: String,
    val value: String,
    val highlight: Boolean = false
)

data class ProjectStatsFileRowUi(
    val fileName: String,
    val value: String
)

data class ProjectStatsDonutSliceUi(
    val label: String,
    val secondaryLabel: String,
    val percentLabel: String,
    val value: Float,
    val colorHex: Long,
    val highlight: Boolean = false
)

enum class ProjectStatsChartType {
    Bars,
    Line
}

data class ProjectStatsExportDocument(
    val title: String,
    val generatedAt: String,
    val rangeLabel: String,
    val repositories: String,
    val sections: List<ProjectStatsExportSection>
)

data class ProjectStatsExportSection(
    val title: String,
    val summaryLines: List<String>,
    val rows: List<Pair<String, String>> = emptyList()
)
