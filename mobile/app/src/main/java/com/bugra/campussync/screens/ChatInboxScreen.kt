package com.bugra.campussync.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugra.campussync.network.ChatContact
import com.bugra.campussync.network.ChatConversation
import com.bugra.campussync.utils.LocalAppStrings
import com.bugra.campussync.viewmodels.ChatInboxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInboxScreen(onOpenChat: (Int, String) -> Unit) {
    val strings = LocalAppStrings.current
    val viewModel: ChatInboxViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val conversations = state.conversations
    val contacts = state.contacts
    val isLoading = state.isLoading

    var showNewChat   by remember { mutableStateOf(false) }
    var searchQuery   by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.chatInboxTitle, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.loadContacts()
                    showNewChat = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) { Icon(Icons.Default.Add, strings.chatInboxNewMessage, tint = Color.White) }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                conversations.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Text(strings.chatInboxNone, color = Color.Gray)
                        Text(strings.chatInboxStartHint, color = Color.LightGray, fontSize = 13.sp)
                    }
                }
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(conversations, key = { it.partner_id }) { conv ->
                        ConversationRow(conv = conv, onClick = { onOpenChat(conv.partner_id, conv.partner_name) })
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }

    if (showNewChat) {
        NewChatDialog(
            contacts = contacts,
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            onDismiss = { showNewChat = false; searchQuery = "" },
            onSelect = { contact ->
                showNewChat = false
                searchQuery = ""
                onOpenChat(contact.id, contact.name)
            }
        )
    }
}

@Composable
private fun ConversationRow(conv: ChatConversation, onClick: () -> Unit) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = conv.partner_name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    conv.partner_name,
                    fontWeight = if (conv.unread_count > 0) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 15.sp
                )
                Text(
                    conv.last_message_at.take(10),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = (if (conv.is_mine) strings.chatYouPrefix else "") + conv.last_message,
                    fontSize = 13.sp,
                    color = if (conv.unread_count > 0) MaterialTheme.colorScheme.onSurface else Color.Gray,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                if (conv.unread_count > 0) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Text(
                            "${conv.unread_count}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewChatDialog(
    contacts: List<ChatContact>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelect: (ChatContact) -> Unit
) {
    val strings = LocalAppStrings.current
    val filtered = contacts.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.username.contains(searchQuery, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.chatInboxNewChat) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text(strings.chatInboxSearch) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(filtered, key = { it.id }) { contact ->
                        ListItem(
                            headlineContent = { Text(contact.name) },
                            supportingContent = { Text("@${contact.username} · ${contact.role.lowercase().replaceFirstChar { it.uppercase() }}", fontSize = 12.sp) },
                            modifier = Modifier.clickable { onSelect(contact) }
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                    if (filtered.isEmpty()) {
                        item { Text(strings.chatInboxNotFound, color = Color.Gray, modifier = Modifier.padding(8.dp)) }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
