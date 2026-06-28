package com.tes.telephotos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tes.telephotos.domain.model.SyncState

@Entity(tableName = "media")
data class MediaEntity(
    @PrimaryKey val id: Long, // MediaStore ID
    val localUri: String?, // Null if 'Free up space' is used
    val mimeType: String,
    val dateAdded: Long,
    val width: Int,
    val height: Int,
    val telegramMessageId: Long? = null,
    val telegramFileId: String? = null,
    val syncState: SyncState = SyncState.PENDING
)