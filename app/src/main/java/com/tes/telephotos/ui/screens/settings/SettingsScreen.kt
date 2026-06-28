package com.tes.telephotos.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Token
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val token by viewModel.botToken.collectAsState()
    val chatId by viewModel.chatId.collectAsState()
    val totalCount by viewModel.totalMediaCount.collectAsState()
    val syncedCount by viewModel.syncedMediaCount.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Status") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Backup Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isUploading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Backing up photos...", style = MaterialTheme.typography.titleMedium)
                        } else if (syncedCount == totalCount && totalCount > 0) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF4CAF50))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Backup complete", style = MaterialTheme.typography.titleMedium)
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Waiting for backup", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Backed up $syncedCount of $totalCount items")

                    if (totalCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (totalCount == 0) 0f else syncedCount.toFloat() / totalCount.toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Credentials Card
            Text(
                "Telegram Credentials",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Token, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Bot Token", style = MaterialTheme.typography.labelMedium)
                            Text(if (token.length > 10) "${token.take(10)}..." else token, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Token, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Chat ID", style = MaterialTheme.typography.labelMedium)
                            Text(chatId, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}