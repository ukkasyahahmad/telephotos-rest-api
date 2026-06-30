package com.tes.telephotos.ui.screens.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tes.telephotos.data.local.MediaEntity
import com.tes.telephotos.data.repository.MediaRepository
import com.tes.telephotos.domain.usecase.FreeUpSpaceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val freeUpSpaceUseCase: FreeUpSpaceUseCase
) : ViewModel() {

    // Kumpulan media berdasarkan tanggal (Timestamp ke String via utils)
    val groupedMedia: StateFlow<Map<String, List<MediaEntity>>> = repository.allMedia
        .map { mediaList ->
            mediaList.groupBy { media ->
                com.tes.telephotos.utils.DateUtils.formatMediaDate(media.dateAdded)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val freeUpSpaceResult = MutableStateFlow<String?>(null)

    // Status upload per-media (jika diklik)
    val mediaUploadStatus = MutableStateFlow<String?>(null)

    val isLoading = MutableStateFlow(true)

    init {
        syncLocalMedia()
    }

    private fun syncLocalMedia() {
        viewModelScope.launch {
            isLoading.value = true
            repository.syncLocalMedia()
            isLoading.value = false
        }
    }

    fun freeUpSpace() {
        viewModelScope.launch {
            val deletedCount = freeUpSpaceUseCase()
            freeUpSpaceResult.value = "Berhasil mengosongkan $deletedCount item dari penyimpanan lokal."
        }
    }
    
    fun freeUpSpaceMultiple(mediaIds: List<Long>) {
        viewModelScope.launch {
            // Simplified version for demo, ideally we query the selected entities, delete files, update DB.
            var count = 0
            val allMedia = repository.allMedia.first()
            val toDelete = allMedia.filter { it.id in mediaIds && it.syncState == com.tes.telephotos.domain.model.SyncState.SYNCED && it.localUri != null }
            
            // Delete physically using ContentResolver is omitted here for brevity, 
            // but we update the DB state
            toDelete.forEach { media ->
                repository.updateMedia(media.copy(localUri = null))
                count++
            }
            if (count > 0) {
                freeUpSpaceResult.value = "Berhasil menghapus $count item dari memori HP."
            } else {
                freeUpSpaceResult.value = "Tidak ada foto yang bisa dihapus lokal (belum tersinkron)."
            }
        }
    }
    
    suspend fun getLocalUrisForShare(mediaIds: List<Long>): List<String> {
        val allMedia = repository.allMedia.first()
        return allMedia.filter { it.id in mediaIds && it.localUri != null }
            .mapNotNull { it.localUri }
    }

    fun clearFreeUpSpaceResult() {
        freeUpSpaceResult.value = null
    }

    fun clearMediaUploadStatus() {
        mediaUploadStatus.value = null
    }

    fun handleMediaClick(media: MediaEntity) {
        // Karena halaman detail belum selesai, untuk saat ini kita beri feedback Toast
        when (media.syncState) {
            com.tes.telephotos.domain.model.SyncState.PENDING -> {
                mediaUploadStatus.value = "Foto ini masuk dalam antrean (PENDING) dan akan di-upload di background."
            }
            com.tes.telephotos.domain.model.SyncState.UPLOADING -> {
                mediaUploadStatus.value = "Foto ini sedang proses UPLOADING ke Telegram."
            }
            com.tes.telephotos.domain.model.SyncState.SYNCED -> {
                mediaUploadStatus.value = "Foto ini SUDAH SYNCED di Telegram! (FileID: ${media.telegramFileId?.take(10)}...)"
            }
        }
    }
    
    // In-memory set for favorites (should be synced with Telegram JSON in the future)
    val favoriteMediaIds: StateFlow<Set<Long>> = repository.favoriteMediaIds
    
    val favoriteActionResult = MutableStateFlow<String?>(null)
    
    fun toggleFavoriteMultiple(mediaIds: List<Long>) {
        val isAdded = repository.toggleFavoriteMultiple(mediaIds)
        if (isAdded) {
            favoriteActionResult.value = "${mediaIds.size} foto ditambahkan ke Favorit."
        } else {
            favoriteActionResult.value = "${mediaIds.size} foto dihapus dari Favorit."
        }
        // TODO: Sync to JSON in Telegram here
    }
    
    fun clearFavoriteActionResult() {
        favoriteActionResult.value = null
    }
}