package com.spbu.projecttrack.user.data.model

import com.spbu.projecttrack.projects.data.model.Project
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileResponse(
    val projects: List<Project> = emptyList(),
    val user: UserProfileUser = UserProfileUser()
)

@Serializable
data class UserProfileUser(
    val email: String = "",
    val phone: String = "",
    val githubLogin: String = "",
    val fullName: UserProfileFullName = UserProfileFullName()
)

@Serializable
data class UserProfileFullName(
    val name: String = "",
    val surname: String = "",
    val patronymic: String = ""
) {
    fun displayName(): String = listOf(surname, name, patronymic)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(" ")
}

@Serializable
data class EditPersonalDataRequest(
    val name: String,
    val surname: String,
    val patronymic: String,
)

@Serializable
data class EditAccountDataRequest(
    val email: String,
    val phone: String,
)
