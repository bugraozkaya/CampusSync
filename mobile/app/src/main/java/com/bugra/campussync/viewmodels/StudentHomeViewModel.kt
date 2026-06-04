package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.CourseItem
import com.bugra.campussync.network.EnrollmentItem
import com.bugra.campussync.network.NetworkResult
import com.bugra.campussync.repository.StudentHomeRepository
import com.bugra.campussync.utils.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudentHomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val enrollments: List<EnrollmentItem> = emptyList(),
    val availableCourses: List<CourseItem> = emptyList()
)

@HiltViewModel
class StudentHomeViewModel @Inject constructor(
    private val repository: StudentHomeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StudentHomeUiState())
    val state: StateFlow<StudentHomeUiState> = _state.asStateFlow()

    init { loadEnrollments() }

    fun loadEnrollments() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = safeApiCall { repository.getMyEnrollments() }
            when (result) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isLoading = false, enrollments = result.data) }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is NetworkResult.Loading -> { }
            }
        }
    }

    fun loadAvailableCourses() {
        viewModelScope.launch {
            when (val result = safeApiCall { repository.getCourses() }) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(availableCourses = result.data.results) }
                }
                is NetworkResult.Error -> { }
                is NetworkResult.Loading -> { }
            }
        }
    }

    fun enroll(courseId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = safeApiCall { repository.enrollCourse(courseId) }) {
                is NetworkResult.Success -> {
                    loadEnrollments()
                    onSuccess()
                }
                is NetworkResult.Error -> {
                    onError(result.message)
                }
                is NetworkResult.Loading -> { }
            }
        }
    }

    fun unenroll(id: Int, onError: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = safeApiCall { repository.unenrollCourse(id) }) {
                is NetworkResult.Success -> {
                    loadEnrollments()
                }
                is NetworkResult.Error -> {
                    onError(result.message)
                }
                is NetworkResult.Loading -> { }
            }
        }
    }
}
