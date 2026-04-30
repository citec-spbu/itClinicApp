package com.spbu.projecttrack.rating.data.model

data class StatsDetailDataUi(
    val participants: List<StatsDetailParticipantUi> = emptyList(),
    val defaultParticipantId: String? = null,
    val commits: List<StatsDetailCommitUi> = emptyList(),
    val issues: List<StatsDetailIssueUi> = emptyList(),
    val pullRequests: List<StatsDetailPullRequestUi> = emptyList(),
)

data class StatsDetailParticipantUi(
    val id: String,
    val name: String,
    val subtitle: String,
    val isCurrentUser: Boolean = false,
)

data class StatsDetailCommitUi(
    val authorId: String? = null,
    val authorName: String,
    val authorAvatarUrl: String? = null,
    val message: String,
    val committedAtIso: String? = null,
    val committedAtLabel: String,
    val url: String? = null,
    val sha: String? = null,
    val additions: Int = 0,
    val deletions: Int = 0,
    val changes: Int = 0,
    val files: List<StatsDetailCommitFileUi> = emptyList(),
)

data class StatsDetailCommitFileUi(
    val fileName: String,
    val additions: Int = 0,
    val deletions: Int = 0,
    val changes: Int = 0,
    val status: String? = null,
)

data class StatsDetailIssueUi(
    val creatorId: String? = null,
    val creatorName: String,
    val creatorAvatarUrl: String? = null,
    val assigneeIds: List<String> = emptyList(),
    val assigneeNames: List<String> = emptyList(),
    val assigneeAvatarUrls: List<String?> = emptyList(),
    val createdAtIso: String? = null,
    val createdAtLabel: String,
    val closedAtIso: String? = null,
    val closedAtLabel: String? = null,
    val title: String,
    val number: Int? = null,
    val state: String? = null,
    val labels: List<String> = emptyList(),
    val comments: Int? = null,
    val thumbsUpCount: Int? = null,
    val thumbsDownCount: Int? = null,
    val url: String? = null,
)

data class StatsDetailPullRequestUi(
    val authorId: String? = null,
    val authorName: String,
    val assigneeIds: List<String> = emptyList(),
    val assigneeNames: List<String> = emptyList(),
    val createdAtIso: String? = null,
    val createdAtLabel: String,
    val closedAtIso: String? = null,
    val closedAtLabel: String? = null,
    val effectiveEndAtIso: String? = null,
    val title: String,
    val number: Int? = null,
    val state: String? = null,
    val comments: Int? = null,
    val commitsCount: Int? = null,
    val additions: Int? = null,
    val deletions: Int? = null,
    val changedFiles: Int? = null,
    val url: String? = null,
)
