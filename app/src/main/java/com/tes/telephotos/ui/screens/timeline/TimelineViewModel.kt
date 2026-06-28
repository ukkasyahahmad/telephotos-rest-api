package com.tes.telephotos.ui.screens.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tes.telephotos.data.local.MediaEntity
import com.tes.telephotos.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val repository: MediaRepository
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

    init {
        syncLocalMedia()
    }

    private fun syncLocalMedia() {
        viewModelScope.launch {
            repository.syncLocalMedia()
        }
    }
}