package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.NetworkResult
import com.bugra.campussync.repository.ChangePasswordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChangePasswordUiState(
    val isLoading: Boolean = false,
    val errorMessage: String = ""
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val repository: ChangePasswordRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChangePasswordUiState())
    val state: StateFlow<ChangePasswordUiState> = _state.asStateFlow()

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = "") }
            when (val result = repository.changePassword(currentPassword, newPassword)) {
                is NetworkResult.Success -> {
                    onSuccess()
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(errorMessage = result.message) }
                }
                is NetworkResult.Loading -> { }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = "") }
    }
}
