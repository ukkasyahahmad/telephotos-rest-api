package com.tes.telephotos.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tes.telephotos.data.local.MediaDao
import com.tes.telephotos.data.local.prefs.SettingsManager
import com.tes.telephotos.data.telegram.TelegramBotWrapper
import com.tes.telephotos.domain.model.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val mediaDao: MediaDao,
    private val botWrapper: TelegramBotWrapper
) : ViewModel() {

    val botToken: StateFlow<String> = settingsManager.botToken
        .map { it ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val chatId: StateFlow<String> = settingsManager.chatId
        .map { it ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val totalMediaCount = MutableStateFlow(0)
    val syncedMediaCount = MutableStateFlow(0)
    val isUploading = MutableStateFlow(false)

    val connectionTestResult = MutableStateFlow<String?>(null)
    val isTestingConnection = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            mediaDao.getAllMedia().collect { mediaList ->
                totalMediaCount.value = mediaList.size
                syncedMediaCount.value = mediaList.count { it.syncState == SyncState.SYNCED }
                isUploading.value = mediaList.any { it.syncState == SyncState.UPLOADING }
            }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            isTestingConnection.value = true
            val token = botToken.value
            if (token.isBlank()) {
                connectionTestResult.value = "Token kosong. Silakan setup ulang."
                isTestingConnection.value = false
                return@launch
            }

            val (isSuccess, message) = botWrapper.checkConnection(token)
            if (isSuccess) {
                connectionTestResult.value = "Sukses: $message"
            } else {
                connectionTestResult.value = "Gagal: $message"
            }
            isTestingConnection.value = false
        }
    }

    fun clearConnectionTestResult() {
        connectionTestResult.value = null
    }
}