package com.bugra.campussync.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
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
import com.bugra.campussync.utils.TokenManager
import com.bugra.campussync.viewmodels.ChangePasswordViewModel

@Composable
fun ChangePasswordScreen(onPasswordChanged: () -> Unit) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val viewModel: ChangePasswordViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val isLoading = state.isLoading
    val errorMessage = state.errorMessage

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    val passwordsMatch = newPassword == confirmPassword || confirmPassword.isEmpty()
    val isStrong = newPassword.length >= 6
    val canSubmit = currentPassword.isNotBlank() && newPassword.length >= 6 && newPassword == confirmPassword && !isLoading

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Lock, null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Şifre Değiştir", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "Hesabınıza ilk girişinizde şifrenizi değiştirmeniz gerekmektedir.",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = currentPassword,
            onValueChange = { currentPassword = it; viewModel.clearError() },
            label = { Text("Mevcut Şifre") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            visualTransformation = if (showCurrent) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showCurrent = !showCurrent }) {
                    Icon(if (showCurrent) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                }
            },
            isError = currentPassword.isEmpty() && errorMessage.isNotEmpty()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it; viewModel.clearError() },
            label = { Text("Yeni Şifre (en az 6 karakter)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showNew = !showNew }) {
                    Icon(if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                }
            },
            isError = newPassword.isNotEmpty() && !isStrong,
            supportingText = if (newPassword.isNotEmpty() && !isStrong) {
                { Text("En az 6 karakter olmalıdır", color = MaterialTheme.colorScheme.error) }
            } else null
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; viewModel.clearError() },
            label = { Text("Yeni Şifre (Tekrar)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showConfirm = !showConfirm }) {
                    Icon(if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                }
            },
            isError = confirmPassword.isNotEmpty() && !passwordsMatch,
            supportingText = if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                { Text("Şifreler uyuşmuyor", color = MaterialTheme.colorScheme.error) }
            } else null
        )

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(errorMessage, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp), fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                viewModel.changePassword(
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                    onSuccess = {
                        tokenManager.clearMustChangePassword()
                        Toast.makeText(context, "Şifreniz başarıyla güncellendi.", Toast.LENGTH_SHORT).show()
                        onPasswordChanged()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = canSubmit
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Kaydediliyor...")
            } else {
                Text("Şifreyi Kaydet", fontSize = 16.sp)
            }
        }
    }
}
