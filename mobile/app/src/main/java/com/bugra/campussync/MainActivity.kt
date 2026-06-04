package com.bugra.campussync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.bugra.campussync.navigation.Screen
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

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themePreferences: ThemePreferences

    @Inject
    lateinit var tokenManager: TokenManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission granted or denied
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val themeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val languageCode by themePreferences.languageCode.collectAsState(initial = "tr")
            val strings: AppStrings = if (languageCode == "en") EnglishStrings else TurkishStrings

            CompositionLocalProvider(LocalAppStrings provides strings) {
                CampusSyncTheme(themeMode = themeMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(
                            themePreferences = themePreferences,
                            tokenManager = tokenManager
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    themePreferences: ThemePreferences,
    tokenManager: TokenManager
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current

    // Token refresh başarısız olunca SessionManager login ekranına yönlendirir
    LaunchedEffect(Unit) {
        SessionManager.logoutEvent.collect {
            tokenManager.clearAll()
            navController.navigate(Screen.Auth) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val strings = LocalAppStrings.current

    // Rolü dinamik state olarak tut
    var userRole by remember { mutableStateOf(tokenManager.getRole() ?: "") }

    // Rota değiştiğinde rolü senkronize et
    LaunchedEffect(currentDestination) {
        val updatedRole = tokenManager.getRole() ?: ""
        if (updatedRole != userRole) {
            userRole = updatedRole
        }
    }

    // Başlangıç rotasını belirle
    val startDest: Any = remember {
        when {
            tokenManager.getToken() == null -> Screen.Onboarding
            tokenManager.getMustChangePassword() -> Screen.ChangePassword
            (tokenManager.getRole() ?: "").uppercase() == "STUDENT" -> Screen.StudentHome
            (tokenManager.getRole() ?: "").uppercase().contains("SUPER") -> Screen.SuperAdmin
            else -> Screen.Home
        }
    }

    // Rol bazlı bottom nav öğeleri (CONSOLIDATED)
    val bottomNavItems = remember(userRole) {
        val role = userRole.uppercase()
        when {
            role.contains("SUPER") ->
                listOf(Screen.SuperAdmin, Screen.Users, Screen.Settings)
            role.contains("ADMIN") || role.contains("STAFF") || role.contains("IT") ->
                listOf(Screen.Home, Screen.Calendar, Screen.Classrooms, Screen.Data, Screen.Announcements, Screen.ChatInbox, Screen.Settings)
            role == "LECTURER" ->
                listOf(Screen.Home, Screen.Availability, Screen.Announcements, Screen.CourseContent, Screen.ChatInbox, Screen.Settings)
            role == "STUDENT" ->
                listOf(Screen.StudentHome, Screen.Announcements, Screen.CourseContent, Screen.ChatInbox, Screen.Settings)
            else ->
                listOf(Screen.Home, Screen.Announcements, Screen.ChatInbox, Screen.Settings)
        }
    }

    // Bottom bar'ın gösterileceği rotalar
    val bottomBarRouteClasses = remember {
        setOf(
            Screen.Home::class, Screen.Calendar::class, Screen.Classrooms::class, Screen.Data::class, Screen.Settings::class,
            Screen.Availability::class, Screen.Users::class, Screen.SuperAdmin::class, Screen.StudentHome::class, Screen.Announcements::class, Screen.Attendance::class,
            Screen.ChatInbox::class, Screen.Materials::class, Screen.Grades::class, Screen.CourseDetail::class, Screen.CourseContent::class
        )
    }

    Scaffold(
        bottomBar = {
            val showBottomBar = currentDestination?.let { dest ->
                bottomBarRouteClasses.any { dest.hasRoute(it) }
            } ?: false

            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                when (screen) {
                                    is Screen.Home          -> Icon(Icons.Default.Home, contentDescription = null)
                                    is Screen.Calendar      -> Icon(Icons.Default.DateRange, contentDescription = null)
                                    is Screen.Classrooms    -> Icon(Icons.Default.MeetingRoom, contentDescription = null)
                                    is Screen.Data          -> Icon(Icons.Default.Group, contentDescription = null)
                                    is Screen.Settings      -> Icon(Icons.Default.Settings, contentDescription = null)
                                    is Screen.Availability  -> Icon(Icons.Default.Schedule, contentDescription = null)
                                    is Screen.Users         -> Icon(Icons.Default.People, contentDescription = null)
                                    is Screen.SuperAdmin    -> Icon(Icons.Default.SupervisorAccount, contentDescription = null)
                                    is Screen.StudentHome   -> Icon(Icons.Default.School, contentDescription = null)
                                    is Screen.Announcements -> Icon(Icons.Default.Notifications, contentDescription = null)
                                    is Screen.ChatInbox     -> Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                                    is Screen.CourseContent -> Icon(Icons.Default.LibraryBooks, contentDescription = null)
                                    is Screen.Materials     -> Icon(Icons.Default.Folder, contentDescription = null)
                                    is Screen.Grades        -> Icon(Icons.Default.Grade, contentDescription = null)
                                    is Screen.Attendance    -> Icon(Icons.Default.HowToReg, contentDescription = null)
                                    else -> Icon(Icons.Default.Circle, contentDescription = null)
                                }
                            },
                            label = {
                                Text(
                                    when (screen) {
                                        is Screen.Home          -> strings.navHome
                                        is Screen.Calendar      -> strings.navCalendar
                                        is Screen.Classrooms    -> strings.navClassrooms
                                        is Screen.Data          -> strings.navLecturers
                                        is Screen.Settings      -> strings.navSettings
                                        is Screen.Availability  -> strings.navAvailability
                                        is Screen.Users         -> strings.navUsers
                                        is Screen.SuperAdmin    -> strings.navManagement
                                        is Screen.StudentHome   -> strings.navHome
                                        is Screen.Announcements -> strings.navAnnouncements
                                        is Screen.ChatInbox     -> strings.navMessages
                                        is Screen.CourseContent -> strings.navCourseContent
                                        is Screen.Materials     -> strings.navMaterials
                                        is Screen.Grades        -> strings.navGrades
                                        is Screen.Attendance    -> strings.navAttendance
                                        else -> ""
                                    }
                                )
                            },
                            selected = currentDestination?.hasRoute(screen::class) == true,
                            onClick = {
                                if (currentDestination?.hasRoute(screen::class) == false) {
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
            composable<Screen.Onboarding> {
                OnboardingScreen(onNavigateToAuth = {
                    navController.navigate(Screen.Auth) {
                        popUpTo(Screen.Onboarding) { inclusive = true }
                    }
                })
            }

            composable<Screen.Auth> {
                AuthScreen(onLoginSuccess = { mustChange ->
                    userRole = tokenManager.getRole() ?: ""
                    if (mustChange) {
                        navController.navigate(Screen.ChangePassword) {
                            popUpTo(Screen.Auth) { inclusive = true }
                        }
                    } else if (userRole.uppercase() == "STUDENT") {
                        navController.navigate(Screen.StudentHome) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else if (userRole.uppercase().contains("SUPER")) {
                        navController.navigate(Screen.SuperAdmin) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Home) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                })
            }

            composable<Screen.ChangePassword> {
                ChangePasswordScreen(
                    onPasswordChanged = {
                        val dest = if ((tokenManager.getRole() ?: "").uppercase() == "STUDENT") Screen.StudentHome else Screen.Home
                        navController.navigate(dest) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onLogout = {
                        tokenManager.clearAll()
                        userRole = ""
                        navController.navigate(Screen.Auth) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable<Screen.Home> {
                HomeScreen(
                    onLogoutClick = {
                        tokenManager.clearAll()
                        userRole = ""
                        navController.navigate(Screen.Auth) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings)
                    },
                    onNavigateToCourseDetail = { id, name, code ->
                        navController.navigate(Screen.CourseDetail(id, name, code))
                    }
                )
            }

            composable<Screen.Calendar> { CalendarScreen() }
            composable<Screen.Classrooms> { ClassroomScreen() }
            composable<Screen.Data> {
                DataScreen(
                    onNavigateToChat = { partnerId, partnerName ->
                        navController.navigate(Screen.Chat(partnerId, partnerName))
                    }
                )
            }
            composable<Screen.Availability> { AvailabilityScreen() }
            composable<Screen.Users> { UserManagementScreen() }
            composable<Screen.SuperAdmin> { SuperAdminScreen() }

            composable<Screen.StudentHome> {
                StudentHomeScreen(onNavigateToCourseDetail = { id, name, code ->
                    navController.navigate(Screen.CourseDetail(id, name, code))
                })
            }

            composable<Screen.Announcements> { AnnouncementsScreen() }
            composable<Screen.Attendance> { AttendanceScreen() }

            composable<Screen.CourseContent> {
                CourseContentScreen(onNavigateToCourseDetail = { id, name, code ->
                    navController.navigate(Screen.CourseDetail(id, name, code))
                })
            }

            composable<Screen.Settings> {
                SettingsScreen(
                    onProfileSaved = { userRole = tokenManager.getRole() ?: "" },
                    onLogoutClick = {
                        tokenManager.clearAll()
                        userRole = ""
                        navController.navigate(Screen.Auth) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable<Screen.CourseDetail> { backStackEntry ->
                val detail = backStackEntry.toRoute<Screen.CourseDetail>()
                CourseDetailScreen(
                    courseId = detail.courseId,
                    courseName = detail.courseName,
                    courseCode = detail.courseCode,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Screen.ChatInbox> {
                ChatInboxScreen(onOpenChat = { partnerId, partnerName ->
                    navController.navigate(Screen.Chat(partnerId, partnerName))
                })
            }

            composable<Screen.Materials> { CourseMaterialsScreen() }
            composable<Screen.Grades>    { GradeBookScreen() }

            composable<Screen.Chat> { backStackEntry ->
                val chat = backStackEntry.toRoute<Screen.Chat>()
                ChatScreen(
                    partnerId   = chat.partnerId,
                    partnerName = chat.partnerName,
                    onBack      = { navController.popBackStack() }
                )
            }
        }
    }
}
