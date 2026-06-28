package com.tes.telephotos.sync

import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.tes.telephotos.R
import com.tes.telephotos.data.local.MediaDao
import com.tes.telephotos.data.telegram.TelegramBotWrapper
import com.tes.telephotos.domain.model.SyncState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.FileOutputStream

@HiltWorker
class MediaUploadWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val mediaDao: MediaDao,
    private val telegramBotWrapper: TelegramBotWrapper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = androidx.core.app.NotificationCompat.Builder(
            appContext,
            "telephotos_backup_channel"
        ).apply {
            setContentTitle("TelePhotos Backup")
            setContentText("Sedang meng-upload foto/video ke Telegram...")
            setSmallIcon(R.mipmap.ic_launcher)
            setOngoing(true)
            priority = androidx.core.app.NotificationCompat.PRIORITY_LOW
        }.build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                1001,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(1001, notification)
        }
    }

    override suspend fun doWork(): Result {
        val pendingMedia = mediaDao.getMediaBySyncState(SyncState.PENDING)
        if (pendingMedia.isEmpty()) return Result.success()

        // Set ini menjadi foreground worker (notification bar)
        // agar sistem tidak mematikannya walau aplikasi di-close
        setForeground(getForegroundInfo())

        for (media in pendingMedia) {
            val localUriStr = media.localUri ?: continue

            // Cek apakah file fisiknya masih ada
            if (!uriExists(localUriStr)) continue

            val uri = Uri.parse(localUriStr)
            val tempFile = copyToCache(uri) ?: continue

            // Batasi ukuran (Bot API = 50MB)
            if (tempFile.length() > 50 * 1024 * 1024) {
                tempFile.delete()
                continue
            }

            mediaDao.updateMedia(media.copy(syncState = SyncState.UPLOADING))

            val isVideo = media.mimeType.startsWith("video")
            val response = telegramBotWrapper.uploadMedia(tempFile, isVideo)

            if (response != null && response.ok && response.result != null) {
                var fileId: String? = null
                val result = response.result
                if (isVideo) {
                    fileId = result.video?.file_id
                } else {
                    fileId = result.photo?.lastOrNull()?.file_id
                }

                mediaDao.updateMedia(
                    media.copy(
                        syncState = SyncState.SYNCED,
                        telegramMessageId = result.message_id,
                        telegramFileId = fileId
                    )
                )
            } else {
                // Jika gagal, kembalikan ke PENDING agar bisa diulang nanti
                mediaDao.updateMedia(media.copy(syncState = SyncState.PENDING))
            }

            tempFile.delete()
        }

        return Result.success()
    }

    private fun uriExists(uriStr: String): Boolean {
        return try {
            val uri = Uri.parse(uriStr)
            appContext.contentResolver.openInputStream(uri) != null
        } catch (e: Exception) {
            false
        }
    }

    private fun copyToCache(uri: Uri): File? {
        return try {
            val inputStream = appContext.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(appContext.cacheDir, "temp_upload_${System.currentTimeMillis()}")
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