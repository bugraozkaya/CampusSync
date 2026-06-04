package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.CourseItem
import com.bugra.campussync.network.GradeItem
import com.bugra.campussync.network.NetworkResult
import com.bugra.campussync.repository.GradeBookRepository
import com.bugra.campussync.utils.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GradeBookUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val grades: List<GradeItem> = emptyList(),
    val courses: List<CourseItem> = emptyList(),
    val selectedCourseId: Int? = null,
    val classAverage: Double? = null,
    val error: String? = null
)

@HiltViewModel
class GradeBookViewModel @Inject constructor(
    private val repository: GradeBookRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GradeBookUiState())
    val state: StateFlow<GradeBookUiState> = _state.asStateFlow()

    fun loadCourses() {
        viewModelScope.launch {
            when (val result = safeApiCall { repository.getCourses() }) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(courses = result.data.results) }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(error = result.message) }
                }
                is NetworkResult.Loading -> { }
            }
        }
    }

    fun selectCourse(id: Int) {
        _state.update { it.copy(selectedCourseId = id) }
        loadGrades(isStudent = false)
    }

    fun loadGrades(isStudent: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = if (isStudent) {
                safeApiCall { repository.getMyGrades() }
            } else {
                _state.value.selectedCourseId?.let {
                    safeApiCall { repository.getCourseGrades(it) }
                } ?: NetworkResult.Success(com.bugra.campussync.network.PagedResponse(0, null, null, emptyList()))
            }

            when (result) {
                is NetworkResult.Success -> {
                    val grades = result.data.results
                    val avg = if (grades.isNotEmpty()) grades.map { it.percentage }.average() else null
                    _state.update { it.copy(isLoading = false, grades = grades, classAverage = avg) }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is NetworkResult.Loading -> { }
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
            val usersResult = safeApiCall { repository.getUsers(search = studentUsername) }
            if (usersResult is NetworkResult.Success) {
                val student = usersResult.data.results.firstOrNull { (it["username"] as? String) == studentUsername }
                val rawId = student?.get("id")
                val studentId = when (rawId) {
                    is Double -> rawId.toInt()
                    is Int -> rawId
                    is Long -> rawId.toInt()
                    else -> null
                } ?: run {
                    onError("Öğrenci bulunamadı: $studentUsername")
                    _state.update { it.copy(isSubmitting = false) }
                    return@launch
                }

                val createResult = safeApiCall {
                    repository.createGrade(
                        mapOf("student" to studentId, "course" to courseId, "grade_type" to gradeType,
                            "score" to score, "max_score" to maxScore, "notes" to notes)
                    )
                }

                when (createResult) {
                    is NetworkResult.Success -> {
                        loadGrades(isStudent = false)
                        onSuccess()
                        _state.update { it.copy(isSubmitting = false) }
                    }
                    is NetworkResult.Error -> {
                        onError("Hata: ${createResult.message}")
                        _state.update { it.copy(isSubmitting = false) }
                    }
                    is NetworkResult.Loading -> { }
                }
            } else if (usersResult is NetworkResult.Error) {
                onError("Öğrenci arama hatası: ${usersResult.message}")
                _state.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun deleteGrade(id: Int, isStudent: Boolean, onError: () -> Unit) {
        viewModelScope.launch {
            when (val result = safeApiCall { repository.deleteGrade(id) }) {
                is NetworkResult.Success -> {
                    loadGrades(isStudent)
                }
                is NetworkResult.Error -> {
                    onError()
                }
                is NetworkResult.Loading -> { }
            }
        }
    }
}
