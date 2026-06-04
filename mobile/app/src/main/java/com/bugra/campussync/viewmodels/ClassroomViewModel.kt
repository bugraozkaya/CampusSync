package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.ClassroomItem
import com.bugra.campussync.network.NetworkResult
import com.bugra.campussync.repository.ClassroomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject

data class ClassroomUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val classrooms: List<ClassroomItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ClassroomViewModel @Inject constructor(
    private val repository: ClassroomRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ClassroomUiState())
    val state: StateFlow<ClassroomUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getClassrooms()) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isLoading = false, classrooms = result.data.results) }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is NetworkResult.Loading -> { }
            }
        }
    }

    fun create(roomCode: String, capacity: Int, type: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            val result = repository.createClassroom(roomCode, capacity, type)
            when (result) {
                is NetworkResult.Success -> {
                    load()
                    onSuccess()
                }
                is NetworkResult.Error -> {
                    load()
                    onError("Hata: ${result.message}")
                }
                is NetworkResult.Loading -> { }
            }
            _state.update { it.copy(isSubmitting = false) }
        }
    }

    fun update(id: Int, roomCode: String, capacity: Int, type: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            when (val result = repository.updateClassroom(id, roomCode, capacity, type)) {
                is NetworkResult.Success -> { load(); onSuccess() }
                is NetworkResult.Error   -> onError("Hata: ${result.message}")
                is NetworkResult.Loading -> { }
            }
            _state.update { it.copy(isSubmitting = false) }
        }
    }

    fun delete(id: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            when (val result = repository.deleteClassroom(id)) {
                is NetworkResult.Success -> { load(); onSuccess() }
                is NetworkResult.Error   -> { load(); onError("Silme hatası: ${result.message}") }
                is NetworkResult.Loading -> { }
            }
            _state.update { it.copy(isSubmitting = false) }
        }
    }

    fun bulkImport(filePart: MultipartBody.Part, onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = repository.bulkImportClassrooms(filePart)
            when (result) {
                is NetworkResult.Success -> {
                    load()
                    onSuccess(result.data.size)
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isLoading = false) }
                    onError("İçe aktarma hatası: ${result.message}")
                }
                is NetworkResult.Loading -> { }
            }
        }
    }
}
