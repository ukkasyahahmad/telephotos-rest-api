package com.tes.telephotos.ui.screens.timeline

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.tes.telephotos.data.local.MediaEntity
import com.tes.telephotos.domain.model.SyncState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel = hiltViewModel(),
    onMediaClick: (MediaEntity) -> Unit
) {
    val groupedMedia by viewModel.groupedMedia.collectAsState()
    val freeUpSpaceResult by viewModel.freeUpSpaceResult.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(freeUpSpaceResult) {
        freeUpSpaceResult?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearFreeUpSpaceResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photos") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { viewModel.freeUpSpace() }) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Free up space"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = paddingValues,
            modifier = Modifier.fillMaxSize()
        ) {
            groupedMedia.forEach { (date, mediaList) ->
                // Date Header
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth()
                    )
                }

                // Media Grid
                items(mediaList, key = { it.id }) { media ->
                    MediaGridItem(media = media, onClick = { onMediaClick(media) })
                }
            }
        }
    }
}

@Composable
fun MediaGridItem(media: MediaEntity, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .clickable { onClick() }
    ) {
        // Thumbnail Image
        if (media.localUri != null) {
            AsyncImage(
                model = media.localUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Jika local file sudah dihapus (Free up space)
            // Nantinya harus fetch thumbnail dari Telegram File ID.
            // Sementara kita tampilkan placeholder gray
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray)
            )
        }

        // Sync Status Indicator
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                .padding(4.dp)
        ) {
            when (media.syncState) {
                SyncState.PENDING -> {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Pending",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                SyncState.UPLOADING -> {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                }
                SyncState.SYNCED -> {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "Synced",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}