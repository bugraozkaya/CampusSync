package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.NetworkResult
import com.bugra.campussync.repository.UserManagementRepository
import com.bugra.campussync.utils.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserManagementUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val users: List<Map<String, Any>> = emptyList()
)

@OptIn(FlowPreview::class)
@HiltViewModel
class UserManagementViewModel @Inject constructor(
    private val repository: UserManagementRepository
) : ViewModel() {

    private val _state = MutableStateFlow(UserManagementUiState())
    val state: StateFlow<UserManagementUiState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        load()
        _searchQuery
            .debounce(400)
            .distinctUntilChanged()
            .onEach { query -> fetchUsers(query.ifBlank { null }) }
            .launchIn(viewModelScope)
    }

    fun load() { fetchUsers(null) }

    fun onSearch(query: String) {
        _searchQuery.value = query
    }

    private fun fetchUsers(search: String?) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = safeApiCall { repository.getUsers(search = search) }
            when (result) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isLoading = false, users = result.data.results) }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is NetworkResult.Loading -> { }
            }
        }
    }
}
