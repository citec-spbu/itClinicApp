package com.spbu.projecttrack.rating.presentation.projectstats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spbu.projecttrack.rating.data.model.ProjectStatsUiModel
import com.spbu.projecttrack.rating.data.repository.ProjectStatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ProjectStatsUiState {
    data object Loading : ProjectStatsUiState()
    data class Success(val data: ProjectStatsUiModel) : ProjectStatsUiState()
    data class Error(val message: String) : ProjectStatsUiState()
}

class ProjectStatsViewModel(
    private val repository: ProjectStatsRepository,
    private val projectId: String
) : ViewModel() {
    private val _uiState = MutableStateFlow<ProjectStatsUiState>(ProjectStatsUiState.Loading)
    val uiState: StateFlow<ProjectStatsUiState> = _uiState.asStateFlow()
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var selectedRepositoryId: String? = null
    private var selectedStartDate: String? = null
    private var selectedEndDate: String? = null
    private var rapidThresholdMinutes: Int? = null
    private var hasLoaded = false

    fun load(force: Boolean = false) {
        if (hasLoaded && !force) return
        request(forceRefresh = force, preservePreviousSuccessOnFailure = false)
    }

    fun retry() {
        hasLoaded = false
        request(forceRefresh = true, preservePreviousSuccessOnFailure = false)
    }

    fun refresh() {
        request(forceRefresh = true, preservePreviousSuccessOnFailure = true, refreshing = true)
    }

    fun selectRepository(repositoryId: String) {
        selectedRepositoryId = repositoryId
        request(forceRefresh = true, preservePreviousSuccessOnFailure = false)
    }

    fun selectStartDate(isoDate: String) {
        selectedStartDate = isoDate
        if (!selectedEndDate.isNullOrBlank() && selectedEndDate!! < isoDate) {
            selectedEndDate = isoDate
        }
        request(forceRefresh = true, preservePreviousSuccessOnFailure = false)
    }

    fun selectEndDate(isoDate: String) {
        selectedEndDate = isoDate
        if (!selectedStartDate.isNullOrBlank() && selectedStartDate!! > isoDate) {
            selectedStartDate = isoDate
        }
        request(forceRefresh = true, preservePreviousSuccessOnFailure = false)
    }

    fun selectDateRange(startIsoDate: String, endIsoDate: String) {
        if (startIsoDate <= endIsoDate) {
            selectedStartDate = startIsoDate
            selectedEndDate = endIsoDate
        } else {
            selectedStartDate = endIsoDate
            selectedEndDate = startIsoDate
        }
        request(forceRefresh = true, preservePreviousSuccessOnFailure = false)
    }

    fun updateRapidThreshold(days: Int, hours: Int, minutes: Int) {
        rapidThresholdMinutes = (days.coerceAtLeast(0) * 24 * 60) +
            (hours.coerceIn(0, 23) * 60) +
            minutes.coerceIn(0, 59)
        request(forceRefresh = true, preservePreviousSuccessOnFailure = false)
    }

    private fun request(
        forceRefresh: Boolean,
        preservePreviousSuccessOnFailure: Boolean,
        refreshing: Boolean = false,
    ) {
        viewModelScope.launch {
            if (refreshing) {
                _isRefreshing.value = true
            }

            if (_uiState.value !is ProjectStatsUiState.Success) {
                _uiState.value = ProjectStatsUiState.Loading
            }

            val result = withContext(Dispatchers.Default) {
                repository.loadProjectStats(
                    projectId = projectId,
                    selectedRepositoryId = selectedRepositoryId,
                    selectedStartDate = selectedStartDate,
                    selectedEndDate = selectedEndDate,
                    selectedRapidThresholdMinutes = rapidThresholdMinutes,
                    forceRefresh = forceRefresh,
                )
            }

            result.onSuccess { model ->
                hasLoaded = true
                selectedRepositoryId = model.selectedRepositoryId
                selectedStartDate = model.visibleRange.startIsoDate
                selectedEndDate = model.visibleRange.endIsoDate
                rapidThresholdMinutes = model.rapidThreshold.totalMinutes
                _uiState.value = ProjectStatsUiState.Success(model)
            }.onFailure { error ->
                if (!preservePreviousSuccessOnFailure || _uiState.value !is ProjectStatsUiState.Success) {
                    _uiState.value = ProjectStatsUiState.Error(
                        error.message ?: "Не удалось загрузить статистику проекта"
                    )
                }
            }

            if (refreshing) {
                _isRefreshing.value = false
            }
        }
    }
}
