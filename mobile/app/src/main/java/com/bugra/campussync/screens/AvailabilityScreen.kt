package com.bugra.campussync.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugra.campussync.viewmodels.AvailabilityViewModel

val AVAILABILITY_DAYS = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma")
val AVAILABILITY_DAY_CODES = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
val AVAILABILITY_SLOTS = listOf("08:00-10:00", "10:00-12:00", "13:00-15:00", "15:00-17:00")

@Composable
fun AvailabilityScreen() {
    val context = LocalContext.current
    val viewModel: AvailabilityViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val busySlots = state.busySlots
    val isLoading = state.isLoading
    val isSaving = state.isSaving

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Müsaitlik Durumum", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(
            "Meşgul olduğunuz (ders alamayacağınız) zaman dilimlerini işaretleyin.",
            fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem(color = Color(0xFFF1F8E9), borderColor = Color(0xFF81C784), label = "Müsait")
            LegendItem(color = Color(0xFFFFEBEE), borderColor = Color(0xFFEF5350), label = "Meşgul")
        }

        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Yükleniyor...", fontSize = 13.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(AVAILABILITY_DAYS.zip(AVAILABILITY_DAY_CODES)) { (displayDay, codeDay) ->
                    AvailabilityDayRow(
                        displayDay = displayDay,
                        codeDay = codeDay,
                        busySlots = busySlots,
                        onSlotClick = { slot -> viewModel.toggleSlot(slot) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                viewModel.save(
                    onSuccess = { Toast.makeText(context, "Tercihleriniz kaydedildi.", Toast.LENGTH_SHORT).show() },
                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                )
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = !isSaving && !isLoading
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Kaydediliyor...")
            } else {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tercihlerimi Kaydet", fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun AvailabilityDayRow(
    displayDay: String,
    codeDay: String,
    busySlots: Set<String>,
    onSlotClick: (String) -> Unit
) {
    Column {
        Text(displayDay, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AVAILABILITY_SLOTS.forEach { slot ->
                val key = "$codeDay-$slot"
                val isBusy = busySlots.contains(key)
                SlotChip(label = slot, isBusy = isBusy, modifier = Modifier.weight(1f), onClick = { onSlotClick(key) })
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp), color = Color.LightGray, thickness = 0.5.dp)
    }
}

@Composable
fun SlotChip(label: String, isBusy: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(50.dp).clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (isBusy) Color(0xFFFFEBEE) else Color(0xFFF1F8E9),
        border = BorderStroke(width = 1.dp, color = if (isBusy) Color(0xFFEF5350) else Color(0xFF81C784))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label.replace("-", "\n"),
                fontSize = 9.sp, fontWeight = FontWeight.Medium,
                color = if (isBusy) Color(0xFFC62828) else Color(0xFF2E7D32),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
fun LegendItem(color: Color, borderColor: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(14.dp),
            shape = RoundedCornerShape(3.dp),
            color = color,
            border = BorderStroke(1.dp, borderColor)
        ) {}
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}
