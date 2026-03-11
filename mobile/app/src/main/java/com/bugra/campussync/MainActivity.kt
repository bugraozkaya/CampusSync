package com.bugra.campussync // Kendi paket adın

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.bugra.campussync.screens.AuthScreen
import com.bugra.campussync.screens.HomeScreen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bugra.campussync.screens.OnboardingScreen // Kendi paket adına göre düzelt
import com.bugra.campussync.ui.theme.CampusSyncTheme // Senin tema adın (genelde ProjeAdiTheme olur)

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

    // Uygulama açıldığında Token var mı diye kontrol ediyoruz (YENİ EKLENDİ)
    val context = androidx.compose.ui.platform.LocalContext.current
    val tokenManager = com.bugra.campussync.utils.TokenManager(context)
    val startScreen = if (tokenManager.getToken() != null) "home" else "onboarding"

    // startDestination'ı dinamik yaptık
    NavHost(navController = navController, startDestination = startScreen) {

        composable("onboarding") {
            OnboardingScreen(
                onNavigateToAuth = { navController.navigate("auth") }
            )
        }

        composable("auth") {
            AuthScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                onLogoutClick = {
                    navController.navigate("auth") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}