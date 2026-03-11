package com.bugra.campussync.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugra.campussync.network.RetrofitClient
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

// Veri sınıfı burada kalabilir ama ApiService'in görmesi için public olmalı
data class ImportedCourse(
    val code: String,
    val name: String,
    val lecturer: String
)

data class GeneratedAccount(
    val lecturer: String,
    val generated_user: String,
    val generated_pass: String
)

@Composable
fun DataScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var importedList by remember { mutableStateOf<List<ImportedCourse>>(emptyList()) }
    var generatedAccounts by remember { mutableStateOf<List<GeneratedAccount>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val result = readTxtFile(context, it)
            if (result.isNotEmpty()) {
                importedList = result
                generatedAccounts = emptyList()
            } else {
                Toast.makeText(context, "Dosya okunamadı veya format yanlış!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (importedList.isEmpty() && generatedAccounts.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = { filePickerLauncher.launch("text/plain") },
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Import Data",
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text("Veri Yüklemek İçin Dokunun (TXT)", fontSize = 14.sp)
            }
        } else if (generatedAccounts.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Oluşturulan Hoca Hesapları", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(generatedAccounts) { acc ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Hoca: ${acc.lecturer}", fontWeight = FontWeight.Bold)
                                Text("Kullanıcı Adı: ${acc.generated_user}", color = MaterialTheme.colorScheme.primary)
                                Text("Şifre: ${acc.generated_pass}", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                            }
                        }
                    }
                }
                Button(onClick = { generatedAccounts = emptyList() }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Text("Tamam")
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Yüklenecek Liste", fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(importedList) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "${item.code}: ${item.name}", style = MaterialTheme.typography.titleMedium)
                                Text(text = "Hoca: ${item.lecturer}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            try {
                                val response = RetrofitClient.apiService.bulkImport(importedList)
                                generatedAccounts = response.map { item ->
                                    GeneratedAccount(
                                        lecturer = item["lecturer"] ?: "Unknown",
                                        generated_user = item["generated_user"] ?: "",
                                        generated_pass = item["generated_pass"] ?: ""
                                    )
                                }
                                importedList = emptyList()
                                Toast.makeText(context, "Hesaplar başarıyla oluşturuldu!", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    else Text("Sisteme Kaydet ve Hesapları Oluştur")
                }
            }
        }
    }
}

fun readTxtFile(context: android.content.Context, uri: Uri): List<ImportedCourse> {
    val courses = mutableListOf<ImportedCourse>()
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line: String? = reader.readLine()
        while (line != null) {
            val parts = line.split(",")
            if (parts.size >= 3) {
                courses.add(ImportedCourse(parts[0].trim(), parts[1].trim(), parts[2].trim()))
            }
            line = reader.readLine()
        }
        reader.close()
    } catch (e: Exception) { e.printStackTrace() }
    return courses
}