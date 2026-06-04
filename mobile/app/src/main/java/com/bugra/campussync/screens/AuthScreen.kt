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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bugra.campussync.utils.LocalAppStrings
import com.bugra.campussync.utils.TokenManager
import com.bugra.campussync.viewmodels.AuthViewModel

@Composable
fun AuthScreen(onLoginSuccess: (mustChangePassword: Boolean) -> Unit) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val focusManager = LocalFocusManager.current
    val strings = LocalAppStrings.current

    val viewModel: AuthViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val isLoading = state.isLoading

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var showForgotDialog by remember { mutableStateOf(false) }
    var forgotUsername by remember { mutableStateOf("") }

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
        if (state.forgotPasswordResult != null) {
            AlertDialog(
                onDismissRequest = {
                    showForgotDialog = false
                    viewModel.clearForgotState()
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
                                state.forgotPasswordResult!!,
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
                        password = state.forgotPasswordResult ?: ""
                        showForgotDialog = false
                        viewModel.clearForgotState()
                        forgotUsername = ""
                    }) { Text("Giriş Yap") }
                }
            )
        } else {
            Dialog(
                onDismissRequest = { showForgotDialog = false; viewModel.clearForgotState() },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(0.95f).wrapContentHeight().imePadding(),
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = AlertDialogDefaults.TonalElevation,
                    color = AlertDialogDefaults.containerColor
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Şifremi Unuttum", style = MaterialTheme.typography.headlineSmall)
                        Text("Kullanıcı adınızı girin, size geçici bir şifre oluşturalım.")
                        OutlinedTextField(
                            value = forgotUsername,
                            onValueChange = { forgotUsername = it; viewModel.clearForgotState() },
                            label = { Text(strings.loginUsername) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            isError = state.forgotPasswordError != null
                        )
                        if (state.forgotPasswordError != null) {
                            Text(state.forgotPasswordError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showForgotDialog = false; viewModel.clearForgotState() }) { Text(strings.cancel) }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.forgotPassword(forgotUsername) },
                                enabled = forgotUsername.isNotBlank() && !state.isForgotLoading
                            ) {
                                if (state.isForgotLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                else Text("Sıfırla")
                            }
                        }
                    }
                }
            }
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
