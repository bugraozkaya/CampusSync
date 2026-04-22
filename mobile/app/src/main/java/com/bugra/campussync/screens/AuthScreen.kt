package com.bugra.campussync.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugra.campussync.utils.TokenManager
import com.bugra.campussync.viewmodels.AuthViewModel

@Composable
fun AuthScreen(onLoginSuccess: (mustChangePassword: Boolean) -> Unit) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val focusManager = LocalFocusManager.current

    val viewModel: AuthViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val isLoading = state.isLoading
    val errorMessage = state.errorMessage

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Consume login result — save to TokenManager and navigate
    LaunchedEffect(state.loginResult) {
        val result = state.loginResult ?: return@LaunchedEffect
        tokenManager.saveAuthData(
            token = result.access,
            refreshToken = result.refresh,
            role = result.role,
            username = result.username,
            mustChangePassword = result.mustChangePassword
        )
        if (!result.firstName.isNullOrBlank() || !result.lastName.isNullOrBlank()) {
            tokenManager.saveUserInfo(result.firstName, result.lastName, result.title)
        }
        viewModel.consumeLoginResult()
        onLoginSuccess(result.mustChangePassword)
    }

    val canLogin = username.isNotBlank() && password.isNotBlank() && !isLoading

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("CampusSync", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        Text(
            "Ders Programı Yönetim Sistemi",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 40.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it; viewModel.clearError() },
            label = { Text("Kullanıcı Adı") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            isError = errorMessage.isNotEmpty()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; viewModel.clearError() },
            label = { Text("Şifre") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Şifreyi gizle" else "Şifreyi göster"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); viewModel.login(username, password) }),
            isError = errorMessage.isNotEmpty()
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
            onClick = { viewModel.login(username, password) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = canLogin
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Giriş yapılıyor...")
            } else {
                Text("Giriş Yap", fontSize = 16.sp)
            }
        }
    }
}
