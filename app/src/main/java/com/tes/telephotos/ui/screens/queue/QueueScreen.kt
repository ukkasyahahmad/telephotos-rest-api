package com.tes.telephotos.ui.screens.queue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.tes.telephotos.data.local.MediaEntity
import com.tes.telephotos.domain.model.SyncState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    viewModel: QueueViewModel = hiltViewModel()
) {
    val pendingMedia by viewModel.pendingMedia.collectAsState()
    val syncedMedia by viewModel.syncedMedia.collectAsState()
    val isUploading = pendingMedia.any { it.syncState == SyncState.UPLOADING }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup Queue") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Upload Stats Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isUploading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Uploading in progress...", style = MaterialTheme.typography.titleMedium)
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Idle (waiting)", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${syncedMedia.size} synced / ${pendingMedia.size + syncedMedia.size} total")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Upload Queue Section
            if (pendingMedia.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "In Queue (${pendingMedia.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                items(pendingMedia, key = { it.id }) { media ->
                    QueueItemRow(media = media)
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }

            // Synced Section
            if (syncedMedia.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Synced (${syncedMedia.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }

                items(syncedMedia, key = { it.id }) { media ->
                    QueueItemRow(media = media)
                }
            }

            if (pendingMedia.isEmpty() && syncedMedia.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No media to display yet.")
                    }
                }
            }
        }
    }
}

@Composable
fun QueueItemRow(media: MediaEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            if (media.localUri != null) {
                AsyncImage(
                    model = media.localUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info Media
            Column(modifier = Modifier.weight(1f)) {
                val fileName = media.localUri?.substringAfterLast('/') ?: "Remote File"
                Text(
                    text = fileName.takeLast(20),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Text(
                    text = if (media.mimeType.startsWith("video")) "Video" else "Photo",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Status Indicator
            when (media.syncState) {
                SyncState.PENDING -> {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Pending",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                SyncState.UPLOADING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
                SyncState.SYNCED -> {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Synced",
                        tint = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}