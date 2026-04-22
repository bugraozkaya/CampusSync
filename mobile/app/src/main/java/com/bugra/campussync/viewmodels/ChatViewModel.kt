package com.bugra.campussync.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.ChatContact
import com.bugra.campussync.network.ChatConversation
import com.bugra.campussync.network.ChatMessage
import com.bugra.campussync.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatInboxUiState(
    val isLoading: Boolean = true,
    val conversations: List<ChatConversation> = emptyList(),
    val contacts: List<ChatContact> = emptyList()
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false
)

class ChatInboxViewModel : ViewModel() {

    private val _state = MutableStateFlow(ChatInboxUiState())
    val state: StateFlow<ChatInboxUiState> = _state.asStateFlow()

    init { loadConversations() }

    fun loadConversations() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val convs = RetrofitClient.apiService.getChatInbox()
                _state.update { it.copy(isLoading = false, conversations = convs) }
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadContacts() {
        viewModelScope.launch {
            try {
                val contacts = RetrofitClient.apiService.getChatContacts()
                _state.update { it.copy(contacts = contacts) }
            } catch (_: Exception) {}
        }
    }
}

class ChatViewModel : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var currentPartnerId: Int = -1

    fun startPolling(partnerId: Int) {
        if (currentPartnerId == partnerId) return
        currentPartnerId = partnerId
        viewModelScope.launch {
            while (true) {
                loadMessages()
                delay(3000)
            }
        }
    }

    private suspend fun loadMessages() {
        try {
            val fetched = RetrofitClient.apiService.getChatMessages(currentPartnerId)
            _state.update { it.copy(messages = fetched) }
        } catch (_: Exception) {}
    }

    fun sendMessage(
        partnerId: Int, text: String,
        onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isSending = true) }
            try {
                RetrofitClient.apiService.sendChatMessage(mapOf("receiver_id" to partnerId, "content" to text))
                loadMessages()
                onSuccess()
            } catch (e: Exception) {
                onError("Gönderilemedi: ${e.message}")
            } finally {
                _state.update { it.copy(isSending = false) }
            }
        }
    }
}
