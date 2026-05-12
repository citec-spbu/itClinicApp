package com.spbu.projecttrack.rating.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spbu.projecttrack.core.network.toShortMessage
import com.spbu.projecttrack.rating.data.model.RankingData
import com.spbu.projecttrack.rating.data.model.RankingFilters
import com.spbu.projecttrack.rating.data.model.rankingDefaultFilters
import com.spbu.projecttrack.rating.data.repository.IRankingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RankingUiState {
    data object Idle : RankingUiState()
    data object Loading : RankingUiState()
    data class Success(val data: RankingData) : RankingUiState()
    data class Error(val message: String) : RankingUiState()
}

class RankingViewModel(
    private val repository: IRankingRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<RankingUiState>(RankingUiState.Idle)
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var hasLoaded = false
    private var currentFilters = rankingDefaultFilters()

    fun load(
        force: Boolean = false,
        filters: RankingFilters = currentFilters,
    ) {
        if (hasLoaded && !force) return
        currentFilters = filters
        viewModelScope.launch {
            _uiState.value = RankingUiState.Loading
            val result = repository.loadRatings(
                filters = currentFilters,
                forceRefresh = force,
            )
            if (result.isSuccess) {
                hasLoaded = true
                _uiState.value = RankingUiState.Success(result.getOrThrow())
            } else {
                _uiState.value = RankingUiState.Error(
                    result.exceptionOrNull().toShortMessage("Ошибка загрузки", "Loading error")
                )
            }
        }
    }

    fun retry() {
        hasLoaded = false
        load(force = true, filters = currentFilters)
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = repository.loadRatings(
                filters = currentFilters,
                forceRefresh = true,
            )
            if (result.isSuccess) {
                hasLoaded = true
                _uiState.value = RankingUiState.Success(result.getOrThrow())
            } else if (_uiState.value !is RankingUiState.Success) {
                _uiState.value = RankingUiState.Error(
                    result.exceptionOrNull().toShortMessage("Ошибка загрузки", "Loading error")
                )
            }
            _isRefreshing.value = false
        }
    }

    fun applyFilters(filters: RankingFilters) {
        currentFilters = filters
        viewModelScope.launch {
            val result = repository.loadRatings(filters = currentFilters)
            if (result.isSuccess) {
                hasLoaded = true
                _uiState.value = RankingUiState.Success(result.getOrThrow())
            } else if (_uiState.value !is RankingUiState.Success) {
                _uiState.value = RankingUiState.Error(
                    result.exceptionOrNull().toShortMessage("Ошибка загрузки", "Loading error")
                )
            }
        }
    }

    fun reset() {
        hasLoaded = false
        currentFilters = rankingDefaultFilters()
        _isRefreshing.value = false
        _uiState.value = RankingUiState.Idle
    }

    // These are mutable on purpose: the composable persists tab/page scroll state on dispose.
    var savedPage: Int = 0
    var savedProjectsScrollIndex: Int = 0
    var savedProjectsScrollOffset: Int = 0
    var savedStudentsScrollIndex: Int = 0
    var savedStudentsScrollOffset: Int = 0
    var savedProjectsStickyHeightPx: Int = 0
    var savedStudentsStickyHeightPx: Int = 0
}
