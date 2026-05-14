package com.spbu.projecttrack.projects.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spbu.projecttrack.core.network.toShortMessage
import com.spbu.projecttrack.projects.data.model.Project
import com.spbu.projecttrack.projects.data.model.Tag
import com.spbu.projecttrack.projects.data.repository.IProjectsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProjectsUiState {
    data object Loading : ProjectsUiState()
    data class Success(
        val projects: List<Project>, 
        val tags: List<Tag>,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = true
    ) : ProjectsUiState()
    data class Error(val message: String) : ProjectsUiState()
}

class ProjectsViewModel(
    private val repository: IProjectsRepository,
    private val uiDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ProjectsUiState>(ProjectsUiState.Loading)
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()
    
    private var currentPage = 1
    private var isLoadingMore = false
    
    init {
        loadProjects()
    }
    
    fun loadProjects() {
        viewModelScope.launch(uiDispatcher) {
            _uiState.value = ProjectsUiState.Loading
            currentPage = 1
            
            repository.getProjects(page = currentPage)
                .onSuccess { response ->
                    _uiState.value = ProjectsUiState.Success(
                        projects = response.projects,
                        tags = response.tags,
                        hasMorePages = response.projects.size >= 5
                    )
                }
                .onFailure { error ->
                    _uiState.value = ProjectsUiState.Error(
                        message = error.toShortMessage(
                            "Ошибка загрузки",
                            "Loading error",
                        )
                    )
                }
        }
    }
    
    fun loadMoreProjects() {
        val currentState = _uiState.value
        if (currentState !is ProjectsUiState.Success || isLoadingMore || !currentState.hasMorePages) {
            return
        }
        
        isLoadingMore = true
        _uiState.value = currentState.copy(isLoadingMore = true)
        
        viewModelScope.launch(uiDispatcher) {
            currentPage++
            
            repository.getProjects(page = currentPage)
                .onSuccess { response ->
                    val allProjects = currentState.projects + response.projects
                    val allTags = (currentState.tags + response.tags).distinctBy { it.id }
                    
                    _uiState.value = ProjectsUiState.Success(
                        projects = allProjects,
                        tags = allTags,
                        isLoadingMore = false,
                        hasMorePages = response.projects.size >= 5
                    )
                    isLoadingMore = false
                }
                .onFailure { error ->
                    _uiState.value = currentState.copy(isLoadingMore = false)
                    isLoadingMore = false
                }
        }
    }
    
    fun retry() {
        loadProjects()
    }
}
