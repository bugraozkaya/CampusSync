package com.bugra.campussync.screens

import android.graphics.Bitmap
import android.graphics.Color
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugra.campussync.network.AttendanceRecordItem
import com.bugra.campussync.network.AttendanceSessionItem
import com.bugra.campussync.network.RetrofitClient
import com.bugra.campussync.network.ScheduleItem
import com.bugra.campussync.utils.TokenManager
import com.bugra.campussync.viewmodels.AttendanceViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen() {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val role = (tokenManager.getRole() ?: "").uppercase()
    val isStudent = role == "STUDENT"

    val viewModel: AttendanceViewModel = viewModel()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yoklama", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                if (isStudent) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(18.dp)) },
                        text = { Text("QR Tara") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.History, null, modifier = Modifier.size(18.dp)) },
                        text = { Text("Geçmişim") })
                } else {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.QrCode, null, modifier = Modifier.size(18.dp)) },
                        text = { Text("QR Oluştur") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.History, null, modifier = Modifier.size(18.dp)) },
                        text = { Text("Oturumlar") })
                }
            }
            when {
                isStudent && selectedTab == 0 -> StudentCheckInTab(
                    onCheckIn = { token ->
                        viewModel.checkIn(
                            token = token,
                            onSuccess = { course -> Toast.makeText(context, "✓ Yoklama alındı: $course", Toast.LENGTH_SHORT).show() },
                            onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                        )
                    }
                )
                isStudent && selectedTab == 1 -> StudentAttendanceHistoryTab(viewModel)
                !isStudent && selectedTab == 0 -> LecturerQRTab(viewModel)
                else -> LecturerSessionsTab()
            }
        }
    }
}

@Composable
private fun StudentCheckInTab(onCheckIn: (String) -> Unit) {
    var token by remember { mutableStateOf("") }

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
        Text("QR Kodu Tarayın", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            "Hocanızın gösterdiği QR kodu telefonunuzla tarayın veya kodu manuel girin.",
            textAlign = TextAlign.Center,
            color = androidx.compose.ui.graphics.Color.Gray,
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
            Text("Yoklamaya Katıl", fontSize = 16.sp)
        }
    }
}

@Composable
private fun StudentAttendanceHistoryTab(viewModel: AttendanceViewModel) {
    val state by viewModel.state.collectAsState()
    val records = state.history
    val isLoading = state.isLoading

    LaunchedEffect(Unit) { viewModel.loadHistory() }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else if (records.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Henüz yoklama kaydınız yok.", color = androidx.compose.ui.graphics.Color.Gray)
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
                            Text("Oturum #${record.session}", fontWeight = FontWeight.SemiBold)
                            Text(record.checked_in_at.take(16).replace("T", " "),
                                fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.Gray)
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
                    Text("${activeSession!!.course_code} – Yoklama QR", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    Text("Katılım: ${sessionRecords.size} öğrenci", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    OutlinedButton(onClick = { viewModel.endSession() }) {
                        Text("Yoklamayı Bitir")
                    }
                }
            }
            // Live attendance list
            if (sessionRecords.isNotEmpty()) {
                Text("Yoklamaya Katılanlar", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
                Text("Ders programınız bulunamadı.", color = androidx.compose.ui.graphics.Color.Gray)
            } else {
                ExposedDropdownMenuBox(
                    expanded = scheduleExpanded,
                    onExpandedChange = { scheduleExpanded = !scheduleExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedScheduleLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ders Seç") },
                        placeholder = { Text("Ders seçin…") },
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
                        Text("Yoklama QR Oluştur", fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LecturerSessionsTab() {
    val context = LocalContext.current
    var sessions by remember { mutableStateOf<List<AttendanceSessionItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try { sessions = RetrofitClient.apiService.getMySessions() } catch (e: Exception) {
            Toast.makeText(context, "Yüklenemedi.", Toast.LENGTH_SHORT).show()
        } finally { isLoading = false }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else if (sessions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Henüz yoklama oturumu oluşturmadınız.", color = androidx.compose.ui.graphics.Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sessions, key = { it.id }) { session ->
                Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(session.course_code, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Surface(
                                color = if (session.is_expired) MaterialTheme.colorScheme.errorContainer
                                else MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    if (session.is_expired) "Süresi Doldu" else "Aktif",
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = if (session.is_expired) MaterialTheme.colorScheme.onErrorContainer
                                    else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Text(session.course_name, fontSize = 13.sp, color = androidx.compose.ui.graphics.Color.Gray)
                        Text("${session.session_date} · ${session.record_count} katılımcı",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
