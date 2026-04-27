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
import androidx.compose.runtime.CompositionLocalProvider
import com.bugra.campussync.network.RetrofitClient
import com.bugra.campussync.network.SessionManager
import com.bugra.campussync.screens.*
import com.bugra.campussync.ui.theme.CampusSyncTheme
import com.bugra.campussync.ui.theme.ThemeMode
import com.bugra.campussync.utils.AppStrings
import com.bugra.campussync.utils.EnglishStrings
import com.bugra.campussync.utils.LocalAppStrings
import com.bugra.campussync.utils.ThemePreferences
import com.bugra.campussync.utils.TokenManager
import com.bugra.campussync.utils.TurkishStrings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val themePreferences = remember { ThemePreferences(context) }
            val themeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val languageCode by themePreferences.languageCode.collectAsState(initial = "tr")
            val strings: AppStrings = if (languageCode == "en") EnglishStrings else TurkishStrings

            CompositionLocalProvider(LocalAppStrings provides strings) {
                CampusSyncTheme(themeMode = themeMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(themePreferences = themePreferences)
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(themePreferences: ThemePreferences? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    // RetrofitClient Application.onCreate'de init edildi; token buradan okunur
    val tokenManager = remember { TokenManager(context) }

    // Token refresh başarısız olunca SessionManager login ekranına yönlendirir
    LaunchedEffect(Unit) {
        SessionManager.logoutEvent.collect {
            tokenManager.clearAll()
            RetrofitClient.authToken = null
            navController.navigate("auth") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val strings = LocalAppStrings.current

    // Rolü dinamik state olarak tut
    var userRole by remember { mutableStateOf(tokenManager.getRole() ?: "") }

    // Rota değiştiğinde rolü senkronize et
    LaunchedEffect(currentRoute) {
        val updatedRole = tokenManager.getRole() ?: ""
        if (updatedRole != userRole) {
            userRole = updatedRole
        }
    }

    // Başlangıç rotasını belirle: token yok → onboarding, mustChange → change_password, diğer → home
    val startDest = remember {
        when {
            tokenManager.getToken() == null -> "onboarding"
            tokenManager.getMustChangePassword() -> "change_password"
            (tokenManager.getRole() ?: "").uppercase() == "STUDENT" -> "student_home"
            (tokenManager.getRole() ?: "").uppercase().contains("SUPER") -> "superadmin"
            else -> "home"
        }
    }

    // Rol bazlı bottom nav öğeleri
    val bottomNavItems = remember(userRole) {
        val role = userRole.uppercase()
        when {
            role.contains("SUPER") ->
                listOf("superadmin", "users", "settings")
            role.contains("ADMIN") || role.contains("STAFF") || role.contains("IT") ->
                listOf("home", "calendar", "classrooms", "data", "announcements", "chat_inbox", "settings")
            role == "LECTURER" ->
                listOf("home", "calendar", "availability", "announcements", "attendance", "chat_inbox", "materials", "grades", "settings")
            role == "STUDENT" ->
                listOf("student_home", "announcements", "attendance", "chat_inbox", "materials", "grades", "settings")
            else ->
                listOf("home", "calendar", "announcements", "attendance", "chat_inbox", "settings")
        }
    }

    // Bottom bar'ın gösterileceği rotalar
    val bottomBarRoutes = setOf(
        "home", "calendar", "classrooms", "data", "settings",
        "availability", "users", "superadmin", "student_home", "announcements", "attendance",
        "chat_inbox", "materials", "grades"
    )

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                when (screen) {
                                    "home"       -> Icon(Icons.Default.Home, contentDescription = "Ana Sayfa")
                                    "calendar"   -> Icon(Icons.Default.DateRange, contentDescription = "Takvim")
                                    "classrooms" -> Icon(Icons.Default.MeetingRoom, contentDescription = "Sınıflar")
                                    "data"       -> Icon(Icons.Default.Group, contentDescription = "Hocalar")
                                    "settings"   -> Icon(Icons.Default.Settings, contentDescription = "Ayarlar")
                                    "availability" -> Icon(Icons.Default.Schedule, contentDescription = "Müsaitlik")
                                    "users"      -> Icon(Icons.Default.People, contentDescription = "Kullanıcılar")
                                    "superadmin" -> Icon(Icons.Default.SupervisorAccount, contentDescription = "Yönetim")
                                    "student_home" -> Icon(Icons.Default.School, contentDescription = "Ana Sayfa")
                                    "announcements" -> Icon(Icons.Default.Notifications, contentDescription = "Duyurular")
                                    "attendance" -> Icon(Icons.Default.HowToReg, contentDescription = "Yoklama")
                                    "chat_inbox" -> Icon(Icons.Default.Chat, contentDescription = "Mesajlar")
                                    "materials"  -> Icon(Icons.Default.Folder, contentDescription = "Materyaller")
                                    "grades"     -> Icon(Icons.Default.Grade, contentDescription = "Notlar")
                                    else -> Icon(Icons.Default.Circle, contentDescription = screen)
                                }
                            },
                            label = {
                                Text(
                                    when (screen) {
                                        "home"          -> strings.navHome
                                        "calendar"      -> strings.navCalendar
                                        "classrooms"    -> strings.navClassrooms
                                        "data"          -> strings.navLecturers
                                        "settings"      -> strings.navSettings
                                        "availability"  -> strings.navAvailability
                                        "users"         -> strings.navUsers
                                        "superadmin"    -> strings.navManagement
                                        "student_home"  -> strings.navHome
                                        "announcements" -> strings.navAnnouncements
                                        "attendance"    -> strings.navAttendance
                                        "chat_inbox"    -> strings.navMessages
                                        "materials"     -> strings.navMaterials
                                        "grades"        -> strings.navGrades
                                        else -> screen.replaceFirstChar { it.uppercase() }
                                    }
                                )
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
                AuthScreen(onLoginSuccess = { mustChange ->
                    userRole = tokenManager.getRole() ?: ""
                    RetrofitClient.authToken = tokenManager.getToken()
                    if (mustChange) {
                        navController.navigate("change_password") {
                            popUpTo("auth") { inclusive = true }
                        }
                    } else if (userRole.uppercase() == "STUDENT") {
                        navController.navigate("student_home") {
                            popUpTo(0) { inclusive = true }
                        }
                    } else if (userRole.uppercase().contains("SUPER")) {
                        navController.navigate("superadmin") {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigate("home") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                })
            }

            composable("change_password") {
                ChangePasswordScreen(
                    onPasswordChanged = {
                        val dest = if ((tokenManager.getRole() ?: "").uppercase() == "STUDENT") "student_home" else "home"
                        navController.navigate(dest) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onLogout = {
                        tokenManager.clearAll()
                        RetrofitClient.authToken = null
                        userRole = ""
                        navController.navigate("auth") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable("home") {
                HomeScreen(
                    onLogoutClick = {
                        tokenManager.clearAll()
                        RetrofitClient.authToken = null
                        userRole = ""
                        navController.navigate("auth") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }

            composable("calendar") {
                CalendarScreen()
            }

            composable("classrooms") {
                ClassroomScreen()
            }

            composable("data") {
                DataScreen(
                    onNavigateToCalendar = {
                        navController.navigate("calendar")
                    }
                )
            }

            composable("availability") {
                AvailabilityScreen()
            }

            composable("users") {
                UserManagementScreen()
            }

            composable("superadmin") {
                SuperAdminScreen()
            }

            composable("student_home") {
                StudentHomeScreen()
            }

            composable("announcements") {
                AnnouncementsScreen()
            }

            composable("attendance") {
                AttendanceScreen()
            }

            composable("settings") {
                SettingsScreen(
                    onProfileSaved = {
                        userRole = tokenManager.getRole() ?: ""
                    },
                    onLogoutClick = {
                        tokenManager.clearAll()
                        RetrofitClient.authToken = null
                        userRole = ""
                        navController.navigate("auth") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    themePreferences = themePreferences
                )
            }

            composable("chat_inbox") {
                ChatInboxScreen(onOpenChat = { partnerId, partnerName ->
                    navController.navigate("chat/$partnerId/${java.net.URLEncoder.encode(partnerName, "UTF-8")}")
                })
            }

            composable("materials") { CourseMaterialsScreen() }
            composable("grades")    { GradeBookScreen() }

            composable(
                "chat/{partnerId}/{partnerName}",
                arguments = listOf(
                    androidx.navigation.navArgument("partnerId") { type = androidx.navigation.NavType.IntType },
                    androidx.navigation.navArgument("partnerName") { type = androidx.navigation.NavType.StringType }
                )
            ) { backStackEntry ->
                val partnerId   = backStackEntry.arguments?.getInt("partnerId") ?: 0
                val partnerName = backStackEntry.arguments?.getString("partnerName")?.let {
                    java.net.URLDecoder.decode(it, "UTF-8")
                } ?: ""
                ChatScreen(
                    partnerId   = partnerId,
                    partnerName = partnerName,
                    onBack      = { navController.popBackStack() }
                )
            }
        }
    }
}
