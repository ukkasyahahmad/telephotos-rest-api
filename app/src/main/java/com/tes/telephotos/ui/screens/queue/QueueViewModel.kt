package com.tes.telephotos.ui.screens.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.tes.telephotos.data.local.MediaDao
import com.tes.telephotos.data.local.MediaEntity
import com.tes.telephotos.domain.model.SyncState
import com.tes.telephotos.sync.MediaUploadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val mediaDao: MediaDao,
    private val workManager: WorkManager
) : ViewModel() {

    val pendingMedia: StateFlow<List<MediaEntity>> = mediaDao.getAllMedia()
        .map { mediaList ->
            // Ambil hanya yang PENDING atau UPLOADING (belum synced)
            mediaList.filter { it.syncState == SyncState.PENDING || it.syncState == SyncState.UPLOADING }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val syncedMedia: StateFlow<List<MediaEntity>> = mediaDao.getAllMedia()
        .map { mediaList ->
            mediaList.filter { it.syncState == SyncState.SYNCED }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun startBackup() {
        // Atur Constraints agar hanya berjalan ketika terhubung internet
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Memicu worker secara manual dengan mode UNIQUE
        // KEEP: Jika sedang jalan, jangan di-replace. Jika gagal/batal, abaikan.
        val request = OneTimeWorkRequestBuilder<MediaUploadWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            "MediaUploadWorker_Unique",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}