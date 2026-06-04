package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.CourseNoteItem
import com.bugra.campussync.network.NetworkResult
import com.bugra.campussync.repository.NotesRepository
import com.bugra.campussync.utils.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotesUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val notes: List<CourseNoteItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NotesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NotesUiState())
    val state: StateFlow<NotesUiState> = _state.asStateFlow()

    fun load(courseId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = safeApiCall { repository.getNotes(courseId) }) {
                is NetworkResult.Success -> _state.update { it.copy(isLoading = false, notes = result.data) }
                is NetworkResult.Error   -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun create(courseId: Int, title: String, content: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            when (val result = safeApiCall { repository.createNote(courseId, title, content) }) {
                is NetworkResult.Success -> { load(courseId); onSuccess() }
                is NetworkResult.Error   -> onError("Hata: ${result.message}")
                is NetworkResult.Loading -> {}
            }
            _state.update { it.copy(isSubmitting = false) }
        }
    }

    fun delete(id: Int, courseId: Int, onError: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val response = repository.deleteNote(id)
                if (response.isSuccessful) {
                    load(courseId)
                } else {
                    _state.update { it.copy(isLoading = false) }
                    onError()
                }
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false) }
                onError()
            }
        }
    }
}
