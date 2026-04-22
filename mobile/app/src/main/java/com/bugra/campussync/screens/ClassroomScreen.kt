package com.bugra.campussync.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugra.campussync.network.ClassroomItem
import com.bugra.campussync.viewmodels.ClassroomViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

private val CLASSROOM_TYPES = listOf(
    "LECTURE" to "Derslik",
    "LAB" to "Laboratuvar",
    "COMPUTER_LAB" to "Bilgisayar Laboratuvarı",
    "SEMINAR" to "Seminer Salonu",
    "OTHER" to "Diğer"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassroomScreen() {
    val context = LocalContext.current
    val viewModel: ClassroomViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val classrooms = state.classrooms
    val isLoading = state.isLoading
    val isSubmitting = state.isSubmitting

    var showAddDialog by remember { mutableStateOf(false) }
    var roomCode by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("LECTURE") }
    var typeExpanded by remember { mutableStateOf(false) }
    var filterType by remember { mutableStateOf("ALL") }
    var filterExpanded by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) {
                var fileName = "classrooms.txt"
                context.contentResolver.query(uri, null, null, null, null)?.use {
                    if (it.moveToFirst()) {
                        val col = it.getColumnIndex("_display_name")
                        if (col != -1) fileName = it.getString(col)
                    }
                }
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val filePart = MultipartBody.Part.createFormData("file", fileName, bytes.toRequestBody(mimeType.toMediaTypeOrNull()))
                viewModel.bulkImport(
                    filePart = filePart,
                    onSuccess = { count -> Toast.makeText(context, "✓ $count sınıf içe aktarıldı.", Toast.LENGTH_SHORT).show() },
                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                )
            }
        }
    }

    val filteredClassrooms = remember(classrooms, filterType) {
        if (filterType == "ALL") classrooms else classrooms.filter { it.classroom_type == filterType }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sınıf Yönetimi", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier.padding(bottom = 8.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = "Dosyadan İçe Aktar")
                }
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Sınıf Ekle", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Sınıflar yükleniyor...", fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        ClassroomSummaryRow(classrooms)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "${filteredClassrooms.size} sınıf",
                                fontSize = 13.sp, color = Color.Gray, modifier = Modifier.weight(1f)
                            )
                            ExposedDropdownMenuBox(
                                expanded = filterExpanded,
                                onExpandedChange = { filterExpanded = !filterExpanded },
                                modifier = Modifier.weight(1.4f)
                            ) {
                                OutlinedTextField(
                                    value = if (filterType == "ALL") "Tümü" else
                                        CLASSROOM_TYPES.find { it.first == filterType }?.second ?: filterType,
                                    onValueChange = {}, readOnly = true,
                                    label = { Text("Filtre", fontSize = 11.sp) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = filterExpanded) },
                                    modifier = Modifier.menuAnchor(),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                                )
                                ExposedDropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }) {
                                    DropdownMenuItem(text = { Text("Tümü") }, onClick = { filterType = "ALL"; filterExpanded = false })
                                    CLASSROOM_TYPES.forEach { (code, label) ->
                                        DropdownMenuItem(text = { Text(label) }, onClick = { filterType = code; filterExpanded = false })
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (filteredClassrooms.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.MeetingRoom, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Kayıtlı sınıf yok.", color = Color.Gray, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "+ ile ekleyin veya dosya yükleyin.\nFormat: oda_kodu  kapasite  [tip]",
                                        color = Color.LightGray, fontSize = 12.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(filteredClassrooms) { room -> ClassroomCard(room) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showAddDialog = false },
            title = { Text("Yeni Sınıf / Lab Ekle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = roomCode,
                        onValueChange = { roomCode = it.uppercase() },
                        label = { Text("Oda Kodu (örn: A101, LAB-3)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        placeholder = { Text("A101") }
                    )
                    OutlinedTextField(
                        value = capacity,
                        onValueChange = { if (it.all { c -> c.isDigit() }) capacity = it },
                        label = { Text("Kapasite (kişi)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = capacity.isNotBlank() && (capacity.toIntOrNull() ?: 0) < 1
                    )
                    if (capacity.isNotBlank() && (capacity.toIntOrNull() ?: 0) < 1) {
                        Text("Kapasite en az 1 olmalı.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                    ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
                        OutlinedTextField(
                            value = CLASSROOM_TYPES.find { it.first == selectedType }?.second ?: selectedType,
                            onValueChange = {}, readOnly = true, label = { Text("Sınıf Tipi") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                            CLASSROOM_TYPES.forEach { (code, label) ->
                                DropdownMenuItem(text = { Text(label) }, onClick = { selectedType = code; typeExpanded = false })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cap = capacity.toIntOrNull()
                        when {
                            roomCode.isBlank() -> Toast.makeText(context, "Oda kodu boş olamaz!", Toast.LENGTH_SHORT).show()
                            cap == null || cap < 1 -> Toast.makeText(context, "Geçerli bir kapasite girin!", Toast.LENGTH_SHORT).show()
                            else -> viewModel.create(
                                roomCode = roomCode, capacity = cap, type = selectedType,
                                onSuccess = {
                                    Toast.makeText(context, "Sınıf eklendi.", Toast.LENGTH_SHORT).show()
                                    showAddDialog = false
                                    roomCode = ""; capacity = ""; selectedType = "LECTURE"
                                },
                                onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                            )
                        }
                    },
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Ekle")
                }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("İptal") } }
        )
    }
}

@Composable
private fun ClassroomSummaryRow(classrooms: List<ClassroomItem>) {
    val counts = classrooms.groupBy { it.classroom_type }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val items = listOf("LECTURE" to "Derslik", "LAB" to "Lab", "COMPUTER_LAB" to "Bil. Lab", "SEMINAR" to "Seminer")
        items.forEach { (type, label) ->
            val count = counts[type]?.size ?: 0
            if (count > 0 || type == "LECTURE") {
                Surface(
                    color = classroomTypeColor(type).copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(classroomTypeIcon(type), null, modifier = Modifier.size(18.dp), tint = classroomTypeColor(type))
                        Text(count.toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = classroomTypeColor(type))
                        Text(label, fontSize = 9.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassroomCard(room: ClassroomItem) {
    val typeColor = classroomTypeColor(room.classroom_type)
    val typeLabel = room.classroom_type_display
        ?: CLASSROOM_TYPES.find { it.first == room.classroom_type }?.second
        ?: room.classroom_type

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = typeColor.copy(alpha = 0.12f), shape = MaterialTheme.shapes.medium) {
                Icon(
                    classroomTypeIcon(room.classroom_type), null,
                    modifier = Modifier.size(44.dp).padding(10.dp), tint = typeColor
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(room.room_code, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("$typeLabel · ${room.capacity} kişi", fontSize = 13.sp, color = Color.Gray)
            }
            Surface(color = typeColor.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
                Text(
                    typeLabel, color = typeColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun classroomTypeColor(type: String): Color {
    val dark = isSystemInDarkTheme()
    return when (type) {
        "LECTURE"      -> if (dark) Color(0xFF90CAF9) else Color(0xFF1976D2)
        "LAB"          -> if (dark) Color(0xFFA5D6A7) else Color(0xFF388E3C)
        "COMPUTER_LAB" -> if (dark) Color(0xFFCE93D8) else Color(0xFF7B1FA2)
        "SEMINAR"      -> if (dark) Color(0xFFFFCC80) else Color(0xFFE65100)
        else           -> if (dark) Color(0xFFBDBDBD) else Color(0xFF616161)
    }
}

@Composable
private fun classroomTypeIcon(type: String) = when (type) {
    "LECTURE"      -> Icons.Default.MeetingRoom
    "LAB"          -> Icons.Default.Science
    "COMPUTER_LAB" -> Icons.Default.Computer
    "SEMINAR"      -> Icons.Default.Weekend
    else           -> Icons.Default.MeetingRoom
}
