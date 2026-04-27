package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.LoginRequest
import com.bugra.campussync.network.LoginResponse
import com.bugra.campussync.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val loginResult: LoginResponse? = null
)

class AuthViewModel : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = "") }
            try {
                val response = RetrofitClient.apiService.login(LoginRequest(username.trim(), password))
                RetrofitClient.authToken = response.access
                _state.update { it.copy(isLoading = false, loginResult = response) }
            } catch (e: HttpException) {
                val body = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                val serverMsg = body?.let {
                    Regex(""""detail"\s*:\s*"([^"]+)"""").find(it)?.groupValues?.get(1)
                }
                val msg = serverMsg ?: when (e.code()) {
                    401 -> "Kullanıcı adı veya şifre hatalı."
                    403 -> "Bu hesaba erişim izniniz yok."
                    423 -> "Hesabınız geçici olarak kilitlendi. Lütfen daha sonra tekrar deneyin."
                    500 -> "Sunucu hatası. Lütfen daha sonra tekrar deneyin."
                    else -> "Giriş başarısız (${e.code()})."
                }
                _state.update { it.copy(isLoading = false, errorMessage = msg) }
            } catch (e: java.net.UnknownHostException) {
                _state.update { it.copy(isLoading = false, errorMessage = "Sunucuya ulaşılamıyor. İnternet bağlantınızı kontrol edin.") }
            } catch (e: java.net.SocketTimeoutException) {
                _state.update { it.copy(isLoading = false, errorMessage = "Bağlantı zaman aşımına uğradı. Tekrar deneyin.") }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = "Bağlantı hatası. Sunucunun çalıştığından emin olun.") }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = "") }
    }

    fun consumeLoginResult() {
        _state.update { it.copy(loginResult = null) }
    }
}
