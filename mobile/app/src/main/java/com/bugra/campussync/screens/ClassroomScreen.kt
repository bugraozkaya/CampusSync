package com.bugra.campussync.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.Delete
import com.bugra.campussync.network.ClassroomItem
import com.bugra.campussync.utils.LocalAppStrings
import com.bugra.campussync.viewmodels.ClassroomViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

private val CLASSROOM_TYPE_CODES = listOf("LECTURE", "LAB", "COMPUTER_LAB", "SEMINAR", "OTHER")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassroomScreen(
    viewModel: ClassroomViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val state by viewModel.state.collectAsState()
    val classrooms = state.classrooms
    val isLoading = state.isLoading
    val isSubmitting = state.isSubmitting

    LaunchedEffect(Unit) { viewModel.load() }

    var showAddDialog by remember { mutableStateOf(false) }
    var roomCode by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("LECTURE") }
    var typeExpanded by remember { mutableStateOf(false) }
    var filterType by remember { mutableStateOf("ALL") }
    var filterExpanded by remember { mutableStateOf(false) }

    var previewItems by remember { mutableStateOf<List<Triple<String, Int, String>>>(emptyList()) }
    var showImportPreview by remember { mutableStateOf(false) }
    var isExcelPreview by remember { mutableStateOf(false) }
    var pendingImportPart by remember { mutableStateOf<MultipartBody.Part?>(null) }
    var importLoading by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf("") }

    var showEditRow by remember { mutableStateOf(false) }
    var editRowIdx by remember { mutableStateOf(-1) }
    var editRowCode by remember { mutableStateOf("") }
    var editRowCap by remember { mutableStateOf("") }
    var editRowType by remember { mutableStateOf("LECTURE") }
    var editTypeExpanded by remember { mutableStateOf(false) }

    var editingRoom by remember { mutableStateOf<ClassroomItem?>(null) }
    var deletingRoom by remember { mutableStateOf<ClassroomItem?>(null) }

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

        var fileName = "classrooms.txt"
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
            val parsed = parseClassroomText(bytes)
            if (parsed.isEmpty()) {
                Toast.makeText(
                    context,
                    "Dosyada geçerli satır bulunamadı.\nBeklenen format: OdaKodu,Kapasite,Tip",
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

    val filteredClassrooms = remember(classrooms, filterType) {
        if (filterType == "ALL") classrooms else classrooms.filter { it.classroom_type == filterType }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.classroomTitle, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text(strings.classroomImportFile) } },
                    state = rememberTooltipState(),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                    }
                }
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text(strings.classroomAdd) } },
                    state = rememberTooltipState()
                ) {
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    }
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
                            Text(strings.classroomLoading, fontSize = 13.sp, color = Color.Gray)
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
                                "${filteredClassrooms.size} ${strings.navClassrooms}",
                                fontSize = 13.sp, color = Color.Gray, modifier = Modifier.weight(1f)
                            )
                            ExposedDropdownMenuBox(
                                expanded = filterExpanded,
                                onExpandedChange = { filterExpanded = !filterExpanded },
                                modifier = Modifier.weight(1.4f)
                            ) {
                                OutlinedTextField(
                                    value = if (filterType == "ALL") strings.classroomAll else
                                        classroomTypeLabel(filterType, strings),
                                    onValueChange = {}, readOnly = true,
                                    label = { Text(strings.classroomFilter, fontSize = 11.sp) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = filterExpanded) },
                                    modifier = Modifier.menuAnchor(),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                                )
                                ExposedDropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }) {
                                    DropdownMenuItem(text = { Text(strings.classroomAll) }, onClick = { filterType = "ALL"; filterExpanded = false })
                                    CLASSROOM_TYPE_CODES.forEach { code ->
                                        DropdownMenuItem(text = { Text(classroomTypeLabel(code, strings)) }, onClick = { filterType = code; filterExpanded = false })
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
                                    Text(strings.classroomNone, color = Color.Gray, fontSize = 15.sp)
                                }
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(filteredClassrooms) { room ->
                                    ClassroomCard(
                                        room = room,
                                        onEdit = { editingRoom = room },
                                        onDelete = { deletingRoom = room }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditRow) {
        EditClassroomRowDialog(
            code = editRowCode, cap = editRowCap, type = editRowType,
            onSave = { code, cap, type ->
                previewItems = previewItems.toMutableList().also { it[editRowIdx] = Triple(code, cap, type) }
                showEditRow = false
            },
            onDismiss = { showEditRow = false }
        )
    }

    editingRoom?.let { room ->
        EditClassroomDialog(
            room = room,
            isSubmitting = isSubmitting,
            onSave = { code, cap, type ->
                viewModel.update(
                    id = room.id, roomCode = code, capacity = cap, type = type,
                    onSuccess = {
                        Toast.makeText(context, "Sınıf güncellendi.", Toast.LENGTH_SHORT).show()
                        editingRoom = null
                    },
                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                )
            },
            onDismiss = { if (!isSubmitting) editingRoom = null }
        )
    }

    deletingRoom?.let { room ->
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) deletingRoom = null },
            title = { Text("Sınıfı Sil") },
            text = { Text("\"${room.room_code}\" sınıfını silmek istediğinize emin misiniz?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.delete(
                            id = room.id,
                            onSuccess = {
                                Toast.makeText(context, "Sınıf silindi.", Toast.LENGTH_SHORT).show()
                                deletingRoom = null
                            },
                            onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                        )
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onError)
                    else Text("Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingRoom = null }, enabled = !isSubmitting) { Text(strings.cancel) }
            }
        )
    }

    if (showAddDialog) {
        Dialog(
            onDismissRequest = { if (!isSubmitting) showAddDialog = false },
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
                    Text(strings.classroomAddNew, style = MaterialTheme.typography.headlineSmall)
                    OutlinedTextField(
                        value = roomCode,
                        onValueChange = { roomCode = it.uppercase() },
                        label = { Text(strings.classroomRoomCode) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        placeholder = { Text("A101") }
                    )
                    OutlinedTextField(
                        value = capacity,
                        onValueChange = { if (it.all { c -> c.isDigit() }) capacity = it },
                        label = { Text(strings.classroomCapacity) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = capacity.isNotBlank() && (capacity.toIntOrNull() ?: 0) < 1
                    )
                    if (capacity.isNotBlank() && (capacity.toIntOrNull() ?: 0) < 1) {
                        Text(strings.classroomCapacityMin, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                    ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
                        OutlinedTextField(
                            value = classroomTypeLabel(selectedType, strings),
                            onValueChange = {}, readOnly = true, label = { Text(strings.classroomType) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                            CLASSROOM_TYPE_CODES.forEach { code ->
                                DropdownMenuItem(text = { Text(classroomTypeLabel(code, strings)) }, onClick = { selectedType = code; typeExpanded = false })
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddDialog = false }) { Text(strings.cancel) }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val cap = capacity.toIntOrNull()
                                when {
                                    roomCode.isBlank() -> Toast.makeText(context, strings.classroomRoomCodeEmpty, Toast.LENGTH_SHORT).show()
                                    cap == null || cap < 1 -> Toast.makeText(context, strings.classroomInvalidCapacity, Toast.LENGTH_SHORT).show()
                                    else -> viewModel.create(
                                        roomCode = roomCode, capacity = cap, type = selectedType,
                                        onSuccess = {
                                            Toast.makeText(context, strings.classroomAdded, Toast.LENGTH_SHORT).show()
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
                            else Text(strings.add)
                        }
                    }
                }
            }
        }
    }

    // Mevcut sınıf kodlarını burada hesapla — hem preview hem buton kullanır
    val existingClassroomCodes = classrooms.map { it.room_code.uppercase() }.toSet()

    if (showImportPreview) {
        val duplicateCount = if (!isExcelPreview)
            previewItems.count { (code, _, _) -> code.uppercase() in existingClassroomCodes } else 0
        val newCount = previewItems.size - duplicateCount

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
                    Text("İçe Aktarma Önizlemesi", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(4.dp))

                    if (isExcelPreview) {
                        Text(
                            "Excel dosyası seçildi. İçe aktarmak istiyor musunuz?",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "$newCount yeni",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (duplicateCount > 0) {
                                Text(" · ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "$duplicateCount zaten mevcut (atlanır)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        if (duplicateCount > 0) {
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "⚠ Kırmızı satırlar sistemde zaten var — otomatik atlanır. Silebilir veya düzenleyebilirsiniz.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Oda Kodu", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Kapasite", Modifier.weight(0.55f), fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Tür", Modifier.weight(1.1f), fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(56.dp))
                        }
                        Spacer(Modifier.height(2.dp))
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            itemsIndexed(previewItems) { index, item ->
                                val (code, cap, type) = item
                                val isDuplicate = code.uppercase() in existingClassroomCodes
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isDuplicate) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                                            else Color.Transparent
                                        )
                                        .padding(start = 10.dp, end = 2.dp, top = 3.dp, bottom = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        code, Modifier.weight(1f), fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDuplicate) MaterialTheme.colorScheme.error
                                                else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(cap.toString(), Modifier.weight(0.55f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(classroomTypeLabel(type, strings), Modifier.weight(1.1f), fontSize = 11.sp, color = classroomTypeColor(type))
                                    IconButton(onClick = {
                                        editRowIdx = index; editRowCode = code
                                        editRowCap = cap.toString(); editRowType = type
                                        showEditRow = true
                                    }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = {
                                        previewItems = previewItems.toMutableList().also { it.removeAt(index) }
                                    }, modifier = Modifier.size(28.dp)) {
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
                            onClick = {
                                showImportPreview = false; pendingImportPart = null
                                previewItems = emptyList(); importError = ""; importLoading = false
                            },
                            enabled = !importLoading
                        ) { Text(strings.cancel) }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                importError = ""
                                if (isExcelPreview) {
                                    val part = pendingImportPart ?: return@Button
                                    importLoading = true
                                    viewModel.bulkImport(
                                        filePart = part,
                                        onSuccess = { count ->
                                            importLoading = false; showImportPreview = false
                                            previewItems = emptyList(); pendingImportPart = null
                                            Toast.makeText(context, "✓ $count sınıf içe aktarıldı.", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { err -> importLoading = false; importError = err }
                                    )
                                } else {
                                    // Duplicate'leri filtrele, sadece yeni olanları gönder
                                    val newItems = previewItems.filter { (code, _, _) ->
                                        code.uppercase() !in existingClassroomCodes
                                    }
                                    if (newItems.isEmpty()) {
                                        importError = "Tüm sınıflar zaten sistemde mevcut."
                                        return@Button
                                    }
                                    val csv = newItems.joinToString("\n") { (code, cap, type) -> "$code,$cap,$type" }
                                    val bytes = csv.toByteArray(Charsets.UTF_8)
                                    val filteredPart = MultipartBody.Part.createFormData(
                                        "file", "classrooms.txt",
                                        bytes.toRequestBody("text/plain".toMediaTypeOrNull())
                                    )
                                    importLoading = true
                                    viewModel.bulkImport(
                                        filePart = filteredPart,
                                        onSuccess = { count ->
                                            importLoading = false; showImportPreview = false
                                            previewItems = emptyList(); pendingImportPart = null
                                            Toast.makeText(context, "✓ $count yeni sınıf eklendi.", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { err -> importLoading = false; importError = err }
                                    )
                                }
                            },
                            enabled = !importLoading && (isExcelPreview || newCount > 0)
                        ) {
                            if (importLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text(if (isExcelPreview) "İçe Aktar" else "İçe Aktar ($newCount yeni)")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditClassroomDialog(
    room: ClassroomItem,
    isSubmitting: Boolean,
    onSave: (String, Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var code by remember { mutableStateOf(room.room_code) }
    var cap  by remember { mutableStateOf(room.capacity.toString()) }
    var type by remember { mutableStateOf(room.classroom_type) }
    var typeExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
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
                Text("Sınıfı Düzenle", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text(strings.classroomRoomCode) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = cap,
                    onValueChange = { if (it.all { c -> c.isDigit() }) cap = it },
                    label = { Text(strings.classroomCapacity) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = cap.isNotBlank() && (cap.toIntOrNull() ?: 0) < 1
                )
                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
                    OutlinedTextField(
                        value = classroomTypeLabel(type, strings),
                        onValueChange = {}, readOnly = true, label = { Text(strings.classroomType) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        CLASSROOM_TYPE_CODES.forEach { c ->
                            DropdownMenuItem(text = { Text(classroomTypeLabel(c, strings)) }, onClick = { type = c; typeExpanded = false })
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text(strings.cancel) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val capInt = cap.toIntOrNull() ?: return@Button
                            if (code.isBlank() || capInt < 1) return@Button
                            onSave(code, capInt, type)
                        },
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Text(strings.save)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditClassroomRowDialog(
    code: String, cap: String, type: String,
    onSave: (String, Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var editCode by remember { mutableStateOf(code) }
    var editCap  by remember { mutableStateOf(cap) }
    var editType by remember { mutableStateOf(type) }
    var typeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Satırı Düzenle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = editCode,
                    onValueChange = { editCode = it.uppercase() },
                    label = { Text(strings.classroomRoomCode) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = editCap,
                    onValueChange = { if (it.all { c -> c.isDigit() }) editCap = it },
                    label = { Text(strings.classroomCapacity) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
                    OutlinedTextField(
                        value = classroomTypeLabel(editType, strings),
                        onValueChange = {}, readOnly = true,
                        label = { Text(strings.classroomType) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        CLASSROOM_TYPE_CODES.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(classroomTypeLabel(c, strings)) },
                                onClick = { editType = c; typeExpanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val capInt = editCap.toIntOrNull() ?: return@Button
                if (editCode.isBlank()) return@Button
                onSave(editCode, capInt, editType)
            }) { Text(strings.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

private fun parseClassroomText(bytes: ByteArray): List<Triple<String, Int, String>> {
    val typeMap = mapOf(
        "LECTURE" to "LECTURE", "DERSLİK" to "LECTURE", "DERSLIK" to "LECTURE",
        "LAB" to "LAB", "LABORATORY" to "LAB", "LABORATUVAR" to "LAB",
        "COMPUTER_LAB" to "COMPUTER_LAB", "BİLGİSAYAR" to "COMPUTER_LAB", "BILGISAYAR" to "COMPUTER_LAB",
        "SEMINAR" to "SEMINAR", "SEMİNER" to "SEMINAR"
    )
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
            val roomCode = cols[0].uppercase()
            if (roomCode.isBlank()) return@mapNotNull null
            val cap = cols[1].toIntOrNull() ?: return@mapNotNull null
            val rawType = if (cols.size >= 3) cols[2].uppercase() else ""
            val clsType = typeMap[rawType] ?: if ("LAB" in roomCode) "LAB" else "LECTURE"
            Triple(roomCode, cap, clsType)
        }
}

private fun classroomTypeLabel(code: String, strings: com.bugra.campussync.utils.AppStrings): String = when (code) {
    "LECTURE"      -> strings.classroomTypeLecture
    "LAB"          -> strings.classroomTypeLab
    "COMPUTER_LAB" -> strings.classroomTypeComputerLab
    "SEMINAR"      -> strings.classroomTypeSeminar
    else           -> strings.other
}

@Composable
private fun ClassroomSummaryRow(classrooms: List<ClassroomItem>) {
    val strings = LocalAppStrings.current
    val counts = classrooms.groupBy { it.classroom_type }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val typeCodes = listOf("LECTURE", "LAB", "COMPUTER_LAB", "SEMINAR")
        typeCodes.forEach { type ->
            val count = counts[type]?.size ?: 0
            if (count > 0 || type == "LECTURE") {
                val label = when (type) {
                    "LECTURE"      -> strings.classroomTypeLecture
                    "LAB"          -> strings.classroomTypeLab
                    "COMPUTER_LAB" -> strings.classroomTypeComputerLabShort
                    else           -> strings.classroomTypeSeminar
                }
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
private fun ClassroomCard(
    room: ClassroomItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    val typeColor = classroomTypeColor(room.classroom_type)
    val typeLabel = room.classroom_type_display ?: classroomTypeLabel(room.classroom_type, strings)

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = typeColor.copy(alpha = 0.12f), shape = MaterialTheme.shapes.medium) {
                Icon(
                    classroomTypeIcon(room.classroom_type), null,
                    modifier = Modifier.size(44.dp).padding(10.dp), tint = typeColor
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(room.room_code, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("$typeLabel · ${room.capacity} ${strings.people}", fontSize = 13.sp, color = Color.Gray)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
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
