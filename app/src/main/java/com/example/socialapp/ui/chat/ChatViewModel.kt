package com.example.socialapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialapp.data.model.Conversation
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

    private val _newNotification = MutableStateFlow<Conversation?>(null)
    val newNotification: StateFlow<Conversation?> = _newNotification.asStateFlow()

    private var currentOtherUid: String = ""
    private var messagesJob: kotlinx.coroutines.Job? = null
    private var allConvsJob: kotlinx.coroutines.Job? = null
    private var initialConvsLoaded = false
    private var lastKnownConvs: List<Conversation> = emptyList()

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

        observeOtherConversations()
    }

    private fun observeOtherConversations() {
        allConvsJob?.cancel()
        val uid = getCurrentUserId()
        allConvsJob = viewModelScope.launch {
            chatRepository.observeConversations().collect { convs ->
                if (!initialConvsLoaded) {
                    lastKnownConvs = convs
                    initialConvsLoaded = true
                    return@collect
                }

                convs.forEach { conv ->
                    val otherId = conv.participants.firstOrNull { it != uid } ?: ""
                    // Nếu là tin nhắn mới từ người KHÁC (không phải người đang chat cùng)
                    if (otherId != currentOtherUid && conv.lastSenderId != uid && !conv.isReadBy(uid)) {
                        val oldConv = lastKnownConvs.find { it.id == conv.id }
                        if (oldConv == null || conv.lastMessageAt != oldConv.lastMessageAt) {
                            _newNotification.value = conv
                            // Tự động xóa thông báo sau 3 giây
                            viewModelScope.launch {
                                kotlinx.coroutines.delay(3000)
                                if (_newNotification.value?.id == conv.id) {
                                    _newNotification.value = null
                                }
                            }
                        }
                    }
                }
                lastKnownConvs = convs
            }
        }
    }

    fun dismissNotification() {
        _newNotification.value = null
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
