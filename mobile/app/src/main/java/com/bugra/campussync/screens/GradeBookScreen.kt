package com.bugra.campussync.screens

import android.widget.Toast
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
import com.bugra.campussync.ui.components.GradeCardItem
import com.bugra.campussync.utils.LocalAppStrings
import com.bugra.campussync.utils.TokenManager
import com.bugra.campussync.viewmodels.GradeBookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeBookScreen(
    viewModel: GradeBookViewModel = hiltViewModel()
) {
    val context      = LocalContext.current
    val strings      = LocalAppStrings.current
    val tokenManager = remember { TokenManager(context) }
    val role         = (tokenManager.getRole() ?: "").uppercase()
    val isStudent    = role == "STUDENT"

    val state by viewModel.state.collectAsState()
    val grades = state.grades
    val courses = state.courses
    val selectedCourseId = state.selectedCourseId
    val classAverage = state.classAverage
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

    val GRADE_TYPES = listOf(
        "MIDTERM" to strings.gradesMidterm,
        "FINAL"   to strings.gradesFinal,
        "QUIZ"    to strings.gradesQuiz,
        "HW"      to strings.gradesHW,
        "LAB"     to strings.gradesLab,
        "OTHER"   to strings.gradesOther
    )

    LaunchedEffect(Unit) {
        if (!isStudent) viewModel.loadCourses()
        viewModel.loadGrades(isStudent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isStudent) strings.gradesMyTitle else strings.gradesBookTitle, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (!isStudent && selectedCourseId != null) {
                FloatingActionButton(onClick = { showAddGrade = true }, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.Add, strings.gradesAdd, tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!isStudent && courses.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = courseExpanded,
                    onExpandedChange = { courseExpanded = !courseExpanded },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = courses.find { it.id == selectedCourseId }?.let { "${it.course_code} – ${it.course_name}" } ?: strings.gradesSelectCourse,
                        onValueChange = {}, readOnly = true, label = { Text(strings.courseLabel) },
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

            if (classAverage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isStudent) strings.gradesOverallAvg else "Sınıf Ortalaması", fontWeight = FontWeight.Medium)
                        Text("%.1f%%".format(classAverage), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
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
                            Text(if (isStudent) strings.gradesNone else strings.gradesNoneForCourse, color = Color.Gray)
                        }
                    }
                !isStudent && selectedCourseId == null ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(strings.gradesSelectCourseHint, color = Color.Gray)
                    }
                else -> {
                    val grouped = if (isStudent) grades.groupBy { it.course_code } else mapOf("" to grades)
                    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                        grouped.forEach { (courseCode, courseGrades) ->
                            if (isStudent && courseCode.isNotBlank()) {
                                item {
                                    Text(courseCode, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                                }
                            }
                            items(courseGrades, key = { it.id }) { grade ->
                                GradeCardItem(
                                    grade = grade,
                                    isStudent = isStudent,
                                    onDelete = if (!isStudent) {
                                        { viewModel.deleteGrade(grade.id, isStudent) { Toast.makeText(context, strings.gradesDeleteFailed, Toast.LENGTH_SHORT).show() } }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddGrade && selectedCourseId != null) {
        Dialog(
            onDismissRequest = { if (!isSubmitting) showAddGrade = false },
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
                    Text(strings.gradesAdd, style = MaterialTheme.typography.headlineSmall)
                    OutlinedTextField(value = gradeStudent, onValueChange = { gradeStudent = it }, label = { Text(strings.gradesStudentUsername) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    ExposedDropdownMenuBox(expanded = gradeTypeExp, onExpandedChange = { gradeTypeExp = !gradeTypeExp }) {
                        OutlinedTextField(
                            value = GRADE_TYPES.find { it.first == gradeType }?.second ?: gradeType,
                            onValueChange = {}, readOnly = true, label = { Text(strings.gradesType) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeTypeExp) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = gradeTypeExp, onDismissRequest = { gradeTypeExp = false }) {
                            GRADE_TYPES.forEach { (code, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { gradeType = code; gradeTypeExp = false }) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = gradeScore, onValueChange = { gradeScore = it }, label = { Text(strings.gradesScore) }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = gradeMax, onValueChange = { gradeMax = it }, label = { Text(strings.gradesMax) }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    OutlinedTextField(value = gradeNotes, onValueChange = { gradeNotes = it }, label = { Text(strings.gradesNote) }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddGrade = false }) { Text(strings.cancel) }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val score = gradeScore.toDoubleOrNull()
                                val max = gradeMax.toDoubleOrNull() ?: 100.0
                                if (gradeStudent.isBlank() || score == null) { Toast.makeText(context, strings.gradesRequired, Toast.LENGTH_SHORT).show(); return@Button }
                                viewModel.addGrade(
                                    studentUsername = gradeStudent,
                                    courseId = selectedCourseId!!,
                                    gradeType = gradeType,
                                    score = score,
                                    maxScore = max,
                                    notes = gradeNotes,
                                    onSuccess = {
                                        showAddGrade = false; gradeStudent = ""; gradeScore = ""; gradeNotes = ""
                                        Toast.makeText(context, strings.gradesAdded, Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                                )
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
}
