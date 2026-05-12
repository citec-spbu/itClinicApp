package com.spbu.projecttrack.projects.data.repository

import com.spbu.projecttrack.projects.data.model.ProjectsResponse

interface IProjectsRepository {
    suspend fun getProjects(page: Int = 1): Result<ProjectsResponse>
}
