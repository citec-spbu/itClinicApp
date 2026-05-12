package com.spbu.projecttrack.projects.data.repository

import com.spbu.projecttrack.projects.data.api.ProjectsApi
import com.spbu.projecttrack.projects.data.model.ProjectsResponse
import com.spbu.projecttrack.projects.data.model.ProjectDetailResponse

class ProjectsRepository(private val api: ProjectsApi) : IProjectsRepository {
    
    override suspend fun getProjects(page: Int): Result<ProjectsResponse> {
        return api.getProjects(page)
    }
    
    suspend fun getAllProjects(): Result<ProjectsResponse> {
        return api.getAllProjects()
    }
    
    suspend fun getActiveProjects(): Result<ProjectsResponse> {
        return api.getActiveProjects()
    }
    
    suspend fun getNewProjects(): Result<ProjectsResponse> {
        return api.getNewProjects()
    }
    
    suspend fun getProjectById(id: String): Result<ProjectDetailResponse> {
        return api.getProjectById(id)
    }

    suspend fun editMemberRole(memberId: Int, role: String): Result<Unit> {
        return api.editMemberRole(memberId, role)
    }
}
