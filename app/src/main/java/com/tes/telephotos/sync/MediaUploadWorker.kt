package com.tes.telephotos.sync

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tes.telephotos.data.local.MediaDao
import com.tes.telephotos.data.telegram.TelegramBotWrapper
import com.tes.telephotos.domain.model.SyncState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.FileOutputStream

@HiltWorker
class MediaUploadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val mediaDao: MediaDao,
    private val telegramBotWrapper: TelegramBotWrapper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val pendingMedia = mediaDao.getMediaBySyncState(SyncState.PENDING)
        if (pendingMedia.isEmpty()) return Result.success()

        for (media in pendingMedia) {
            val localUriStr = media.localUri ?: continue
            val uri = Uri.parse(localUriStr)

            val tempFile = copyToCache(uri) ?: continue

            // Bot API limit is 50MB
            if (tempFile.length() > 50 * 1024 * 1024) {
                tempFile.delete()
                continue
            }

            mediaDao.updateMedia(media.copy(syncState = SyncState.UPLOADING))

            val isVideo = media.mimeType.startsWith("video")
            val response = telegramBotWrapper.uploadMedia(tempFile, isVideo)

            if (response != null && response.ok && response.result != null) {
                var fileId: String? = null
                if (isVideo) {
                    fileId = response.result.video?.file_id
                } else {
                    fileId = response.result.photo?.lastOrNull()?.file_id
                }

                mediaDao.updateMedia(
                    media.copy(
                        syncState = SyncState.SYNCED,
                        telegramMessageId = response.result.message_id,
                        telegramFileId = fileId
                    )
                )
            } else {
                mediaDao.updateMedia(media.copy(syncState = SyncState.PENDING))
            }

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