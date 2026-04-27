package com.bugra.campussync.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugra.campussync.utils.LocalAppStrings
import com.bugra.campussync.utils.TokenManager
import com.bugra.campussync.viewmodels.ChangePasswordViewModel

@Composable
fun ChangePasswordScreen(onPasswordChanged: () -> Unit, onLogout: (() -> Unit)? = null) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val tokenManager = remember { TokenManager(context) }
    val mustChange = remember { tokenManager.getMustChangePassword() }
    val viewModel: ChangePasswordViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    val passwordsMatch = newPassword == confirmPassword || confirmPassword.isEmpty()
    val isStrong = newPassword.length >= 8
    val canSubmit = (mustChange || currentPassword.isNotBlank()) &&
            newPassword.length >= 8 && newPassword == confirmPassword && !state.isLoading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Lock, null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(12.dp))
        Text(strings.changePassTitle, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            strings.changePassFirstLogin,
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (state.errorMessage.isNotEmpty() && !state.isLoading) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    state.errorMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (!mustChange) {
            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = { Text(strings.changePassCurrent) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showCurrent) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showCurrent = !showCurrent }) {
                        Icon(if (showCurrent) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text(strings.changePassNew) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showNew = !showNew }) {
                    Icon(if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                }
            },
            isError = newPassword.isNotEmpty() && !isStrong,
            supportingText = if (newPassword.isNotEmpty() && !isStrong) {
                { Text("En az 8 karakter olmalı.", color = MaterialTheme.colorScheme.error) }
            } else null
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text(strings.changePassNewRepeat) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showConfirm = !showConfirm }) {
                    Icon(if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                }
            },
            isError = confirmPassword.isNotEmpty() && !passwordsMatch,
            supportingText = if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                { Text(strings.changePassMismatch, color = MaterialTheme.colorScheme.error) }
            } else null
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.changePassword(
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                    onSuccess = {
                        tokenManager.clearMustChangePassword()
                        Toast.makeText(context, strings.changePassSuccess, Toast.LENGTH_SHORT).show()
                        onPasswordChanged()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = canSubmit
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(strings.saving)
            } else {
                Text(strings.changePassSave, fontSize = 16.sp)
            }
        }

        if (onLogout != null) {
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Text(strings.logout, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
