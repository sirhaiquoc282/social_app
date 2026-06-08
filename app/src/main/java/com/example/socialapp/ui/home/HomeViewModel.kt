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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.content.Context
import com.example.socialapp.util.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var lastConvs: List<Conversation> = emptyList()

    init {
        loadData()
    }

    private fun loadData() {
        val uid = authRepository.currentUser?.uid ?: return

        _isLoading.value = true
        viewModelScope.launch {
            _currentUser.value = authRepository.getUser(uid)
        }

        // Lắng nghe danh sách người dùng để cache
        viewModelScope.launch {
            authRepository.observeAllUsers()
                .catch { }
                .collect { _allUsers.value = it }
        }

        // Lắng nghe cuộc trò chuyện và tự động gộp với thông tin người dùng mới nhất
        viewModelScope.launch {
            chatRepository.observeConversations()
                .catch { e -> _isLoading.value = false }
                .collectLatest { convs ->
                    val enriched = convs.map { conv ->
                        val otherId = conv.participants.firstOrNull { it != uid } ?: ""
                        // Ưu tiên lấy từ danh sách allUsers đang được observe real-time
                        val other = _allUsers.value.find { it.uid == otherId } 
                            ?: authRepository.getUser(otherId)
                        
                        conv.copy(
                            otherUserId = otherId,
                            otherUserName = other?.displayName ?: "Người dùng",
                            otherUserAvatar = other?.avatarUrl ?: ""
                        )
                    }

                    // Xử lý thông báo tin nhắn mới
                    checkNewMessages(enriched)

                    _conversations.value = enriched
                    _isLoading.value = false
                    lastConvs = enriched
                }
        }
    }

    private fun checkNewMessages(newConvs: List<Conversation>) {
        // Log để debug xem hàm có được chạy không
        android.util.Log.d("HomeViewModel", "Checking new messages. Count: ${newConvs.size}")
        
        if (lastConvs.isEmpty()) {
            lastConvs = newConvs
            return
        }
        
        newConvs.forEach { newConv ->
            val oldConv = lastConvs.find { it.id == newConv.id }
            
            // Log chi tiết từng conversation
            android.util.Log.d("HomeViewModel", "Conv ${newConv.id}: isRead=${newConv.isRead}, lastSender=${newConv.lastSenderId}")

            // Nếu có tin nhắn mới từ người khác và chưa đọc
            if (newConv.lastSenderId != getCurrentUid() && !newConv.isRead) {
                // Kiểm tra xem tin nhắn có thực sự mới hơn cái cũ không
                if (oldConv == null || newConv.lastMessageAt != oldConv.lastMessageAt) {
                    android.util.Log.d("HomeViewModel", "SHOWING NOTIFICATION for ${newConv.otherUserName}")
                    NotificationHelper.showToast(context, "Bạn có tin nhắn mới từ ${newConv.otherUserName}")
                    NotificationHelper.showLocalNotification(
                        context, 
                        newConv.otherUserName, 
                        newConv.lastMessage
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    fun getCurrentUid(): String = authRepository.currentUser?.uid ?: ""
}

