package com.example.socialapp.ui.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialapp.data.model.CallSignal
import com.example.socialapp.data.repository.AuthRepository
import com.example.socialapp.data.repository.CallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CallsViewModel @Inject constructor(
    private val callRepository: CallRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _callHistory = MutableStateFlow<List<CallSignal>>(emptyList())
    val callHistory: StateFlow<List<CallSignal>> = _callHistory.asStateFlow()

    val currentUid: String get() = authRepository.currentUser?.uid ?: ""

    init {
        viewModelScope.launch {
            callRepository.observeCallHistory()
                .catch { }
                .collect { _callHistory.value = it }
        }
    }
}
