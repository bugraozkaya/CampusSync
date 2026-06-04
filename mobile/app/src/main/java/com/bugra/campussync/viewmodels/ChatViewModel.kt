package com.bugra.campussync.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugra.campussync.network.*
import com.bugra.campussync.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatInboxUiState(
    val isLoading: Boolean = true,
    val conversations: List<ChatConversation> = emptyList(),
    val contacts: List<ChatContact> = emptyList()
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false
)

@HiltViewModel
class ChatInboxViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatInboxUiState())
    val state: StateFlow<ChatInboxUiState> = _state.asStateFlow()

    init {
        loadConversations()
        viewModelScope.launch {
            while (true) {
                delay(5000)
                val result = repository.getChatInbox()
                if (result is NetworkResult.Success) {
                    _state.update { it.copy(conversations = result.data) }
                }
            }
        }
    }

    fun loadConversations() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = repository.getChatInbox()
            if (result is NetworkResult.Success) {
                _state.update { it.copy(isLoading = false, conversations = result.data) }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadContacts() {
        viewModelScope.launch {
            val result = repository.getChatContacts()
            if (result is NetworkResult.Success) {
                _state.update { it.copy(contacts = result.data) }
            }
        }
    }
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

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
        val result = repository.getChatMessages(currentPartnerId)
        if (result is NetworkResult.Success) {
            _state.update { it.copy(messages = result.data) }
        }
    }

    fun sendMessage(
        partnerId: Int, text: String,
        onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isSending = true) }
            val result = repository.sendChatMessage(partnerId, text)
            when (result) {
                is NetworkResult.Success -> {
                    loadMessages()
                    onSuccess()
                }
                is NetworkResult.Error -> {
                    Log.e("ChatViewModel", "sendMessage failed: partnerId=$partnerId, error=${result.message}")
                    onError(result.message)
                }
                is NetworkResult.Loading -> { }
            }
            _state.update { it.copy(isSending = false) }
        }
    }

    fun sendFile(
        partnerId: Int, content: String,
        file: okhttp3.MultipartBody.Part,
        onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSending = true) }
            val result = repository.sendChatMessageWithFile(partnerId, content, file)
            when (result) {
                is NetworkResult.Success -> {
                    loadMessages()
                    onSuccess()
                }
                is NetworkResult.Error -> {
                    onError(result.message)
                }
                is NetworkResult.Loading -> { }
                else -> {}
            }
            _state.update { it.copy(isSending = false) }
        }
    }
}
