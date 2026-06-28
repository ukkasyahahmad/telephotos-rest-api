package com.tes.telephotos.data.telegram

import android.content.Context
import com.tes.telephotos.data.local.prefs.SettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class TelegramClientWrapper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager
) {
    var client: Client? = null
        private set

    private val _authState = MutableStateFlow<TdApi.AuthorizationState?>(null)
    val authState: StateFlow<TdApi.AuthorizationState?> = _authState

    init {
        createClient()
    }

    private fun createClient() {
        client = Client.create({ obj ->
            when (obj) {
                is TdApi.UpdateAuthorizationState -> {
                    _authState.value = obj.authorizationState
                    if (obj.authorizationState is TdApi.AuthorizationStateWaitTdlibParameters) {
                        setTdlibParameters()
                    }
                }
            }
        }, { e ->
            e.printStackTrace()
        }, { e ->
            e.printStackTrace()
        })
    }

    private fun setTdlibParameters() {
        CoroutineScope(Dispatchers.IO).launch {
            val apiId = settingsManager.apiId.firstOrNull() ?: 94575
            val apiHash = settingsManager.apiHash.firstOrNull() ?: "a3406de8d171bb422bb6ddf3bbd800e2"

            val parameters = TdApi.SetTdlibParameters()
            parameters.apiId = apiId
            parameters.apiHash = apiHash
            parameters.databaseDirectory = File(context.filesDir, "tdlib").absolutePath
            parameters.useMessageDatabase = true
            parameters.useSecretChats = true
            parameters.systemLanguageCode = "en"
            parameters.deviceModel = "Android"
            parameters.systemVersion = "Unknown"
            parameters.applicationVersion = "1.0"

            client?.send(parameters) { _ -> }
        }
    }

    fun sendPhoneNumber(phoneNumber: String) {
        client?.send(TdApi.SetAuthenticationPhoneNumber(phoneNumber, null)) { _ -> }
    }

    fun sendAuthenticationCode(code: String) {
        client?.send(TdApi.CheckAuthenticationCode(code)) { _ -> }
    }

    fun logOut() {
        client?.send(TdApi.LogOut()) { _ -> }
    }

    suspend fun getSavedMessagesChatId(): Long? = suspendCoroutine { continuation ->
        client?.send(TdApi.GetChats(TdApi.ChatListMain(), 100)) { result ->
            if (result is TdApi.Chats) {
                client?.send(TdApi.GetMe()) { meResult ->
                    if (meResult is TdApi.User) {
                        continuation.resume(meResult.id.toLong())
                    } else {
                        continuation.resume(null)
                    }
                }
            } else {
                continuation.resume(null)
            }
        } ?: continuation.resume(null)
    }
}