package com.bugra.campussync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bugra.campussync.network.RetrofitClient
import com.bugra.campussync.network.ScheduleItem // Model importu
import com.bugra.campussync.screens.*
import com.bugra.campussync.ui.theme.CampusSyncTheme
import com.bugra.campussync.utils.TokenManager
import kotlinx.coroutines.launch

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }

    // Hata buradaydı: scheduleList artık tanımlı ve state olarak tutuluyor
    var scheduleList by remember { mutableStateOf<List<ScheduleItem>>(emptyList()) }

    // Proje dokümanındaki 4 ana menü
    val items = listOf("home", "calendar", "data", "settings")

    Scaffold(
        bottomBar = {
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
            composable("onboarding") {
                OnboardingScreen(onNavigateToAuth = { navController.navigate("auth") })
            }

            composable("auth") {
                AuthScreen(onLoginSuccess = {
                    // Giriş başarılı olunca dersleri çekip ana ekrana gidelim
                    scope.launch {
                        val token = tokenManager.getToken()
                        if (token != null) {
                            try {
                                scheduleList = RetrofitClient.apiService.getSchedules("Bearer $token")
                            } catch (e: Exception) { /* Hata yönetimi */ }
                        }
                        navController.navigate("home") {
                            popUpTo("auth") { inclusive = true }
                        }
                    }
                })
            }

            composable("home") {
                HomeScreen(onLogoutClick = {
                    navController.navigate("auth") {
                        popUpTo("home") { inclusive = true }
                    }
                })
            }

            composable("calendar") {
                // Yer tutucu Box sildik, sadece asıl ekranı çağırdık
                CalendarScreen(schedules = scheduleList)
            }

            composable("data") {
                DataScreen()
            }

            composable("settings") {
                SettingsScreen()
            }
        }
    }
}