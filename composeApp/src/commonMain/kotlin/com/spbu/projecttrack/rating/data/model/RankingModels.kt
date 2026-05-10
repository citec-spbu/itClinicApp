package com.spbu.projecttrack.rating.data.model

data class RankingItem(
    val key: String,
    val title: String,
    val score: Double?,
    val scoreText: String,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val projectName: String? = null,
    val markerLabel: String? = null,
    val previousPosition: Int? = null,
    val positionDelta: Int? = null,
    val historyAvailable: Boolean = false
)

data class RankingData(
    val projects: List<RankingItem>,
    val students: List<RankingItem>,
    val currentUserName: String? = null,
    val currentUserProjectName: String? = null,
)
