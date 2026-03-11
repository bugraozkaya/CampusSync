package com.bugra.campussync.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE) }

    // Dokümandaki alanlar [cite: 45, 56]
    var name by remember { mutableStateOf(sharedPref.getString("user_name", "") ?: "") }
    var selectedDept by remember { mutableStateOf(sharedPref.getString("user_dept", "Bölüm Seçin") ?: "Bölüm Seçin") }
    var selectedPos by remember { mutableStateOf(sharedPref.getString("user_pos", "Pozisyon Seçin") ?: "Pozisyon Seçin") }

    // Menü açılıp kapanma durumları
    var deptExpanded by remember { mutableStateOf(false) }
    var posExpanded by remember { mutableStateOf(false) }

    // Dokümandaki listeler [cite: 46-51, 52-54]
    val departments = listOf(
        "Computer Engineering",
        "Electrical and Electronics Engineering",
        "Mechanical Engineering",
        "Aeronautical Engineering",
        "Agricultural Department"
    )
    val positions = listOf("Admin (IT)", "Lecturer")

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Profil Ayarları", fontSize = 24.sp, modifier = Modifier.padding(bottom = 24.dp))

        // Ad Soyad Girişi
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Ad Soyad") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Departman Seçimi (Açılır Liste) [cite: 45, 46]
        ExposedDropdownMenuBox(
            expanded = deptExpanded,
            onExpandedChange = { deptExpanded = !deptExpanded }
        ) {
            OutlinedTextField(
                value = selectedDept,
                onValueChange = {},
                readOnly = true,
                label = { Text("Bölüm Seçiniz") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = deptExpanded, onDismissRequest = { deptExpanded = false }) {
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

        // Pozisyon Seçimi (Açılır Liste) [cite: 52]
        ExposedDropdownMenuBox(
            expanded = posExpanded,
            onExpandedChange = { posExpanded = !posExpanded }
        ) {
            OutlinedTextField(
                value = selectedPos,
                onValueChange = {},
                readOnly = true,
                label = { Text("Pozisyon Seçiniz") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = posExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = posExpanded, onDismissRequest = { posExpanded = false }) {
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

        // Kaydet Butonu [cite: 57]
        Button(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            onClick = {
                if (name.isNotEmpty() && selectedDept != "Bölüm Seçin" && selectedPos != "Pozisyon Seçin") {
                    sharedPref.edit().apply {
                        putString("user_name", name)
                        putString("user_dept", selectedDept)
                        putString("user_pos", selectedPos)
                        putBoolean("is_registered", true) // Kayıt tamamlandı bayrağı
                        apply()
                    }
                    Toast.makeText(context, "Profil Kaydedildi!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Lütfen tüm alanları doldurun!", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            Text("Kaydet")
        }
    }
}