package com.tes.telephotos.data.telegram

import android.content.Context
import com.tes.telephotos.data.local.prefs.SettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelegramBotWrapper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager
) {
    private val api: TelegramBotApi
    private val baseUrl = "https://api.telegram.org/"

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(TelegramBotApi::class.java)
    }

    suspend fun checkConnection(token: String): Pair<Boolean, String> {
        return try {
            val endpoint = "${baseUrl}bot$token/getMe"
            val response = api.getMe(endpoint)

            if (response.ok && response.result != null) {
                Pair(true, "Connected as: ${response.result.first_name}")
            } else {
                Pair(false, response.description ?: "Invalid Token")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, "Connection failed: ${e.localizedMessage}")
        }
    }

    suspend fun uploadMedia(file: File, isVideo: Boolean): TelegramResponse? {
        val token = settingsManager.botToken.firstOrNull() ?: return null
        val chatId = settingsManager.chatId.firstOrNull() ?: return null

        val mimeType = if (isVideo) "video/mp4" else "image/jpeg"
        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData(if (isVideo) "video" else "photo", file.name, requestFile)

        return try {
            if (isVideo) {
                val endpoint = "${baseUrl}bot$token/sendVideo"
                api.sendVideo(endpoint, chatId, body)
            } else {
                val endpoint = "${baseUrl}bot$token/sendPhoto"
                api.sendPhoto(endpoint, chatId, body)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}