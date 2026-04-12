package com.spbu.projecttrack.projects.data.model

import kotlinx.serialization.Serializable

@Serializable
data class EmailRequest(
    val name: String = "",
    val email: String = "",
    val subject: String? = null,
    val message: String? = null,
    val to: String? = null,
)
