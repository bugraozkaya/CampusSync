package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.ClassroomItem
import com.bugra.campussync.network.CourseItem
import com.bugra.campussync.network.NetworkResult
import com.bugra.campussync.repository.CalendarRepository
import com.bugra.campussync.utils.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CalendarUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val detailedSchedules: List<Map<String, Any>> = emptyList(),
    val lecturers: List<Map<String, Any>> = emptyList(),
    val classrooms: List<Map<String, Any>> = emptyList(),
    val courses: List<CourseItem> = emptyList(),
    val availableClassrooms: List<ClassroomItem> = emptyList(),
    val autoScheduleMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: CalendarRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    fun loadSchedules() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = safeApiCall { repository.getScheduleDetails() }) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(detailedSchedules = result.data.results, isLoading = false) }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is NetworkResult.Loading -> { }
            }
        }
    }

    fun loadAdminData() {
        viewModelScope.launch {
            val lecturersResult = safeApiCall { repository.getUsers() }
            val classroomsResult = safeApiCall { repository.getClassrooms() }
            val coursesResult = safeApiCall { repository.getCourses() }

            val lecturers = if (lecturersResult is NetworkResult.Success) {
                lecturersResult.data.results.filter {
                    val r = it["role"]?.toString()?.uppercase() ?: ""
                    r == "LECTURER" || r == "STAFF" || r == "IT"
                }
            } else emptyList()

            val classrooms = if (classroomsResult is NetworkResult.Success) {
                classroomsResult.data.results.map { cr ->
                    mapOf(
                        "id" to (cr.id as Any),
                        "room_code" to cr.room_code,
                        "capacity" to cr.capacity,
                        "classroom_type" to cr.classroom_type,
                        "classroom_type_display" to (cr.classroom_type_display ?: cr.classroom_type)
                    )
                }
            } else emptyList()

            val courses = if (coursesResult is NetworkResult.Success) {
                coursesResult.data.results
            } else emptyList()

            _state.update { it.copy(lecturers = lecturers, classrooms = classrooms, courses = courses) }
        }
    }

    fun loadAvailableClassrooms(day: String, startTime: String, endTime: String, sessionType: String, courseId: String) {
        viewModelScope.launch {
            when (val result = safeApiCall { repository.getAvailableClassrooms(day, startTime, endTime, sessionType, courseId) }) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(availableClassrooms = result.data) }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(availableClassrooms = emptyList()) }
                }
                is NetworkResult.Loading -> { }
            }
        }
    }

    fun clearAvailableClassrooms() {
        _state.update { it.copy(availableClassrooms = emptyList()) }
    }

    fun createSchedule(
        courseId: String, lecturerId: String, classroomId: String,
        day: String, startTime: String, endTime: String, sessionType: String,
        existingId: Int?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            
            if (existingId != null) {
                safeApiCall { repository.deleteSchedule(existingId) }
            }

            val data = mapOf(
                "course" to courseId, "lecturer" to lecturerId,
                "classroom" to classroomId, "day_of_week" to day,
                "start_time" to startTime, "end_time" to endTime,
                "session_type" to sessionType
            )

            when (val result = safeApiCall { repository.createSchedule(data) }) {
                is NetworkResult.Success -> {
                    loadSchedules()
                    onSuccess()
                }
                is NetworkResult.Error -> {
                    val msg = if (result.code == 409) "⚠️ Çakışma! Bu hoca veya sınıf bu saatte dolu." else result.message
                    onError(msg)
                }
                is NetworkResult.Loading -> { }
            }
            _state.update { it.copy(isSubmitting = false) }
        }
    }

    fun deleteSchedule(id: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = safeApiCall { repository.deleteSchedule(id) }) {
                is NetworkResult.Success -> {
                    loadSchedules()
                    onSuccess()
                }
                is NetworkResult.Error -> {
                    onError(result.message)
                }
                is NetworkResult.Loading -> { }
            }
        }
    }

    fun autoSchedule(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = safeApiCall { repository.generateAutoSchedule() }) {
                is NetworkResult.Success -> {
                    val count = result.data["scheduled_count"]?.toString() ?: "?"
                    loadSchedules()
                    onSuccess("✓ $count ders otomatik programlandı.")
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isLoading = false) }
                    onError(result.message)
                }
                is NetworkResult.Loading -> { }
            }
        }
    }
}
