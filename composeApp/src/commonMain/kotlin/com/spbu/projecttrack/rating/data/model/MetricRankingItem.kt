package com.spbu.projecttrack.rating.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MetricRankingItem(
    val id: String,
    val name: String,
    val score: Double? = null
)
