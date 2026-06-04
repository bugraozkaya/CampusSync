package com.bugra.campussync.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.bugra.campussync.network.AttendanceRecordItem
import com.bugra.campussync.network.AttendanceSessionItem
import com.bugra.campussync.network.ScheduleItem
import com.bugra.campussync.utils.LocalAppStrings
import com.bugra.campussync.utils.TokenManager
import com.bugra.campussync.viewmodels.AttendanceViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val tokenManager = remember { TokenManager(context) }
    val role = (tokenManager.getRole() ?: "").uppercase()
    val isStudent = role == "STUDENT"

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.attendanceTitle, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                if (isStudent) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(18.dp)) },
                        text = { Text(strings.attendanceScanQR) })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.History, null, modifier = Modifier.size(18.dp)) },
                        text = { Text(strings.attendanceHistory) })
                } else {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.QrCode, null, modifier = Modifier.size(18.dp)) },
                        text = { Text(strings.attendanceCreateQR) })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.History, null, modifier = Modifier.size(18.dp)) },
                        text = { Text(strings.attendanceSessions) })
                }
            }
            when {
                isStudent && selectedTab == 0 -> StudentCheckInTab(
                    onCheckIn = { token ->
                        viewModel.checkIn(
                            token = token,
                            onSuccess = { course -> Toast.makeText(context, "✓ ${strings.attendanceRecorded}: $course", Toast.LENGTH_SHORT).show() },
                            onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                        )
                    }
                )
                isStudent && selectedTab == 1 -> StudentAttendanceHistoryTab(viewModel)
                !isStudent && selectedTab == 0 -> LecturerQRTab(viewModel)
                else -> LecturerSessionsTab(viewModel)
            }
        }
    }
}

@Composable
private fun StudentCheckInTab(onCheckIn: (String) -> Unit) {
    var token by remember { mutableStateOf("") }
    val strings = LocalAppStrings.current

    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Icon(Icons.Default.QrCodeScanner, null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary)
        Text(strings.attendanceScanTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            strings.attendanceScanHint,
            textAlign = TextAlign.Center,
            color = Color.Gray,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = token,
            onValueChange = { token = it.trim() },
            label = { Text("Token (UUID)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx") }
        )
        Button(
            onClick = { if (token.isNotBlank()) onCheckIn(token) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = token.isNotBlank()
        ) {
            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(strings.attendanceJoin, fontSize = 16.sp)
        }
    }
}

@Composable
private fun StudentAttendanceHistoryTab(viewModel: AttendanceViewModel) {
    val strings = LocalAppStrings.current
    val state by viewModel.state.collectAsState()
    val records = state.history
    val isLoading = state.isLoading

    LaunchedEffect(Unit) { viewModel.loadHistory() }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else if (records.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(strings.attendanceNoHistory, color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(records, key = { it.id }) { record ->
                Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("${strings.attendanceSession} #${record.session}", fontWeight = FontWeight.SemiBold)
                            Text(record.checked_in_at.take(16).replace("T", " "),
                                fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LecturerQRTab(viewModel: AttendanceViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val schedules = state.schedules
    val activeSession = state.activeSession
    val qrBitmap = state.qrBitmap
    val secondsLeft = state.secondsLeft
    val isLoading = state.isLoading
    val isCreating = state.isCreating
    val sessionRecords = state.sessionRecords
    val strings = LocalAppStrings.current

    var scheduleExpanded by remember { mutableStateOf(false) }
    var selectedScheduleId by remember { mutableStateOf("") }
    var selectedScheduleLabel by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadSchedules() }

    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (activeSession != null && secondsLeft > 0) {
            // Show QR Code
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("${activeSession.course_code} – Yoklama QR", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    qrBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.size(240.dp)
                        )
                    }
                    Surface(
                        color = if (secondsLeft > 30) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            "$secondsLeft saniye",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    Text("${sessionRecords.size} ${strings.attendanceParticipants}", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    OutlinedButton(onClick = { viewModel.endSession() }) {
                        Text(strings.attendanceEndSession)
                    }
                }
            }
            // Live attendance list
            if (sessionRecords.isNotEmpty()) {
                Text(strings.attendanceParticipantList, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                sessionRecords.forEach { record ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("${record.student_first_name} ${record.student_last_name}".trim().ifBlank { record.student_username })
                        }
                    }
                }
            }
        } else {
            // Schedule picker
            if (isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (schedules.isEmpty()) {
                Text(strings.attendanceNoSchedule, color = Color.Gray)
            } else {
                ExposedDropdownMenuBox(
                    expanded = scheduleExpanded,
                    onExpandedChange = { scheduleExpanded = !scheduleExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedScheduleLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.attendanceSelectCourse) },
                        placeholder = { Text(strings.attendanceSelectCourse) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = scheduleExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = scheduleExpanded,
                        onDismissRequest = { scheduleExpanded = false }
                    ) {
                        schedules.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s.course_code ?: s.course_name} – ${s.day} ${s.start_time.take(5)}") },
                                onClick = {
                                    selectedScheduleId = s.id.toString()
                                    selectedScheduleLabel = "${s.course_code ?: ""} – ${s.day} ${s.start_time.take(5)}"
                                    scheduleExpanded = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (selectedScheduleId.isBlank()) return@Button
                        val cal = java.util.Calendar.getInstance()
                        val today = String.format(
                            "%04d-%02d-%02d",
                            cal.get(java.util.Calendar.YEAR),
                            cal.get(java.util.Calendar.MONTH) + 1,
                            cal.get(java.util.Calendar.DAY_OF_MONTH)
                        )
                        viewModel.createSession(
                            scheduleId = selectedScheduleId.toIntOrNull() ?: 0,
                            sessionDate = today,
                            onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = selectedScheduleId.isNotBlank() && !isCreating
                ) {
                    if (isCreating) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                    else {
                        Icon(Icons.Default.QrCode, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.attendanceCreateQRTitle, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LecturerSessionsTab(viewModel: AttendanceViewModel) {
    val strings = LocalAppStrings.current
    val state by viewModel.state.collectAsState()
    val sessions = state.sessions
    val isLoading = state.isLoading
    val sessionRecords = state.sessionRecords

    var showRecordsDialog by remember { mutableStateOf(false) }
    var selectedSessionTitle by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadMySessions() }

    if (isLoading && sessions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else if (sessions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(strings.attendanceNoSessions, color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sessions, key = { it.id }) { session ->
                Card(
                    Modifier.fillMaxWidth().clickable {
                        selectedSessionTitle = "${session.course_code} (${session.session_date})"
                        viewModel.fetchSessionRecords(session.id)
                        showRecordsDialog = true
                    }, 
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(session.course_code, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Surface(
                                color = if (session.is_expired) MaterialTheme.colorScheme.errorContainer
                                else MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    if (session.is_expired) strings.attendanceExpired else strings.attendanceActive,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = if (session.is_expired) MaterialTheme.colorScheme.onErrorContainer
                                    else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Text(session.course_name, fontSize = 13.sp, color = Color.Gray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${session.session_date} · ${session.record_count} ${strings.attendanceParticipants}",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Group, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    if (showRecordsDialog) {
        Dialog(
            onDismissRequest = { showRecordsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.8f),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedSessionTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { showRecordsDialog = false }) { Icon(Icons.Default.Close, null) }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    
                    if (isLoading) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    } else if (sessionRecords.isEmpty()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Katılım yok.", color = Color.Gray) }
                    } else {
                        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                            items(sessionRecords) { record ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("${record.student_first_name} ${record.student_last_name}".trim().ifBlank { record.student_username }, fontWeight = FontWeight.Medium)
                                        Text(record.checked_in_at.take(16).replace("T", " "), fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}
