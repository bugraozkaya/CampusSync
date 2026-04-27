package com.bugra.campussync.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.bugra.campussync.utils.LocalAppStrings
import com.bugra.campussync.viewmodels.DataViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(onNavigateToCalendar: () -> Unit) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current

    val viewModel: DataViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val importResults = state.lecturers
    val isUploading = state.isUploading
    val isFetchingExisting = state.isLoading
    val isSubmittingLecturer = state.isSubmittingLecturer
    val isSubmittingCourse = state.isSubmittingCourse
    val isSubmittingStudent = state.isSubmittingStudent
    val passwordCache = viewModel.passwordCache

    var selectedExcelUri by remember { mutableStateOf<Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> if (uri != null) selectedExcelUri = uri }

    var showManualDialog by remember { mutableStateOf(false) }
    var showCourseDialog by remember { mutableStateOf(false) }
    var showStudentDialog by remember { mutableStateOf(false) }
    var manualName by remember { mutableStateOf("") }
    var manualSurname by remember { mutableStateOf("") }
    var manualDept by remember { mutableStateOf("") }
    var courseName by remember { mutableStateOf("") }
    var courseCode by remember { mutableStateOf("") }
    var studentName by remember { mutableStateOf("") }
    var studentSurname by remember { mutableStateOf("") }
    var studentNumber by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            if (selectedExcelUri == null) {
                Column(horizontalAlignment = Alignment.End) {
                    SmallFloatingActionButton(
                        onClick = { showCourseDialog = true },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) { Icon(Icons.Default.Add, "Ders Ekle") }
                    SmallFloatingActionButton(
                        onClick = { showStudentDialog = true },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) { Icon(Icons.Default.School, "Öğrenci Ekle") }
                    SmallFloatingActionButton(
                        onClick = { showManualDialog = true },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) { Icon(Icons.Default.PersonAdd, "Hoca Ekle") }
                    FloatingActionButton(
                        onClick = {
                            filePickerLauncher.launch(
                                arrayOf(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "application/vnd.ms-excel",
                                    "text/plain"
                                )
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) { Icon(Icons.Default.Add, "Excel Yükle", tint = Color.White) }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            // -- Excel yükleme modu --
            if (selectedExcelUri != null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(strings.dataFileSelected, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = selectedExcelUri?.lastPathSegment ?: "",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            val uri = selectedExcelUri ?: return@Button
                            val inputStream = context.contentResolver.openInputStream(uri)
                            val bytes = inputStream?.readBytes()
                            inputStream?.close()
                            if (bytes != null) {
                                var fileName = "data.txt"
                                context.contentResolver.query(uri, null, null, null, null)?.use {
                                    if (it.moveToFirst()) {
                                        val col = it.getColumnIndex("_display_name")
                                        if (col != -1) fileName = it.getString(col)
                                    }
                                }
                                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                                val filePart = MultipartBody.Part.createFormData("file", fileName, requestBody)
                                viewModel.bulkImport(
                                    filePart = filePart,
                                    onSuccess = { count ->
                                        Toast.makeText(context, "✓ $count kayıt başarıyla yüklendi!", Toast.LENGTH_SHORT).show()
                                        selectedExcelUri = null
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        enabled = !isUploading,
                        modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(strings.uploading)
                        } else {
                            Text(strings.dataProcessToSystem, fontSize = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { selectedExcelUri = null }) { Text(strings.cancel) }
                }

            } else {
                // -- Hoca listesi modu --
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
                        // Boş durum
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Group,
                                    null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    strings.dataNoLecturers,
                                    color = Color.Gray,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    strings.dataAddHint,
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
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
                                        Text(
                                            text = res.lecturer,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = res.course,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 13.sp
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    "👤 ${res.generatedUser}",
                                                    fontSize = 12.sp,
                                                    color = Color.DarkGray
                                                )
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
                                            OutlinedButton(
                                                onClick = onNavigateToCalendar,
                                                modifier = Modifier.height(32.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
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

        // -- Ders Ekle Dialog --
        if (showCourseDialog) {
            AlertDialog(
                onDismissRequest = { if (!isSubmittingCourse) showCourseDialog = false },
                title = { Text(strings.dataAddCourse) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = courseName,
                            onValueChange = { courseName = it },
                            label = { Text(strings.dataCourseName) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = courseCode,
                            onValueChange = { courseCode = it },
                            label = { Text(strings.dataCourseCode) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (courseName.isBlank()) {
                                Toast.makeText(context, strings.dataCourseNameEmpty, Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (courseCode.isBlank()) {
                                Toast.makeText(context, strings.dataCourseCodeEmpty, Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.createCourse(
                                name = courseName, code = courseCode,
                                onSuccess = {
                                    Toast.makeText(context, strings.dataCourseAdded, Toast.LENGTH_SHORT).show()
                                    showCourseDialog = false
                                    courseName = ""; courseCode = ""
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        enabled = !isSubmittingCourse
                    ) {
                        if (isSubmittingCourse) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Text(strings.save)
                    }
                },
                dismissButton = { TextButton(onClick = { showCourseDialog = false }) { Text(strings.cancel) } }
            )
        }

        // -- Hoca Ekle Dialog --
        if (showManualDialog) {
            AlertDialog(
                onDismissRequest = { if (!isSubmittingLecturer) showManualDialog = false },
                title = { Text(strings.dataAddLecturer) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = manualName,
                            onValueChange = { manualName = it },
                            label = { Text(strings.settingsFirstName) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = manualSurname,
                            onValueChange = { manualSurname = it },
                            label = { Text(strings.settingsLastName) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = manualDept,
                            onValueChange = { manualDept = it },
                            label = { Text(strings.dataDepartment) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = manualDept.isBlank() && manualName.isNotBlank()
                        )
                        if (manualName.isNotEmpty() && manualSurname.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${strings.loginUsername}: ${viewModel.generateUsername(manualName, manualSurname)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            when {
                                manualName.isBlank() ->
                                    Toast.makeText(context, strings.dataFirstNameEmpty, Toast.LENGTH_SHORT).show()
                                manualSurname.isBlank() ->
                                    Toast.makeText(context, strings.dataLastNameEmpty, Toast.LENGTH_SHORT).show()
                                manualDept.isBlank() ->
                                    Toast.makeText(context, strings.dataDepartmentEmpty, Toast.LENGTH_SHORT).show()
                                else -> {
                                    viewModel.createLecturer(
                                        firstName = manualName,
                                        lastName = manualSurname,
                                        department = manualDept,
                                        onSuccess = { _, password ->
                                            Toast.makeText(context, "${strings.dataLecturerAdded}$password", Toast.LENGTH_LONG).show()
                                            showManualDialog = false
                                            manualName = ""; manualSurname = ""; manualDept = ""
                                        },
                                        onError = { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            }
                        },
                        enabled = !isSubmittingLecturer
                    ) {
                        if (isSubmittingLecturer) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Text(strings.save)
                    }
                },
                dismissButton = { TextButton(onClick = { showManualDialog = false }) { Text(strings.cancel) } }
            )
        }

        // -- Student Add Dialog --
        if (showStudentDialog) {
            AlertDialog(
                onDismissRequest = { if (!isSubmittingStudent) showStudentDialog = false },
                title = { Text(strings.dataAddStudent) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = studentName, onValueChange = { studentName = it }, label = { Text(strings.settingsFirstName) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = studentSurname, onValueChange = { studentSurname = it }, label = { Text(strings.settingsLastName) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(
                            value = studentNumber, onValueChange = { studentNumber = it },
                            label = { Text(strings.dataStudentNo) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            placeholder = { Text("örn: 20230001") }
                        )
                        if (studentName.isNotEmpty() && studentSurname.isNotEmpty()) {
                            val previewUsername = if (studentNumber.isNotBlank()) studentNumber
                                                  else viewModel.generateUsername(studentName, studentSurname)
                            Text("${strings.loginUsername}: $previewUsername", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            when {
                                studentName.isBlank() -> Toast.makeText(context, strings.dataFirstNameEmpty, Toast.LENGTH_SHORT).show()
                                studentSurname.isBlank() -> Toast.makeText(context, strings.dataLastNameEmpty, Toast.LENGTH_SHORT).show()
                                else -> viewModel.createStudent(
                                    firstName = studentName, lastName = studentSurname, studentNumber = studentNumber,
                                    onSuccess = { username, password ->
                                        Toast.makeText(context, "${strings.dataStudentAdded}\n${strings.loginUsername}: $username\n${strings.loginPassword}: $password", Toast.LENGTH_LONG).show()
                                        showStudentDialog = false
                                        studentName = ""; studentSurname = ""; studentNumber = ""
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
                },
                dismissButton = { TextButton(onClick = { showStudentDialog = false }) { Text(strings.cancel) } }
            )
        }
    }
}
