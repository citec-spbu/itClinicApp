package com.spbu.projecttrack.rating.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RatingSyncMember(
    val name: String,
    val roles: List<String> = emptyList(),
    val identifiers: List<RatingSyncIdentifier> = emptyList()
)

@Serializable
data class RatingSyncIdentifier(
    val platform: String,
    val value: String
)

@Serializable
data class RatingSyncProject(
    val id: String,
    val name: String,
    val description: String? = null,
    val dateStart: String? = null,
    val dateEnd: String? = null,
    val githubUrl: String? = null,
    val members: List<RatingSyncMember> = emptyList()
)

@Serializable
data class RatingSyncRequest(
    val projects: List<RatingSyncProject> = emptyList()
)
