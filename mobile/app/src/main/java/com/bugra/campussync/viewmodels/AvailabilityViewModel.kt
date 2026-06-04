package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.NetworkResult
import com.bugra.campussync.repository.AvailabilityRepository
import com.bugra.campussync.utils.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AvailabilityUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val busySlots: Set<String> = emptySet(),
    val error: String? = null
)

@HiltViewModel
class AvailabilityViewModel @Inject constructor(
    private val repository: AvailabilityRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AvailabilityUiState())
    val state: StateFlow<AvailabilityUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = safeApiCall { repository.getUnavailability() }) {
                is NetworkResult.Success -> {
                    val loaded = result.data.mapNotNull { item ->
                        val day = item["day"]?.takeIf { it.isNotBlank() && it != "null" }
                        val hour = item["hour"]?.takeIf { it.isNotBlank() && it != "null" }
                        if (day != null && hour != null) "$day-$hour" else null
                    }.toSet()
                    _state.update { it.copy(isLoading = false, busySlots = loaded) }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is NetworkResult.Loading -> { }
            }
        }
    }

    fun toggleSlot(slot: String) {
        _state.update { s ->
            val updated = if (s.busySlots.contains(slot)) s.busySlots - slot else s.busySlots + slot
            s.copy(busySlots = updated)
        }
    }

    fun save(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val dataToSend = _state.value.busySlots.map { slot ->
                val dashIdx = slot.indexOf('-')
                mapOf(
                    "day" to slot.substring(0, dashIdx),
                    "hour" to slot.substring(dashIdx + 1)
                )
            }
            when (val result = safeApiCall { repository.syncUnavailability(dataToSend) }) {
                is NetworkResult.Success -> {
                    onSuccess()
                }
                is NetworkResult.Error -> {
                    onError(result.message)
                }
                is NetworkResult.Loading -> { }
            }
            _state.update { it.copy(isSaving = false) }
        }
    }
}
