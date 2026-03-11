package com.bugra.campussync // Kendi paket adın

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.bugra.campussync.screens.AuthScreen
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
    // Navigasyon kontrolcümüzü oluşturuyoruz
    val navController = rememberNavController()

    // Ekranların rotalarını ve sırasını belirliyoruz
    NavHost(navController = navController, startDestination = "onboarding") {

        // 1. Ekran: Karşılama (Onboarding)
        composable("onboarding") {
            OnboardingScreen(
                onNavigateToAuth = {
                    // Butona basılınca 'auth' rotasına git
                    navController.navigate("auth")
                }
            )
        }

        // 2. Ekran: Giriş ve Kayıt Seçimi
        composable("auth") {
            AuthScreen(
                onLoginSuccess = {
                    // İleride buraya ana sayfaya (Takvim vs) geçiş kodunu yazacağız
                    // navController.navigate("home")
                    println("Başarıyla giriş yapıldı!")
                }
            )
        }
    }
}