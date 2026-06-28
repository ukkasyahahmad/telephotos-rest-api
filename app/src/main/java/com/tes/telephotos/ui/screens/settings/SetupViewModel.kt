package com.tes.telephotos.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tes.telephotos.data.local.prefs.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {

    val isSetupCompleted: StateFlow<Boolean> = settingsManager.isSetupCompleted
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun saveCredentials(apiId: Int, apiHash: String) {
        viewModelScope.launch {
            settingsManager.saveApiCredentials(apiId, apiHash)
        }
    }
}