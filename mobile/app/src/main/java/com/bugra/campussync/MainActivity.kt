package com.bugra.campussync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bugra.campussync.screens.*
import com.bugra.campussync.ui.theme.CampusSyncTheme
import com.bugra.campussync.utils.TokenManager

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
    val tokenManager = remember { TokenManager(context) }
    
    // Rolü dinamik bir state yapalım
    var userRole by remember { mutableStateOf(tokenManager.getRole() ?: "") }

    // Rota değiştiğinde rolü tekrar kontrol et (Güvenlik için)
    LaunchedEffect(currentRoute) {
        val updatedRole = tokenManager.getRole() ?: ""
        if (updatedRole != userRole) {
            userRole = updatedRole
        }
    }

    val startDest = remember {
        if (tokenManager.getToken() != null) "home" else "onboarding"
    }

    val bottomNavItems = remember(userRole) {
        when (userRole.uppercase()) {
            "SUPERADMIN" -> listOf("home", "admin_panel", "settings")
            "ADMIN" -> listOf("home", "calendar", "data", "users", "settings")
            "LECTURER" -> listOf("home", "calendar", "availability", "settings")
            else -> listOf("home", "calendar", "settings")
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomNavItems) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                when(screen) {
                                    "home" -> Icon(Icons.Default.Home, contentDescription = "Home")
                                    "calendar" -> Icon(Icons.Default.DateRange, contentDescription = "Calendar")
                                    "data" -> Icon(Icons.Default.Add, contentDescription = "Data")
                                    "users" -> Icon(Icons.Default.People, contentDescription = "Users")
                                    "availability" -> Icon(Icons.Default.EditCalendar, contentDescription = "Availability")
                                    "admin_panel" -> Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin")
                                    "settings" -> Icon(Icons.Default.Settings, contentDescription = "Settings")
                                }
                            },
                            label = { 
                                val label = when(screen) {
                                    "data" -> "Excel"
                                    "users" -> "Personel"
                                    "admin_panel" -> "Yönetim"
                                    "availability" -> "Müsaitlik"
                                    else -> screen.replaceFirstChar { it.uppercase() }
                                }
                                Text(label) 
                            },
                            selected = currentRoute == screen,
                            onClick = {
                                if (currentRoute != screen) {
                                    navController.navigate(screen) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
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
            startDestination = startDest,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("onboarding") {
                OnboardingScreen(onNavigateToAuth = { 
                    navController.navigate("auth") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                })
            }

            composable("auth") {
                AuthScreen(onLoginSuccess = {
                    // Login başarılı olunca rolü hemen güncelle
                    userRole = tokenManager.getRole() ?: ""
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                })
            }

            composable("home") {
                HomeScreen(onLogoutClick = {
                    tokenManager.clearAll()
                    userRole = ""
                    navController.navigate("auth") {
                        popUpTo(0) { inclusive = true }
                    }
                })
            }

            composable("calendar") {
                CalendarScreen(schedules = emptyList()) 
            }

            composable("data") {
                DataScreen()
            }

            composable("users") {
                UserManagementScreen()
            }

            composable("admin_panel") {
                SuperAdminScreen()
            }

            composable("availability") {
                AvailabilityScreen()
            }

            composable("settings") {
                SettingsScreen()
            }
        }
    }
}
