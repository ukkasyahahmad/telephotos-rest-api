package com.tes.telephotos.ui.screens.auth

import androidx.lifecycle.ViewModel
import com.tes.telephotos.data.telegram.TelegramClientWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import org.drinkless.tdlib.TdApi
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val telegramClientWrapper: TelegramClientWrapper
) : ViewModel() {

    val authState = telegramClientWrapper.authState

    fun sendPhoneNumber(phoneNumber: String) {
        telegramClientWrapper.sendPhoneNumber(phoneNumber)
    }

    fun sendAuthenticationCode(code: String) {
        telegramClientWrapper.sendAuthenticationCode(code)
    }
}