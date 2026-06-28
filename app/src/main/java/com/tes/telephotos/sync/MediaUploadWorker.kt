package com.tes.telephotos.sync

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tes.telephotos.data.local.MediaDao
import com.tes.telephotos.data.telegram.TelegramClientWrapper
import com.tes.telephotos.domain.model.SyncState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.drinkless.tdlib.TdApi
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltWorker
class MediaUploadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val mediaDao: MediaDao,
    private val telegramClientWrapper: TelegramClientWrapper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val pendingMedia = mediaDao.getMediaBySyncState(SyncState.PENDING)
        if (pendingMedia.isEmpty()) return Result.success()

        // Ambil Saved Messages Chat ID
        val chatId = telegramClientWrapper.getSavedMessagesChatId() ?: return Result.retry()

        for (media in pendingMedia) {
            val localUriStr = media.localUri ?: continue
            val uri = Uri.parse(localUriStr)

            // File harus berupa fisik file untuk dikirim via TDLib
            // Karena ContentResolver URI tidak selalu bisa dibaca TDLib, kita copy dulu sementara ke cache
            val tempFile = copyToCache(uri) ?: continue

            mediaDao.updateMedia(media.copy(syncState = SyncState.UPLOADING))

            val inputFile = TdApi.InputFileLocal(tempFile.absolutePath)

            val content = if (media.mimeType.startsWith("video")) {
                TdApi.InputMessageVideo(inputFile, null, null, 0, media.width, media.height, null, 0, null)
            } else {
                TdApi.InputMessagePhoto(inputFile, null, null, media.width, media.height, null, 0)
            }

            val result = suspendCoroutine<TdApi.Object> { continuation ->
                telegramClientWrapper.client?.send(
                    TdApi.SendMessage(chatId, 0, 0, null, null, content)
                ) { obj ->
                    continuation.resume(obj)
                } ?: continuation.resume(TdApi.Error(500, "Client null"))
            }

            if (result is TdApi.Message) {
                // Ekstrak file_id (Bergantung pada Photo/Video)
                var fileId: String? = null
                when (val msgContent = result.content) {
                    is TdApi.MessagePhoto -> {
                        fileId = msgContent.photo.sizes.lastOrNull()?.photo?.remote?.id
                    }
                    is TdApi.MessageVideo -> {
                        fileId = msgContent.video.video.remote.id
                    }
                }

                mediaDao.updateMedia(
                    media.copy(
                        syncState = SyncState.SYNCED,
                        telegramMessageId = result.id,
                        telegramFileId = fileId
                    )
                )
            } else {
                mediaDao.updateMedia(media.copy(syncState = SyncState.PENDING))
            }

            // Hapus file sementara
            tempFile.delete()
        }

        return Result.success()
    }

    private fun copyToCache(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}")
            val outputStream = FileOutputStream(tempFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}