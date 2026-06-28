package com.tes.telephotos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tes.telephotos.domain.model.SyncState
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media ORDER BY dateAdded DESC")
    fun getAllMedia(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media WHERE syncState = :state")
    suspend fun getMediaBySyncState(state: SyncState): List<MediaEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMedia(media: List<MediaEntity>)

    @Update
    suspend fun updateMedia(media: MediaEntity)
}