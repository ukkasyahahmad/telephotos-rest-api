package com.tes.telephotos.data.telegram

import retrofit2.http.GET
import retrofit2.http.Url

interface TelegramBotApi {

    @retrofit2.http.Multipart
    @retrofit2.http.POST
    suspend fun sendPhoto(
        @Url url: String,
        @retrofit2.http.Query("chat_id") chatId: String,
        @retrofit2.http.Part photo: okhttp3.MultipartBody.Part
    ): TelegramResponse

    @retrofit2.http.Multipart
    @retrofit2.http.POST
    suspend fun sendVideo(
        @Url url: String,
        @retrofit2.http.Query("chat_id") chatId: String,
        @retrofit2.http.Part video: okhttp3.MultipartBody.Part
    ): TelegramResponse

    @GET
    suspend fun getMe(@Url url: String): TelegramResponse
}

data class TelegramResponse(
    val ok: Boolean,
    val result: MessageResult?,
    val description: String? = null
)

data class MessageResult(
    val id: Long? = null,
    val first_name: String? = null,
    val username: String? = null,

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