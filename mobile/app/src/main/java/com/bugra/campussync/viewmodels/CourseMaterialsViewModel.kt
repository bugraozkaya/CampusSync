package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.CourseMaterialItem
import com.bugra.campussync.network.CourseItem
import com.bugra.campussync.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

data class CourseMaterialsUiState(
    val isLoading: Boolean = true,
    val isUploading: Boolean = false,
    val materials: List<CourseMaterialItem> = emptyList(),
    val courses: List<CourseItem> = emptyList()
)

class CourseMaterialsViewModel : ViewModel() {

    private val _state = MutableStateFlow(CourseMaterialsUiState())
    val state: StateFlow<CourseMaterialsUiState> = _state.asStateFlow()

    init { load(null) }

    fun load(filterCourse: Int? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val materials = RetrofitClient.apiService.getMaterials(filterCourse).results
                val courses = RetrofitClient.apiService.getCourses().results
                _state.update { it.copy(isLoading = false, materials = materials, courses = courses) }
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun upload(
        filePart: MultipartBody.Part,
        course: okhttp3.RequestBody,
        title: okhttp3.RequestBody,
        description: okhttp3.RequestBody,
        materialType: okhttp3.RequestBody,
        onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isUploading = true) }
            try {
                RetrofitClient.apiService.uploadMaterial(
                    course = course, title = title,
                    description = description, materialType = materialType, file = filePart
                )
                load()
                onSuccess()
            } catch (e: Exception) {
                onError("Yükleme hatası: ${e.message}")
            } finally {
                _state.update { it.copy(isUploading = false) }
            }
        }
    }

    fun delete(id: Int, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.deleteMaterial(id)
                load()
            } catch (_: Exception) { onError() }
        }
    }
}
