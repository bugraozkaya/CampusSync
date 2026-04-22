package com.bugra.campussync.screens

import android.widget.Toast
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
import com.bugra.campussync.network.GradeItem
import com.bugra.campussync.utils.TokenManager
import com.bugra.campussync.viewmodels.GradeBookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeBookScreen() {
    val context      = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val role         = (tokenManager.getRole() ?: "").uppercase()
    val isStudent    = role == "STUDENT"

    val viewModel: GradeBookViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val grades = state.grades
    val courses = state.courses
    val selectedCourseId = state.selectedCourseId
    val isLoading = state.isLoading
    val isSubmitting = state.isSubmitting

    var showAddGrade by remember { mutableStateOf(false) }
    var courseExpanded by remember { mutableStateOf(false) }

    // Add grade dialog state
    var gradeStudent  by remember { mutableStateOf("") }
    var gradeType     by remember { mutableStateOf("MIDTERM") }
    var gradeScore    by remember { mutableStateOf("") }
    var gradeMax      by remember { mutableStateOf("100") }
    var gradeNotes    by remember { mutableStateOf("") }
    var gradeTypeExp  by remember { mutableStateOf(false) }

    val GRADE_TYPES = listOf("MIDTERM" to "Vize", "FINAL" to "Final", "QUIZ" to "Quiz", "HW" to "Ödev", "LAB" to "Lab", "OTHER" to "Diğer")

    LaunchedEffect(Unit) {
        if (!isStudent) viewModel.loadCourses()
        viewModel.loadGrades(isStudent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isStudent) "Notlarım" else "Not Defteri", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (!isStudent && selectedCourseId != null) {
                FloatingActionButton(onClick = { showAddGrade = true }, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.Add, "Not Ekle", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Course picker for lecturers
            if (!isStudent && courses.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = courseExpanded,
                    onExpandedChange = { courseExpanded = !courseExpanded },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = courses.find { it.id == selectedCourseId }?.let { "${it.course_code} – ${it.course_name}" } ?: "Ders Seçin",
                        onValueChange = {}, readOnly = true, label = { Text("Ders") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = courseExpanded, onDismissRequest = { courseExpanded = false }) {
                        courses.forEach { c ->
                            DropdownMenuItem(text = { Text("${c.course_code} – ${c.course_name}") }, onClick = { viewModel.selectCourse(c.id); courseExpanded = false })
                        }
                    }
                }
            }

            // Summary for student
            if (isStudent && grades.isNotEmpty()) {
                val avg = grades.map { it.percentage }.average()
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Genel Ortalama", fontWeight = FontWeight.Medium)
                        Text("%.1f%%".format(avg), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                grades.isEmpty() && (isStudent || selectedCourseId != null) ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Grade, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                            Spacer(Modifier.height(8.dp))
                            Text(if (isStudent) "Henüz notunuz yok." else "Bu ders için not girilmemiş.", color = Color.Gray)
                        }
                    }
                !isStudent && selectedCourseId == null ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Yukarıdan bir ders seçin.", color = Color.Gray)
                    }
                else -> {
                    // Group by course for students
                    val grouped = if (isStudent) grades.groupBy { it.course_code } else mapOf("" to grades)
                    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                        grouped.forEach { (courseCode, courseGrades) ->
                            if (isStudent && courseCode.isNotBlank()) {
                                item {
                                    Text(courseCode, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                                }
                            }
                            items(courseGrades, key = { it.id }) { grade ->
                                GradeCard(grade = grade, isStudent = isStudent, onDelete = if (!isStudent) {
                                    { viewModel.deleteGrade(grade.id, isStudent) { Toast.makeText(context, "Silinemedi.", Toast.LENGTH_SHORT).show() } }
                                } else null)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddGrade && selectedCourseId != null) {
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showAddGrade = false },
            title = { Text("Not Ekle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = gradeStudent, onValueChange = { gradeStudent = it }, label = { Text("Öğrenci Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    ExposedDropdownMenuBox(expanded = gradeTypeExp, onExpandedChange = { gradeTypeExp = !gradeTypeExp }) {
                        OutlinedTextField(
                            value = GRADE_TYPES.find { it.first == gradeType }?.second ?: gradeType,
                            onValueChange = {}, readOnly = true, label = { Text("Not Türü") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeTypeExp) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = gradeTypeExp, onDismissRequest = { gradeTypeExp = false }) {
                            GRADE_TYPES.forEach { (code, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { gradeType = code; gradeTypeExp = false }) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = gradeScore, onValueChange = { gradeScore = it }, label = { Text("Puan") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = gradeMax, onValueChange = { gradeMax = it }, label = { Text("Maks") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    OutlinedTextField(value = gradeNotes, onValueChange = { gradeNotes = it }, label = { Text("Not (opsiyonel)") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val score = gradeScore.toDoubleOrNull()
                        val max = gradeMax.toDoubleOrNull() ?: 100.0
                        if (gradeStudent.isBlank() || score == null) { Toast.makeText(context, "Kullanıcı adı ve puan zorunlu.", Toast.LENGTH_SHORT).show(); return@Button }
                        viewModel.addGrade(
                            studentUsername = gradeStudent,
                            courseId = selectedCourseId!!,
                            gradeType = gradeType,
                            score = score,
                            maxScore = max,
                            notes = gradeNotes,
                            onSuccess = {
                                showAddGrade = false; gradeStudent = ""; gradeScore = ""; gradeNotes = ""
                                Toast.makeText(context, "Not eklendi.", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                        )
                    },
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Ekle")
                }
            },
            dismissButton = { TextButton(onClick = { showAddGrade = false }) { Text("İptal") } }
        )
    }
}

@Composable
private fun GradeCard(grade: GradeItem, isStudent: Boolean, onDelete: (() -> Unit)?) {
    val pct = grade.percentage
    val color = when {
        pct >= 85 -> MaterialTheme.colorScheme.primary
        pct >= 60 -> MaterialTheme.colorScheme.tertiary
        else      -> MaterialTheme.colorScheme.error
    }
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("%.0f".format(grade.score), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
                    Text("/${grade.max_score.toInt()}", fontSize = 10.sp, color = color.copy(alpha = 0.7f))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(grade.grade_type_display, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                if (!isStudent) Text(grade.student_name.ifBlank { grade.student_username }, fontSize = 12.sp, color = Color.Gray)
                if (isStudent) Text(grade.course_code, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                if (grade.notes.isNotBlank()) Text(grade.notes, fontSize = 11.sp, color = Color.LightGray)
            }
            Surface(color = color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.extraLarge) {
                Text("%.1f%%".format(pct), fontWeight = FontWeight.Bold, color = color, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
            onDelete?.let {
                IconButton(onClick = it, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
