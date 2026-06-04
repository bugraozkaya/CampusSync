package com.bugra.campussync.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bugra.campussync.utils.TokenManager
import com.bugra.campussync.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onProfileSaved: () -> Unit,
    onLogoutClick: () -> Unit,
    themePreferences: Any? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val state by viewModel.state.collectAsState()

    var firstName by remember { mutableStateOf(tokenManager.getFirstName() ?: "") }
    var lastName by remember { mutableStateOf(tokenManager.getLastName() ?: "") }
    var title by remember { mutableStateOf(tokenManager.getTitle() ?: "") }
    var department by remember { mutableStateOf(tokenManager.getDepartment() ?: "") }

    // Dropdown States
    var titleExpanded by remember { mutableStateOf(false) }
    var deptExpanded by remember { mutableStateOf(false) }

    val defaultTitles = listOf("Prof. Dr.", "Doç. Dr.", "Dr. Öğr. Üyesi", "Öğr. Gör.", "Arş. Gör.", "Uzman")
    val defaultDepts = listOf(
        "Bilgisayar Mühendisliği", 
        "Yazılım Mühendisliği", 
        "Elektrik-Elektronik Mühendisliği", 
        "Endüstri Mühendisliği",
        "Makine Mühendisliği",
        "İşletme",
        "Psikoloji",
        "Hukuk",
        "Tıp"
    )

    LaunchedEffect(state.success) {
        if (state.success) {
            Toast.makeText(context, "Profil Başarıyla Güncellendi!", Toast.LENGTH_SHORT).show()
            onProfileSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PROFİL AYARLARI", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogoutClick) { Icon(Icons.Default.Logout, null, tint = Color.Red) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Kişisel Bilgiler", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)

            // 1. UNVAN (Dropdown + Manuel)
            ExposedDropdownMenuBox(
                expanded = titleExpanded,
                onExpandedChange = { titleExpanded = !titleExpanded }
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Unvan") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    placeholder = { Text("Örn: Prof. Dr. (Seçin veya yazın)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = titleExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = titleExpanded,
                    onDismissRequest = { titleExpanded = false }
                ) {
                    defaultTitles.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                                title = suggestion
                                titleExpanded = false
                            }
                        )
                    }
                }
            }

            // 2. BÖLÜM (Dropdown + Manuel)
            ExposedDropdownMenuBox(
                expanded = deptExpanded,
                onExpandedChange = { deptExpanded = !deptExpanded }
            ) {
                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Bölüm / Departman") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    placeholder = { Text("Örn: Bilgisayar Müh. (Seçin veya yazın)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = deptExpanded,
                    onDismissRequest = { deptExpanded = false }
                ) {
                    defaultDepts.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                                department = suggestion
                                deptExpanded = false
                            }
                        )
                    }
                }
            }

            // 3. AD
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("Ad") },
                modifier = Modifier.fillMaxWidth()
            )

            // 4. SOYAD
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Soyad") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (firstName.isBlank() || lastName.isBlank() || title.isBlank() || department.isBlank()) {
                        Toast.makeText(context, "Lütfen tüm alanları doldurun!", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.updateProfile(firstName, lastName, title, department)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !state.isSaving
            ) {
                if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                else Text("DEĞİŞİKLİKLERİ KAYDET", fontWeight = FontWeight.Bold)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Hesap Detayları", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Kullanıcı Adı: @${tokenManager.getUsername()}", fontSize = 13.sp)
                    Text("Rol: ${tokenManager.getRole()}", fontSize = 13.sp)
                }
            }
        }
    }
}
