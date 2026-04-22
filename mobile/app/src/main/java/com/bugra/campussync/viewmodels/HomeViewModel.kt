package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.RetrofitClient
import com.bugra.campussync.network.ScheduleItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val schedules: List<ScheduleItem> = emptyList(),
    val adminSummary: Map<String, Any> = emptyMap(),
    val unreadCount: Int = 0
)

class HomeViewModel : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun load(isAdmin: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                if (isAdmin) {
                    val summary = RetrofitClient.apiService.getAdminSummary()
                    _state.update { it.copy(adminSummary = summary) }
                } else {
                    val schedules = RetrofitClient.apiService.getSchedules().results
                    _state.update { it.copy(schedules = schedules) }
                }
                try {
                    val countResult = RetrofitClient.apiService.getUnreadCount()
                    _state.update { it.copy(unreadCount = countResult["unread_count"] ?: 0) }
                } catch (_: Exception) {}
                _state.update { it.copy(isLoading = false) }
            } catch (e: java.net.UnknownHostException) {
                _state.update { it.copy(isLoading = false, error = "Sunucuya ulaşılamıyor. İnternet bağlantınızı kontrol edin.") }
            } catch (e: java.net.SocketTimeoutException) {
                _state.update { it.copy(isLoading = false, error = "Bağlantı zaman aşımına uğradı.") }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Veriler yüklenemedi. Tekrar denemek için aşağıdaki butona tıklayın.") }
            }
        }
    }
}
