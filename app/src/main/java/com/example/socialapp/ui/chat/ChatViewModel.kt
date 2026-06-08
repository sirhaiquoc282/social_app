package com.example.socialapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialapp.data.model.Message
import com.example.socialapp.data.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ChatUiState {
    object Idle : ChatUiState()
    object Loading : ChatUiState()
    data class Error(val message: String?) : ChatUiState()
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private var currentOtherUid: String = ""

    private var messagesJob: kotlinx.coroutines.Job? = null

    fun loadMessages(otherUid: String) {
        if (currentOtherUid == otherUid && messagesJob?.isActive == true) return
        currentOtherUid = otherUid
        messagesJob?.cancel()
        
        _uiState.value = ChatUiState.Loading
        messagesJob = viewModelScope.launch {
            chatRepository.observeMessages(otherUid)
                .catch { _uiState.value = ChatUiState.Error(it.message) }
                .collect { msgs ->
                    _messages.value = msgs
                    _uiState.value = ChatUiState.Idle
                }
        }
    }

    fun setInputText(text: String) {
        _inputText.value = text
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isBlank() || currentOtherUid.isBlank()) return
        _inputText.value = ""
        viewModelScope.launch {
            runCatching { chatRepository.sendMessage(currentOtherUid, text) }
                .onFailure { _uiState.value = ChatUiState.Error(it.message) }
        }
    }

    fun getCurrentUserId(): String = FirebaseAuth.getInstance().currentUser?.uid ?: ""
}

