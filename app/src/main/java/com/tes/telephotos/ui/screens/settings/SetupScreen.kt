package com.tes.telephotos.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SetupScreen(
    viewModel: SetupViewModel = hiltViewModel(),
    onSetupCompleted: () -> Unit
) {
    var botToken by remember { mutableStateOf("") }
    var chatId by remember { mutableStateOf("") }
    val isSetupCompleted by viewModel.isSetupCompleted.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val isChecking by viewModel.isChecking.collectAsState()

    LaunchedEffect(isSetupCompleted) {
        if (isSetupCompleted) {
            onSetupCompleted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to TelePhotos",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "You need a Telegram Bot Token and your Chat ID. Maximum upload size is 50MB per file.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        OutlinedTextField(
            value = botToken,
            onValueChange = { botToken = it },
            label = { Text("Bot Token (e.g. 123:ABC...)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = chatId,
            onValueChange = { chatId = it },
            label = { Text("Your Chat ID") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isChecking) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (botToken.isNotBlank() && chatId.isNotBlank()) {
                        viewModel.checkAndSaveCredentials(botToken.trim(), chatId.trim())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = botToken.isNotBlank() && chatId.isNotBlank()
            ) {
                Text("Check Connection & Save")
            }
        }

        if (connectionStatus != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = connectionStatus!!,
                color = if (connectionStatus!!.contains("failed", ignoreCase = true) || connectionStatus!!.contains("Invalid", ignoreCase = true))
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}