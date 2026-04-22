package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SuperAdminUiState(
    val isLoadingInstitutions: Boolean = true,
    val isSubmittingInstitution: Boolean = false,
    val isSubmittingAdmin: Boolean = false,
    val institutions: List<Map<String, Any>> = emptyList()
)

class SuperAdminViewModel : ViewModel() {

    private val _state = MutableStateFlow(SuperAdminUiState())
    val state: StateFlow<SuperAdminUiState> = _state.asStateFlow()

    init { loadInstitutions() }

    fun loadInstitutions() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingInstitutions = true) }
            try {
                val list = RetrofitClient.apiService.getInstitutions()
                _state.update { it.copy(isLoadingInstitutions = false, institutions = list) }
            } catch (_: Exception) {
                _state.update { it.copy(isLoadingInstitutions = false) }
            }
        }
    }

    fun createInstitution(name: String, type: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmittingInstitution = true) }
            try {
                RetrofitClient.apiService.createInstitution(mapOf("name" to name, "institution_type" to type))
                loadInstitutions()
                onSuccess()
            } catch (e: retrofit2.HttpException) {
                val msg = if (e.code() == 409) "Bu üniversite zaten mevcut." else "Hata: ${e.message}"
                onError(msg)
            } catch (e: Exception) {
                onError("Hata: ${e.message}")
            } finally {
                _state.update { it.copy(isSubmittingInstitution = false) }
            }
        }
    }

    fun createAdmin(
        institutionId: String, username: String, password: String,
        firstName: String, lastName: String,
        onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmittingAdmin = true) }
            try {
                RetrofitClient.apiService.createAdmin(
                    mapOf(
                        "institution_id" to institutionId,
                        "username" to username, "password" to password,
                        "first_name" to firstName, "last_name" to lastName,
                        "role" to "ADMIN"
                    )
                )
                onSuccess()
            } catch (e: retrofit2.HttpException) {
                val msg = if (e.code() in listOf(400, 409)) "Bu kullanıcı adı zaten mevcut." else "Hata: ${e.message}"
                onError(msg)
            } catch (e: Exception) {
                onError("Hata: ${e.message}")
            } finally {
                _state.update { it.copy(isSubmittingAdmin = false) }
            }
        }
    }
}
