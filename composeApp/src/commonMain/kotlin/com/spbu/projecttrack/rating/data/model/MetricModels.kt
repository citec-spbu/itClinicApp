package com.spbu.projecttrack.rating.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class MetricProjectInList(
    val id: String,
    val name: String,
    val description: String? = null,
    val platforms: List<String> = emptyList(),
    val dateStart: String? = null,
    val dateEnd: String? = null,
    val grade: String? = null
)

@Serializable
data class MetricProjectDetail(
    val id: String,
    val name: String,
    val description: String? = null,
    val dateStart: String? = null,
    val dateEnd: String? = null,
    val users: List<MetricProjectUser> = emptyList(),
    val resources: List<MetricProjectResource> = emptyList()
)

@Serializable
data class MetricProjectUser(
    val name: String,
    val roles: List<String> = emptyList(),
    val identifiers: List<MetricIdentifier> = emptyList()
)

@Serializable
data class MetricIdentifier(
    val platform: String,
    val value: String
)

@Serializable
data class MetricProjectResource(
    val id: String,
    val name: String,
    val project: String? = null,
    val params: JsonElement? = null,
    val platform: String? = null,
    val metrics: List<MetricProjectMetric> = emptyList()
)

@Serializable
data class MetricProjectMetric(
    val id: String,
    val name: String,
    val data: List<MetricProjectSnapshot> = emptyList(),
    val resource: String? = null,
    val params: List<MetricProjectMetricParam> = emptyList(),
    val snapshotBased: Boolean? = null,
    val isTracked: Boolean? = null
)

@Serializable
data class MetricProjectSnapshot(
    val error: String? = null,
    val data: JsonElement? = null,
    val timestamp: Double? = null
)

@Serializable
data class MetricProjectMetricParam(
    val type: String? = null,
    val name: String? = null,
    val value: JsonElement? = null
)
