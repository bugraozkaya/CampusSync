package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.NetworkResult
import com.bugra.campussync.repository.SuperAdminRepository
import com.bugra.campussync.utils.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SuperAdminUiState(
    val isLoadingInstitutions: Boolean = true,
    val isSubmittingInstitution: Boolean = false,
    val isSubmittingAdmin: Boolean = false,
    val institutions: List<Map<String, Any>> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class SuperAdminViewModel @Inject constructor(
    private val repository: SuperAdminRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SuperAdminUiState())
    val state: StateFlow<SuperAdminUiState> = _state.asStateFlow()

    init { loadInstitutions() }

    fun loadInstitutions() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingInstitutions = true, error = null) }
            when (val result = safeApiCall { repository.getInstitutions() }) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isLoadingInstitutions = false, institutions = result.data) }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isLoadingInstitutions = false, error = result.message) }
                }
                is NetworkResult.Loading -> { }
            }
        }
    }

    fun createInstitution(name: String, type: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmittingInstitution = true) }
            when (val result = safeApiCall { repository.createInstitution(mapOf("name" to name, "institution_type" to type)) }) {
                is NetworkResult.Success -> {
                    loadInstitutions()
                    onSuccess()
                    _state.update { it.copy(isSubmittingInstitution = false) }
                }
                is NetworkResult.Error -> {
                    val msg = if (result.message.contains("409")) "Bu üniversite zaten mevcut." else "Hata: ${result.message}"
                    onError(msg)
                    _state.update { it.copy(isSubmittingInstitution = false) }
                }
                is NetworkResult.Loading -> { }
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
            val data = mapOf(
                "institution_id" to institutionId,
                "username" to username, "password" to password,
                "first_name" to firstName, "last_name" to lastName,
                "role" to "ADMIN"
            )
            when (val result = safeApiCall { repository.createAdmin(data) }) {
                is NetworkResult.Success -> {
                    onSuccess()
                    _state.update { it.copy(isSubmittingAdmin = false) }
                }
                is NetworkResult.Error -> {
                    val msg = if (result.message.contains("400") || result.message.contains("409")) "Bu kullanıcı adı zaten mevcut." else "Hata: ${result.message}"
                    onError(msg)
                    _state.update { it.copy(isSubmittingAdmin = false) }
                }
                is NetworkResult.Loading -> { }
            }
        }
    }
}
