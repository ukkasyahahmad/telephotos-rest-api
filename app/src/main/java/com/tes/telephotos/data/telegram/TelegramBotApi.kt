package com.tes.telephotos.data.telegram

import retrofit2.http.GET
import retrofit2.http.Path

interface TelegramBotApi {
    // Endpoints sebelumnya
    @retrofit2.http.Multipart
    @retrofit2.http.POST("bot{token}/sendPhoto")
    suspend fun sendPhoto(
        @Path("token") token: String,
        @retrofit2.http.Query("chat_id") chatId: String,
        @retrofit2.http.Part photo: okhttp3.MultipartBody.Part
    ): TelegramResponse

    @retrofit2.http.Multipart
    @retrofit2.http.POST("bot{token}/sendVideo")
    suspend fun sendVideo(
        @Path("token") token: String,
        @retrofit2.http.Query("chat_id") chatId: String,
        @retrofit2.http.Part video: okhttp3.MultipartBody.Part
    ): TelegramResponse

    // Endpoint baru untuk cek koneksi (getMe)
    @GET("bot{token}/getMe")
    suspend fun getMe(@Path("token") token: String): TelegramResponse
}

data class TelegramResponse(
    val ok: Boolean,
    val result: MessageResult?,
    val description: String? = null
)

data class MessageResult(
    val id: Long? = null, // Untuk getMe
    val first_name: String? = null, // Untuk getMe
    val username: String? = null, // Untuk getMe

    val message_id: Long? = null,
    val photo: List<PhotoSize>? = null,
    val video: Video? = null
)

data class PhotoSize(
    val file_id: String
)

data class Video(
    val file_id: String
)