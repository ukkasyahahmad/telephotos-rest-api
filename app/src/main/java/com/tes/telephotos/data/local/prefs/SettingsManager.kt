package com.tes.telephotos.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
        val BOT_TOKEN = stringPreferencesKey("bot_token")
        val CHAT_ID = stringPreferencesKey("chat_id")
    }

    val isSetupCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SETUP_COMPLETED] ?: false
        }

    val botToken: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[BOT_TOKEN]
        }

    val chatId: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[CHAT_ID]
        }

    suspend fun saveBotCredentials(token: String, chat: String) {
        context.dataStore.edit { preferences ->
            preferences[BOT_TOKEN] = token
            preferences[CHAT_ID] = chat
            preferences[SETUP_COMPLETED] = true
        }
    }
}