package com.bugra.campussync.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
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
import com.bugra.campussync.utils.TokenManager
import kotlinx.coroutines.launch

val DAYS = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma")
val HOURS = listOf("09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00")

@Composable
fun AvailabilityScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    val token = tokenManager.getToken() ?: ""

    var busySlots by remember { mutableStateOf(setOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Ekran açıldığında mevcut verileri getir
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val response = RetrofitClient.apiService.getUnavailability("Bearer $token")
            val loadedSlots = response.map { "${it["day"]}-${it["hour"]}" }.toSet()
            busySlots = loadedSlots
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Müsaitlik Durumun",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Ders almak istemediğin (meşgul) saatleri işaretle.",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(DAYS) { day ->
                    DayRow(day = day, busySlots = busySlots) { slot ->
                        busySlots = if (busySlots.contains(slot)) {
                            busySlots - slot
                        } else {
                            busySlots + slot
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                isSaving = true
                scope.launch {
                    try {
                        // Seçili slotları API'nin beklediği formatta hazırla
                        val dataToSend = busySlots.map { slot ->
                            val parts = slot.split("-")
                            mapOf("day" to parts[0], "hour" to parts[1])
                        }
                        
                        RetrofitClient.apiService.syncUnavailability("Bearer $token", dataToSend)
                        Toast.makeText(context, "Tercihleriniz kaydedildi.", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    } finally {
                        isSaving = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(top = 8.dp),
            enabled = !isSaving && !isLoading
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tercihlerimi Kaydet")
            }
        }
    }
}

@Composable
fun DayRow(day: String, busySlots: Set<String>, onSlotClick: (String) -> Unit) {
    Column {
        Text(
            text = day,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Saat dilimlerini Grid şeklinde gösterelim
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val rows = HOURS.chunked(3) // Satırda 3 saat dilimi
            rows.forEach { rowHours ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowHours.forEach { hour ->
                        HourChip(
                            hour = hour,
                            isBusy = busySlots.contains("$day-$hour"),
                            modifier = Modifier.weight(1f)
                        ) {
                            onSlotClick("$day-$hour")
                        }
                    }
                    // Eğer satırda 3'ten az eleman varsa boşluğu doldur
                    repeat(3 - rowHours.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
    }
}

@Composable
fun HourChip(hour: String, isBusy: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .height(45.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isBusy) Color(0xFFFFEBEE) else Color(0xFFF1F8E9),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isBusy) Color(0xFFEF5350) else Color(0xFF81C784)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = hour,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isBusy) Color(0xFFC62828) else Color(0xFF2E7D32)
            )
        }
    }
}
