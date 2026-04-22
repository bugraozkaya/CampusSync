package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AvailabilityUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val busySlots: Set<String> = emptySet(),
    val error: String? = null
)

class AvailabilityViewModel : ViewModel() {

    private val _state = MutableStateFlow(AvailabilityUiState())
    val state: StateFlow<AvailabilityUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = RetrofitClient.apiService.getUnavailability()
                val loaded = response.mapNotNull { item ->
                    val day = item["day"]?.toString()?.takeIf { it.isNotBlank() && it != "null" }
                    val hour = item["hour"]?.toString()?.takeIf { it.isNotBlank() && it != "null" }
                    if (day != null && hour != null) "$day-$hour" else null
                }.toSet()
                _state.update { it.copy(isLoading = false, busySlots = loaded) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
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
            try {
                val dataToSend = _state.value.busySlots.mapNotNull { slot ->
                    val dashIdx = slot.indexOf('-')
                    if (dashIdx <= 0) return@mapNotNull null
                    val day = slot.substring(0, dashIdx)
                    val hour = slot.substring(dashIdx + 1)
                    if (day.isBlank() || hour.isBlank()) return@mapNotNull null
                    mapOf("day" to day, "hour" to hour)
                }
                RetrofitClient.apiService.syncUnavailability(dataToSend)
                onSuccess()
            } catch (e: Exception) {
                onError("Kayıt hatası: ${e.localizedMessage}")
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }
}
