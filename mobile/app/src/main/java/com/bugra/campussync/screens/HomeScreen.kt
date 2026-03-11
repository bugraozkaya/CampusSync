package com.bugra.campussync.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugra.campussync.utils.TokenManager
import androidx.compose.ui.platform.LocalContext

@Composable
fun HomeScreen(onLogoutClick: () -> Unit) {
    val context = LocalContext.current
    val tokenManager = TokenManager(context)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Ana Sayfa - Takvim",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Yakında buraya ders programı gelecek!")

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                // Çıkış yap: Hafızadaki token'ı sil ve Auth ekranına dön
                tokenManager.clearToken()
                onLogoutClick()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text(text = "Çıkış Yap")
        }
    }
}