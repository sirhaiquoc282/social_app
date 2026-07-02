package com.example.socialapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialapp.data.local.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val isActiveStatusOn = settingsDataStore.isActiveStatusOn.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val isPushNotificationOn = settingsDataStore.isPushNotificationOn.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val isSoundOn = settingsDataStore.isSoundOn.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val isVibrateOn = settingsDataStore.isVibrateOn.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setActiveStatus(isEnabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setActiveStatus(isEnabled) }
    }

    fun setPushNotifications(isEnabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setPushNotifications(isEnabled) }
    }

    fun setSound(isEnabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setSound(isEnabled) }
    }

    fun setVibrate(isEnabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setVibrate(isEnabled) }
    }
}
