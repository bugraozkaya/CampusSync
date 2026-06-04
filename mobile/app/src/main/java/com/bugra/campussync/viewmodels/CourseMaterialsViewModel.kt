package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.CourseMaterialItem
import com.bugra.campussync.network.CourseItem
import com.bugra.campussync.network.NetworkResult
import com.bugra.campussync.repository.CourseMaterialsRepository
import com.bugra.campussync.utils.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject

data class CourseMaterialsUiState(
    val isLoading: Boolean = true,
    val isUploading: Boolean = false,
    val materials: List<CourseMaterialItem> = emptyList(),
    val courses: List<CourseItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class CourseMaterialsViewModel @Inject constructor(
    private val repository: CourseMaterialsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CourseMaterialsUiState())
    val state: StateFlow<CourseMaterialsUiState> = _state.asStateFlow()

    init { load(null) }

    fun load(filterCourse: Int? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val materialsResult = safeApiCall { repository.getMaterials(filterCourse) }
            val coursesResult = safeApiCall { repository.getCourses() }

            if (materialsResult is NetworkResult.Success && coursesResult is NetworkResult.Success) {
                _state.update { it.copy(
                    isLoading = false,
                    materials = materialsResult.data.results,
                    courses = coursesResult.data.results
                ) }
            } else if (materialsResult is NetworkResult.Error) {
                _state.update { it.copy(isLoading = false, error = materialsResult.message) }
            } else if (coursesResult is NetworkResult.Error) {
                _state.update { it.copy(isLoading = false, error = coursesResult.message) }
            }
        }
    }

    fun upload(
        filePart: MultipartBody.Part,
        course: okhttp3.RequestBody,
        title: okhttp3.RequestBody,
        description: okhttp3.RequestBody,
        materialType: okhttp3.RequestBody,
        courseIdFilter: Int? = null,
        onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isUploading = true) }
            when (val result = safeApiCall {
                repository.uploadMaterial(
                    course = course, title = title,
                    description = description, materialType = materialType, file = filePart
                )
            }) {
                is NetworkResult.Success -> {
                    load(courseIdFilter)
                    onSuccess()
                    _state.update { it.copy(isUploading = false) }
                }
                is NetworkResult.Error -> {
                    onError("Yükleme hatası: ${result.message}")
                    _state.update { it.copy(isUploading = false) }
                }
                is NetworkResult.Loading -> { }
            }
        }
    }

    fun delete(id: Int, onError: () -> Unit) {
        viewModelScope.launch {
            when (val result = safeApiCall { repository.deleteMaterial(id) }) {
                is NetworkResult.Success -> {
                    load()
                }
                is NetworkResult.Error -> {
                    onError()
                }
                is NetworkResult.Loading -> { }
            }
        }
    }
}
