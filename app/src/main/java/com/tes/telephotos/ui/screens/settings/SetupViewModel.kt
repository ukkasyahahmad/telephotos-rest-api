package com.tes.telephotos.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tes.telephotos.data.local.prefs.SettingsManager
import com.tes.telephotos.data.telegram.TelegramBotWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val botWrapper: TelegramBotWrapper
) : ViewModel() {

    val isSetupCompleted: StateFlow<Boolean> = settingsManager.isSetupCompleted
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val connectionStatus = MutableStateFlow<String?>(null)
    val isChecking = MutableStateFlow(false)

    fun checkAndSaveCredentials(botToken: String, chatId: String) {
        viewModelScope.launch {
            isChecking.value = true
            connectionStatus.value = "Checking connection..."

            val (isSuccess, message) = botWrapper.checkConnection(botToken)

            if (isSuccess) {
                connectionStatus.value = message + " - Saving..."
                settingsManager.saveBotCredentials(botToken, chatId)
            } else {
                connectionStatus.value = message
            }

            isChecking.value = false
        }
    }
}