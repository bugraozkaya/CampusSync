package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.AnnouncementItem
import com.bugra.campussync.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnnouncementsUiState(
    val isLoading: Boolean = true,
    val announcements: List<AnnouncementItem> = emptyList(),
    val readIds: Set<Int> = emptySet()
)

class AnnouncementsViewModel : ViewModel() {

    private val _state = MutableStateFlow(AnnouncementsUiState())
    val state: StateFlow<AnnouncementsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val announcements = RetrofitClient.apiService.getAnnouncements().results
                val readIds = announcements.filter { it.is_read }.map { it.id }.toSet()
                _state.update { it.copy(isLoading = false, announcements = announcements, readIds = readIds) }
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun markRead(id: Int) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.markAnnouncementRead(id)
                _state.update { it.copy(readIds = it.readIds + id) }
            } catch (_: Exception) {}
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.markAllRead()
                _state.update { s -> s.copy(readIds = s.announcements.map { it.id }.toSet()) }
            } catch (_: Exception) {}
        }
    }

    fun create(title: String, body: String, audience: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.createAnnouncement(
                    mapOf("title" to title, "body" to body, "audience" to audience)
                )
                load()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Hata oluştu")
            }
        }
    }
}
