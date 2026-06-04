package com.bugra.campussync.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bugra.campussync.utils.LocalAppStrings
import com.bugra.campussync.utils.TokenManager
import com.bugra.campussync.viewmodels.HomeViewModel
import com.bugra.campussync.viewmodels.StudentHomeViewModel

// Move data class to the top level for better visibility
data class CourseListItem(val id: Int, val name: String, val code: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseContentScreen(
    onNavigateToCourseDetail: (Int, String, String) -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
    studentViewModel: StudentHomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val tokenManager = remember { TokenManager(context) }
    val userRole = remember { tokenManager.getRole()?.uppercase() ?: "" }
    val isLecturer = userRole.contains("LECTURER") || userRole.contains("STAFF")

    val homeState by homeViewModel.state.collectAsState()
    val studentState by studentViewModel.state.collectAsState()

    val isLoading = if (isLecturer) homeState.isLoading else studentState.isLoading
    val error = if (isLecturer) homeState.error else studentState.error
    
    // Use explicit type to avoid inference issues
    val courses: List<CourseListItem> = remember(isLecturer, homeState.schedules, studentState.enrollments) {
        if (isLecturer) {
            homeState.schedules.map { 
                CourseListItem(it.courseId, it.course_name, it.course_code ?: "") 
            }.distinctBy { it.id }
        } else {
            studentState.enrollments.map { 
                CourseListItem(it.course, it.course_name, it.course_code) 
            }
        }
    }

    val loadData = {
        if (isLecturer) homeViewModel.load(isAdmin = false)
        else studentViewModel.loadEnrollments()
    }

    LaunchedEffect(Unit) { loadData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.navCourseContent, fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                isLoading && courses.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null && courses.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(48.dp), tint = Color.Red)
                            Spacer(Modifier.height(12.dp))
                            Text(error!!, textAlign = TextAlign.Center, color = Color.Gray)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { loadData() }) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(strings.retry)
                            }
                        }
                    }
                }
                courses.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Book, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                            Spacer(Modifier.height(16.dp))
                            Text(strings.courseContentNone, color = Color.Gray)
                            Text(strings.courseContentNoneHint, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                    ) {
                        items(courses, key = { it.id }) { course ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    onNavigateToCourseDetail(course.id, course.name, course.code)
                                },
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.School, null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(course.code, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(course.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
