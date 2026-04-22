package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.CourseItem
import com.bugra.campussync.network.GradeItem
import com.bugra.campussync.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GradeBookUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val grades: List<GradeItem> = emptyList(),
    val courses: List<CourseItem> = emptyList(),
    val selectedCourseId: Int? = null
)

class GradeBookViewModel : ViewModel() {

    private val _state = MutableStateFlow(GradeBookUiState())
    val state: StateFlow<GradeBookUiState> = _state.asStateFlow()

    fun loadCourses() {
        viewModelScope.launch {
            try {
                val courses = RetrofitClient.apiService.getCourses().results
                _state.update { it.copy(courses = courses) }
            } catch (_: Exception) {}
        }
    }

    fun selectCourse(id: Int) {
        _state.update { it.copy(selectedCourseId = id) }
        loadGrades(isStudent = false)
    }

    fun loadGrades(isStudent: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val grades = if (isStudent) {
                    RetrofitClient.apiService.getMyGrades().results
                } else {
                    _state.value.selectedCourseId?.let {
                        RetrofitClient.apiService.getCourseGrades(it).results
                    } ?: emptyList()
                }
                _state.update { it.copy(isLoading = false, grades = grades) }
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun addGrade(
        studentUsername: String, courseId: Int, gradeType: String,
        score: Double, maxScore: Double, notes: String,
        onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            try {
                val users = RetrofitClient.apiService.getUsers(search = studentUsername).results
                val student = users.firstOrNull { (it["username"] as? String) == studentUsername }
                val rawId = student?.get("id")
                val studentId = when (rawId) {
                    is Double -> rawId.toInt()
                    is Int -> rawId
                    is Long -> rawId.toInt()
                    else -> null
                } ?: run { onError("Öğrenci bulunamadı: $studentUsername"); _state.update { it.copy(isSubmitting = false) }; return@launch }
                RetrofitClient.apiService.createGrade(
                    mapOf("student" to studentId, "course" to courseId, "grade_type" to gradeType,
                        "score" to score, "max_score" to maxScore, "notes" to notes)
                )
                loadGrades(isStudent = false)
                onSuccess()
            } catch (e: Exception) {
                onError("Hata: ${e.message}")
            } finally {
                _state.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun deleteGrade(id: Int, isStudent: Boolean, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.deleteGrade(id)
                loadGrades(isStudent)
            } catch (_: Exception) { onError() }
        }
    }
}
