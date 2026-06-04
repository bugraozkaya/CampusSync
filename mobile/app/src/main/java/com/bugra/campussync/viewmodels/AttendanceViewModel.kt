package com.bugra.campussync.viewmodels

import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.*
import com.bugra.campussync.repository.AttendanceRepository
import com.bugra.campussync.utils.safeApiCall
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AttendanceUiState(
    val schedules: List<ScheduleItem> = emptyList(),
    val history: List<AttendanceRecordItem> = emptyList(),
    val sessions: List<AttendanceSessionItem> = emptyList(),
    val activeSession: AttendanceSessionItem? = null,
    val sessionRecords: List<AttendanceRecordItem> = emptyList(),
    val qrBitmap: Bitmap? = null,
    val secondsLeft: Int = 0,
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val repository: AttendanceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AttendanceUiState())
    val state: StateFlow<AttendanceUiState> = _state.asStateFlow()

    fun loadSchedules() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = safeApiCall { repository.getSchedules() }) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(schedules = result.data.results, isLoading = false) }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is NetworkResult.Loading -> { }
                else -> {}
            }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = safeApiCall { repository.getMyAttendance() }) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(history = result.data, isLoading = false) }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is NetworkResult.Loading -> { }
                else -> {}
            }
        }
    }

    fun loadMySessions() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = safeApiCall { repository.getMySessions() }) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(sessions = result.data, isLoading = false) }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is NetworkResult.Loading -> { }
                else -> {}
            }
        }
    }

    fun checkIn(token: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = safeApiCall { repository.checkInAttendance(token) }) {
                is NetworkResult.Success -> {
                    onSuccess(result.data["course"] ?: "")
                }
                is NetworkResult.Error -> {
                    onError(result.message)
                }
                is NetworkResult.Loading -> { }
                else -> {}
            }
        }
    }

    fun createSession(scheduleId: Int, sessionDate: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isCreating = true) }
            when (val result = safeApiCall { repository.createAttendanceSession(scheduleId, sessionDate) }) {
                is NetworkResult.Success -> {
                    val session = result.data
                    val qr = generateQR(session.token)
                    _state.update { it.copy(activeSession = session, qrBitmap = qr, secondsLeft = 90, isCreating = false) }
                    startCountdown()
                    pollSessionRecords(session.id)
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isCreating = false) }
                    onError(result.message)
                }
                is NetworkResult.Loading -> { }
                else -> {}
            }
        }
    }

    private fun startCountdown() {
        viewModelScope.launch {
            while (_state.value.secondsLeft > 0) {
                delay(1000)
                _state.update { it.copy(secondsLeft = it.secondsLeft - 1) }
            }
            endSession()
        }
    }

    private fun pollSessionRecords(sessionId: Int) {
        viewModelScope.launch {
            while (_state.value.activeSession != null) {
                val result = safeApiCall { repository.getSessionRecords(sessionId) }
                if (result is NetworkResult.Success) {
                    _state.update { it.copy(sessionRecords = result.data) }
                }
                delay(3000)
            }
        }
    }

    fun fetchSessionRecords(sessionId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = safeApiCall { repository.getSessionRecords(sessionId) }) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(sessionRecords = result.data, isLoading = false) }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is NetworkResult.Loading -> { }
            }
        }
    }

    fun endSession() {
        _state.update { it.copy(activeSession = null, qrBitmap = null, sessionRecords = emptyList(), secondsLeft = 0) }
    }

    private fun generateQR(token: String): Bitmap {
        val writer = QRCodeWriter()
        val matrix = writer.encode(token, BarcodeFormat.QR_CODE, 512, 512)
        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
        for (x in 0 until 512) for (y in 0 until 512)
            bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        return bmp
    }
}
