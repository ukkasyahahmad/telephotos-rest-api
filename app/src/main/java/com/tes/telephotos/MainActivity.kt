package com.tes.telephotos

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.tes.telephotos.ui.screens.main.MainScreen
import com.tes.telephotos.ui.screens.settings.SetupScreen
import com.tes.telephotos.ui.screens.settings.SetupViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionWrapper {
                        AppNavigationWrapper()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionWrapper(content: @Composable () -> Unit) {
    val permissionsToRequest = mutableListOf<String>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
        permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val permissionState = rememberMultiplePermissionsState(permissions = permissionsToRequest)

    if (permissionState.allPermissionsGranted) {
        content()
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Aplikasi membutuhkan izin penyimpanan untuk membaca foto dan video.")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                Text("Berikan Izin")
            }
        }
    }
}

@Composable
fun AppNavigationWrapper(setupViewModel: SetupViewModel = hiltViewModel()) {
    val isSetupCompleted by setupViewModel.isSetupCompleted.collectAsState()

    if (isSetupCompleted) {
        MainScreen()
    } else {
        SetupScreen(
            isEditMode = false,
            onSetupCompleted = {
                // State akan ter-trigger otomatis ke MainScreen()
            }
        )
    }
}