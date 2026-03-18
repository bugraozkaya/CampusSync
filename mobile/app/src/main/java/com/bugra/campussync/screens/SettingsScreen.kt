package com.bugra.campussync.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugra.campussync.utils.TokenManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    // Dokümandaki bölümler
    val departments = listOf(
        "Computer Engineering",
        "Electrical and Electronics Engineering",
        "Mechanical Engineering",
        "Aeronautical Engineering",
        "Agricultural Department"
    )
    
    // Dokümandaki pozisyonlar
    val positions = listOf("Admin (IT)", "Lecturer")

    // State tanımlamaları
    var nameSurname by remember { mutableStateOf(tokenManager.getNameSurname() ?: "") }
    var selectedDept by remember { mutableStateOf(tokenManager.getDepartment() ?: "") }
    var selectedPos by remember { mutableStateOf(tokenManager.getPosition() ?: "") }

    var deptExpanded by remember { mutableStateOf(false) }
    var posExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Ayarlar ve Profil", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))

        // Ad Soyad Girişi
        OutlinedTextField(
            value = nameSurname,
            onValueChange = { nameSurname = it },
            label = { Text("Ad Soyad") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Bölüm Seçimi (Dropdown)
        ExposedDropdownMenuBox(
            expanded = deptExpanded,
            onExpandedChange = { deptExpanded = !deptExpanded }
        ) {
            OutlinedTextField(
                value = selectedDept.ifEmpty { "Bölüm Seçin" },
                onValueChange = {},
                readOnly = true,
                label = { Text("Departman") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = deptExpanded,
                onDismissRequest = { deptExpanded = false }
            ) {
                departments.forEach { dept ->
                    DropdownMenuItem(
                        text = { Text(dept) },
                        onClick = {
                            selectedDept = dept
                            deptExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pozisyon Seçimi (Dropdown)
        ExposedDropdownMenuBox(
            expanded = posExpanded,
            onExpandedChange = { posExpanded = !posExpanded }
        ) {
            OutlinedTextField(
                value = selectedPos.ifEmpty { "Pozisyon Seçin" },
                onValueChange = {},
                readOnly = true,
                label = { Text("Pozisyon") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = posExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = posExpanded,
                onDismissRequest = { posExpanded = false }
            ) {
                positions.forEach { pos ->
                    DropdownMenuItem(
                        text = { Text(pos) },
                        onClick = {
                            selectedPos = pos
                            posExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (nameSurname.isBlank() || selectedDept.isBlank() || selectedPos.isBlank()) {
                    Toast.makeText(context, "Lütfen tüm alanları doldurun!", Toast.LENGTH_SHORT).show()
                } else {
                    tokenManager.saveProfileInfo(nameSurname, selectedDept, selectedPos)
                    Toast.makeText(context, "Profiliniz başarıyla kaydedildi.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Kaydet")
        }
    }
}
