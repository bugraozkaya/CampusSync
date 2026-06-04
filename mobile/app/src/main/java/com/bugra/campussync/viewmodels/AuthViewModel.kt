package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.LoginRequest
import com.bugra.campussync.network.LoginResponse
import com.bugra.campussync.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val loginResult: LoginResponse? = null,
    val forgotPasswordResult: String? = null,
    val forgotPasswordError: String? = null,
    val isForgotLoading: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = "") }
            try {
                val response = authRepository.login(LoginRequest(username.trim(), password))
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

    fun forgotPassword(username: String) {
        if (username.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isForgotLoading = true, forgotPasswordError = null, forgotPasswordResult = null) }
            try {
                val res = authRepository.forgotPassword(username.trim())
                _state.update { it.copy(isForgotLoading = false, forgotPasswordResult = res["temp_password"]) }
            } catch (e: HttpException) {
                val body = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                val match = body?.let { Regex(""""error"\s*:\s*"([^"]+)"""").find(it) }
                val msg = match?.groupValues?.get(1) ?: "Bir hata oluştu."
                _state.update { it.copy(isForgotLoading = false, forgotPasswordError = msg) }
            } catch (e: Exception) {
                _state.update { it.copy(isForgotLoading = false, forgotPasswordError = "Sunucuya ulaşılamıyor.") }
            }
        }
    }

    fun clearForgotState() {
        _state.update { it.copy(forgotPasswordResult = null, forgotPasswordError = null) }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = "") }
    }

    fun consumeLoginResult() {
        _state.update { it.copy(loginResult = null) }
    }
}
