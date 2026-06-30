package com.tes.telephotos.data.telegram

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface TelegramBotApi {

    @retrofit2.http.Multipart
    @POST
    suspend fun sendPhoto(
        @Url url: String,
        @retrofit2.http.Query("chat_id") chatId: String,
        @retrofit2.http.Part photo: okhttp3.MultipartBody.Part
    ): TelegramResponse

    @retrofit2.http.Multipart
    @POST
    suspend fun sendVideo(
        @Url url: String,
        @retrofit2.http.Query("chat_id") chatId: String,
        @retrofit2.http.Part video: okhttp3.MultipartBody.Part
    ): TelegramResponse

    @retrofit2.http.Multipart
    @POST
    suspend fun sendDocument(
        @Url url: String,
        @retrofit2.http.Query("chat_id") chatId: String,
        @retrofit2.http.Part document: okhttp3.MultipartBody.Part
    ): TelegramResponse

    @POST
    suspend fun pinChatMessage(
        @Url url: String,
        @retrofit2.http.Query("chat_id") chatId: String,
        @retrofit2.http.Query("message_id") messageId: Long,
        @retrofit2.http.Query("disable_notification") disableNotification: Boolean = true
    ): TelegramResponse

    @POST
    suspend fun unpinChatMessage(
        @Url url: String,
        @retrofit2.http.Query("chat_id") chatId: String,
        @retrofit2.http.Query("message_id") messageId: Long
    ): TelegramResponse

    @GET
    suspend fun getChat(
        @Url url: String,
        @retrofit2.http.Query("chat_id") chatId: String
    ): TelegramChatResponse

    @GET
    suspend fun getFile(
        @Url url: String,
        @retrofit2.http.Query("file_id") fileId: String
    ): TelegramFileResponse

    @GET
    suspend fun getMe(@Url url: String): TelegramResponse
}

data class TelegramResponse(
    val ok: Boolean,
    val result: MessageResult?,
    val description: String? = null
)

data class TelegramChatResponse(
    val ok: Boolean,
    val result: ChatResult?,
    val description: String? = null
)

data class TelegramFileResponse(
    val ok: Boolean,
    val result: FileResult?,
    val description: String? = null
)

data class ChatResult(
    val id: Long,
    val pinned_message: MessageResult? = null
)

data class FileResult(
    val file_id: String,
    val file_size: Long? = null,
    val file_path: String? = null
)

data class MessageResult(
    val message_id: Long,
    val id: Long? = null,
    val first_name: String? = null,
    val username: String? = null,

    val photo: List<PhotoSize>? = null,
    val video: Video? = null,
    val document: Document? = null
)

data class PhotoSize(
    val file_id: String
)

data class Video(
    val file_id: String
)

data class Document(
    val file_id: String,
    val file_name: String? = null
)