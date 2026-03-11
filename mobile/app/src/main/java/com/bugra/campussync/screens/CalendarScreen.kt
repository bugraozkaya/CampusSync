package com.bugra.campussync.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugra.campussync.network.ScheduleItem

@Composable
fun CalendarScreen(schedules: List<ScheduleItem>) {
    val days = listOf("MON", "TUE", "WED", "THU", "FRI")
    val slots = listOf("Morning", "Afternoon")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Haftalık Ders Takvimi",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text("Sabah: 09:00-12:00 | Öğle: 13:30-16:30", fontSize = 12.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(16.dp))

        // Gün Başlıkları (Pazartesi - Cuma)
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(80.dp)) // Sol taraftaki saat etiketi boşluğu
            days.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Takvim Gövdesi (Satırlar)
        slots.forEach { slotName ->
            Row(
                modifier = Modifier.fillMaxWidth().height(110.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol Zaman Etiketi (Morning/Afternoon)
                Box(
                    modifier = Modifier.width(80.dp).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = slotName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                // Günlük Hücreler
                days.forEach { day ->
                    // Bu gün ve bu slot için dersi buluyoruz
                    val course = schedules.find {
                        it.day.uppercase() == day && checkSlot(it.start_time, slotName)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(3.dp)
                            .border(1.dp, Color.LightGray.copy(alpha = 0.4f))
                            .background(
                                if (course != null) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (course != null) {
                            Text(
                                // HATA BURADAYDI: course_code yerine course_name kullanıldı
                                text = course.course_name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Saate göre Morning/Afternoon ayrımı yapar.
 * Sabah: 13:00'den önce başlayanlar.
 */
fun checkSlot(startTime: String, slot: String): Boolean {
    return try {
        val hour = startTime.split(":")[0].toInt()
        if (slot == "Morning") hour < 13 else hour >= 13
    } catch (e: Exception) {
        false
    }
}