package com.tes.telephotos.ui.screens.timeline

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.tes.telephotos.data.local.MediaEntity
import com.tes.telephotos.domain.model.SyncState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel = hiltViewModel(),
    isFavoritesOnly: Boolean = false,
    onMediaClick: (MediaEntity) -> Unit = {}
) {
    val allGroupedMedia by viewModel.groupedMedia.collectAsState()
    val favoriteMediaIds by viewModel.favoriteMediaIds.collectAsState()
    val freeUpSpaceResult by viewModel.freeUpSpaceResult.collectAsState()
    val uploadStatus by viewModel.mediaUploadStatus.collectAsState()
    val favoriteActionResult by viewModel.favoriteActionResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Filter media for Favorites tab
    val groupedMedia = remember(allGroupedMedia, isFavoritesOnly, favoriteMediaIds) {
        if (!isFavoritesOnly) {
            allGroupedMedia
        } else {
            allGroupedMedia.mapValues { (_, mediaList) ->
                mediaList.filter { it.id in favoriteMediaIds }
            }.filterValues { it.isNotEmpty() }
        }
    }



    var showFreeUpSpaceDialog by remember { mutableStateOf(false) }
    
    // Pinch to zoom state
    var gridCellsCount by remember { mutableFloatStateOf(3f) }
    
    // Selection state
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedMediaIds = remember { mutableStateListOf<Long>() }

    // Detail View State
    var selectedDetailMedia by remember { mutableStateOf<MediaEntity?>(null) }
    
    // Tabs state (0 = Photos, 1 = Videos)
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Photos", "Videos")

    // Filter media for Favorites tab and Selected Tab (Photos/Videos)
    val filteredGroupedMedia = remember(groupedMedia, selectedTab) {
        groupedMedia.mapValues { (_, mediaList) ->
            if (selectedTab == 0) {
                mediaList.filter { !it.mimeType.startsWith("video") }
            } else {
                mediaList.filter { it.mimeType.startsWith("video") }
            }
        }.filterValues { it.isNotEmpty() }
    }

    LaunchedEffect(favoriteActionResult) {
        favoriteActionResult?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearFavoriteActionResult()
        }
    }

    LaunchedEffect(freeUpSpaceResult) {
        freeUpSpaceResult?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearFreeUpSpaceResult()
        }
    }

    LaunchedEffect(uploadStatus) {
        uploadStatus?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMediaUploadStatus()
        }
    }

    if (showFreeUpSpaceDialog) {
        AlertDialog(
            onDismissRequest = { showFreeUpSpaceDialog = false },
            title = { Text("Kosongkan Ruang") },
            text = { Text("Apakah Anda yakin ingin menghapus file lokal untuk foto yang sudah ter-backup di Telegram? Foto tetap aman di Telegram.") },
            confirmButton = {
                TextButton(onClick = {
                    showFreeUpSpaceDialog = false
                    viewModel.freeUpSpace()
                }) {
                    Text("Ya, Hapus Semua")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFreeUpSpaceDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Detail Modal Overlay
    if (selectedDetailMedia != null) {
        Dialog(
            onDismissRequest = { selectedDetailMedia = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (selectedDetailMedia?.localUri != null) {
                        if (selectedDetailMedia?.mimeType?.startsWith("video") == true) {
                            // ExoPlayer Video View
                            val exoPlayer = remember { ExoPlayer.Builder(context).build() }
                            DisposableEffect(Unit) {
                                val mediaItem = MediaItem.fromUri(selectedDetailMedia!!.localUri!!)
                                exoPlayer.setMediaItem(mediaItem)
                                exoPlayer.prepare()
                                exoPlayer.playWhenReady = true
                                onDispose {
                                    exoPlayer.release()
                                }
                            }
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        player = exoPlayer
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Pinch to zoom inside Detail for Images
                            var scale by remember { mutableFloatStateOf(1f) }
                            var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                            
                            AsyncImage(
                                model = selectedDetailMedia?.localUri,
                                contentDescription = "Detail",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            scale = (scale * zoom).coerceIn(1f, 5f)
                                            offset += pan
                                        }
                                    }
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    )
                            )
                        }
                    } else {
                        // Cloud only image placeholder or download trigger
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Foto hanya tersedia di Telegram. Klik tombol Download untuk melihat.", color = Color.White)
                        }
                    }
                    
                    // Top Bar for Detail
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedDetailMedia = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        // EXIF / Meta Info Basic
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Size: ${selectedDetailMedia?.width}x${selectedDetailMedia?.height}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                            val stateStr = when(selectedDetailMedia?.syncState) {
                                SyncState.SYNCED -> "Cloud Synced"
                                SyncState.PENDING -> "Local Only"
                                else -> "Uploading..."
                            }
                            Text("Status: $stateStr", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isSelectionMode) {
                        Text("${selectedMediaIds.size} terpilih")
                    } else {
                        Text("Photos") 
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { 
                            selectedMediaIds.clear()
                            isSelectionMode = false 
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Unselect All")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { 
                            viewModel.toggleFavoriteMultiple(selectedMediaIds.toList())
                            selectedMediaIds.clear()
                            isSelectionMode = false
                        }) {
                            Icon(Icons.Default.Star, contentDescription = "Favorit")
                        }
                        IconButton(onClick = { 
                            viewModel.freeUpSpaceMultiple(selectedMediaIds.toList())
                            selectedMediaIds.clear()
                            isSelectionMode = false
                        }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Hapus Lokal")
                        }
                        IconButton(onClick = { 
                            coroutineScope.launch {
                                val urisStr = viewModel.getLocalUrisForShare(selectedMediaIds.toList())
                                if (urisStr.isEmpty()) {
                                    Toast.makeText(context, "Tidak ada file lokal untuk dibagikan. Download terlebih dahulu.", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                val uris = ArrayList(urisStr.map { Uri.parse(it) })
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND_MULTIPLE
                                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                    type = "*/*"
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Bagikan Foto"))
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    } else {
                        IconButton(onClick = { showFreeUpSpaceDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Free up space"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    val newCount = gridCellsCount / zoom
                    gridCellsCount = newCount.coerceIn(2f, 6f)
                }
            }
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                if (isLoading && filteredGroupedMedia.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (filteredGroupedMedia.isEmpty()) {
                    Text(
                        text = "Tidak ada media.",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.Gray
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridCellsCount.toInt()),
                        modifier = Modifier.fillMaxSize()
                    ) {
                filteredGroupedMedia.forEach { (date, mediaList) ->
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
                        val isSelected = selectedMediaIds.contains(media.id)
                        val isFavorite = favoriteMediaIds.contains(media.id)
                        MediaGridItem(
                            media = media,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            isFavorite = isFavorite,
                            onClick = {
                                if (isSelectionMode) {
                                    if (isSelected) {
                                        selectedMediaIds.remove(media.id)
                                        if (selectedMediaIds.isEmpty()) isSelectionMode = false
                                    } else {
                                        selectedMediaIds.add(media.id)
                                    }
                                } else {
                                    // Open Detail Overlay
                                    selectedDetailMedia = media
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                    selectedMediaIds.add(media.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGridItem(
    media: MediaEntity, 
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isFavorite: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray)
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        }

        // Selection Checkbox
        if (isSelectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                contentDescription = "Select",
                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
            )
        }
        
        // Favorite Indicator
        if (isFavorite) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Favorite",
                tint = Color.Yellow,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
            )
        }
        
        // Video Play Icon
        if (media.mimeType.startsWith("video")) {
            Icon(
                imageVector = Icons.Default.PlayCircleOutline,
                contentDescription = "Video",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
            )
        }

        // Sync Status Indicator (Badge)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                .padding(4.dp)
        ) {
            when {
                // Ada di HP & Telegram
                media.localUri != null && media.syncState == SyncState.SYNCED -> {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Tersimpan di HP & Telegram",
                        tint = Color.Green,
                        modifier = Modifier.size(16.dp)
                    )
                }
                // Hanya di Telegram
                media.localUri == null && media.syncState == SyncState.SYNCED -> {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "Hanya di Telegram",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                // Hanya di HP (Pending/Uploading)
                else -> {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = "Hanya di HP",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}