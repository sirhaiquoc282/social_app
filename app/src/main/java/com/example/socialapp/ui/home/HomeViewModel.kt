package com.example.socialapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialapp.data.model.Conversation
import com.example.socialapp.data.model.User
import com.example.socialapp.data.repository.AuthRepository
import com.example.socialapp.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val uid = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            _currentUser.value = authRepository.getUser(uid)
        }

        viewModelScope.launch {
            chatRepository.observeConversations()
                .catch { /* log */ }
                .collect { convs ->
                    // Enrich với thông tin user của bên kia
                    val enriched = convs.map { conv ->
                        val otherId = conv.participants.firstOrNull { it != uid } ?: ""
                        val other = authRepository.getUser(otherId)
                        conv.copy(
                            otherUserId = otherId,
                            otherUserName = other?.displayName ?: "Người dùng",
                            otherUserAvatar = other?.avatarUrl ?: ""
                        )
                    }
                    _conversations.value = enriched
                }
        }

        viewModelScope.launch {
            authRepository.observeAllUsers()
                .catch { }
                .collect { _allUsers.value = it }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    fun getCurrentUid(): String = authRepository.currentUser?.uid ?: ""
}

