package com.bugra.campussync.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugra.campussync.network.CourseMaterialItem
import com.bugra.campussync.utils.TokenManager
import com.bugra.campussync.viewmodels.CourseMaterialsViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseMaterialsScreen() {
    val context      = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val role         = (tokenManager.getRole() ?: "").uppercase()
    val canUpload    = role.let { it.contains("LECTURER") || it.contains("ADMIN") || it.contains("SUPER") }

    val viewModel: CourseMaterialsViewModel = viewModel()
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

    val MATERIAL_TYPES = listOf(
        "LECTURE_NOTES" to "Ders Notu",
        "ASSIGNMENT" to "Ödev",
        "EXAM" to "Sınav",
        "RESOURCE" to "Kaynak",
        "OTHER" to "Diğer"
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

    LaunchedEffect(Unit) { viewModel.load(null) }
    LaunchedEffect(filterCourse) { viewModel.load(filterCourse) }

    val typeIcon = @Composable { type: String ->
        when (type) {
            "LECTURE_NOTES" -> Icon(Icons.Default.MenuBook, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            "ASSIGNMENT"    -> Icon(Icons.Default.Assignment, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.tertiary)
            "EXAM"          -> Icon(Icons.Default.Quiz, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
            "RESOURCE"      -> Icon(Icons.Default.Link, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
            else            -> Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(20.dp), tint = Color.Gray)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ders Materyalleri", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (canUpload) {
                FloatingActionButton(onClick = { showUpload = true }, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.Upload, "Materyal Yükle", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Filter row
            if (courses.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    ExposedDropdownMenuBox(expanded = filterExpanded, onExpandedChange = { filterExpanded = !filterExpanded }) {
                        OutlinedTextField(
                            value = if (filterCourse == null) "Tüm Dersler" else courses.find { it.id == filterCourse }?.course_code ?: "Tüm Dersler",
                            onValueChange = {}, readOnly = true,
                            label = { Text("Filtre", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = filterExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                        )
                        ExposedDropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }) {
                            DropdownMenuItem(text = { Text("Tüm Dersler") }, onClick = { filterCourse = null; filterExpanded = false })
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
                        Text("Materyal bulunamadı.", color = Color.Gray)
                    }
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(materials, key = { it.id }) { mat ->
                        Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                typeIcon(mat.material_type)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(mat.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("${mat.course_code} · ${mat.material_type_display}", fontSize = 12.sp, color = Color.Gray)
                                    Text(mat.uploaded_by_name, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                if (mat.file_url != null) {
                                    IconButton(onClick = {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(mat.file_url))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Açılamadı.", Toast.LENGTH_SHORT).show()
                                        }
                                    }) { Icon(Icons.Default.Download, "İndir", tint = MaterialTheme.colorScheme.primary) }
                                }
                                if (canUpload) {
                                    IconButton(onClick = {
                                        viewModel.delete(mat.id) {
                                            Toast.makeText(context, "Silinemedi.", Toast.LENGTH_SHORT).show()
                                        }
                                    }) { Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Upload Dialog
    if (showUpload) {
        AlertDialog(
            onDismissRequest = { if (!isUploading) showUpload = false },
            title = { Text("Materyal Yükle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = uploadTitle, onValueChange = { uploadTitle = it }, label = { Text("Başlık") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = uploadDesc, onValueChange = { uploadDesc = it }, label = { Text("Açıklama (opsiyonel)") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)

                    ExposedDropdownMenuBox(expanded = courseExpanded, onExpandedChange = { courseExpanded = !courseExpanded }) {
                        OutlinedTextField(
                            value = courses.find { it.id.toString() == uploadCourseId }?.let { "${it.course_code} – ${it.course_name}" } ?: "Ders seçin",
                            onValueChange = {}, readOnly = true, label = { Text("Ders") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = courseExpanded, onDismissRequest = { courseExpanded = false }) {
                            courses.forEach { c ->
                                DropdownMenuItem(text = { Text("${c.course_code} – ${c.course_name}") }, onClick = { uploadCourseId = c.id.toString(); courseExpanded = false })
                            }
                        }
                    }

                    ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
                        OutlinedTextField(
                            value = MATERIAL_TYPES.find { it.first == uploadType }?.second ?: uploadType,
                            onValueChange = {}, readOnly = true, label = { Text("Tür") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                            MATERIAL_TYPES.forEach { (code, label) ->
                                DropdownMenuItem(text = { Text(label) }, onClick = { uploadType = code; typeExpanded = false })
                            }
                        }
                    }

                    OutlinedButton(onClick = { filePicker.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (uploadFileName.isBlank()) "Dosya Seç" else uploadFileName, maxLines = 1)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = uploadFileUri ?: return@Button
                        if (uploadTitle.isBlank() || uploadCourseId.isBlank()) {
                            Toast.makeText(context, "Başlık ve ders zorunlu.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                        val allowedMimes = setOf(
                            "application/pdf", "image/jpeg", "image/png", "image/gif",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "application/vnd.ms-powerpoint",
                            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                            "application/vnd.ms-excel",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "text/plain", "application/zip", "application/x-zip-compressed"
                        )
                        if (mimeType !in allowedMimes) { Toast.makeText(context, "Desteklenmeyen dosya türü.", Toast.LENGTH_SHORT).show(); return@Button }
                        val fileSize = context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
                        if (fileSize > 50 * 1024 * 1024L) { Toast.makeText(context, "Dosya boyutu 50MB sınırını aşıyor.", Toast.LENGTH_SHORT).show(); return@Button }
                        val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: return@Button
                        val filePart = MultipartBody.Part.createFormData("file", uploadFileName, bytes.toRequestBody(mimeType.toMediaTypeOrNull()))
                        viewModel.upload(
                            filePart = filePart,
                            course = uploadCourseId.toRequestBody("text/plain".toMediaTypeOrNull()),
                            title = uploadTitle.toRequestBody("text/plain".toMediaTypeOrNull()),
                            description = uploadDesc.toRequestBody("text/plain".toMediaTypeOrNull()),
                            materialType = uploadType.toRequestBody("text/plain".toMediaTypeOrNull()),
                            onSuccess = {
                                showUpload = false
                                uploadTitle = ""; uploadDesc = ""; uploadCourseId = ""; uploadFileName = ""; uploadFileUri = null
                                Toast.makeText(context, "Materyal yüklendi.", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                        )
                    },
                    enabled = !isUploading
                ) {
                    if (isUploading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Yükle")
                }
            },
            dismissButton = { TextButton(onClick = { showUpload = false }) { Text("İptal") } }
        )
    }
}
