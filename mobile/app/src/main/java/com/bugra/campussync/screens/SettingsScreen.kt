package com.bugra.campussync.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugra.campussync.network.RetrofitClient
import com.bugra.campussync.ui.theme.ThemeMode
import com.bugra.campussync.utils.LocalAppStrings
import com.bugra.campussync.utils.ThemePreferences
import com.bugra.campussync.utils.TokenManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onProfileSaved: () -> Unit,
    onLogoutClick: () -> Unit,
    themePreferences: ThemePreferences? = null
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()

    val strings = LocalAppStrings.current

    val themeMode by (themePreferences?.themeMode
        ?: kotlinx.coroutines.flow.flowOf(ThemeMode.SYSTEM))
        .collectAsState(initial = ThemeMode.SYSTEM)

    val languageCode by (themePreferences?.languageCode
        ?: kotlinx.coroutines.flow.flowOf("tr"))
        .collectAsState(initial = "tr")

    val role     = tokenManager.getRole()     ?: ""
    val username = tokenManager.getUsername() ?: ""
    val canEditProfile = role.uppercase().let {
        it != "ADMIN" && it != "SUPERADMIN" && it != "STAFF" && it != "IT"
    }

    var editMode    by remember { mutableStateOf(false) }
    var firstName   by remember { mutableStateOf(tokenManager.getFirstName() ?: "") }
    var lastName    by remember { mutableStateOf(tokenManager.getLastName()  ?: "") }
    var isSaving    by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Başlık ───────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(strings.settingsTitle, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onLogoutClick) {
                Icon(Icons.Default.Logout, contentDescription = strings.logout, tint = MaterialTheme.colorScheme.error)
            }
        }

        // ── Profil Kartı ─────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (!editMode && (firstName.isNotBlank() || lastName.isNotBlank()))
                            "$firstName $lastName".trim() else if (!editMode) username else strings.settingsEditProfile,
                        fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (!editMode && canEditProfile) {
                        IconButton(onClick = { editMode = true }) {
                            Icon(Icons.Default.Edit, strings.edit, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                if (!editMode) {
                    if (username.isNotBlank()) {
                        Text("@$username", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    }
                    if (role.isNotBlank()) {
                        val roleLabel = when (role.uppercase()) {
                            "SUPERADMIN" -> strings.roleSuperAdmin
                            "ADMIN"      -> strings.roleAdmin
                            "LECTURER"   -> strings.roleLecturer
                            "STAFF"      -> strings.roleStaff
                            "IT"         -> strings.roleIT
                            "STUDENT"    -> strings.roleStudent
                            else -> role
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = roleLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                } else if (canEditProfile) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text(strings.settingsFirstName) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text(strings.settingsLastName) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                firstName = tokenManager.getFirstName() ?: ""
                                lastName  = tokenManager.getLastName()  ?: ""
                                editMode = false
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(strings.cancel) }
                        Button(
                            onClick = {
                                if (firstName.isBlank() && lastName.isBlank()) {
                                    Toast.makeText(context, strings.settingsNameEmpty, Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isSaving = true
                                scope.launch {
                                    try {
                                        RetrofitClient.apiService.updateProfile(
                                            mapOf("first_name" to firstName, "last_name" to lastName)
                                        )
                                        tokenManager.saveUserInfo(firstName, lastName)
                                        editMode = false
                                        Toast.makeText(context, strings.settingsProfileUpdated, Toast.LENGTH_SHORT).show()
                                        onProfileSaved()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "${strings.settingsUpdateFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isSaving
                        ) {
                            if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            else {
                                Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.save)
                            }
                        }
                    }
                }
            }
        }

        // ── Görünüm ──────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings.settingsAppearance, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        selected = themeMode == ThemeMode.LIGHT,
                        onClick = { scope.launch { themePreferences?.setThemeMode(ThemeMode.LIGHT) } },
                        icon = { Icon(Icons.Default.LightMode, null, modifier = Modifier.size(16.dp)) }
                    ) { Text(strings.settingsThemeLight, fontSize = 13.sp) }

                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        selected = themeMode == ThemeMode.SYSTEM,
                        onClick = { scope.launch { themePreferences?.setThemeMode(ThemeMode.SYSTEM) } },
                        icon = { Icon(Icons.Default.SettingsBrightness, null, modifier = Modifier.size(16.dp)) }
                    ) { Text(strings.settingsThemeSystem, fontSize = 13.sp) }

                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        selected = themeMode == ThemeMode.DARK,
                        onClick = { scope.launch { themePreferences?.setThemeMode(ThemeMode.DARK) } },
                        icon = { Icon(Icons.Default.DarkMode, null, modifier = Modifier.size(16.dp)) }
                    ) { Text(strings.settingsThemeDark, fontSize = 13.sp) }
                }
            }
        }

        // ── Dil ──────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings.settingsLanguage, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        selected = languageCode == "tr",
                        onClick = {
                            scope.launch {
                                themePreferences?.setLanguageCode("tr")
                                val locale = java.util.Locale("tr")
                                java.util.Locale.setDefault(locale)
                                val config = context.resources.configuration
                                config.setLocale(locale)
                                @Suppress("DEPRECATION")
                                context.resources.updateConfiguration(config, context.resources.displayMetrics)
                            }
                        }
                    ) { Text("Türkçe", fontSize = 13.sp) }

                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        selected = languageCode == "en",
                        onClick = {
                            scope.launch {
                                themePreferences?.setLanguageCode("en")
                                val locale = java.util.Locale("en")
                                java.util.Locale.setDefault(locale)
                                val config = context.resources.configuration
                                config.setLocale(locale)
                                @Suppress("DEPRECATION")
                                context.resources.updateConfiguration(config, context.resources.displayMetrics)
                            }
                        }
                    ) { Text("English", fontSize = 13.sp) }
                }
            }
        }

        // ── Çıkış ────────────────────────────────────────────────────
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onLogoutClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.logout, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}
