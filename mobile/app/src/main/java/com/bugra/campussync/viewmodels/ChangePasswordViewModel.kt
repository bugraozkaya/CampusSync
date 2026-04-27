package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class ChangePasswordUiState(
    val isLoading: Boolean = false,
    val errorMessage: String = ""
)

class ChangePasswordViewModel : ViewModel() {

    private val _state = MutableStateFlow(ChangePasswordUiState())
    val state: StateFlow<ChangePasswordUiState> = _state.asStateFlow()

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = "") }
            try {
                RetrofitClient.apiService.changePassword(
                    mapOf("current_password" to currentPassword, "new_password" to newPassword)
                )
                onSuccess()
            } catch (e: HttpException) {
                val body = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                val msg = when {
                    body?.contains("error") == true -> {
                        val match = Regex(""""error"\s*:\s*"([^"]+)"""").find(body)
                        match?.groupValues?.get(1) ?: "Şifre değiştirilemedi (${e.code()})."
                    }
                    e.code() == 401 -> "Oturum süresi dolmuş. Lütfen tekrar giriş yapın."
                    else -> "Şifre değiştirilemedi (${e.code()})."
                }
                _state.update { it.copy(errorMessage = msg) }
            } catch (e: java.net.UnknownHostException) {
                _state.update { it.copy(errorMessage = "Sunucuya ulaşılamıyor.") }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "Hata: ${e.localizedMessage}") }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = "") }
    }
}
