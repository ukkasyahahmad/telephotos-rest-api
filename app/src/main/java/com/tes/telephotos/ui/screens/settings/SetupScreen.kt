package com.tes.telephotos.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(
    viewModel: SetupViewModel = hiltViewModel(),
    onSetupCompleted: () -> Unit
) {
    var apiId by remember { mutableStateOf("") }
    var apiHash by remember { mutableStateOf("") }
    val isSetupCompleted by viewModel.isSetupCompleted.collectAsState()

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

        Text(
            text = "Before we start, you need to provide your Telegram API ID and Hash. You can get these by creating an application on my.telegram.org.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = apiId,
            onValueChange = { apiId = it },
            label = { Text("API ID (e.g., 123456)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = apiHash,
            onValueChange = { apiHash = it },
            label = { Text("API Hash") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val id = apiId.toIntOrNull()
                if (id != null && apiHash.isNotBlank()) {
                    viewModel.saveCredentials(id, apiHash)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = apiId.isNotBlank() && apiHash.isNotBlank()
        ) {
            Text("Save & Continue")
        }
    }
}