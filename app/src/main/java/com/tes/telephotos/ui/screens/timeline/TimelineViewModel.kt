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

    init {
        syncLocalMedia()
    }

    private fun syncLocalMedia() {
        viewModelScope.launch {
            repository.syncLocalMedia()
        }
    }

    fun freeUpSpace() {
        viewModelScope.launch {
            val deletedCount = freeUpSpaceUseCase()
            freeUpSpaceResult.value = "Berhasil mengosongkan $deletedCount item dari penyimpanan lokal."
        }
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
}