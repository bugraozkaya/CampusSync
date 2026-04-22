package com.bugra.campussync.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
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
import com.bugra.campussync.network.EnrollmentItem
import com.bugra.campussync.network.ScheduleItem
import com.bugra.campussync.utils.TokenManager
import com.bugra.campussync.viewmodels.StudentHomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen() {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    val viewModel: StudentHomeViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val enrollments = state.enrollments
    val schedule = state.schedule
    val allCourses = state.allCourses
    val isLoading = state.isLoading

    var showEnrollDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    val firstName = tokenManager.getFirstName() ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Merhaba${if (firstName.isNotBlank()) ", $firstName" else ""}!", fontWeight = FontWeight.Bold)
                        Text("Öğrenci Paneli", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        viewModel.loadCourses()
                        showEnrollDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Derse Kaydol", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Book, null, modifier = Modifier.size(18.dp)) },
                    text = { Text("Derslerim") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.DateRange, null, modifier = Modifier.size(18.dp)) },
                    text = { Text("Program") }
                )
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else when (selectedTab) {
                0 -> EnrollmentsTab(
                    enrollments = enrollments,
                    onUnenroll = { id ->
                        viewModel.unenroll(id) {
                            Toast.makeText(context, "Kayıt silinemedi.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                1 -> ScheduleTab(schedule = schedule)
            }
        }
    }

    if (showEnrollDialog) {
        val enrolledCourseIds = enrollments.map { it.course }.toSet()
        val availableCourses = allCourses.filter { it.id !in enrolledCourseIds }
        EnrollDialog(
            courses = availableCourses,
            onDismiss = { showEnrollDialog = false },
            onEnroll = { courseId ->
                viewModel.enroll(
                    courseId = courseId,
                    onSuccess = {
                        showEnrollDialog = false
                        Toast.makeText(context, "Derse kaydolundu.", Toast.LENGTH_SHORT).show()
                    },
                    onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                )
            }
        )
    }
}

@Composable
private fun EnrollmentsTab(
    enrollments: List<EnrollmentItem>,
    onUnenroll: (Int) -> Unit
) {
    if (enrollments.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Book, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                Spacer(Modifier.height(12.dp))
                Text("Kayıtlı ders yok.", color = Color.Gray)
                Text("+ ile derse kaydolun.", color = Color.LightGray, fontSize = 13.sp)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(enrollments, key = { it.id }) { enrollment ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(enrollment.course_code, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(enrollment.course_name, fontSize = 13.sp, color = Color.Gray)
                            Text(enrollment.department_name, fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onUnenroll(enrollment.id) }) {
                            Icon(Icons.Default.Delete, null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

private val DAYS = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma")

@Composable
private fun ScheduleTab(schedule: List<ScheduleItem>) {
    if (schedule.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Ders programı bulunamadı.", color = Color.Gray)
        }
        return
    }
    val byDay = schedule.groupBy { it.day }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DAYS.forEach { day ->
            val slots = byDay[day] ?: return@forEach
            item {
                Text(day, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp))
            }
            items(slots, key = { it.id }) { slot ->
                Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = if (slot.session_type == "LAB")
                                MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "${slot.start_time.take(5)}\n${slot.end_time.take(5)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = if (slot.session_type == "LAB")
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "${slot.course_code ?: ""}${if (slot.session_type == "LAB") " LAB" else ""}",
                                fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                            )
                            Text(slot.course_name, fontSize = 12.sp, color = Color.Gray)
                            Text(
                                text = listOfNotNull(slot.classroom_name, slot.lecturer_name)
                                    .joinToString(" · "),
                                fontSize = 11.sp, color = Color.LightGray
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnrollDialog(
    courses: List<CourseItem>,
    onDismiss: () -> Unit,
    onEnroll: (Int) -> Unit
) {
    var selectedCourseId by remember { mutableStateOf<Int?>(null) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Derse Kaydol") },
        text = {
            Column {
                if (courses.isEmpty()) {
                    Text("Kayıt olabileceğiniz ders bulunamadı.", color = Color.Gray)
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = courses.find { it.id == selectedCourseId }
                                ?.let { "${it.course_code} – ${it.course_name}" } ?: "Ders seçin…",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Ders") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            courses.forEach { course ->
                                DropdownMenuItem(
                                    text = { Text("${course.course_code} – ${course.course_name}") },
                                    onClick = { selectedCourseId = course.id; expanded = false }
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
            ) { Text("Kaydol") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}
