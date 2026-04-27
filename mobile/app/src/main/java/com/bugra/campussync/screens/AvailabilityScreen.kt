package com.bugra.campussync.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugra.campussync.utils.LocalAppStrings
import com.bugra.campussync.viewmodels.AvailabilityViewModel

val AVAILABILITY_DAY_CODES = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
val AVAILABILITY_HOURS = listOf("08:00", "09:00", "10:00", "11:00", "13:00", "14:00", "15:00", "16:00")

@Composable
fun AvailabilityScreen() {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val viewModel: AvailabilityViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val busySlots = state.busySlots
    val isLoading = state.isLoading
    val isSaving = state.isSaving

    val availabilityDays = listOf(
        strings.dayMonday, strings.dayTuesday, strings.dayWednesday,
        strings.dayThursday, strings.dayFriday
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            strings.availabilityTitle,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            strings.availabilitySubtitle,
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendItem(color = Color(0xFFF1F8E9), borderColor = Color(0xFF81C784), label = strings.availabilityAvailable)
            LegendItem(color = Color(0xFFFFEBEE), borderColor = Color(0xFFEF5350), label = strings.availabilityBusy)
        }

        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(strings.loading, fontSize = 13.sp, color = Color.Gray)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Saat başlıkları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(72.dp))
                    Row(modifier = Modifier.weight(1f)) {
                        AVAILABILITY_HOURS.forEach { hour ->
                            Text(
                                text = hour.take(2),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                availabilityDays.zip(AVAILABILITY_DAY_CODES).forEach { (displayDay, codeDay) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayDay,
                            modifier = Modifier.width(72.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            AVAILABILITY_HOURS.forEach { hour ->
                                val key = "$codeDay-$hour"
                                val isBusy = busySlots.contains(key)
                                HourCell(
                                    isBusy = isBusy,
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    onClick = { viewModel.toggleSlot(key) }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                viewModel.save(
                    onSuccess = {
                        Toast.makeText(context, strings.availabilitySaved, Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = !isSaving && !isLoading
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(strings.saving)
            } else {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.availabilitySaveBtn, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun HourCell(isBusy: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(6.dp),
        color = if (isBusy) Color(0xFFFFEBEE) else Color(0xFFF1F8E9),
        border = BorderStroke(1.dp, if (isBusy) Color(0xFFEF5350) else Color(0xFF81C784))
    ) {}
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
