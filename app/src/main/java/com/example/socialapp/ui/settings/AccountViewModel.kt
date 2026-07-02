package com.example.socialapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialapp.data.model.User
import com.example.socialapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadUser()
    }

    fun loadUser() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _currentUser.value = authRepository.getUser(uid)
        }
    }

    fun updateDisplayName(newName: String) {
        viewModelScope.launch {
            val result = authRepository.updateDisplayName(newName)
            if (result.isSuccess) {
                _message.value = "Display name updated successfully."
                loadUser() // Reload
            } else {
                _message.value = "Failed to update: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun resetPassword() {
        val email = _currentUser.value?.email ?: return
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            if (result.isSuccess) {
                _message.value = "Password reset email sent."
            } else {
                _message.value = "Failed to send reset email: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
