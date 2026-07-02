package com.example.socialapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.dataStore

    companion object {
        val ACTIVE_STATUS_KEY = booleanPreferencesKey("active_status")
        val PUSH_NOTIFICATIONS_KEY = booleanPreferencesKey("push_notifications")
        val SOUND_KEY = booleanPreferencesKey("sound")
        val VIBRATE_KEY = booleanPreferencesKey("vibrate")
    }

    val isActiveStatusOn: Flow<Boolean> = dataStore.data.map { it[ACTIVE_STATUS_KEY] ?: true }
    val isPushNotificationOn: Flow<Boolean> = dataStore.data.map { it[PUSH_NOTIFICATIONS_KEY] ?: true }
    val isSoundOn: Flow<Boolean> = dataStore.data.map { it[SOUND_KEY] ?: true }
    val isVibrateOn: Flow<Boolean> = dataStore.data.map { it[VIBRATE_KEY] ?: true }

    suspend fun setActiveStatus(isEnabled: Boolean) {
        dataStore.edit { it[ACTIVE_STATUS_KEY] = isEnabled }
    }

    suspend fun setPushNotifications(isEnabled: Boolean) {
        dataStore.edit { it[PUSH_NOTIFICATIONS_KEY] = isEnabled }
    }

    suspend fun setSound(isEnabled: Boolean) {
        dataStore.edit { it[SOUND_KEY] = isEnabled }
    }

    suspend fun setVibrate(isEnabled: Boolean) {
        dataStore.edit { it[VIBRATE_KEY] = isEnabled }
    }
}
