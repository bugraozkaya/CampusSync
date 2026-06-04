package com.bugra.campussync.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.bugra.campussync.network.ChatMessage
import com.bugra.campussync.utils.LocalAppStrings
import com.bugra.campussync.utils.TokenManager
import com.bugra.campussync.viewmodels.ChatViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    partnerId: Int,
    partnerName: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val context      = LocalContext.current
    val strings      = LocalAppStrings.current
    val tokenManager = remember { TokenManager(context) }
    val myUsername   = tokenManager.getUsername() ?: ""

    val state by viewModel.state.collectAsState()
    val messages = state.messages
    val isSending = state.isSending

    var inputText by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    
    val listState = rememberLazyListState()

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedFileUri = uri
            context.contentResolver.query(uri, null, null, null, null)?.use {
                if (it.moveToFirst()) {
                    val col = it.getColumnIndex("_display_name")
                    if (col != -1) selectedFileName = it.getString(col)
                }
            }
            if (selectedFileName.isBlank()) selectedFileName = "dosya"
        }
    }

    LaunchedEffect(partnerId) { viewModel.startPolling(partnerId) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(partnerName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(strings.chatOnline, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, strings.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.imePadding()
            ) {
                Column {
                    if (selectedFileUri != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(selectedFileName, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1)
                                IconButton(onClick = { selectedFileUri = null; selectedFileName = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { filePicker.launch("*/*") }) {
                            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text(strings.chatMessageHint) },
                            modifier = Modifier.weight(1f),
                            maxLines = 4,
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                        
                        FilledIconButton(
                            onClick = {
                                val text = inputText.trim()
                                val uri = selectedFileUri
                                if (uri != null) {
                                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: return@FilledIconButton
                                    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                                    val filePart = MultipartBody.Part.createFormData("file", selectedFileName, bytes.toRequestBody(mimeType.toMediaTypeOrNull()))
                                    
                                    viewModel.sendFile(
                                        partnerId = partnerId,
                                        content = text,
                                        file = filePart,
                                        onSuccess = {
                                            inputText = ""
                                            selectedFileUri = null
                                            selectedFileName = ""
                                        },
                                        onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                    )
                                } else {
                                    viewModel.sendMessage(
                                        partnerId = partnerId,
                                        text = text,
                                        onSuccess = { inputText = "" },
                                        onError = { Toast.makeText(context, strings.chatSendFailed, Toast.LENGTH_SHORT).show() }
                                    )
                                }
                            },
                            enabled = (inputText.isNotBlank() || selectedFileUri != null) && !isSending,
                            modifier = Modifier.size(48.dp)
                        ) {
                            if (isSending) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            else Icon(Icons.AutoMirrored.Filled.Send, strings.chatSend)
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (messages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(strings.chatStartConversation, color = Color.Gray)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isMine = msg.sender_username == myUsername || msg.sender_username.isBlank()
                    ChatBubble(msg = msg, isMine = isMine)
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage, isMine: Boolean) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = if (isMine)
                RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
            else
                RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            modifier = Modifier.widthIn(max = 280.dp),
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                if (msg.file_url != null) {
                    val isImage = msg.file_url.lowercase().let { it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") || it.endsWith(".gif") || it.endsWith(".webp") }
                    
                    if (isImage) {
                        AsyncImage(
                            model = msg.file_url,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(msg.file_url))
                                    context.startActivity(intent)
                                },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            color = if (isMine) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(msg.file_url))
                                context.startActivity(intent)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, null, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = msg.file?.split("/")?.lastOrNull() ?: "Dosya",
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    if (msg.content.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                    }
                }
                
                if (msg.content.isNotBlank()) {
                    Text(
                        text = msg.content,
                        color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                        fontSize = 15.sp
                    )
                }
            }
        }
        Text(
            text = msg.created_at.take(16).replace("T", " "),
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
