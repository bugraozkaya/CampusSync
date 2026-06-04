package com.bugra.campussync.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.bugra.campussync.ui.components.MaterialCard
import com.bugra.campussync.utils.LocalAppStrings
import com.bugra.campussync.utils.TokenManager
import com.bugra.campussync.viewmodels.AttendanceViewModel
import com.bugra.campussync.viewmodels.CourseMaterialsViewModel
import com.bugra.campussync.viewmodels.NotesViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    courseId: Int,
    courseName: String,
    courseCode: String,
    onBack: () -> Unit,
    materialsViewModel: CourseMaterialsViewModel = hiltViewModel(),
    attendanceViewModel: AttendanceViewModel = hiltViewModel(),
    notesViewModel: NotesViewModel = hiltViewModel()
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val role = (tokenManager.getRole() ?: "").uppercase()
    val isLecturer = role in listOf("LECTURER", "ADMIN", "STAFF", "IT")
    val isStudent = role == "STUDENT"

    val isProfileComplete = tokenManager.isProfileComplete()
    
    // Profil eksikse doğrudan Yoklama tabına (veya bilgilendirmeye) yönlendirme mantığı
    var selectedTab by remember { mutableIntStateOf(if (!isProfileComplete) 2 else 0) }
    val tabs = listOf(strings.navMaterials, "Notlar", strings.navAttendance)

    var showUpload     by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(courseId) {
        materialsViewModel.load(courseId)
        notesViewModel.load(courseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(courseCode, fontSize = 12.sp, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.primary)
                        Text(courseName, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri",
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            if (isLecturer) when (selectedTab) {
                0 -> FloatingActionButton(onClick = { showUpload = true }, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.Upload, "Materyal Yükle", tint = Color.White)
                }
                1 -> FloatingActionButton(onClick = { showNoteDialog = true }, containerColor = MaterialTheme.colorScheme.secondary) {
                    Icon(Icons.Default.Add, "Not Ekle", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 13.sp) })
                }
            }

            when (selectedTab) {
                0 -> MaterialsTab(materialsViewModel, courseId)
                1 -> NotesTab(notesViewModel, isLecturer, courseId, showNoteDialog, onDismissNoteDialog = { showNoteDialog = false })
                2 -> AttendanceTab(attendanceViewModel, isLecturer, courseId)
            }
        }
    }

    if (showUpload) {
        UploadMaterialDialog(
            viewModel = materialsViewModel,
            courseId = courseId,
            onDismiss = { showUpload = false }
        )
    }
}

@Composable
fun MaterialsTab(viewModel: CourseMaterialsViewModel, courseId: Int) {
    val state by viewModel.state.collectAsState()
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.materials.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(56.dp), tint = Color.LightGray)
                Spacer(Modifier.height(8.dp))
                Text(strings.materialsNone, color = Color.Gray)
            }
        }
        else -> LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(state.materials) { mat ->
                MaterialCard(
                    material = mat,
                    canDelete = true,
                    onDownload = {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(mat.file_url))
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, strings.materialsCannotOpen, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDelete = { viewModel.delete(mat.id) {} }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadMaterialDialog(
    viewModel: CourseMaterialsViewModel,
    courseId: Int,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    var uploadTitle    by remember { mutableStateOf("") }
    var uploadDesc     by remember { mutableStateOf("") }
    var uploadType     by remember { mutableStateOf("LECTURE_NOTES") }
    var uploadFileUri  by remember { mutableStateOf<Uri?>(null) }
    var uploadFileName by remember { mutableStateOf("") }
    var typeExpanded   by remember { mutableStateOf(false) }
    var uploadError    by remember { mutableStateOf("") }

    val materialTypes = listOf(
        "LECTURE_NOTES" to strings.materialsTypeLectureNotes,
        "ASSIGNMENT"    to strings.materialsTypeAssignment,
        "EXAM"          to strings.materialsTypeExam,
        "RESOURCE"      to strings.materialsTypeResource,
        "OTHER"         to strings.materialsTypeOther
    )

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploadFileUri = uri
            context.contentResolver.query(uri, null, null, null, null)?.use {
                if (it.moveToFirst()) {
                    val col = it.getColumnIndex("_display_name")
                    if (col != -1) uploadFileName = it.getString(col)
                }
            }
            if (uploadFileName.isBlank()) uploadFileName = "file"
        }
    }

    Dialog(
        onDismissRequest = { if (!state.isUploading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).wrapContentHeight().imePadding(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.materialsUpload, style = MaterialTheme.typography.headlineSmall)
                    IconButton(onClick = { if (!state.isUploading) onDismiss() }) { Icon(Icons.Default.Close, null) }
                }
                OutlinedTextField(value = uploadTitle, onValueChange = { uploadTitle = it; uploadError = "" },
                    label = { Text(strings.materialsHeadline) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = uploadDesc, onValueChange = { uploadDesc = it },
                    label = { Text(strings.materialsDescription) }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
                    OutlinedTextField(
                        value = materialTypes.find { it.first == uploadType }?.second ?: uploadType,
                        onValueChange = {}, readOnly = true, label = { Text(strings.materialsType) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        materialTypes.forEach { (code, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { uploadType = code; typeExpanded = false })
                        }
                    }
                }
                OutlinedButton(onClick = { filePicker.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        if (uploadFileUri == null) Icons.Default.AttachFile else Icons.Default.CheckCircle,
                        null, modifier = Modifier.size(16.dp),
                        tint = if (uploadFileUri == null) LocalContentColor.current else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (uploadFileUri == null) strings.materialsFileSelect else uploadFileName, maxLines = 1)
                }
                if (uploadError.isNotEmpty()) {
                    Text(uploadError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onDismiss() }, enabled = !state.isUploading) { Text(strings.cancel) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            uploadError = ""
                            val uri = uploadFileUri
                            when {
                                uploadTitle.isBlank() -> { uploadError = strings.materialsTitleRequired; return@Button }
                                uri == null -> { uploadError = "Lütfen önce bir dosya seçin."; return@Button }
                            }
                            val mimeType = context.contentResolver.getType(uri!!) ?: "application/octet-stream"
                            val bytes = try { context.contentResolver.openInputStream(uri)?.readBytes() }
                                catch (e: Exception) { uploadError = "Dosya okunamadı."; return@Button }
                            if (bytes == null) { uploadError = "Dosya okunamadı."; return@Button }
                            val filePart = MultipartBody.Part.createFormData("file", uploadFileName, bytes.toRequestBody(mimeType.toMediaTypeOrNull()))
                            viewModel.upload(
                                filePart = filePart,
                                course = courseId.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                                title = uploadTitle.toRequestBody("text/plain".toMediaTypeOrNull()),
                                description = uploadDesc.toRequestBody("text/plain".toMediaTypeOrNull()),
                                materialType = uploadType.toRequestBody("text/plain".toMediaTypeOrNull()),
                                courseIdFilter = courseId,
                                onSuccess = {
                                    onDismiss()
                                    Toast.makeText(context, strings.materialsUploaded, Toast.LENGTH_SHORT).show()
                                },
                                onError = { err -> uploadError = err }
                            )
                        },
                        enabled = !state.isUploading
                    ) {
                        if (state.isUploading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Text(strings.upload)
                    }
                }
            }
        }
    }
}

@Composable
fun NotesTab(
    viewModel: NotesViewModel,
    isLecturer: Boolean,
    courseId: Int,
    showAddDialog: Boolean,
    onDismissNoteDialog: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var noteTitle    by remember { mutableStateOf("") }
    var noteContent  by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Int?>(null) }

    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.notes.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Notes, null, modifier = Modifier.size(56.dp), tint = Color.LightGray)
                Spacer(Modifier.height(8.dp))
                Text("Henüz not eklenmemiş.", color = Color.Gray)
            }
        }
        else -> LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(state.notes, key = { it.id }) { note ->
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(note.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            if (isLecturer) {
                                IconButton(onClick = { deleteTarget = note.id }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(note.content, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("${note.author_name} • ${note.created_at.take(10)}", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
    }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Notu Sil") },
            text = { Text("Bu not silinecek. Emin misiniz?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.delete(deleteTarget!!, courseId) {}; deleteTarget = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Sil") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("İptal") } }
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { if (!state.isSubmitting) { onDismissNoteDialog(); noteTitle = ""; noteContent = "" } },
            title = { Text("Not Ekle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = noteTitle, onValueChange = { noteTitle = it },
                        label = { Text("Başlık") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = noteContent, onValueChange = { noteContent = it },
                        label = { Text("İçerik") }, modifier = Modifier.fillMaxWidth(), maxLines = 6)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteTitle.isBlank()) { Toast.makeText(context, "Başlık boş olamaz.", Toast.LENGTH_SHORT).show(); return@Button }
                        if (noteContent.isBlank()) { Toast.makeText(context, "İçerik boş olamaz.", Toast.LENGTH_SHORT).show(); return@Button }
                        viewModel.create(courseId = courseId, title = noteTitle.trim(), content = noteContent.trim(),
                            onSuccess = { onDismissNoteDialog(); noteTitle = ""; noteContent = "" },
                            onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() })
                    },
                    enabled = !state.isSubmitting
                ) {
                    if (state.isSubmitting) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("Kaydet")
                }
            },
            dismissButton = { TextButton(onClick = { onDismissNoteDialog(); noteTitle = ""; noteContent = "" }) { Text("İptal") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceTab(viewModel: AttendanceViewModel, isLecturer: Boolean, courseId: Int) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    
    val activeSession = state.activeSession
    val qrBitmap = state.qrBitmap
    val secondsLeft = state.secondsLeft
    val sessionRecords = state.sessionRecords
    val isLoading = state.isLoading
    
    // Note: In ScheduleItem, courseId is 'courseId'. In AttendanceSession, it might be different.
    // Let's check state properties.
    val history = state.history.filter { it.session != 0 } // Filter placeholder
    val mySessions = state.sessions

    var showRecordsDialog by remember { mutableStateOf(false) }
    var selectedSessionTitle by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadSchedules()
        if (isLecturer) viewModel.loadMySessions()
        else viewModel.loadHistory()
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (isLecturer) {
            if (activeSession != null && secondsLeft > 0) {
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
                            Image(bitmap = bmp.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.size(200.dp))
                        }
                        Surface(
                            color = if (secondsLeft > 30) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("$secondsLeft saniye", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        }
                        Text("${sessionRecords.size} ${strings.attendanceParticipants}", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        OutlinedButton(onClick = { viewModel.endSession() }) { Text(strings.attendanceEndSession) }
                    }
                }
            } else {
                Text(strings.attendanceCreateQRTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                
                val courseSchedules = state.schedules.filter { it.courseId == courseId }
                if (courseSchedules.isEmpty()) {
                    Text(strings.attendanceNoSchedule, color = Color.Gray, fontSize = 13.sp)
                } else {
                    var scheduleExpanded by remember { mutableStateOf(false) }
                    var selectedSchedId by remember { mutableStateOf<Int?>(null) }
                    
                    ExposedDropdownMenuBox(expanded = scheduleExpanded, onExpandedChange = { scheduleExpanded = !scheduleExpanded }) {
                        OutlinedTextField(
                            value = courseSchedules.find { it.id == selectedSchedId }?.let { "${it.day} ${it.start_time.take(5)}" } ?: strings.attendanceSelectCourse,
                            onValueChange = {}, readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = scheduleExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = scheduleExpanded, onDismissRequest = { scheduleExpanded = false }) {
                            courseSchedules.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text("${s.day} ${s.start_time.take(5)} - ${s.end_time.take(5)}") },
                                    onClick = { selectedSchedId = s.id; scheduleExpanded = false }
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            selectedSchedId?.let { id ->
                                val cal = java.util.Calendar.getInstance()
                                val today = String.format("%04d-%02d-%02d", cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH))
                                viewModel.createSession(id, today) { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedSchedId != null && !state.isCreating
                    ) {
                        if (state.isCreating) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        else {
                            Icon(Icons.Default.QrCode, null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.attendanceCreateQR)
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            Text(strings.attendanceSessions, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            if (mySessions.isEmpty()) {
                Text(strings.attendanceNoSessions, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                mySessions.forEach { session ->
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                            selectedSessionTitle = "${session.course_code} (${session.session_date})"
                            viewModel.fetchSessionRecords(session.id)
                            showRecordsDialog = true
                        },
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(session.session_date, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Surface(
                                    color = if (session.is_expired) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Text(if (session.is_expired) strings.attendanceExpired else strings.attendanceActive, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Text("${session.record_count} ${strings.attendanceParticipants}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        } else {
            var token by remember { mutableStateOf("") }
            Text(strings.attendanceScanTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(strings.attendanceScanHint, fontSize = 13.sp, color = Color.Gray)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = token, onValueChange = { token = it.trim() }, label = { Text("Token") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    if (token.isNotBlank()) {
                        viewModel.checkIn(token, 
                            onSuccess = { c -> Toast.makeText(context, "✓ ${strings.attendanceRecorded}: $c", Toast.LENGTH_SHORT).show() },
                            onError = { m -> Toast.makeText(context, m, Toast.LENGTH_LONG).show() }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = token.isNotBlank()
            ) {
                Icon(Icons.Default.CheckCircle, null)
                Spacer(Modifier.width(8.dp))
                Text(strings.attendanceJoin)
            }
            
            Spacer(Modifier.height(24.dp))
            Text(strings.attendanceHistory, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            if (history.isEmpty()) {
                Text(strings.attendanceNoHistory, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                history.forEach { record ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("${strings.attendanceSession} #${record.session}", fontWeight = FontWeight.Medium)
                                Text(record.checked_in_at.take(16).replace("T", " "), fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRecordsDialog) {
        Dialog(onDismissRequest = { showRecordsDialog = false }) {
            Surface(modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.7f), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedSessionTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { showRecordsDialog = false }) { Icon(Icons.Default.Close, null) }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    if (state.isLoading) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    else if (sessionRecords.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Katılım yok.", color = Color.Gray) }
                    else {
                        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                            items(sessionRecords) { record ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("${record.student_first_name} ${record.student_last_name}".trim().ifBlank { record.student_username }, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        Text(record.checked_in_at.take(16).replace("T", " "), fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }
        }
    }
}

