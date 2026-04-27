package com.bugra.campussync.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bugra.campussync.network.AnnouncementItem
import com.bugra.campussync.utils.LocalAppStrings
import com.bugra.campussync.utils.TokenManager
import com.bugra.campussync.viewmodels.AnnouncementsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsScreen() {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val tokenManager = remember { TokenManager(context) }
    val role = tokenManager.getRole() ?: ""
    val canCreate = role.uppercase().let { it.contains("ADMIN") || it.contains("SUPER") || it == "STAFF" || it == "IT" }

    val viewModel: AnnouncementsViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val announcements = state.announcements
    val isLoading = state.isLoading
    val readIds = state.readIds

    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.announcementsTitle, fontWeight = FontWeight.Bold) },
                actions = {
                    if (announcements.any { it.id !in readIds }) {
                        TextButton(onClick = { viewModel.markAllRead() }) {
                            Text(strings.announcementsMarkAllRead)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (canCreate) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, strings.announcementsAdd, tint = Color.White)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                announcements.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.NotificationsNone, null,
                            modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(Modifier.height(12.dp))
                        Text(strings.announcementsNone, color = Color.Gray)
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(announcements, key = { it.id }) { ann ->
                        val isRead = ann.id in readIds
                        AnnouncementCard(
                            announcement = ann,
                            isRead = isRead,
                            onRead = { viewModel.markRead(ann.id) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateAnnouncementDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, body, audience ->
                viewModel.create(
                    title, body, audience,
                    onSuccess = {
                        showCreateDialog = false
                        Toast.makeText(context, strings.announcementsPublished, Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, "${strings.error}: $err", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnnouncementCard(
    announcement: AnnouncementItem,
    isRead: Boolean,
    onRead: () -> Unit
) {
    val containerColor = if (isRead)
        MaterialTheme.colorScheme.surface
    else
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(if (isRead) 1.dp else 3.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        onClick = { if (!isRead) onRead() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isRead) Icons.Default.NotificationsNone else Icons.Default.Notifications,
                    null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isRead) Color.Gray else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    announcement.title,
                    fontWeight = if (isRead) FontWeight.Normal else FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(announcement.body, fontSize = 13.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    announcement.created_by_name,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                val dateStr = announcement.created_at.take(16).replace("T", " ")
                Text(dateStr, fontSize = 11.sp, color = Color.LightGray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateAnnouncementDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit
) {
    var title    by remember { mutableStateOf("") }
    var body     by remember { mutableStateOf("") }
    var audience by remember { mutableStateOf("ALL") }
    var audienceExpanded by remember { mutableStateOf(false) }

    val strings = LocalAppStrings.current
    val audienceOptions = listOf(
        "ALL" to strings.announcementsAllUsers,
        "LECTURER" to strings.announcementsLecturers,
        "STUDENT" to strings.announcementsStudents,
        "ADMIN" to strings.announcementsAdmins
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .imePadding(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    strings.announcementsNew,
                    style = MaterialTheme.typography.headlineSmall
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.announcementsHeadline) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text(strings.announcementsContent) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5
                )
                ExposedDropdownMenuBox(
                    expanded = audienceExpanded,
                    onExpandedChange = { audienceExpanded = !audienceExpanded }
                ) {
                    OutlinedTextField(
                        value = audienceOptions.find { it.first == audience }?.second ?: audience,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.announcementsAudience) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = audienceExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = audienceExpanded,
                        onDismissRequest = { audienceExpanded = false }
                    ) {
                        audienceOptions.forEach { (code, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { audience = code; audienceExpanded = false }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text(strings.cancel) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { if (title.isNotBlank() && body.isNotBlank()) onCreate(title, body, audience) },
                        enabled = title.isNotBlank() && body.isNotBlank()
                    ) { Text(strings.announcementsPublish) }
                }
            }
        }
    }
}
