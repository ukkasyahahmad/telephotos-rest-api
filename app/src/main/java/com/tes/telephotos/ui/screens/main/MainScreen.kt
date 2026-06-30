package com.tes.telephotos.ui.screens.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tes.telephotos.ui.screens.queue.QueueScreen
import com.tes.telephotos.ui.screens.settings.SettingsScreen
import com.tes.telephotos.ui.screens.settings.SetupScreen
import com.tes.telephotos.ui.screens.timeline.TimelineScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            if (navBackStackEntry?.destination?.route != "edit_setup") {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "timeline",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("timeline") {
                TimelineScreen(
                    onMediaClick = { media ->
                        // Todo: Navigate to Detail
                    }
                )
            }
            composable("favorites") {
                TimelineScreen(
                    isFavoritesOnly = true,
                    onMediaClick = { media ->
                        // Todo: Navigate to Detail
                    }
                )
            }
            composable("queue") {
                QueueScreen()
            }
            composable("settings") {
                SettingsScreen(
                    onEditCredentialsClick = {
                        navController.navigate("edit_setup")
                    }
                )
            }
            composable("edit_setup") {
                SetupScreen(
                    isEditMode = true,
                    onSetupCompleted = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("Photos", "timeline", Icons.Default.PhotoLibrary),
        BottomNavItem("Favorites", "favorites", Icons.Default.Star),
        BottomNavItem("Queue", "queue", Icons.Default.Backup),
        BottomNavItem("Settings", "settings", Icons.Default.Settings)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)