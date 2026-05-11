package com.spbu.projecttrack.core.di

import com.spbu.projecttrack.core.network.HttpClientFactory
import com.spbu.projecttrack.core.auth.MobileAuthApi
import com.spbu.projecttrack.projects.data.api.ContactRequestApi
import com.spbu.projecttrack.projects.data.api.MemberApi
import com.spbu.projecttrack.projects.data.api.ProjectsApi
import com.spbu.projecttrack.user.data.api.UserRoleApi
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
    private val userRoleApi by lazy { UserRoleApi(httpClient) }
    private val memberApi by lazy { MemberApi(httpClient) }
    private val metricApi by lazy { MetricApi(httpClient) }
    private val mobileAuthApi by lazy { MobileAuthApi(httpClient) }
    
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
            projectsApi = projectsApi,
            userProfileApi = userProfileApi
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

    fun provideProjectsApi(): ProjectsApi {
        return projectsApi
    }

    fun provideUserProfileApi(): UserProfileApi {
        return userProfileApi
    }

    fun provideUserRoleApi(): UserRoleApi {
        return userRoleApi
    }

    fun provideMobileAuthApi(): MobileAuthApi {
        return mobileAuthApi
    }

    fun provideMemberApi(): MemberApi {
        return memberApi
    }

    // Keep a single instance so tab switches do not trigger a full ranking reload.
    private val rankingViewModel by lazy { RankingViewModel(rankingRepository) }

    fun provideRankingViewModel(): RankingViewModel {
        return rankingViewModel
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
