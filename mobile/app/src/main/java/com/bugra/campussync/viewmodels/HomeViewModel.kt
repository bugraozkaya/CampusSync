package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.NetworkResult
import com.bugra.campussync.network.ScheduleItem
import com.bugra.campussync.repository.HomeRepository
import com.bugra.campussync.utils.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val schedules: List<ScheduleItem> = emptyList(),
    val adminSummary: Map<String, Any> = emptyMap(),
    val unreadCount: Int = 0,
    val isExporting: Boolean = false,
    val exportError: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        // Observe local cache
        homeRepository.localSchedules.onEach { localSchedules ->
            if (_state.value.schedules != localSchedules) {
                _state.update { it.copy(schedules = localSchedules) }
            }
        }.launchIn(viewModelScope)
    }

    fun load(isAdmin: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            if (isAdmin) {
                when (val result = safeApiCall { homeRepository.getAdminSummary() }) {
                    is NetworkResult.Success -> {
                        _state.update { it.copy(adminSummary = result.data, isLoading = false) }
                    }
                    is NetworkResult.Error -> {
                        _state.update { it.copy(error = result.message, isLoading = false) }
                    }
                    is NetworkResult.Loading -> { }
                }
            } else {
                when (val result = safeApiCall { homeRepository.getSchedules() }) {
                    is NetworkResult.Success -> {
                        // Update state immediately as well as caching
                        _state.update { it.copy(schedules = result.data.results, isLoading = false) }
                    }
                    is NetworkResult.Error -> {
                        if (_state.value.schedules.isEmpty()) {
                            _state.update { it.copy(error = result.message, isLoading = false) }
                        } else {
                            _state.update { it.copy(isLoading = false) }
                        }
                    }
                    is NetworkResult.Loading -> { }
                }
            }

            val unreadResult = safeApiCall { homeRepository.getUnreadCount() }
            if (unreadResult is NetworkResult.Success) {
                _state.update { it.copy(unreadCount = unreadResult.data["unread_count"] ?: 0) }
            }
        }
    }

    fun exportPdf(type: String, onPdfDownloaded: (ByteArray) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true, exportError = null) }
            val result = safeApiCall { homeRepository.exportSchedulePdf(type) }
            when (result) {
                is NetworkResult.Success -> {
                    onPdfDownloaded(result.data.bytes())
                    _state.update { it.copy(isExporting = false) }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isExporting = false, exportError = result.message) }
                }
                is NetworkResult.Loading -> { }
            }
        }
    }

    fun clearExportError() {
        _state.update { it.copy(exportError = null) }
    }
}
