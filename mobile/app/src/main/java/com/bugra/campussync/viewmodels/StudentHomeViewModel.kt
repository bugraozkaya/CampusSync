package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.CourseItem
import com.bugra.campussync.network.EnrollmentItem
import com.bugra.campussync.network.RetrofitClient
import com.bugra.campussync.network.ScheduleItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudentHomeUiState(
    val isLoading: Boolean = true,
    val enrollments: List<EnrollmentItem> = emptyList(),
    val schedule: List<ScheduleItem> = emptyList(),
    val allCourses: List<CourseItem> = emptyList()
)

class StudentHomeViewModel : ViewModel() {

    private val _state = MutableStateFlow(StudentHomeUiState())
    val state: StateFlow<StudentHomeUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val enrollments = RetrofitClient.apiService.getMyEnrollments()
                val schedule = RetrofitClient.apiService.getStudentSchedule()
                _state.update { it.copy(isLoading = false, enrollments = enrollments, schedule = schedule) }
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadCourses() {
        viewModelScope.launch {
            try {
                val courses = RetrofitClient.apiService.getCourses().results
                _state.update { it.copy(allCourses = courses) }
            } catch (_: Exception) {}
        }
    }

    fun enroll(courseId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.enrollCourse(mapOf("course" to courseId))
                load()
                onSuccess()
            } catch (e: retrofit2.HttpException) {
                onError(if (e.code() == 400) "Bu derse zaten kayıtlısınız." else "Kayıt hatası.")
            } catch (e: Exception) {
                onError("Hata: ${e.message}")
            }
        }
    }

    fun unenroll(id: Int, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.unenrollCourse(id)
                load()
            } catch (_: Exception) { onError() }
        }
    }
}
