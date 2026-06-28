package com.tes.telephotos.domain.usecase

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.tes.telephotos.data.local.MediaDao
import com.tes.telephotos.domain.model.SyncState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FreeUpSpaceUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaDao: MediaDao
) {
    /**
     * @return Number of files deleted (bytes saved can be calculated similarly if we store sizes)
     */
    suspend operator fun invoke(): Int = withContext(Dispatchers.IO) {
        val syncedMedia = mediaDao.getMediaBySyncState(SyncState.SYNCED)
        var deletedCount = 0

        val contentResolver: ContentResolver = context.contentResolver

        for (media in syncedMedia) {
            // Abaikan jika sudah pernah dihapus lokalnya (localUri = null)
            if (media.localUri == null) continue

            try {
                val uri = if (media.mimeType.startsWith("video")) {
                    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, media.id)
                } else {
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, media.id)
                }

                val deletedRows = contentResolver.delete(uri, null, null)

                if (deletedRows > 0) {
                    // Update database: hapus localUri, tetapi pertahankan fileId Telegram
                    mediaDao.updateMedia(media.copy(localUri = null))
                    deletedCount++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext deletedCount
    }
}