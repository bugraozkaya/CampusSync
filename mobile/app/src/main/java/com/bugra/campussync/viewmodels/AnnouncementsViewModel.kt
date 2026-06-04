package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.AnnouncementItem
import com.bugra.campussync.network.CourseItem
import com.bugra.campussync.network.NetworkResult
import com.bugra.campussync.repository.AnnouncementsRepository
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

data class AnnouncementsUiState(
    val isLoading: Boolean = true,
    val announcements: List<AnnouncementItem> = emptyList(),
    val readIds: Set<Int> = emptySet(),
    val courses: List<CourseItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class AnnouncementsViewModel @Inject constructor(
    private val repository: AnnouncementsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AnnouncementsUiState())
    val state: StateFlow<AnnouncementsUiState> = _state.asStateFlow()

    init {
        repository.localAnnouncements.onEach { localItems ->
            if (_state.value.announcements != localItems) {
                val readIds = localItems.filter { it.is_read }.map { it.id }.toSet()
                _state.update { it.copy(announcements = localItems, readIds = readIds) }
            }
        }.launchIn(viewModelScope)

        load()
        loadCourses()
    }

    private fun loadCourses() {
        viewModelScope.launch {
            try {
                val courses = repository.getCourses()
                _state.update { it.copy(courses = courses) }
            } catch (_: Exception) { }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = safeApiCall { repository.getAnnouncements() }) {
                is NetworkResult.Success -> {
                    // updated via flow observation
                    _state.update { it.copy(isLoading = false) }
                }
                is NetworkResult.Error -> {
                    if (_state.value.announcements.isEmpty()) {
                        _state.update { it.copy(isLoading = false, error = result.message) }
                    } else {
                        _state.update { it.copy(isLoading = false) }
                    }
                }
                is NetworkResult.Loading -> { }
            }
        }
    }

    fun markRead(id: Int) {
        viewModelScope.launch {
            when (safeApiCall { repository.markAnnouncementRead(id) }) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(readIds = it.readIds + id) }
                }
                is NetworkResult.Error -> {}
                is NetworkResult.Loading -> { }
                else -> {}
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            when (safeApiCall { repository.markAllRead() }) {
                is NetworkResult.Success -> {
                    _state.update { s -> s.copy(readIds = s.announcements.map { it.id }.toSet()) }
                }
                is NetworkResult.Error -> {}
                is NetworkResult.Loading -> { }
                else -> {}
            }
        }
    }

    fun create(title: String, body: String, audience: String, courseId: Int? = null, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = safeApiCall { repository.createAnnouncement(title, body, audience, courseId) }) {
                is NetworkResult.Success -> {
                    load()
                    onSuccess()
                }
                is NetworkResult.Error -> {
                    onError(result.message)
                }
                is NetworkResult.Loading -> { }
                else -> {}
            }
        }
    }
}
