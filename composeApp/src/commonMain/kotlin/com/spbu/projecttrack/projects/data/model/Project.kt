package com.spbu.projecttrack.projects.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProjectsResponse(
    val projects: List<Project> = emptyList(),
    val tags: List<Tag> = emptyList()
)

@Serializable
data class ProjectDetailResponse(
    val project: ProjectDetail? = null,
    val tags: List<Tag> = emptyList(),
    val teams: List<Team>? = null,
    val members: List<Member>? = null,
    val users: List<User>? = null
)

@Serializable
data class FindManyRequest(
    val filters: Map<String, String> = emptyMap(),
    val page: Int = 1
)

@Serializable
data class Project(
    val id: String,
    val name: String,
    val description: String? = null,
    val shortDescription: String? = null,
    val dateStart: String? = null,
    val dateEnd: String? = null,
    val slug: String? = null,
    val tags: List<Int>? = null,
    val client: String? = null, // Заказчик
    val links: List<ProjectLink> = emptyList(),
    val teams: List<Int>? = null,
    val teamLimit: Int? = null,
)

@Serializable
data class ProjectDetail(
    val id: String,
    val name: String,
    val description: String? = null,
    val shortDescription: String? = null,
    val dateStart: String? = null,
    val dateEnd: String? = null,
    val slug: String? = null,
    val tags: List<Int>? = null,
    val team: String? = null,
    val status: String? = null,
    @SerialName("teamLimit")
    val teamLimit: Int? = null,
    val client: String? = null, // Заказчик

    @SerialName("clientContact")
    val contact: String? = null, // Контактное лицо

    @SerialName("projectRequirements")
    val requirements: List<String>? = null, // Требования проекта

    @SerialName("developerRequirements")
    val executorRequirements: List<String>? = null, // Требования для исполнителей
    val links: List<ProjectLink> = emptyList(),
)

@Serializable
data class ProjectLink(
    val id: Int,
    val platform: String? = null,
    val link: String? = null,
)

@Serializable
data class Tag(
    val id: Int,
    val name: String,
    val description: String? = null
)

@Serializable
data class Team(
    val id: Int,
    val name: String,
    val description: String? = null,
    val members: List<Int>? = null,
    val project: String? = null,
    val administrators: List<Int>? = null,
    val documents: List<String>? = null
)

@Serializable
data class Member(
    val id: Int,
    val name: String,
    val role: String ? = null,
    val isAdministrator: Boolean? = null,
    val user: Int? = null,
    val team: Int? = null
)

@Serializable
data class User(
    val id: String,
    val name: String? = null
)
