package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.ClassroomItem
import com.bugra.campussync.network.CourseItem
import com.bugra.campussync.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

class CalendarViewModel : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    fun loadSchedules() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val schedules = RetrofitClient.apiService.getScheduleDetails().results
                _state.update { it.copy(detailedSchedules = schedules, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadAdminData() {
        viewModelScope.launch {
            try {
                val lecturers = RetrofitClient.apiService.getUsers().results.filter {
                    val r = it["role"]?.toString()?.uppercase() ?: ""
                    r == "LECTURER" || r == "STAFF" || r == "IT"
                }
                val classrooms = RetrofitClient.apiService.getClassrooms().results.map { cr ->
                    mapOf(
                        "id" to (cr.id as Any),
                        "room_code" to cr.room_code,
                        "capacity" to cr.capacity,
                        "classroom_type" to cr.classroom_type,
                        "classroom_type_display" to (cr.classroom_type_display ?: cr.classroom_type)
                    )
                }
                val courses = RetrofitClient.apiService.getCourses().results
                _state.update { it.copy(lecturers = lecturers, classrooms = classrooms, courses = courses) }
            } catch (_: Exception) {}
        }
    }

    fun loadAvailableClassrooms(day: String, startTime: String, endTime: String, sessionType: String, courseId: String) {
        viewModelScope.launch {
            try {
                val available = RetrofitClient.apiService.getAvailableClassrooms(
                    day = day, startTime = startTime, endTime = endTime,
                    sessionType = sessionType, courseId = courseId
                )
                _state.update { it.copy(availableClassrooms = available) }
            } catch (_: Exception) {
                _state.update { it.copy(availableClassrooms = emptyList()) }
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
            try {
                existingId?.let { RetrofitClient.apiService.deleteSchedule(it) }
                RetrofitClient.apiService.createSchedule(
                    mapOf(
                        "course" to courseId, "lecturer" to lecturerId,
                        "classroom" to classroomId, "day_of_week" to day,
                        "start_time" to startTime, "end_time" to endTime,
                        "session_type" to sessionType
                    )
                )
                loadSchedules()
                onSuccess()
            } catch (e: retrofit2.HttpException) {
                val msg = when (e.code()) {
                    409 -> "⚠️ Çakışma! Bu hoca veya sınıf bu saatte dolu."
                    400 -> "Geçersiz veri. Alanları kontrol edin."
                    else -> "Hata: ${e.message}"
                }
                onError(msg)
            } catch (e: Exception) {
                onError("Bağlantı hatası: ${e.message}")
            } finally {
                _state.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun deleteSchedule(id: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.deleteSchedule(id)
                loadSchedules()
                onSuccess()
            } catch (e: Exception) {
                onError("Silme hatası: ${e.message}")
            }
        }
    }

    fun autoSchedule(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val result = RetrofitClient.apiService.generateAutoSchedule()
                val count = result["scheduled_count"]?.toString() ?: "?"
                loadSchedules()
                onSuccess("✓ $count ders otomatik programlandı.")
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                onError("Otomatik programlama hatası: ${e.message}")
            }
        }
    }
}
