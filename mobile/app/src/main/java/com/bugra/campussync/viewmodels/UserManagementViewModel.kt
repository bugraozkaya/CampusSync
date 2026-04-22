package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.RetrofitClient
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UserManagementUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val users: List<Map<String, Any>> = emptyList()
)

@OptIn(FlowPreview::class)
class UserManagementViewModel : ViewModel() {

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
            try {
                val users = RetrofitClient.apiService.getUsers(search = search).results
                _state.update { it.copy(isLoading = false, users = users) }
            } catch (e: java.net.UnknownHostException) {
                _state.update { it.copy(isLoading = false, error = "Sunucuya ulaşılamıyor.") }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Kullanıcılar yüklenemedi: ${e.message}") }
            }
        }
    }
}
