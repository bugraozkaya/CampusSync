package com.bugra.campussync.viewmodels

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.CourseItem
import com.bugra.campussync.network.NetworkResult
import com.bugra.campussync.repository.DataRepository
import com.bugra.campussync.utils.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import java.util.Locale
import javax.inject.Inject

data class LecturerEntry(
    val course: String,
    val lecturer: String,
    val generatedUser: String = "",
    val generatedPass: String = "",
    val userId: Int = 0
)

data class DataUiState(
    val isLoading: Boolean = true,
    val isUploading: Boolean = false,
    val isSubmittingLecturer: Boolean = false,
    val isSubmittingCourse: Boolean = false,
    val isSubmittingStudent: Boolean = false,
    val lecturers: List<LecturerEntry> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class DataViewModel @Inject constructor(
    private val repository: DataRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DataUiState())
    val state: StateFlow<DataUiState> = _state.asStateFlow()

    val passwordCache = mutableStateMapOf<String, String>()

    private val _allCourses = MutableStateFlow<List<CourseItem>>(emptyList())
    val allCourses: StateFlow<List<CourseItem>> = _allCourses.asStateFlow()

    private val _lecturerAssignedCourseIds = MutableStateFlow<Set<Int>>(emptySet())
    val lecturerAssignedCourseIds: StateFlow<Set<Int>> = _lecturerAssignedCourseIds.asStateFlow()

    private val _isLoadingCourses = MutableStateFlow(false)
    val isLoadingCourses: StateFlow<Boolean> = _isLoadingCourses.asStateFlow()

    private val _lecturerSchedule = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val lecturerSchedule: StateFlow<List<Map<String, Any>>> = _lecturerSchedule.asStateFlow()

    private val _lecturerUnavailability = MutableStateFlow<List<Map<String, String>>>(emptyList())
    val lecturerUnavailability: StateFlow<List<Map<String, String>>> = _lecturerUnavailability.asStateFlow()

    private val _isLoadingCalendar = MutableStateFlow(false)
    val isLoadingCalendar: StateFlow<Boolean> = _isLoadingCalendar.asStateFlow()

    init { fetchLecturers() }

    fun fetchLecturers() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = safeApiCall { repository.getUsers() }) {
                is NetworkResult.Success -> {
                    val entries = result.data.results.filter { user ->
                        val role = user["role"]?.toString()?.uppercase() ?: ""
                        role == "LECTURER" || role == "STAFF"
                    }.map { user ->
                        val username = user["username"].toString()
                        val rawPass = passwordCache[username] ?: "••••••"
                        val userId = when (val id = user["id"]) {
                            is Double -> id.toInt()
                            is Int -> id
                            else -> 0
                        }
                        LecturerEntry(
                            course = user["department_name"]?.toString()
                                ?: user["department"]?.toString()
                                ?: "Departman Belirtilmemiş",
                            lecturer = "${user["first_name"] ?: ""} ${user["last_name"] ?: ""}".trim()
                                .ifBlank { username },
                            generatedUser = username,
                            generatedPass = rawPass,
                            userId = userId
                        )
                    }
                    _state.update { it.copy(isLoading = false, lecturers = entries) }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is NetworkResult.Loading -> { }
            }
        }
    }

    fun loadCoursesForAssignment(lecturerId: Int) {
        viewModelScope.launch {
            _isLoadingCourses.value = true
            _lecturerAssignedCourseIds.value = emptySet()
            val coursesResult = safeApiCall { repository.getCourses() }
            if (coursesResult is NetworkResult.Success) {
                _allCourses.value = coursesResult.data.results
            }
            val assignedResult = safeApiCall { repository.getLecturerCourses(lecturerId) }
            if (assignedResult is NetworkResult.Success) {
                _lecturerAssignedCourseIds.value = assignedResult.data.mapNotNull { map ->
                    when (val id = map["id"]) {
                        is Double -> id.toInt()
                        is Int -> id
                        else -> null
                    }
                }.toSet()
            }
            _isLoadingCourses.value = false
        }
    }

    fun saveLecturerCourses(
        lecturerId: Int,
        originalIds: Set<Int>,
        newIds: Set<Int>,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val toAdd = newIds - originalIds
            val toRemove = originalIds - newIds
            var errorMsg: String? = null
            for (id in toAdd) {
                val r = safeApiCall { repository.assignCourse(lecturerId, id) }
                if (r is NetworkResult.Error) errorMsg = r.message
            }
            for (id in toRemove) {
                val r = safeApiCall { repository.removeCourse(lecturerId, id) }
                if (r is NetworkResult.Error) errorMsg = r.message
            }
            if (errorMsg != null) onError(errorMsg) else onDone()
        }
    }

    fun loadLecturerCalendar(lecturerId: Int) {
        viewModelScope.launch {
            _isLoadingCalendar.value = true
            _lecturerSchedule.value = emptyList()
            _lecturerUnavailability.value = emptyList()
            val schedResult = safeApiCall { repository.getLecturerSchedules(lecturerId) }
            if (schedResult is NetworkResult.Success) {
                _lecturerSchedule.value = schedResult.data.results
            }
            val unavailResult = safeApiCall { repository.getLecturerUnavailability(lecturerId) }
            if (unavailResult is NetworkResult.Success) {
                _lecturerUnavailability.value = unavailResult.data
            }
            _isLoadingCalendar.value = false
        }
    }

    fun bulkImport(filePart: MultipartBody.Part, onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isUploading = true) }
            when (val result = safeApiCall { repository.bulkImport(filePart) }) {
                is NetworkResult.Success -> {
                    result.data.forEach { item ->
                        val u = item["generated_user"] ?: ""
                        val p = item["generated_pass"] ?: ""
                        if (u.isNotEmpty() && p.isNotEmpty()) passwordCache[u] = p
                    }
                    fetchLecturers()
                    onSuccess(result.data.size)
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

    fun bulkImportLecturers(
        items: List<Triple<String, String, String>>,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isUploading = true) }
            var successCount = 0
            val errors = mutableListOf<String>()

            items.forEach { (firstName, lastName, dept) ->
                val username = generateUsername(firstName, lastName)
                val password = generateAlphanumericPass()
                val data = mapOf(
                    "username" to username,
                    "password" to password,
                    "first_name" to firstName,
                    "last_name" to lastName,
                    "role" to "LECTURER",
                    "department_name" to dept.ifBlank { "Genel" },
                    "must_change_password" to "true"
                )
                when (val result = safeApiCall { repository.createAdmin(data) }) {
                    is NetworkResult.Success -> {
                        passwordCache[username] = password
                        successCount++
                    }
                    is NetworkResult.Error -> {
                        errors.add("$firstName $lastName")
                    }
                    is NetworkResult.Loading -> { }
                }
            }

            fetchLecturers()
            _state.update { it.copy(isUploading = false) }
            if (successCount > 0) onSuccess(successCount)
            if (errors.isNotEmpty()) onError(
                "${errors.size} kayıt eklenemedi: ${errors.take(3).joinToString(", ")}" +
                if (errors.size > 3) "..." else ""
            )
        }
    }

    fun createLecturer(
        firstName: String, lastName: String, department: String,
        onSuccess: (String, String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmittingLecturer = true) }
            val username = generateUsername(firstName, lastName)
            val password = generateAlphanumericPass()
            val data = mapOf(
                "username" to username, "password" to password,
                "first_name" to firstName, "last_name" to lastName,
                "role" to "LECTURER", "department_name" to department,
                "must_change_password" to "true"
            )
            when (val result = safeApiCall { repository.createAdmin(data) }) {
                is NetworkResult.Success -> {
                    passwordCache[username] = password
                    fetchLecturers()
                    onSuccess(username, password)
                    _state.update { it.copy(isSubmittingLecturer = false) }
                }
                is NetworkResult.Error -> {
                    val msg = if (result.message.contains("400") || result.message.contains("409"))
                        "Bu kullanıcı adı zaten mevcut. Farklı bir isim deneyin."
                    else "Hata: ${result.message}"
                    onError(msg)
                    _state.update { it.copy(isSubmittingLecturer = false) }
                }
                is NetworkResult.Loading -> { }
            }
        }
    }

    fun createCourse(name: String, code: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmittingCourse = true) }
            when (val result = safeApiCall { repository.createCourse(mapOf("course_name" to name, "course_code" to code)) }) {
                is NetworkResult.Success -> {
                    onSuccess()
                    _state.update { it.copy(isSubmittingCourse = false) }
                }
                is NetworkResult.Error -> {
                    onError("Hata: ${result.message}")
                    _state.update { it.copy(isSubmittingCourse = false) }
                }
                is NetworkResult.Loading -> { }
            }
        }
    }

    fun createStudent(
        firstName: String, lastName: String, studentNumber: String,
        onSuccess: (String, String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmittingStudent = true) }
            val username = if (studentNumber.isNotBlank()) studentNumber
                           else generateUsername(firstName, lastName)
            val password = generateAlphanumericPass()
            val data = mapOf(
                "username" to username, "password" to password,
                "first_name" to firstName, "last_name" to lastName,
                "role" to "STUDENT", "must_change_password" to "true"
            )
            when (val result = safeApiCall { repository.createAdmin(data) }) {
                is NetworkResult.Success -> {
                    onSuccess(username, password)
                    _state.update { it.copy(isSubmittingStudent = false) }
                }
                is NetworkResult.Error -> {
                    val msg = if (result.message.contains("400") || result.message.contains("409")) "Bu öğrenci numarası zaten kayıtlı."
                              else "Hata: ${result.message}"
                    onError(msg)
                    _state.update { it.copy(isSubmittingStudent = false) }
                }
                is NetworkResult.Loading -> { }
            }
        }
    }

    fun generateUsername(firstName: String, lastName: String): String {
        val f = normalizeTurkish(firstName.lowercase(Locale.ROOT)).trim().replace(" ", "")
        val l = normalizeTurkish(lastName.lowercase(Locale.ROOT)).trim().replace(" ", "")
        return "${f}_${l}"
    }

    private fun normalizeTurkish(text: String): String =
        text.replace("ğ", "g").replace("Ğ", "g")
            .replace("ü", "u").replace("Ü", "u")
            .replace("ş", "s").replace("Ş", "s")
            .replace("ı", "i").replace("İ", "i")
            .replace("ö", "o").replace("Ö", "o")
            .replace("ç", "c").replace("Ç", "c")

    private fun generateAlphanumericPass(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
