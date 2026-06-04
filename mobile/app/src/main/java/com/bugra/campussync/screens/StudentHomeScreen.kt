package com.bugra.campussync.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bugra.campussync.utils.LocalAppStrings
import com.bugra.campussync.viewmodels.StudentHomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    onNavigateToCourseDetail: (Int, String, String) -> Unit,
    viewModel: StudentHomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val state by viewModel.state.collectAsState()
    val myCourses = state.enrollments
    val availableCourses = state.availableCourses
    val isLoading = state.isLoading

    var showEnrollDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadEnrollments()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.studentPanel, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (!isLoading) {
                FloatingActionButton(
                    onClick = {
                        viewModel.loadAvailableCourses()
                        showEnrollDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = strings.studentEnroll, tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading && myCourses.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (myCourses.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Book, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(Modifier.height(16.dp))
                        Text(strings.studentNoCourses, color = Color.Gray)
                        Text(strings.studentEnrollHint, color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                Text(
                    strings.studentMyCourses,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(myCourses, key = { it.id }) { enrollment ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onNavigateToCourseDetail(
                                        enrollment.course,
                                        enrollment.course_name,
                                        enrollment.course_code
                                    )
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.School, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(enrollment.course_code, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(enrollment.course_name, fontSize = 16.sp)
                                    Text(enrollment.department_name, fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEnrollDialog) {
        EnrollDialog(
            courses = availableCourses,
            onDismiss = { showEnrollDialog = false },
            onEnroll = { courseId ->
                viewModel.enroll(courseId, 
                    onSuccess = {
                        showEnrollDialog = false
                        Toast.makeText(context, strings.studentEnrolled, Toast.LENGTH_SHORT).show()
                    },
                    onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnrollDialog(
    courses: List<com.bugra.campussync.network.CourseItem>,
    onDismiss: () -> Unit,
    onEnroll: (Int) -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedCourseId by remember { mutableStateOf<Int?>(null) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.studentEnroll) },
        text = {
            Column {
                if (courses.isEmpty()) {
                    Text(strings.studentNoAvailableCourses)
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = courses.find { it.id == selectedCourseId }?.let { "${it.course_code} - ${it.course_name}" } ?: strings.studentSelectCourse,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            courses.forEach { course ->
                                DropdownMenuItem(
                                    text = { Text("${course.course_code} - ${course.course_name}") },
                                    onClick = {
                                        selectedCourseId = course.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedCourseId?.let { onEnroll(it) } },
                enabled = selectedCourseId != null
            ) { Text(strings.studentEnrollButton) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
