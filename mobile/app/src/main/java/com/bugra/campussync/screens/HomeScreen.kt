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

@Composable
fun HomeScreen(onLogoutClick: () -> Unit) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    val nameSurname = tokenManager.getNameSurname() ?: ""
    val department = tokenManager.getDepartment() ?: ""
    val position = tokenManager.getPosition() ?: ""
    val isProfileComplete = tokenManager.isProfileComplete()

    // Alert Dialog görünürlük kontrolü
    var showDialog by remember { mutableStateOf(!isProfileComplete) }

    var schedules by remember { mutableStateOf<List<ScheduleItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val token = tokenManager.getToken()
        if (token != null) {
            try {
                val response = RetrofitClient.apiService.getSchedules("Bearer $token")
                schedules = response
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error fetching schedules: ${e.message}")
                errorMessage = "Could not load schedule."
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (nameSurname.isNotEmpty()) "Welcome $nameSurname" else "Welcome",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (department.isNotEmpty()) {
                    Text(text = department, fontSize = 14.sp, color = Color.Gray)
                }
                if (position.isNotEmpty()) {
                    Text(text = "Position: $position", fontSize = 12.sp, color = Color.Gray)
                }
            }
            TextButton(onClick = onLogoutClick) {
                Text("Logout", color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // UYARI DÜZELTMESİ: State kontrolü eklendi
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Registration Incomplete") },
                text = { Text("Please go to Settings and complete your department and position selection first.") },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }

        Text(
            text = "Your Course Schedule",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(32.dp))
        } else if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = Color.Red)
        } else if (schedules.isEmpty()) {
            Text(text = "No courses assigned yet.", modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(schedules) { schedule ->
                    ScheduleCard(schedule)
                }
            }
        }
    }
}

@Composable
fun ScheduleCard(schedule: ScheduleItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                text = "Classroom: ${schedule.classroom_name ?: "Not Assigned"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
