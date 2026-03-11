package com.bugra.campussync.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugra.campussync.network.RetrofitClient
import com.bugra.campussync.network.ScheduleItem
import com.bugra.campussync.utils.TokenManager
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(onLogoutClick: () -> Unit) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val coroutineScope = rememberCoroutineScope()

    // Ekranda tutacağımız veriler
    var schedules by remember { mutableStateOf<List<ScheduleItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    // Ekran İLK AÇILDIĞINDA bu blok otomatik çalışır (API'den verileri çeker)
    LaunchedEffect(Unit) {
        val token = tokenManager.getToken()
        if (token != null) {
            try {
                // Token'ın başına "Bearer " eklemek standart bir güvenlik protokolüdür
                val response = RetrofitClient.apiService.getSchedules("Bearer $token")
                schedules = response
            } catch (e: Exception) {
                Log.e("API_ERROR", "Dersler çekilemedi: ${e.message}")
                errorMessage = "Ders programı yüklenemedi. Sunucu bağlantısını kontrol et."
            } finally {
                isLoading = false
            }
        } else {
            errorMessage = "Oturum hatası, lütfen tekrar giriş yapın."
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Üst Başlık ve Çıkış Butonu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ders Programım",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(onClick = {
                tokenManager.clearToken()
                onLogoutClick()
            }) {
                Text("Çıkış Yap", color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Yükleniyor durumu
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(32.dp))
        }
        // Hata durumu
        else if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = Color.Red, modifier = Modifier.padding(16.dp))
        }
        // Veri boşsa
        else if (schedules.isEmpty()) {
            Text(text = "Henüz eklenmiş bir ders bulunmuyor.", modifier = Modifier.padding(16.dp))
        }
        // Veri başarıyla geldiyse (Kartları listele)
        else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(schedules) { schedule ->
                    ScheduleCard(schedule)
                }
            }
        }
    }
}

// Tek bir Ders Kartı Tasarımı
// Tek bir Ders Kartı Tasarımı
// Tek bir Ders Kartı Tasarımı
@Composable
fun ScheduleCard(schedule: ScheduleItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Artık ID'yi değil, Django'dan gelen gerçek ismi gösteriyoruz!
            Text(
                text = schedule.course_name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${schedule.day} | ${schedule.start_time} - ${schedule.end_time}",
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                // Sınıf boşsa "Belirtilmedi" yazdırıyoruz
                text = "Sınıf: ${schedule.classroom_name ?: "Belirtilmedi"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}