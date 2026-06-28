package com.tes.telephotos.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    // Dengan Telegram Bot API, tidak ada login OTP.
    // AuthScreen langsung redirect jika setup sudah selesai (ditangani di MainActivity)
    LaunchedEffect(Unit) {
        onAuthSuccess()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}