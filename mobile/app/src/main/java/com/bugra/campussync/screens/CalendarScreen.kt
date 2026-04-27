package com.bugra.campussync.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugra.campussync.network.ClassroomItem
import com.bugra.campussync.network.CourseItem
import com.bugra.campussync.utils.LocalAppStrings
import com.bugra.campussync.utils.TokenManager
import com.bugra.campussync.viewmodels.CalendarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen() {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val tokenManager = remember { TokenManager(context) }
    val userRole = tokenManager.getRole() ?: ""
    val isAdmin = userRole.uppercase().let { it.contains("ADMIN") || it.contains("SUPER") || it == "STAFF" || it == "IT" }

    val viewModel: CalendarViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val detailedSchedules = state.detailedSchedules
    val isLoading = state.isLoading
    val isSubmitting = state.isSubmitting
    val lecturers = state.lecturers
    val courses = state.courses
    var availableClassrooms = state.availableClassrooms

    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf("") }
    var selectedSlotName by remember { mutableStateOf("") }
    var selectedCourseId by remember { mutableStateOf("") }
    var selectedLecturerId by remember { mutableStateOf("") }
    var selectedClassroomId by remember { mutableStateOf("") }
    var selectedScheduleId by remember { mutableStateOf<Int?>(null) }
    var courseExpanded by remember { mutableStateOf(false) }
    var lecturerExpanded by remember { mutableStateOf(false) }
    var classroomExpanded by remember { mutableStateOf(false) }
    var sessionType by remember { mutableStateOf("LECTURE") }
    var selectedClassroomName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadSchedules()
        if (isAdmin) viewModel.loadAdminData()
    }

    val days = listOf("MON", "TUE", "WED", "THU", "FRI")
    val dayLabels = listOf(strings.dayMonAbbr, strings.dayTueAbbr, strings.dayWedAbbr, strings.dayThuAbbr, strings.dayFriAbbr)
    val slots = listOf("08:00-10:00", "10:00-12:00", "13:00-15:00", "15:00-17:00")

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isAdmin) strings.calendarTitle else strings.calendarMySchedule,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isAdmin) strings.calendarHint else strings.calendarAssignedCourses,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            if (isAdmin) {
                IconButton(
                    onClick = {
                        viewModel.autoSchedule(
                            onSuccess = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() },
                            onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                        )
                    }
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = strings.calendarAutoSchedule,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(strings.loading, fontSize = 13.sp, color = Color.Gray)
                }
            }
        } else {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Header row
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.width(60.dp))
                    dayLabels.forEach { label ->
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(2.dp))

                slots.forEach { slotName ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Slot label
                        Box(
                            modifier = Modifier.width(60.dp).fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = slotName.replace("-", "\n"),
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 10.sp
                            )
                        }

                        days.forEach { day ->
                            val assignment = detailedSchedules.find {
                                val rawDay = it["day_of_week"]?.toString() ?: ""
                                val st = it["start_time"]?.toString() ?: ""
                                val lecturerUser = it["lecturer_username"]?.toString() ?: ""
                                val currentUsername = tokenManager.getUsername() ?: ""
                                val isVisible = isAdmin || (lecturerUser == currentUsername)
                                normalizeDayOfWeek(rawDay) == day && checkSlot(st, slotName) && isVisible
                            }

                            val hasAssignment = assignment != null
                            val cellSessionType = assignment?.get("session_type")?.toString()
                            val isLab = cellSessionType == "LAB"

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(2.dp)
                                    .border(
                                        width = if (hasAssignment) 1.5.dp else 0.5.dp,
                                        color = when {
                                            isLab -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                                            hasAssignment -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                            else -> Color.LightGray.copy(alpha = 0.4f)
                                        }
                                    )
                                    .background(
                                        when {
                                            isLab -> MaterialTheme.colorScheme.tertiaryContainer
                                            hasAssignment -> MaterialTheme.colorScheme.primaryContainer
                                            else -> MaterialTheme.colorScheme.surface
                                        }
                                    )
                                    .clickable(enabled = isAdmin) {
                                        selectedDay = dayToFullName(day)
                                        selectedSlotName = slotName
                                        selectedCourseId = assignment?.get("course")?.toString() ?: ""
                                        selectedLecturerId = assignment?.get("lecturer")?.toString() ?: ""
                                        selectedClassroomId = assignment?.get("classroom")?.toString() ?: ""
                                        selectedScheduleId = assignment?.get("id")?.toString()?.toIntOrNull()
                                        courseExpanded = false
                                        lecturerExpanded = false
                                        classroomExpanded = false
                                        sessionType = assignment?.get("session_type")?.toString() ?: "LECTURE"
                                        availableClassrooms = emptyList()
                                        selectedClassroomName = assignment?.get("classroom_name")?.toString() ?: ""
                                        showAddDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (hasAssignment) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(2.dp)
                                    ) {
                                        val code = assignment!!["course_code"]?.toString()
                                        if (!code.isNullOrBlank()) {
                                            Surface(
                                                color = if (isLab)
                                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                                else
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                shape = MaterialTheme.shapes.extraSmall
                                            ) {
                                                Text(
                                                    text = if (isLab) "$code 🔬" else code,
                                                    fontSize = 6.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isLab)
                                                        MaterialTheme.colorScheme.tertiary
                                                    else
                                                        MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = assignment["course_name"]?.toString() ?: "",
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 9.sp,
                                            maxLines = 2
                                        )
                                        val classroom = assignment["classroom_name"]?.toString()
                                        if (!classroom.isNullOrBlank()) {
                                            Text(
                                                text = "[$classroom]",
                                                fontSize = 6.5.sp,
                                                color = MaterialTheme.colorScheme.secondary,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        if (isAdmin) {
                                            val lecName = assignment["lecturer_name"]?.toString() ?: ""
                                            if (lecName.isNotBlank()) {
                                                Text(
                                                    text = lecName.split(" ").lastOrNull() ?: lecName,
                                                    fontSize = 6.sp,
                                                    color = Color.DarkGray,
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 8.sp
                                                )
                                            }
                                        }
                                    }
                                } else if (isAdmin) {
                                    Icon(
                                        Icons.Default.Add,
                                        null,
                                        tint = Color.LightGray.copy(alpha = 0.5f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 0.5.dp)
                }

                if (detailedSchedules.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            strings.calendarNoAssignments,
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Available classrooms — re-fetch when slot/course/sessionType changes
    LaunchedEffect(selectedDay, selectedSlotName, selectedCourseId, sessionType) {
        if (selectedDay.isNotBlank() && selectedSlotName.isNotBlank() && selectedCourseId.isNotBlank()) {
            val (st, et) = slotToTimes(selectedSlotName)
            viewModel.loadAvailableClassrooms(selectedDay, st, et, sessionType, selectedCourseId)
        }
    }

    LaunchedEffect(availableClassrooms) {
        if (availableClassrooms.none { it.id.toString() == selectedClassroomId }) {
            selectedClassroomId = ""
            selectedClassroomName = ""
        }
    }

    // Atama/Düzenleme Dialog'u
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isSubmitting) {
                    showAddDialog = false
                    sessionType = "LECTURE"
                    availableClassrooms = emptyList()
                    selectedClassroomId = ""
                    selectedClassroomName = ""
                }
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "$selectedDay — ${slotLabel(selectedSlotName)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (selectedScheduleId != null) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            enabled = !isSubmitting
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = strings.delete,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Course dropdown
                    Text(strings.calendarSelectCourse, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    ExposedDropdownMenuBox(
                        expanded = courseExpanded,
                        onExpandedChange = {
                            courseExpanded = !courseExpanded
                            if (courseExpanded) { lecturerExpanded = false; classroomExpanded = false }
                        }
                    ) {
                        val selCourse = courses.find { it.id.toString() == selectedCourseId }
                        OutlinedTextField(
                            value = selCourse?.let { "${it.course_code} – ${it.course_name}" } ?: strings.calendarSelectCourse,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors()
                        )
                        ExposedDropdownMenu(expanded = courseExpanded, onDismissRequest = { courseExpanded = false }) {
                            if (courses.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(strings.calendarNoCourses, fontSize = 12.sp, color = Color.Gray) },
                                    onClick = {}
                                )
                            }
                            courses.forEach { course ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(course.course_code, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary)
                                            Text(course.course_name, fontSize = 13.sp)
                                            if (course.has_lab) {
                                                Text("🔬 ${strings.calendarHasLab}", fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.tertiary)
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedCourseId = course.id.toString()
                                        courseExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Lecturer dropdown
                    Text(strings.calendarLecturerLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    ExposedDropdownMenuBox(
                        expanded = lecturerExpanded,
                        onExpandedChange = {
                            lecturerExpanded = !lecturerExpanded
                            if (lecturerExpanded) { courseExpanded = false; classroomExpanded = false }
                        }
                    ) {
                        val selLec = lecturers.find { it["id"]?.toString() == selectedLecturerId }
                        val lecText = selLec?.let {
                            val title = it["title"]?.toString()?.trim() ?: ""
                            val name = "${it["first_name"] ?: ""} ${it["last_name"] ?: ""}".trim()
                            if (title.isNotEmpty()) "$title $name" else name
                        } ?: strings.calendarSelectLecturer
                        OutlinedTextField(
                            value = lecText,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lecturerExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors()
                        )
                        ExposedDropdownMenu(expanded = lecturerExpanded, onDismissRequest = { lecturerExpanded = false }) {
                            if (lecturers.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(strings.calendarNoLecturers, fontSize = 12.sp, color = Color.Gray) },
                                    onClick = {}
                                )
                            }
                            lecturers.forEach { lec ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            val title = lec["title"]?.toString()?.trim() ?: ""
                                            val name = "${lec["first_name"] ?: ""} ${lec["last_name"] ?: ""}".trim()
                                            Text(if (title.isNotEmpty()) "$title $name" else name, fontSize = 13.sp)
                                            Text("@${lec["username"] ?: ""}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    },
                                    onClick = {
                                        selectedLecturerId = lec["id"]?.toString() ?: ""
                                        lecturerExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Session type selector (only shown when the selected course has a lab)
                    val selectedCourse = courses.find { it.id.toString() == selectedCourseId }
                    if (selectedCourse?.has_lab == true) {
                        Text(strings.calendarSessionType, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = sessionType == "LECTURE",
                                onClick = { sessionType = "LECTURE" },
                                label = { Text(strings.calendarLecture) }
                            )
                            FilterChip(
                                selected = sessionType == "LAB",
                                onClick = { sessionType = "LAB" },
                                label = { Text(strings.calendarLab) }
                            )
                        }
                    }

                    // Classroom dropdown
                    Text(strings.calendarClassroomLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    ExposedDropdownMenuBox(
                        expanded = classroomExpanded,
                        onExpandedChange = {
                            classroomExpanded = !classroomExpanded
                            if (classroomExpanded) { courseExpanded = false; lecturerExpanded = false }
                        }
                    ) {
                        OutlinedTextField(
                            value = selectedClassroomName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(strings.calendarClassroomLabel) },
                            placeholder = {
                                Text(
                                    if (selectedCourseId.isBlank() || selectedSlotName.isBlank()) strings.calendarSelectSlotFirst
                                    else if (availableClassrooms.isEmpty()) strings.calendarNoRooms
                                    else strings.calendarSelectRoom
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classroomExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors()
                        )
                        ExposedDropdownMenu(
                            expanded = classroomExpanded,
                            onDismissRequest = { classroomExpanded = false }
                        ) {
                            if (availableClassrooms.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (selectedCourseId.isBlank() || selectedSlotName.isBlank())
                                                strings.calendarSelectSlotFirst
                                            else strings.calendarNoRooms,
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    },
                                    onClick = {}
                                )
                            } else {
                                availableClassrooms.forEach { room ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(room.room_code, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                val typeLabel = room.classroom_type_display ?: room.classroom_type
                                                Text("$typeLabel · ${room.capacity} ${strings.people}",
                                                    fontSize = 11.sp, color = Color.Gray)
                                            }
                                        },
                                        onClick = {
                                            selectedClassroomId = room.id.toString()
                                            selectedClassroomName = room.room_code
                                            classroomExpanded = false
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
                    onClick = {
                        if (selectedCourseId.isBlank()) {
                            Toast.makeText(context, strings.calendarPleaseSelectCourse, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (selectedLecturerId.isBlank()) {
                            Toast.makeText(context, strings.calendarPleaseSelectLecturer, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (selectedClassroomId.isBlank()) {
                            Toast.makeText(context, strings.calendarPleaseSelectRoom, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val (st, et) = slotToTimes(selectedSlotName)
                        viewModel.createSchedule(
                            courseId = selectedCourseId,
                            lecturerId = selectedLecturerId,
                            classroomId = selectedClassroomId,
                            day = selectedDay,
                            startTime = st,
                            endTime = et,
                            sessionType = sessionType,
                            existingId = selectedScheduleId,
                            onSuccess = {
                                Toast.makeText(context, strings.calendarAssignmentSaved, Toast.LENGTH_SHORT).show()
                                showAddDialog = false
                                sessionType = "LECTURE"
                                viewModel.clearAvailableClassrooms()
                                selectedClassroomId = ""
                                selectedClassroomName = ""
                            },
                            onError = { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(if (selectedScheduleId != null) strings.update else strings.save)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        sessionType = "LECTURE"
                        availableClassrooms = emptyList()
                        selectedClassroomId = ""
                        selectedClassroomName = ""
                    },
                    enabled = !isSubmitting
                ) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Delete confirm dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(strings.calendarDeleteAssignment) },
            text = { Text(strings.calendarDeleteConfirm) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        showAddDialog = false
                        selectedScheduleId?.let { id ->
                            viewModel.deleteSchedule(
                                id = id,
                                onSuccess = { Toast.makeText(context, strings.calendarAssignmentDeleted, Toast.LENGTH_SHORT).show() },
                                onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(strings.cancel) }
            }
        )
    }
}

// Gün adı formatlarını normalize et
fun normalizeDayOfWeek(day: String): String {
    return when (day.lowercase().trim()) {
        "monday", "mon", "pazartesi"                -> "MON"
        "tuesday", "tue", "sali", "salı"            -> "TUE"
        "wednesday", "wed", "carsamba", "çarşamba"  -> "WED"
        "thursday", "thu", "persembe", "perşembe"   -> "THU"
        "friday", "fri", "cuma"                     -> "FRI"
        else -> day.uppercase().take(3)
    }
}

fun checkSlot(startTime: String, slot: String): Boolean {
    return try {
        val hour = startTime.split(":").getOrNull(0)?.toIntOrNull() ?: return false
        when (slot) {
            "08:00-10:00" -> hour == 8
            "10:00-12:00" -> hour == 10
            "13:00-15:00" -> hour == 13
            "15:00-17:00" -> hour == 15
            else          -> false
        }
    } catch (e: Exception) { false }
}

private fun slotToTimes(slot: String): Pair<String, String> = when (slot) {
    "08:00-10:00" -> Pair("08:00", "10:00")
    "10:00-12:00" -> Pair("10:00", "12:00")
    "13:00-15:00" -> Pair("13:00", "15:00")
    "15:00-17:00" -> Pair("15:00", "17:00")
    else          -> Pair("08:00", "10:00")
}

private fun slotLabel(slot: String): String = when (slot) {
    "08:00-10:00" -> "08:00 – 10:00"
    "10:00-12:00" -> "10:00 – 12:00"
    "13:00-15:00" -> "13:00 – 15:00"
    "15:00-17:00" -> "15:00 – 17:00"
    else -> slot
}

private fun dayToFullName(day: String) = when (day) {
    "MON" -> "Monday"
    "TUE" -> "Tuesday"
    "WED" -> "Wednesday"
    "THU" -> "Thursday"
    "FRI" -> "Friday"
    else  -> day
}
