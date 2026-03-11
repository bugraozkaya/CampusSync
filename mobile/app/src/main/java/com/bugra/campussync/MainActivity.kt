package com.bugra.campussync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bugra.campussync.screens.*
import com.bugra.campussync.ui.theme.CampusSyncTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CampusSyncTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Proje dokümanında belirtilen 4 ana menü [cite: 3]
    val items = listOf("home", "calendar", "data", "settings")

    Scaffold(
        bottomBar = {
            // Sadece ana uygulama ekranlarındaysak (Onboarding/Auth hariç) BottomBar gösterilir
            if (currentRoute in items) {
                NavigationBar {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                when(screen) {
                                    "home" -> Icon(Icons.Default.Home, contentDescription = "Home")
                                    "calendar" -> Icon(Icons.Default.DateRange, contentDescription = "Calendar")
                                    "data" -> Icon(Icons.Default.Add, contentDescription = "Data")
                                    "settings" -> Icon(Icons.Default.Settings, contentDescription = "Settings")
                                }
                            },
                            label = { Text(screen.replaceFirstChar { it.uppercase() }) },
                            selected = currentRoute == screen,
                            onClick = {
                                navController.navigate(screen) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "onboarding",
            modifier = Modifier.padding(innerPadding)
        ) {
            // 0. Giriş Akışı
            composable("onboarding") {
                OnboardingScreen(onNavigateToAuth = { navController.navigate("auth") })
            }

            composable("auth") {
                AuthScreen(onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                })
            }

            // 1. Navigation -> Home [cite: 4, 8]
            composable("home") {
                HomeScreen(onLogoutClick = {
                    navController.navigate("auth") {
                        popUpTo("home") { inclusive = true }
                    }
                })
            }

            // 2. Navigation -> Calendar [cite: 5, 10]
            composable("calendar") {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Takvim Ekranı (Ders Programı Detayları) [cite: 10, 36]")
                }
            }

            // 3. Navigation -> Data [cite: 6, 17]
            composable("data") {
                DataScreen()
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Veri Aktarma (Excel/TXT Import) [cite: 19]")
                }
            }

            // 4. Navigation -> Settings [cite: 7, 44]
            composable("settings") {
                SettingsScreen()
            }
        }
    }
}