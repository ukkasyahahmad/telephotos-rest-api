package com.tes.telephotos.data.telegram

import android.content.Context
import com.tes.telephotos.data.local.prefs.SettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
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
    private val okHttpClient: OkHttpClient
    private val baseUrl = "https://api.telegram.org/"

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
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

    // --- JSON State Management ---
    
    /**
     * Mengambil file JSON yang di-pin di channel
     * @return Isi JSON dalam bentuk String, atau null jika gagal/tidak ada
     */
    suspend fun getPinnedJsonState(): String? {
        val token = settingsManager.botToken.firstOrNull() ?: return null
        val chatId = settingsManager.chatId.firstOrNull() ?: return null

        try {
            val chatResponse = api.getChat("${baseUrl}bot$token/getChat", chatId)
            if (!chatResponse.ok || chatResponse.result == null) return null
            
            val pinnedMsg = chatResponse.result.pinned_message ?: return null
            val document = pinnedMsg.document ?: return null
            
            if (document.file_name != "telephotos_meta.json") return null
            
            val fileResponse = api.getFile("${baseUrl}bot$token/getFile", document.file_id)
            if (!fileResponse.ok || fileResponse.result == null) return null
            
            val filePath = fileResponse.result.file_path ?: return null
            val downloadUrl = "https://api.telegram.org/file/bot$token/$filePath"
            
            val request = Request.Builder().url(downloadUrl).build()
            val response = okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                return response.body?.string()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Mengupload file JSON baru dan menandainya sebagai Pinned Message.
     * Menggantikan pin yang lama (jika ada).
     */
    suspend fun uploadPinnedJsonState(jsonString: String): Boolean {
        val token = settingsManager.botToken.firstOrNull() ?: return false
        val chatId = settingsManager.chatId.firstOrNull() ?: return false

        try {
            // Cek pesan yang dipin saat ini untuk di-unpin nanti
            val chatResponse = api.getChat("${baseUrl}bot$token/getChat", chatId)
            val oldPinnedMsgId = chatResponse.result?.pinned_message?.message_id

            // Simpan string ke file temporary
            val tempFile = File(context.cacheDir, "telephotos_meta.json")
            tempFile.writeText(jsonString)

            val requestFile = tempFile.asRequestBody("application/json".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("document", tempFile.name, requestFile)

            // Upload Document
            val uploadResponse = api.sendDocument("${baseUrl}bot$token/sendDocument", chatId, body)
            tempFile.delete()

            if (uploadResponse.ok && uploadResponse.result != null) {
                val newMsgId = uploadResponse.result.message_id
                
                // Pin pesan baru
                api.pinChatMessage("${baseUrl}bot$token/pinChatMessage", chatId, newMsgId)

                // Unpin pesan lama agar tidak menumpuk di daftar Pinned Messages
                if (oldPinnedMsgId != null) {
                    api.unpinChatMessage("${baseUrl}bot$token/unpinChatMessage", chatId, oldPinnedMsgId)
                }
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}