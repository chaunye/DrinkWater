package com.drinkwater.data.db

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val POPUP_MODE = stringPreferencesKey("popup_mode") // "fullscreen" or "floating"
        val DELAY_MINUTES = intPreferencesKey("delay_minutes")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        val MUST_READ_DISMISSED = booleanPreferencesKey("must_read_dismissed")
    }

    val popupMode: Flow<String> = context.dataStore.data.map { it[POPUP_MODE] ?: "floating" }
    val delayMinutes: Flow<Int> = context.dataStore.data.map { it[DELAY_MINUTES] ?: 5 }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }
    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { it[FIRST_LAUNCH] ?: true }
    val serviceEnabled: Flow<Boolean> = context.dataStore.data.map { it[SERVICE_ENABLED] ?: true }
    val mustReadDismissed: Flow<Boolean> = context.dataStore.data.map { it[MUST_READ_DISMISSED] ?: false }

    suspend fun setPopupMode(mode: String) {
        context.dataStore.edit { it[POPUP_MODE] = mode }
    }

    suspend fun setDelayMinutes(minutes: Int) {
        context.dataStore.edit { it[DELAY_MINUTES] = minutes }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setFirstLaunchDone() {
        context.dataStore.edit { it[FIRST_LAUNCH] = false }
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[SERVICE_ENABLED] = enabled }
    }

    suspend fun setMustReadDismissed() {
        context.dataStore.edit { it[MUST_READ_DISMISSED] = true }
    }
}
