package com.spbu.projecttrack.rating.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spbu.projecttrack.rating.data.model.RankingData
import com.spbu.projecttrack.rating.data.repository.RankingRepository
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
    private val repository: RankingRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<RankingUiState>(RankingUiState.Idle)
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var hasLoaded = false

    fun load(force: Boolean = false) {
        if (hasLoaded && !force) return
        viewModelScope.launch {
            _uiState.value = RankingUiState.Loading
            val result = repository.loadRatings()
            if (result.isSuccess) {
                hasLoaded = true
                _uiState.value = RankingUiState.Success(result.getOrThrow())
            } else {
                _uiState.value = RankingUiState.Error(
                    result.exceptionOrNull()?.message ?: "Не удалось загрузить рейтинг"
                )
            }
        }
    }

    fun retry() {
        hasLoaded = false
        load(force = true)
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = repository.loadRatings()
            if (result.isSuccess) {
                hasLoaded = true
                _uiState.value = RankingUiState.Success(result.getOrThrow())
            } else if (_uiState.value !is RankingUiState.Success) {
                _uiState.value = RankingUiState.Error(
                    result.exceptionOrNull()?.message ?: "Не удалось загрузить рейтинг"
                )
            }
            _isRefreshing.value = false
        }
    }

    fun reset() {
        hasLoaded = false
        _isRefreshing.value = false
        _uiState.value = RankingUiState.Idle
    }
}
