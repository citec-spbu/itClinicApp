package com.spbu.projecttrack.core.di

import com.spbu.projecttrack.core.network.HttpClientFactory
import com.spbu.projecttrack.projects.data.api.ContactRequestApi
import com.spbu.projecttrack.projects.data.api.ProjectsApi
import com.spbu.projecttrack.user.data.api.UserProfileApi
import com.spbu.projecttrack.projects.data.repository.ProjectsRepository
import com.spbu.projecttrack.projects.presentation.ProjectsViewModel
import com.spbu.projecttrack.projects.presentation.detail.ProjectDetailViewModel
import com.spbu.projecttrack.rating.data.api.MetricApi
import com.spbu.projecttrack.rating.data.repository.ProjectStatsRepository
import com.spbu.projecttrack.rating.data.repository.RankingRepository
import com.spbu.projecttrack.rating.data.repository.UserStatsRepository
import com.spbu.projecttrack.rating.presentation.RankingViewModel
import com.spbu.projecttrack.rating.presentation.projectstats.ProjectStatsViewModel
import com.spbu.projecttrack.rating.presentation.userstats.UserStatsViewModel

object DependencyContainer {
    
    private val httpClient by lazy { HttpClientFactory.create() }
    
    private val projectsApi by lazy { ProjectsApi(httpClient) }
    private val contactRequestApi by lazy { ContactRequestApi(httpClient) }
    private val userProfileApi by lazy { UserProfileApi(httpClient) }
    private val metricApi by lazy { MetricApi(httpClient) }
    
    private val projectsRepository by lazy { ProjectsRepository(projectsApi) }
    private val rankingRepository by lazy {
        RankingRepository(
            api = metricApi,
            projectsApi = projectsApi,
            userProfileApi = userProfileApi
        )
    }
    private val projectStatsRepository by lazy {
        ProjectStatsRepository(
            metricApi = metricApi,
            projectsApi = projectsApi,
            userProfileApi = userProfileApi
        )
    }
    private val userStatsRepository by lazy {
        UserStatsRepository(
            metricApi = metricApi,
            projectsApi = projectsApi
        )
    }
    
    fun provideProjectsViewModel(): ProjectsViewModel {
        return ProjectsViewModel(projectsRepository)
    }
    
    fun provideProjectDetailViewModel(projectId: String): ProjectDetailViewModel {
        return ProjectDetailViewModel(projectsRepository, projectId)
    }

    fun provideContactRequestApi(): ContactRequestApi {
        return contactRequestApi
    }

    fun provideUserProfileApi(): UserProfileApi {
        return userProfileApi
    }

    fun provideRankingViewModel(): RankingViewModel {
        return RankingViewModel(rankingRepository)
    }

    fun provideProjectStatsViewModel(projectId: String): ProjectStatsViewModel {
        return ProjectStatsViewModel(
            repository = projectStatsRepository,
            projectId = projectId
        )
    }

    fun provideUserStatsViewModel(
        userId: String,
        userName: String,
        preferredProjectName: String?,
    ): UserStatsViewModel {
        return UserStatsViewModel(
            repository = userStatsRepository,
            userId = userId,
            userName = userName,
            preferredProjectName = preferredProjectName
        )
    }
}
