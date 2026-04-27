package com.bugra.campussync.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
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
import com.bugra.campussync.network.RetrofitClient
import com.bugra.campussync.network.ScheduleItem
import com.bugra.campussync.utils.LocalAppStrings
import com.bugra.campussync.utils.TokenManager
import com.bugra.campussync.viewmodels.HomeViewModel

@Composable
fun HomeScreen(onLogoutClick: () -> Unit, onNavigateToSettings: () -> Unit) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val tokenManager = remember { TokenManager(context) }
    val userRole = tokenManager.getRole() ?: ""
    val isAdmin = userRole.uppercase().let { it.contains("ADMIN") || it.contains("SUPER") || it == "STAFF" || it == "IT" }

    val firstName = tokenManager.getFirstName()
    val lastName = tokenManager.getLastName()
    val apiTitle = tokenManager.getTitle()
    val nameSurname = tokenManager.getNameSurname() ?: ""
    val department = tokenManager.getDepartment() ?: ""
    val position = tokenManager.getPosition() ?: ""
    val isProfileComplete = tokenManager.isProfileComplete()

    val displayName = when {
        firstName != null && lastName != null ->
            listOfNotNull(apiTitle?.ifBlank { null }, firstName, lastName).joinToString(" ")
        nameSurname.isNotEmpty() -> nameSurname
        else -> tokenManager.getUsername() ?: ""
    }

    var showProfileDialog by remember { mutableStateOf(!isAdmin && !isProfileComplete) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    val viewModel: HomeViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(isAdmin) { viewModel.load(isAdmin) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Üst bar: karşılama + çıkış
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${strings.homeWelcome}${if (displayName.isNotEmpty()) ", $displayName" else ""}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (department.isNotEmpty()) {
                    Text(text = department, fontSize = 13.sp, color = Color.Gray)
                }
                if (position.isNotEmpty()) {
                    Text(
                        text = position,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            IconButton(onClick = { showLogoutConfirm = true }) {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = strings.logout,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // Profil tamamlanmamış uyarı dialog'u
        if (showProfileDialog) {
            AlertDialog(
                onDismissRequest = { },
                icon = { Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error) },
                title = { Text(strings.homeProfileIncomplete) },
                text = { Text(strings.homeProfileDialog) },
                confirmButton = {
                    Button(onClick = {
                        showProfileDialog = false
                        onNavigateToSettings()
                    }) { Text(strings.homeGoToSettings) }
                },
                dismissButton = {
                    TextButton(onClick = { showProfileDialog = false }) { Text(strings.homeLater) }
                }
            )
        }

        // Logout confirm dialog
        if (showLogoutConfirm) {
            AlertDialog(
                onDismissRequest = { showLogoutConfirm = false },
                title = { Text(strings.logout) },
                text = { Text(strings.homeLogoutConfirm) },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutConfirm = false
                            RetrofitClient.authToken = null
                            onLogoutClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text(strings.logout) }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutConfirm = false }) { Text(strings.cancel) }
                }
            )
        }

        // İçerik
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(strings.loading, fontSize = 13.sp, color = Color.Gray)
                }
            }
        } else if (state.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(state.error!!, color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.load(isAdmin) }) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.retry)
                    }
                }
            }
        } else {
            if (state.unreadCount > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "${state.unreadCount} okunmamış duyuru var",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            val exportType = if (isAdmin) "institution" else "lecturer"
            PdfExportButton(
                exportType = exportType,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )
            Spacer(Modifier.height(8.dp))
            if (isAdmin) {
                AdminSummaryView(state.adminSummary)
            } else {
                LecturerHomeView(state.schedules)
            }
        }
    }
}

@Composable
fun ScheduleCard(schedule: ScheduleItem) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!schedule.course_code.isNullOrBlank()) {
                    Text(
                        text = schedule.course_code,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = schedule.course_name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.EventNote,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${dayLocalName(schedule.day, strings)} | ${schedule.start_time} – ${schedule.end_time}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Book,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${strings.homeClassroom}: ${schedule.classroom_name ?: strings.homeNotAssigned}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun dayLocalName(day: String, strings: com.bugra.campussync.utils.AppStrings): String =
    when (normalizeDayOfWeek(day)) {
        "MON" -> strings.dayMonday
        "TUE" -> strings.dayTuesday
        "WED" -> strings.dayWednesday
        "THU" -> strings.dayThursday
        "FRI" -> strings.dayFriday
        else  -> day
    }

@Composable
fun AdminSummaryView(summary: Map<String, Any>) {
    val strings = LocalAppStrings.current
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(strings.homeSummaryPanel, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            SummaryCard(
                strings.homeUnassignedLecturers,
                summary["unassigned_lecturers"] as? List<Map<String, Any>> ?: emptyList(),
                nameKey = "username"
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        item {
            SummaryCard(
                strings.homeUnassignedCourses,
                summary["unassigned_courses"] as? List<Map<String, Any>> ?: emptyList(),
                nameKey = "name"
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        item {
            SummaryCard(
                strings.homeAvailableClassrooms,
                summary["available_classrooms"] as? List<Map<String, Any>> ?: emptyList(),
                nameKey = "room_code"
            )
        }
    }
}

@Composable
fun SummaryCard(title: String, items: List<Map<String, Any>>, nameKey: String = "name") {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Badge(
                    containerColor = if (items.isEmpty()) MaterialTheme.colorScheme.tertiary
                                     else MaterialTheme.colorScheme.error
                ) {
                    Text("${items.size}", modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (items.isEmpty()) {
                Text(strings.homeAllAssigned, fontSize = 13.sp, color = Color.Gray)
            } else {
                items.take(5).forEach { item ->
                    val display = item[nameKey]?.toString()
                        ?: item["name"]?.toString()
                        ?: item["room_code"]?.toString()
                        ?: item["username"]?.toString()
                        ?: "—"
                    Text("• $display", fontSize = 13.sp, modifier = Modifier.padding(vertical = 1.dp))
                }
                if (items.size > 5) {
                    Text(strings.homeAndMore.format(items.size - 5), fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun LecturerHomeView(schedules: List<ScheduleItem>) {
    val strings = LocalAppStrings.current
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.homeWeeklySchedule,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (schedules.isNotEmpty()) {
                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                    Text("${schedules.size} ${strings.homeCourse}", modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (schedules.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.EventNote,
                        null,
                        modifier = Modifier.size(56.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = strings.homeNoAssignedCourses,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(schedules) { schedule ->
                    ScheduleCard(schedule)
                }
            }
        }
    }
}
