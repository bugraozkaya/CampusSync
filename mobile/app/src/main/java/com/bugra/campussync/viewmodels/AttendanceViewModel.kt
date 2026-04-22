package com.bugra.campussync.viewmodels

import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.AttendanceRecordItem
import com.bugra.campussync.network.AttendanceSessionItem
import com.bugra.campussync.network.RetrofitClient
import com.bugra.campussync.network.ScheduleItem
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AttendanceUiState(
    val schedules: List<ScheduleItem> = emptyList(),
    val history: List<AttendanceRecordItem> = emptyList(),
    val activeSession: AttendanceSessionItem? = null,
    val sessionRecords: List<AttendanceRecordItem> = emptyList(),
    val qrBitmap: Bitmap? = null,
    val secondsLeft: Int = 0,
    val isLoading: Boolean = true,
    val isCreating: Boolean = false
)

class AttendanceViewModel : ViewModel() {

    private val _state = MutableStateFlow(AttendanceUiState())
    val state: StateFlow<AttendanceUiState> = _state.asStateFlow()

    fun loadSchedules() {
        viewModelScope.launch {
            try {
                val schedules = RetrofitClient.apiService.getSchedules().results
                _state.update { it.copy(schedules = schedules, isLoading = false) }
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            try {
                val records = RetrofitClient.apiService.getMyAttendance()
                _state.update { it.copy(history = records, isLoading = false) }
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun checkIn(token: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = RetrofitClient.apiService.checkInAttendance(mapOf("token" to token))
                onSuccess(result["course"] as? String ?: "")
            } catch (e: retrofit2.HttpException) {
                onError(when (e.code()) {
                    404 -> "Geçersiz veya süresi dolmuş QR kodu."
                    409 -> "Zaten yoklamaya katıldınız."
                    410 -> "QR kodunun süresi doldu."
                    403 -> "Bu derse kayıtlı değilsiniz."
                    else -> "Hata: ${e.message}"
                })
            } catch (e: Exception) {
                onError("Hata: ${e.message}")
            }
        }
    }

    fun createSession(scheduleId: Int, sessionDate: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isCreating = true) }
            try {
                val session = RetrofitClient.apiService.createAttendanceSession(
                    mapOf("schedule_id" to scheduleId, "session_date" to sessionDate)
                )
                val qr = generateQR(session.token)
                _state.update { it.copy(activeSession = session, qrBitmap = qr, secondsLeft = 90, isCreating = false) }
                startCountdown()
                pollSessionRecords(session.id)
            } catch (e: Exception) {
                _state.update { it.copy(isCreating = false) }
                onError("Oturum oluşturulamadı: ${e.message}")
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
                try {
                    val records = RetrofitClient.apiService.getSessionRecords(sessionId)
                    _state.update { it.copy(sessionRecords = records) }
                } catch (_: Exception) {}
                delay(3000)
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
