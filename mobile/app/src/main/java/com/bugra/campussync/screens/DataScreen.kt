package com.bugra.campussync.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bugra.campussync.utils.LocalAppStrings
import com.bugra.campussync.viewmodels.DataViewModel
import com.bugra.campussync.viewmodels.LecturerEntry
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    onNavigateToChat: (Int, String) -> Unit = { _, _ -> },
    viewModel: DataViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current

    val state by viewModel.state.collectAsState()
    val importResults = state.lecturers
    val isUploading = state.isUploading

    val isFetchingExisting = state.isLoading
    val isSubmittingLecturer = state.isSubmittingLecturer
    val isSubmittingCourse = state.isSubmittingCourse
    val isSubmittingStudent = state.isSubmittingStudent
    val passwordCache = viewModel.passwordCache

    // Lecturer calendar dialog state
    val lecturerSchedule by viewModel.lecturerSchedule.collectAsState()
    val lecturerUnavailability by viewModel.lecturerUnavailability.collectAsState()
    val isLoadingCalendar by viewModel.isLoadingCalendar.collectAsState()
    var showCalendarDialog by remember { mutableStateOf(false) }
    var calendarLecturer by remember { mutableStateOf<LecturerEntry?>(null) }

    // Course assignment dialog state
    val allCourses by viewModel.allCourses.collectAsState()
    val lecturerAssignedIds by viewModel.lecturerAssignedCourseIds.collectAsState()
    val isLoadingCourses by viewModel.isLoadingCourses.collectAsState()
    var showCoursesDialog by remember { mutableStateOf(false) }
    var selectedLecturer by remember { mutableStateOf<LecturerEntry?>(null) }
    var dialogSelectedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var originalDialogIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    LaunchedEffect(lecturerAssignedIds) {
        if (showCoursesDialog) {
            dialogSelectedIds = lecturerAssignedIds
            originalDialogIds = lecturerAssignedIds
        }
    }

    var showManualDialog  by remember { mutableStateOf(false) }
    var showCourseDialog  by remember { mutableStateOf(false) }
    var showStudentDialog by remember { mutableStateOf(false) }
    var manualName     by remember { mutableStateOf("") }
    var manualSurname  by remember { mutableStateOf("") }
    var manualDept     by remember { mutableStateOf("") }
    var courseName     by remember { mutableStateOf("") }
    var courseCode     by remember { mutableStateOf("") }
    var studentName    by remember { mutableStateOf("") }
    var studentSurname by remember { mutableStateOf("") }
    var studentNumber  by remember { mutableStateOf("") }

    var previewItems       by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }
    var showImportPreview  by remember { mutableStateOf(false) }
    var isExcelPreview     by remember { mutableStateOf(false) }
    var pendingImportPart  by remember { mutableStateOf<MultipartBody.Part?>(null) }
    var importLoading      by remember { mutableStateOf(false) }
    var importError        by remember { mutableStateOf("") }

    var showEditRow  by remember { mutableStateOf(false) }
    var editRowIdx   by remember { mutableStateOf(-1) }
    var editFirst    by remember { mutableStateOf("") }
    var editLast     by remember { mutableStateOf("") }
    var editDept     by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        var readError: String? = null
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) { readError = e.localizedMessage; null }

        if (bytes == null) {
            Toast.makeText(context, "Dosya okunamadı: ${readError ?: "Erişim reddedildi"}", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        if (bytes.isEmpty()) {
            Toast.makeText(context, "Dosya boş.", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }

        var fileName = "lecturers.txt"
        context.contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                val col = it.getColumnIndex("_display_name")
                if (col != -1) fileName = it.getString(col)
            }
        }
        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val filePart = MultipartBody.Part.createFormData(
            "file", fileName, bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        )
        val lowerName = fileName.lowercase()
        if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
            isExcelPreview = true
            previewItems = emptyList()
            pendingImportPart = filePart
            showImportPreview = true
        } else {
            val parsed = parseLecturerText(bytes)
            if (parsed.isEmpty()) {
                Toast.makeText(
                    context,
                    "Dosyada geçerli satır bulunamadı.\nBeklenen format: Ad,Soyad,Departman",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                isExcelPreview = false
                previewItems = parsed
                pendingImportPart = filePart
                showImportPreview = true
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FabWithTooltip(label = "Ders Ekle", modifier = Modifier.padding(bottom = 8.dp)) {
                    SmallFloatingActionButton(
                        onClick = { showCourseDialog = true },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ) { Icon(Icons.Default.Add, null) }
                }
                FabWithTooltip(label = "Öğrenci Ekle", modifier = Modifier.padding(bottom = 8.dp)) {
                    SmallFloatingActionButton(
                        onClick = { showStudentDialog = true },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ) { Icon(Icons.Default.School, null) }
                }
                FabWithTooltip(label = "Hoca Ekle", modifier = Modifier.padding(bottom = 8.dp)) {
                    SmallFloatingActionButton(
                        onClick = { showManualDialog = true },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) { Icon(Icons.Default.PersonAdd, null) }
                }
                FabWithTooltip(label = "Dosyadan Yükle (TXT / Excel)") {
                    FloatingActionButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) { Icon(Icons.Default.FileUpload, null, tint = Color.White) }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.dataRegisteredLecturers, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { viewModel.fetchLecturers() }) {
                        if (isFetchingExisting)
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else
                            Icon(Icons.Default.Refresh, strings.refresh)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isFetchingExisting && importResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (importResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Group, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(strings.dataNoLecturers, color = Color.Gray, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(strings.dataAddHint, color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(importResults) { res ->
                            var passwordVisible by remember { mutableStateOf(false) }
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            res.lecturer,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                if (res.userId > 0) onNavigateToChat(res.userId, res.lecturer)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Message,
                                                contentDescription = "Mesaj Gönder",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Text(res.course, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("👤 ${res.generatedUser}", fontSize = 12.sp, color = Color.DarkGray)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "🔑 " + if (passwordVisible) res.generatedPass else "••••••",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (res.generatedPass == "••••••") Color.Gray
                                                            else MaterialTheme.colorScheme.error
                                                )
                                                if (res.generatedPass != "••••••") {
                                                    Spacer(Modifier.width(4.dp))
                                                    IconButton(
                                                        onClick = { passwordVisible = !passwordVisible },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedButton(
                                                onClick = {
                                                    selectedLecturer = res
                                                    showCoursesDialog = true
                                                    viewModel.loadCoursesForAssignment(res.userId)
                                                },
                                                modifier = Modifier.height(32.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.MenuBook, null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Dersler", fontSize = 11.sp)
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    calendarLecturer = res
                                                    showCalendarDialog = true
                                                    viewModel.loadLecturerCalendar(res.userId)
                                                },
                                                modifier = Modifier.height(32.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(strings.dataCalendar, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Hoca Takvim Dialog ---
        if (showCalendarDialog && calendarLecturer != null) {
            LecturerCalendarDialog(
                lecturerName = calendarLecturer!!.lecturer,
                schedules = lecturerSchedule,
                unavailability = lecturerUnavailability,
                isLoading = isLoadingCalendar,
                onDismiss = { showCalendarDialog = false }
            )
        }

        // --- Ders Atama Dialog ---
        if (showCoursesDialog && selectedLecturer != null) {
            CourseAssignmentDialog(
                lecturerName = selectedLecturer!!.lecturer,
                allCourses = allCourses,
                selectedIds = dialogSelectedIds,
                isLoading = isLoadingCourses,
                onToggle = { courseId ->
                    dialogSelectedIds = if (courseId in dialogSelectedIds)
                        dialogSelectedIds - courseId
                    else
                        dialogSelectedIds + courseId
                },
                onSave = {
                    val orig = originalDialogIds
                    val newIds = dialogSelectedIds
                    viewModel.saveLecturerCourses(
                        lecturerId = selectedLecturer!!.userId,
                        originalIds = orig,
                        newIds = newIds,
                        onDone = {
                            showCoursesDialog = false
                            Toast.makeText(context, "Ders ataması güncellendi.", Toast.LENGTH_SHORT).show()
                        },
                        onError = { err ->
                            Toast.makeText(context, "Hata: $err", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onDismiss = { showCoursesDialog = false }
            )
        }

        // --- Önizleme satır düzenleme ---
        if (showEditRow) {
            AlertDialog(
                onDismissRequest = { showEditRow = false },
                title = { Text("Satırı Düzenle") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = editFirst, onValueChange = { editFirst = it }, label = { Text(strings.settingsFirstName) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = editLast,  onValueChange = { editLast  = it }, label = { Text(strings.settingsLastName) },  modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = editDept,  onValueChange = { editDept  = it }, label = { Text(strings.dataDepartment) },     modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (editFirst.isBlank() || editLast.isBlank()) return@Button
                        previewItems = previewItems.toMutableList().also {
                            it[editRowIdx] = Triple(editFirst.trim(), editLast.trim(), editDept.trim())
                        }
                        showEditRow = false
                    }) { Text(strings.save) }
                },
                dismissButton = {
                    TextButton(onClick = { showEditRow = false }) { Text(strings.cancel) }
                }
            )
        }

        // --- Hoca İçe Aktarma Önizleme Dialog ---
        if (showImportPreview) {
            Dialog(
                onDismissRequest = {
                    if (!importLoading) {
                        showImportPreview = false
                        pendingImportPart = null
                        previewItems = emptyList()
                        importError = ""
                    }
                },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(if (isExcelPreview) 0.28f else 0.80f),
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = AlertDialogDefaults.TonalElevation,
                    color = AlertDialogDefaults.containerColor
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                        Text("Hoca İçe Aktarma Önizlemesi", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(4.dp))

                        if (isExcelPreview) {
                            Text(
                                "Excel dosyası seçildi. İçe aktarmak istiyor musunuz?",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                "${previewItems.size} hoca içe aktarılacak",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Ad Soyad", Modifier.weight(1.2f), fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("Departman", Modifier.weight(1.5f), fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(Modifier.height(2.dp))
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                itemsIndexed(previewItems) { index, item ->
                                    val (firstName, lastName, dept) = item
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 10.dp, end = 2.dp, top = 3.dp, bottom = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("$firstName $lastName", Modifier.weight(1.2f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text(
                                            dept.ifBlank { "—" },
                                            Modifier.weight(1.3f),
                                            fontSize = 12.sp,
                                            color = if (dept.isBlank()) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        IconButton(
                                            onClick = { editRowIdx = index; editFirst = firstName; editLast = lastName; editDept = dept; showEditRow = true },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(
                                            onClick = { previewItems = previewItems.toMutableList().also { it.removeAt(index) } },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                }
                            }
                        }

                        if (importError.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(importError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                onClick = { showImportPreview = false; pendingImportPart = null; previewItems = emptyList(); importError = ""; importLoading = false },
                                enabled = !importLoading
                            ) { Text(strings.cancel) }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    importError = ""
                                    importLoading = true
                                    if (isExcelPreview) {
                                        val part = pendingImportPart ?: run { importLoading = false; return@Button }
                                        viewModel.bulkImport(
                                            filePart = part,
                                            onSuccess = { count ->
                                                importLoading = false; showImportPreview = false; previewItems = emptyList(); pendingImportPart = null
                                                Toast.makeText(context, "✓ $count kayıt içe aktarıldı!", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { err -> importLoading = false; importError = err }
                                        )
                                    } else {
                                        val items = previewItems.toList()
                                        viewModel.bulkImportLecturers(
                                            items = items,
                                            onSuccess = { count ->
                                                importLoading = false; showImportPreview = false; previewItems = emptyList(); pendingImportPart = null
                                                Toast.makeText(context, "✓ $count hoca başarıyla eklendi!", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { err -> importLoading = false; importError = err }
                                        )
                                    }
                                },
                                enabled = !importLoading
                            ) {
                                if (importLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                else Text(if (isExcelPreview) "İçe Aktar" else "İçe Aktar (${previewItems.size})")
                            }
                        }
                    }
                }
            }
        }

        // --- Ders Ekle Dialog ---
        if (showCourseDialog) {
            Dialog(
                onDismissRequest = { if (!isSubmittingCourse) showCourseDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(0.95f).wrapContentHeight().imePadding(),
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = AlertDialogDefaults.TonalElevation,
                    color = AlertDialogDefaults.containerColor
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(strings.dataAddCourse, style = MaterialTheme.typography.headlineSmall)
                        OutlinedTextField(value = courseName, onValueChange = { courseName = it }, label = { Text(strings.dataCourseName) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = courseCode, onValueChange = { courseCode = it }, label = { Text(strings.dataCourseCode) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showCourseDialog = false }) { Text(strings.cancel) }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (courseName.isBlank()) { Toast.makeText(context, strings.dataCourseNameEmpty, Toast.LENGTH_SHORT).show(); return@Button }
                                    if (courseCode.isBlank()) { Toast.makeText(context, strings.dataCourseCodeEmpty, Toast.LENGTH_SHORT).show(); return@Button }
                                    viewModel.createCourse(
                                        name = courseName, code = courseCode,
                                        onSuccess = { Toast.makeText(context, strings.dataCourseAdded, Toast.LENGTH_SHORT).show(); showCourseDialog = false; courseName = ""; courseCode = "" },
                                        onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                                    )
                                },
                                enabled = !isSubmittingCourse
                            ) {
                                if (isSubmittingCourse) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                else Text(strings.save)
                            }
                        }
                    }
                }
            }
        }

        // --- Hoca Ekle Dialog ---
        if (showManualDialog) {
            Dialog(
                onDismissRequest = { if (!isSubmittingLecturer) showManualDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(0.95f).wrapContentHeight().imePadding(),
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = AlertDialogDefaults.TonalElevation,
                    color = AlertDialogDefaults.containerColor
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(strings.dataAddLecturer, style = MaterialTheme.typography.headlineSmall)
                        OutlinedTextField(value = manualName, onValueChange = { manualName = it }, label = { Text(strings.settingsFirstName) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = manualSurname, onValueChange = { manualSurname = it }, label = { Text(strings.settingsLastName) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(
                            value = manualDept, onValueChange = { manualDept = it },
                            label = { Text(strings.dataDepartment) }, modifier = Modifier.fillMaxWidth(),
                            singleLine = true, isError = manualDept.isBlank() && manualName.isNotBlank()
                        )
                        if (manualName.isNotEmpty() && manualSurname.isNotEmpty()) {
                            Text("${strings.loginUsername}: ${viewModel.generateUsername(manualName, manualSurname)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showManualDialog = false }) { Text(strings.cancel) }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    when {
                                        manualName.isBlank() -> Toast.makeText(context, strings.dataFirstNameEmpty, Toast.LENGTH_SHORT).show()
                                        manualSurname.isBlank() -> Toast.makeText(context, strings.dataLastNameEmpty, Toast.LENGTH_SHORT).show()
                                        manualDept.isBlank() -> Toast.makeText(context, strings.dataDepartmentEmpty, Toast.LENGTH_SHORT).show()
                                        else -> viewModel.createLecturer(
                                            firstName = manualName, lastName = manualSurname, department = manualDept,
                                            onSuccess = { _, password ->
                                                Toast.makeText(context, "${strings.dataLecturerAdded}$password", Toast.LENGTH_LONG).show()
                                                showManualDialog = false; manualName = ""; manualSurname = ""; manualDept = ""
                                            },
                                            onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                                        )
                                    }
                                },
                                enabled = !isSubmittingLecturer
                            ) {
                                if (isSubmittingLecturer) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                else Text(strings.save)
                            }
                        }
                    }
                }
            }
        }

        // --- Öğrenci Ekle Dialog ---
        if (showStudentDialog) {
            Dialog(
                onDismissRequest = { if (!isSubmittingStudent) showStudentDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(0.95f).wrapContentHeight().imePadding(),
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = AlertDialogDefaults.TonalElevation,
                    color = AlertDialogDefaults.containerColor
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(strings.dataAddStudent, style = MaterialTheme.typography.headlineSmall)
                        OutlinedTextField(value = studentName, onValueChange = { studentName = it }, label = { Text(strings.settingsFirstName) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = studentSurname, onValueChange = { studentSurname = it }, label = { Text(strings.settingsLastName) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(
                            value = studentNumber, onValueChange = { studentNumber = it },
                            label = { Text(strings.dataStudentNo) }, modifier = Modifier.fillMaxWidth(),
                            singleLine = true, placeholder = { Text("örn: 20230001") }
                        )
                        if (studentName.isNotEmpty() && studentSurname.isNotEmpty()) {
                            val previewUsername = if (studentNumber.isNotBlank()) studentNumber else viewModel.generateUsername(studentName, studentSurname)
                            Text("${strings.loginUsername}: $previewUsername", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showStudentDialog = false }) { Text(strings.cancel) }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    when {
                                        studentName.isBlank() -> Toast.makeText(context, strings.dataFirstNameEmpty, Toast.LENGTH_SHORT).show()
                                        studentSurname.isBlank() -> Toast.makeText(context, strings.dataLastNameEmpty, Toast.LENGTH_SHORT).show()
                                        else -> viewModel.createStudent(
                                            firstName = studentName, lastName = studentSurname, studentNumber = studentNumber,
                                            onSuccess = { username, password ->
                                                Toast.makeText(context, "${strings.dataStudentAdded}\n${strings.loginUsername}: $username\n${strings.loginPassword}: $password", Toast.LENGTH_LONG).show()
                                                showStudentDialog = false; studentName = ""; studentSurname = ""; studentNumber = ""
                                            },
                                            onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                                        )
                                    }
                                },
                                enabled = !isSubmittingStudent
                            ) {
                                if (isSubmittingStudent) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                else Text(strings.save)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LecturerCalendarDialog(
    lecturerName: String,
    schedules: List<Map<String, Any>>,
    unavailability: List<Map<String, String>>,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    val days = listOf("MON", "TUE", "WED", "THU", "FRI")
    val dayLabels = listOf("Pzt", "Sal", "Çar", "Per", "Cum")
    val slots = listOf("08:00-10:00", "10:00-12:00", "13:00-15:00", "15:00-17:00")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.98f).fillMaxHeight(0.88f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Haftalık Takvim", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(lecturerName, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Legend
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    LegendItem(color = MaterialTheme.colorScheme.primaryContainer, label = "Ders")
                    LegendItem(color = Color(0xFFFFCDD2), label = "Müsait Değil")
                    LegendItem(color = MaterialTheme.colorScheme.surfaceVariant, label = "Boş")
                }

                if (isLoading) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("Takvim yükleniyor...", fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                } else {
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        // Header row
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Spacer(modifier = Modifier.width(52.dp))
                            dayLabels.forEachIndexed { i, label ->
                                Text(
                                    text = label,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider()

                        slots.forEach { slot ->
                            Row(
                                modifier = Modifier.fillMaxWidth().height(80.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.width(52.dp).fillMaxHeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = slot.replace("-", "\n"),
                                        fontSize = 7.sp,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 9.sp
                                    )
                                }

                                days.forEach { day ->
                                    val schedule = schedules.firstOrNull { item ->
                                        val rawDay = item["day_of_week"]?.toString() ?: ""
                                        val st = item["start_time"]?.toString() ?: ""
                                        normalizeDayOfWeek(rawDay) == day && checkSlot(st, slot)
                                    }
                                    val isUnavailable = unavailability.any { u ->
                                        normalizeDayOfWeek(u["day"] ?: "") == day && u["hour"] == slot
                                    }

                                    val bgColor = when {
                                        schedule != null -> MaterialTheme.colorScheme.primaryContainer
                                        isUnavailable   -> Color(0xFFFFCDD2)
                                        else            -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    }
                                    val borderColor = when {
                                        schedule != null -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                        isUnavailable   -> Color(0xFFE57373).copy(alpha = 0.6f)
                                        else            -> Color.LightGray.copy(alpha = 0.3f)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(1.5.dp)
                                            .border(
                                                width = if (schedule != null || isUnavailable) 1.dp else 0.5.dp,
                                                color = borderColor
                                            )
                                            .background(bgColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when {
                                            schedule != null -> {
                                                val code = schedule["course_code"]?.toString() ?: ""
                                                val name = schedule["course_name"]?.toString() ?: ""
                                                val sessionType = schedule["session_type"]?.toString() ?: ""
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.padding(2.dp)
                                                ) {
                                                    Text(
                                                        code,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        name,
                                                        fontSize = 7.sp,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (sessionType == "LAB") {
                                                        Text("🔬", fontSize = 8.sp)
                                                    }
                                                }
                                            }
                                            isUnavailable -> {
                                                Text(
                                                    "🚫",
                                                    fontSize = 14.sp,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                            else -> {}
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = MaterialTheme.shapes.extraSmall)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CourseAssignmentDialog(
    lecturerName: String,
    allCourses: List<com.bugra.campussync.network.CourseItem>,
    selectedIds: Set<Int>,
    isLoading: Boolean,
    onToggle: (Int) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.80f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ders Ataması", style = MaterialTheme.typography.headlineSmall)
                        Text(lecturerName, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }
                Spacer(Modifier.height(12.dp))

                when {
                    isLoading -> {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(8.dp))
                                Text("Dersler yükleniyor...", fontSize = 13.sp, color = Color.Gray)
                            }
                        }
                    }
                    allCourses.isEmpty() -> {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text("Henüz ders eklenmemiş.", color = Color.Gray)
                        }
                    }
                    else -> {
                        Text(
                            "${selectedIds.size} ders seçili",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(allCourses) { course ->
                                val checked = course.id in selectedIds
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { onToggle(course.id) }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(course.course_name, fontSize = 14.sp, fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal)
                                        Text(course.course_code, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("İptal") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onSave, enabled = !isLoading) {
                        Text("Kaydet")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FabWithTooltip(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
        modifier = modifier
    ) {
        content()
    }
}

private fun parseLecturerText(bytes: ByteArray): List<Triple<String, String, String>> {
    return String(bytes, Charsets.UTF_8).lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .mapNotNull { line ->
            val sep = when {
                '\t' in line -> '\t'
                ';' in line  -> ';'
                else         -> ','
            }
            val cols = line.split(sep).map { it.trim() }
            if (cols.size < 2) return@mapNotNull null
            val firstName = cols[0].ifBlank { return@mapNotNull null }
            val lastName  = cols[1].ifBlank { return@mapNotNull null }
            val dept      = if (cols.size >= 3) cols[2] else ""
            Triple(firstName, lastName, dept)
        }
}
