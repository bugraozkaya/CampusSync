package com.bugra.campussync.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.bugra.campussync.network.RetrofitClient
import com.bugra.campussync.utils.LocalAppStrings
import com.bugra.campussync.utils.TokenManager
import com.bugra.campussync.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(onLoginSuccess: (mustChangePassword: Boolean) -> Unit) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val focusManager = LocalFocusManager.current
    val strings = LocalAppStrings.current

    val viewModel: AuthViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val isLoading = state.isLoading

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var showForgotDialog by remember { mutableStateOf(false) }
    var forgotUsername by remember { mutableStateOf("") }
    var forgotLoading by remember { mutableStateOf(false) }
    var forgotResult by remember { mutableStateOf<String?>(null) }
    var forgotError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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

    if (showForgotDialog) {
        if (forgotResult != null) {
            AlertDialog(
                onDismissRequest = {
                    showForgotDialog = false
                    forgotResult = null
                    forgotUsername = ""
                },
                title = { Text("Geçici Şifreniz") },
                text = {
                    Column {
                        Text("Kullanıcı adı: $forgotUsername")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Geçici şifre:")
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Text(
                                forgotResult!!,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Bu şifreyle giriş yapıp yeni şifrenizi belirleyin.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        username = forgotUsername
                        password = forgotResult ?: ""
                        showForgotDialog = false
                        forgotResult = null
                        forgotUsername = ""
                    }) { Text("Giriş Yap") }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { showForgotDialog = false; forgotError = null },
                title = { Text("Şifremi Unuttum") },
                text = {
                    Column {
                        Text("Kullanıcı adınızı girin, size geçici bir şifre oluşturalım.")
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = forgotUsername,
                            onValueChange = { forgotUsername = it; forgotError = null },
                            label = { Text(strings.loginUsername) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            isError = forgotError != null
                        )
                        if (forgotError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(forgotError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                forgotLoading = true
                                forgotError = null
                                try {
                                    val res = RetrofitClient.apiService.forgotPassword(mapOf("username" to forgotUsername.trim()))
                                    forgotResult = res["temp_password"]
                                } catch (e: retrofit2.HttpException) {
                                    val body = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                                    val match = body?.let { Regex(""""error"\s*:\s*"([^"]+)"""").find(it) }
                                    forgotError = match?.groupValues?.get(1) ?: "Bir hata oluştu."
                                } catch (e: Exception) {
                                    forgotError = "Sunucuya ulaşılamıyor."
                                } finally {
                                    forgotLoading = false
                                }
                            }
                        },
                        enabled = forgotUsername.isNotBlank() && !forgotLoading
                    ) {
                        if (forgotLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Sıfırla")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showForgotDialog = false; forgotError = null }) { Text(strings.cancel) }
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("CampusSync", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        Text(
            strings.loginAppSubtitle,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (state.errorMessage.isNotEmpty() && !isLoading) {
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
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(strings.loginUsername) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            isError = state.errorMessage.isNotEmpty() && !isLoading
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(strings.loginPassword) },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) strings.loginPasswordHide else strings.loginPasswordShow
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            isError = state.errorMessage.isNotEmpty() && !isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.login(username, password) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = canLogin
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(strings.loginLoggingIn)
            } else {
                Text(strings.loginButton, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { showForgotDialog = true; forgotUsername = username }) {
            Text("Şifremi Unuttum", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
