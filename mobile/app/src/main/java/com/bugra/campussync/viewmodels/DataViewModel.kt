package com.bugra.campussync.viewmodels

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import java.util.Locale

data class LecturerEntry(
    val course: String,
    val lecturer: String,
    val generatedUser: String = "",
    val generatedPass: String = ""
)

data class DataUiState(
    val isLoading: Boolean = true,
    val isUploading: Boolean = false,
    val isSubmittingLecturer: Boolean = false,
    val isSubmittingCourse: Boolean = false,
    val isSubmittingStudent: Boolean = false,
    val lecturers: List<LecturerEntry> = emptyList()
)

class DataViewModel : ViewModel() {

    private val _state = MutableStateFlow(DataUiState())
    val state: StateFlow<DataUiState> = _state.asStateFlow()

    val passwordCache = mutableStateMapOf<String, String>()

    init { fetchLecturers() }

    fun fetchLecturers() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val users = RetrofitClient.apiService.getUsers().results
                val entries = users.filter { user ->
                    val role = user["role"]?.toString()?.uppercase() ?: ""
                    role == "LECTURER" || role == "STAFF"
                }.map { user ->
                    val username = user["username"].toString()
                    val rawPass = passwordCache[username] ?: "••••••"
                    LecturerEntry(
                        course = user["department_name"]?.toString()
                            ?: user["department"]?.toString()
                            ?: "Departman Belirtilmemiş",
                        lecturer = "${user["first_name"] ?: ""} ${user["last_name"] ?: ""}".trim()
                            .ifBlank { username },
                        generatedUser = username,
                        generatedPass = rawPass
                    )
                }
                _state.update { it.copy(isLoading = false, lecturers = entries) }
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun bulkImport(filePart: MultipartBody.Part, onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isUploading = true) }
            try {
                val response = RetrofitClient.apiService.bulkImport(filePart)
                response.forEach { item ->
                    val u = item["generated_user"] ?: ""
                    val p = item["generated_pass"] ?: ""
                    if (u.isNotEmpty() && p.isNotEmpty()) passwordCache[u] = p
                }
                fetchLecturers()
                onSuccess(response.size)
            } catch (e: retrofit2.HttpException) {
                onError("Sunucu hatası (${e.code()}): ${e.message}")
            } catch (e: Exception) {
                onError("Yükleme hatası: ${e.message}")
            } finally {
                _state.update { it.copy(isUploading = false) }
            }
        }
    }

    fun createLecturer(
        firstName: String, lastName: String, department: String,
        onSuccess: (String, String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmittingLecturer = true) }
            try {
                val username = generateUsername(firstName, lastName)
                val password = generateAlphanumericPass()
                passwordCache[username] = password
                RetrofitClient.apiService.createAdmin(
                    mapOf(
                        "username" to username, "password" to password,
                        "first_name" to firstName, "last_name" to lastName,
                        "role" to "LECTURER", "department_name" to department,
                        "must_change_password" to "true"
                    )
                )
                fetchLecturers()
                onSuccess(username, password)
            } catch (e: retrofit2.HttpException) {
                val msg = if (e.code() == 400 || e.code() == 409)
                    "Bu kullanıcı adı zaten mevcut. Farklı bir isim deneyin."
                else "Hata: ${e.message}"
                onError(msg)
            } catch (e: Exception) {
                onError("Hata: ${e.message}")
            } finally {
                _state.update { it.copy(isSubmittingLecturer = false) }
            }
        }
    }

    fun createCourse(name: String, code: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmittingCourse = true) }
            try {
                RetrofitClient.apiService.createCourse(mapOf("name" to name, "code" to code))
                onSuccess()
            } catch (e: Exception) {
                onError("Hata: ${e.message}")
            } finally {
                _state.update { it.copy(isSubmittingCourse = false) }
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
            try {
                val username = if (studentNumber.isNotBlank()) studentNumber
                               else generateUsername(firstName, lastName)
                val password = generateAlphanumericPass()
                RetrofitClient.apiService.createAdmin(
                    mapOf(
                        "username" to username, "password" to password,
                        "first_name" to firstName, "last_name" to lastName,
                        "role" to "STUDENT", "must_change_password" to "true"
                    )
                )
                onSuccess(username, password)
            } catch (e: retrofit2.HttpException) {
                val msg = if (e.code() in listOf(400, 409)) "Bu öğrenci numarası zaten kayıtlı."
                          else "Hata: ${e.message}"
                onError(msg)
            } catch (e: Exception) {
                onError("Hata: ${e.message}")
            } finally {
                _state.update { it.copy(isSubmittingStudent = false) }
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
