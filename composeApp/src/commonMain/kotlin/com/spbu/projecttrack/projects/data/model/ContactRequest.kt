package com.spbu.projecttrack.projects.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ContactRequest(
    val name: String,
    val email: String
)
