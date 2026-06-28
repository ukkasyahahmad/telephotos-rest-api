package com.tes.telephotos.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
        val API_ID = intPreferencesKey("api_id")
        val API_HASH = stringPreferencesKey("api_hash")
    }

    val isSetupCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SETUP_COMPLETED] ?: false
        }

    val apiId: Flow<Int?> = context.dataStore.data
        .map { preferences ->
            preferences[API_ID]
        }

    val apiHash: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[API_HASH]
        }

    suspend fun saveApiCredentials(id: Int, hash: String) {
        context.dataStore.edit { preferences ->
            preferences[API_ID] = id
            preferences[API_HASH] = hash
            preferences[SETUP_COMPLETED] = true
        }
    }
}