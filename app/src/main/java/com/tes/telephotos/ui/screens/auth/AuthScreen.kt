package com.tes.telephotos.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.drinkless.tdlib.TdApi

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onAuthSuccess: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()

    var phoneNumber by remember { mutableStateOf("") }
    var authCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Login with Telegram",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        when (authState) {
            is TdApi.AuthorizationStateWaitPhoneNumber -> {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number (+1234...)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.sendPhoneNumber(phoneNumber) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Send Code")
                }
            }

            is TdApi.AuthorizationStateWaitCode -> {
                OutlinedTextField(
                    value = authCode,
                    onValueChange = { authCode = it },
                    label = { Text("Authentication Code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.sendAuthenticationCode(authCode) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Verify Code")
                }
            }

            is TdApi.AuthorizationStateReady -> {
                LaunchedEffect(Unit) {
                    onAuthSuccess()
                }
            }

            else -> {
                CircularProgressIndicator()
                Text(
                    text = "Connecting to Telegram...",
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}