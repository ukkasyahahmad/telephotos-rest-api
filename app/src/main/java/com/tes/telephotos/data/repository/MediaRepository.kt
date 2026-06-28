package com.tes.telephotos.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.tes.telephotos.data.local.MediaDao
import com.tes.telephotos.data.local.MediaEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaDao: MediaDao
) {

    val allMedia: Flow<List<MediaEntity>> = mediaDao.getAllMedia()

    suspend fun syncLocalMedia() = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaEntity>()

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT
        )

        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

        val queryUri = MediaStore.Files.getContentUri("external")
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=" +
                "${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE} OR " +
                "${MediaStore.Files.FileColumns.MEDIA_TYPE}=" +
                "${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}"

        context.contentResolver.query(
            queryUri,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val mimeType = cursor.getString(mimeTypeColumn) ?: ""
                val dateAdded = cursor.getLong(dateAddedColumn)
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)

                val isVideo = mimeType.startsWith("video")
                val contentUri = if (isVideo) {
                    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                } else {
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                }

                mediaList.add(
                    MediaEntity(
                        id = id,
                        localUri = contentUri.toString(),
                        mimeType = mimeType,
                        dateAdded = dateAdded,
                        width = width,
                        height = height
                    )
                )
            }
        }

        // Insert into Room (duplicates ignored due to OnConflictStrategy.IGNORE)
        if (mediaList.isNotEmpty()) {
            mediaDao.insertMedia(mediaList)
        }
    }
}