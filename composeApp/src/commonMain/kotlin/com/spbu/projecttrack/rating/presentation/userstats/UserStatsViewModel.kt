package com.spbu.projecttrack.rating.presentation.userstats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spbu.projecttrack.rating.data.model.UserStatsUiModel
import com.spbu.projecttrack.rating.data.repository.UserStatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class UserStatsUiState {
    data object Loading : UserStatsUiState()
    data class Success(val data: UserStatsUiModel) : UserStatsUiState()
    data class Error(val message: String) : UserStatsUiState()
}

class UserStatsViewModel(
    private val repository: UserStatsRepository,
    private val userId: String,
    private val userName: String,
    private val preferredProjectName: String?,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UserStatsUiState>(UserStatsUiState.Loading)
    val uiState: StateFlow<UserStatsUiState> = _uiState.asStateFlow()

    private var selectedRepositoryId: String? = null
    private var selectedStartDate: String? = null
    private var selectedEndDate: String? = null
    private var rapidThresholdMinutes: Int? = null
    private var hasLoaded = false

    fun load(force: Boolean = false) {
        if (hasLoaded && !force) return
        request(forceRefresh = force)
    }

    fun retry() {
        hasLoaded = false
        request(forceRefresh = true)
    }

    fun selectRepository(repositoryId: String) {
        selectedRepositoryId = repositoryId
        request(forceRefresh = true)
    }

    fun selectStartDate(isoDate: String) {
        selectedStartDate = isoDate
        if (!selectedEndDate.isNullOrBlank() && selectedEndDate!! < isoDate) {
            selectedEndDate = isoDate
        }
        request(forceRefresh = true)
    }

    fun selectEndDate(isoDate: String) {
        selectedEndDate = isoDate
        if (!selectedStartDate.isNullOrBlank() && selectedStartDate!! > isoDate) {
            selectedStartDate = isoDate
        }
        request(forceRefresh = true)
    }

    fun selectDateRange(startIsoDate: String, endIsoDate: String) {
        if (startIsoDate <= endIsoDate) {
            selectedStartDate = startIsoDate
            selectedEndDate = endIsoDate
        } else {
            selectedStartDate = endIsoDate
            selectedEndDate = startIsoDate
        }
        request(forceRefresh = true)
    }

    fun updateRapidThreshold(days: Int, hours: Int, minutes: Int) {
        rapidThresholdMinutes = (days.coerceAtLeast(0) * 24 * 60) +
            (hours.coerceIn(0, 23) * 60) +
            minutes.coerceIn(0, 59)
        request(forceRefresh = true)
    }

    private fun request(forceRefresh: Boolean) {
        viewModelScope.launch {
            if (_uiState.value !is UserStatsUiState.Success) {
                _uiState.value = UserStatsUiState.Loading
            }

            val result = withContext(Dispatchers.Default) {
                repository.loadUserStats(
                    userId = userId,
                    fallbackUserName = userName,
                    preferredProjectName = preferredProjectName,
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
                _uiState.value = UserStatsUiState.Success(model)
            }.onFailure { error ->
                _uiState.value = UserStatsUiState.Error(
                    error.message ?: "Не удалось загрузить личную статистику"
                )
            }
        }
    }
}
