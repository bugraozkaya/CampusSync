package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.NetworkResult
import com.bugra.campussync.repository.SettingsRepository
import com.bugra.campussync.utils.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isSaving: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun updateProfile(firstName: String, lastName: String, title: String, department: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null, success = false) }
            val result = repository.updateProfile(firstName, lastName, title, department)
            when (result) {
                is NetworkResult.Success -> {
                    tokenManager.saveUserInfo(firstName, lastName, title)
                    // Also save to the older fields to satisfy HomeScreen check if needed, 
                    // or I will fix HomeScreen check.
                    tokenManager.saveProfileInfo("$firstName $lastName".trim(), department, title)
                    
                    _state.update { it.copy(isSaving = false, success = true) }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isSaving = false, error = result.message) }
                }
                is NetworkResult.Loading -> { }
                else -> {}
            }
        }
    }

    fun clearState() {
        _state.update { it.copy(error = null, success = false) }
    }
}
