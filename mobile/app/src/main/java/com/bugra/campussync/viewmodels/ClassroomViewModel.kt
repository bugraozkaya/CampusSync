package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.ClassroomItem
import com.bugra.campussync.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

data class ClassroomUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val classrooms: List<ClassroomItem> = emptyList(),
    val error: String? = null
)

class ClassroomViewModel : ViewModel() {

    private val _state = MutableStateFlow(ClassroomUiState())
    val state: StateFlow<ClassroomUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val classrooms = RetrofitClient.apiService.getClassrooms().results
                _state.update { it.copy(isLoading = false, classrooms = classrooms) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun create(roomCode: String, capacity: Int, type: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            try {
                RetrofitClient.apiService.createClassroom(
                    mapOf("room_code" to roomCode, "capacity" to capacity, "classroom_type" to type)
                )
                load()
                onSuccess()
            } catch (e: retrofit2.HttpException) {
                val msg = if (e.code() in listOf(400, 409)) "Bu oda kodu zaten mevcut." else "Hata: ${e.message}"
                onError(msg)
            } catch (e: Exception) {
                onError("Hata: ${e.message}")
            } finally {
                _state.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun bulkImport(filePart: MultipartBody.Part, onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val result = RetrofitClient.apiService.bulkImportClassrooms(filePart)
                load()
                onSuccess(result.size)
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                onError("İçe aktarma hatası: ${e.localizedMessage}")
            }
        }
    }
}
