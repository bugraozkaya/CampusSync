package com.bugra.campussync.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.bugra.campussync.viewmodels.CourseMaterialsViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseMaterialsScreen(
    viewModel: CourseMaterialsViewModel = hiltViewModel()
) {
    val context      = LocalContext.current
    val strings      = LocalAppStrings.current
    val tokenManager = remember { TokenManager(context) }
    val role         = (tokenManager.getRole() ?: "").uppercase()
    val canUpload    = role in listOf("LECTURER", "ADMIN", "STAFF", "IT")

    val state by viewModel.state.collectAsState()
    val materials = state.materials
    val courses = state.courses
    val isLoading = state.isLoading
    val isUploading = state.isUploading

    var showUpload   by remember { mutableStateOf(false) }
    var filterCourse by remember { mutableStateOf<Int?>(null) }
    var filterExpanded by remember { mutableStateOf(false) }

    // Upload dialog state
    var uploadTitle    by remember { mutableStateOf("") }
    var uploadDesc     by remember { mutableStateOf("") }
    var uploadType     by remember { mutableStateOf("LECTURE_NOTES") }
    var uploadCourseId by remember { mutableStateOf("") }
    var uploadFileUri  by remember { mutableStateOf<Uri?>(null) }
    var uploadFileName by remember { mutableStateOf("") }
    var typeExpanded   by remember { mutableStateOf(false) }
    var courseExpanded by remember { mutableStateOf(false) }
    var uploadFileSize by remember { mutableStateOf(0L) }
    var uploadFileMimeType by remember { mutableStateOf("") }
    var uploadError by remember { mutableStateOf("") }

    val MATERIAL_TYPES = listOf(
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
                    val nameCol = it.getColumnIndex("_display_name")
                    if (nameCol != -1) uploadFileName = it.getString(nameCol)
                    try {
                        val sizeCol = it.getColumnIndex("_size")
                        if (sizeCol != -1 && !it.isNull(sizeCol)) uploadFileSize = it.getLong(sizeCol)
                    } catch (_: Exception) { }
                }
            }
            if (uploadFileName.isBlank()) uploadFileName = "file"
            uploadFileMimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
        }
    }

    LaunchedEffect(Unit) { viewModel.load(null) }
    LaunchedEffect(filterCourse) { viewModel.load(filterCourse) }
    LaunchedEffect(showUpload) { if (showUpload) uploadError = "" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.materialsTitle, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (canUpload) {
                FloatingActionButton(onClick = { showUpload = true }, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.Upload, strings.materialsUpload, tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (courses.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    ExposedDropdownMenuBox(expanded = filterExpanded, onExpandedChange = { filterExpanded = !filterExpanded }) {
                        OutlinedTextField(
                            value = if (filterCourse == null) strings.materialsAllCourses else courses.find { it.id == filterCourse }?.course_code ?: strings.materialsAllCourses,
                            onValueChange = {}, readOnly = true,
                            label = { Text(strings.classroomFilter, fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = filterExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                        )
                        ExposedDropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }) {
                            DropdownMenuItem(text = { Text(strings.materialsAllCourses) }, onClick = { filterCourse = null; filterExpanded = false })
                            courses.forEach { course ->
                                DropdownMenuItem(
                                    text = { Text("${course.course_code} – ${course.course_name}") },
                                    onClick = { filterCourse = course.id; filterExpanded = false }
                                )
                            }
                        }
                    }
                }
            }

            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                materials.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(Modifier.height(12.dp))
                        Text(strings.materialsNone, color = Color.Gray)
                    }
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(materials, key = { it.id }) { mat ->
                        MaterialCard(
                            material = mat,
                            canDelete = canUpload,
                            onDownload = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(mat.file_url))
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, strings.materialsCannotOpen, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDelete = {
                                viewModel.delete(mat.id) {
                                    Toast.makeText(context, strings.materialsDeleteFailed, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showUpload) {
        Dialog(
            onDismissRequest = { if (!isUploading) showUpload = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f).imePadding(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = AlertDialogDefaults.TonalElevation,
                color = AlertDialogDefaults.containerColor
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)) {
                    Text(strings.materialsUpload, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(12.dp))

                    // Scrollable form area
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(value = uploadTitle, onValueChange = { uploadTitle = it; uploadError = "" }, label = { Text(strings.materialsHeadline) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = uploadDesc, onValueChange = { uploadDesc = it }, label = { Text(strings.materialsDescription) }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
                        ExposedDropdownMenuBox(expanded = courseExpanded, onExpandedChange = { courseExpanded = !courseExpanded }) {
                            OutlinedTextField(
                                value = courses.find { it.id.toString() == uploadCourseId }?.let { "${it.course_code} – ${it.course_name}" } ?: strings.materialsSelectCourse,
                                onValueChange = {}, readOnly = true, label = { Text(strings.courseLabel) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = courseExpanded, onDismissRequest = { courseExpanded = false }) {
                                courses.forEach { c ->
                                    DropdownMenuItem(text = { Text("${c.course_code} – ${c.course_name}") }, onClick = { uploadCourseId = c.id.toString(); courseExpanded = false; uploadError = "" })
                                }
                            }
                        }
                        ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
                            OutlinedTextField(
                                value = MATERIAL_TYPES.find { it.first == uploadType }?.second ?: uploadType,
                                onValueChange = {}, readOnly = true, label = { Text(strings.materialsType) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                                MATERIAL_TYPES.forEach { (code, label) ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = { uploadType = code; typeExpanded = false })
                                }
                            }
                        }
                        if (uploadFileUri == null) {
                            OutlinedButton(onClick = { filePicker.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.materialsFileSelect, maxLines = 1)
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (uploadFileMimeType.startsWith("image/")) {
                                        AsyncImage(
                                            model = uploadFileUri,
                                            contentDescription = null,
                                            modifier = Modifier.size(60.dp).clip(MaterialTheme.shapes.medium),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Surface(
                                            color = fileMimeColor(uploadFileMimeType).copy(alpha = 0.15f),
                                            shape = MaterialTheme.shapes.medium
                                        ) {
                                            Icon(
                                                fileMimeIcon(uploadFileMimeType),
                                                contentDescription = null,
                                                modifier = Modifier.size(60.dp).padding(14.dp),
                                                tint = fileMimeColor(uploadFileMimeType)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(uploadFileName, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 2, lineHeight = 18.sp)
                                        if (uploadFileSize > 0) Text(formatFileSize(uploadFileSize), fontSize = 12.sp, color = Color.Gray)
                                        Text(fileMimeLabel(uploadFileMimeType), fontSize = 11.sp, color = fileMimeColor(uploadFileMimeType))
                                    }
                                    TextButton(onClick = { filePicker.launch("*/*") }) {
                                        Text("Değiştir", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Error text — always visible, below form
                    if (uploadError.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(uploadError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                    }

                    // Action buttons — always visible at bottom
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            showUpload = false
                            uploadTitle = ""; uploadDesc = ""; uploadCourseId = ""
                            uploadFileName = ""; uploadFileUri = null
                            uploadFileSize = 0L; uploadFileMimeType = ""; uploadError = ""
                        }) { Text(strings.cancel) }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                uploadError = ""
                                val uri = uploadFileUri
                                when {
                                    uri == null -> { uploadError = "Lütfen önce bir dosya seçin."; return@Button }
                                    uploadTitle.isBlank() -> { uploadError = strings.materialsTitleRequired; return@Button }
                                    uploadCourseId.isBlank() -> { uploadError = strings.materialsTitleRequired; return@Button }
                                }
                                val mimeType = context.contentResolver.getType(uri!!) ?: "application/octet-stream"
                                val bytes = try {
                                    context.contentResolver.openInputStream(uri)?.readBytes()
                                } catch (e: Exception) {
                                    uploadError = "Dosya okunamadı: ${e.localizedMessage}"; return@Button
                                }
                                if (bytes == null) { uploadError = "Dosya okunamadı. Tekrar deneyin."; return@Button }
                                val filePart = MultipartBody.Part.createFormData("file", uploadFileName, bytes.toRequestBody(mimeType.toMediaTypeOrNull()))
                                viewModel.upload(
                                    filePart = filePart,
                                    course = uploadCourseId.toRequestBody("text/plain".toMediaTypeOrNull()),
                                    title = uploadTitle.toRequestBody("text/plain".toMediaTypeOrNull()),
                                    description = uploadDesc.toRequestBody("text/plain".toMediaTypeOrNull()),
                                    materialType = uploadType.toRequestBody("text/plain".toMediaTypeOrNull()),
                                    onSuccess = {
                                        showUpload = false
                                        uploadTitle = ""; uploadDesc = ""; uploadCourseId = ""
                                        uploadFileName = ""; uploadFileUri = null
                                        uploadFileSize = 0L; uploadFileMimeType = ""; uploadError = ""
                                        Toast.makeText(context, strings.materialsUploaded, Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { err -> uploadError = "Yükleme hatası: $err" }
                                )
                            },
                            enabled = !isUploading
                        ) {
                            if (isUploading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            else Text(strings.upload)
                        }
                    }
                }
            }
        }
    }
}

private fun fileMimeIcon(mimeType: String): ImageVector = when {
    mimeType.startsWith("image/")   -> Icons.Default.Image
    mimeType == "application/pdf"   -> Icons.Default.Description
    mimeType.startsWith("video/")   -> Icons.Default.PlayCircle
    mimeType.startsWith("audio/")   -> Icons.Default.MusicNote
    mimeType.startsWith("text/")    -> Icons.Default.Article
    else                            -> Icons.Default.AttachFile
}

private fun fileMimeColor(mimeType: String): Color = when {
    mimeType.startsWith("image/")                                     -> Color(0xFF1976D2)
    mimeType == "application/pdf"                                     -> Color(0xFFD32F2F)
    mimeType.contains("word") || mimeType.contains("document")       -> Color(0xFF1565C0)
    mimeType.contains("sheet") || mimeType.contains("excel")         -> Color(0xFF2E7D32)
    mimeType.contains("presentation") || mimeType.contains("powerpoint") -> Color(0xFFE65100)
    mimeType.startsWith("text/")                                      -> Color(0xFF455A64)
    mimeType.startsWith("video/")                                     -> Color(0xFF6A1B9A)
    mimeType.startsWith("audio/")                                     -> Color(0xFF00838F)
    else                                                              -> Color(0xFF757575)
}

private fun fileMimeLabel(mimeType: String): String = when {
    mimeType == "application/pdf"                                     -> "PDF"
    mimeType.contains("word") || mimeType.contains("document")       -> "Word"
    mimeType.contains("sheet") || mimeType.contains("excel")         -> "Excel"
    mimeType.contains("presentation") || mimeType.contains("powerpoint") -> "PowerPoint"
    mimeType.startsWith("image/")  -> mimeType.removePrefix("image/").uppercase()
    mimeType.startsWith("text/")   -> "Metin"
    mimeType.startsWith("video/")  -> "Video"
    mimeType.startsWith("audio/")  -> "Ses"
    else -> mimeType.substringAfterLast('/').take(12).uppercase()
}

private fun formatFileSize(bytes: Long): String = when {
    bytes <= 0          -> ""
    bytes < 1_024       -> "$bytes B"
    bytes < 1_048_576   -> "${bytes / 1_024} KB"
    else                -> "${"%.1f".format(bytes / 1_048_576.0)} MB"
}
