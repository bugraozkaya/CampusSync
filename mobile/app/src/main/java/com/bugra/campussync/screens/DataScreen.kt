package com.bugra.campussync.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
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
import com.bugra.campussync.utils.TokenManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

data class ImportResult(
    val course: String,
    val lecturer: String,
    val generatedUser: String,
    val generatedPass: String
)

@Composable
fun DataScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    val token = tokenManager.getToken() ?: ""

    var selectedExcelUri by remember { mutableStateOf<Uri?>(null) }
    var importResults by remember { mutableStateOf<List<ImportResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedExcelUri = uri
            importResults = emptyList()
            Toast.makeText(context, "Excel dosyası seçildi.", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedExcelUri == null && importResults.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = { filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") },
                    modifier = Modifier.size(100.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Import Excel",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text("Excel Verisi Yükle", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Bölüm | Kod | Ders | Hoca", color = Color.Gray, fontSize = 14.sp)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                OutlinedButton(
                    onClick = {
                        isGenerating = true
                        scope.launch {
                            try {
                                RetrofitClient.apiService.generateAutoSchedule("Bearer $token")
                                Toast.makeText(context, "Akıllı program başarıyla oluşturuldu!", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            } finally {
                                isGenerating = false
                            }
                        }
                    },
                    enabled = !isGenerating
                ) {
                    if (isGenerating) CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    else {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mevcut Verilerle Program Oluştur")
                    }
                }
            }
        } else if (importResults.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Veriler Yüklendi!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(importResults) { res ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = res.lecturer, fontWeight = FontWeight.Bold)
                                Text(text = "Ders: ${res.course}", color = MaterialTheme.colorScheme.primary)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("User: ${res.generatedUser}", fontSize = 12.sp)
                                    Text("Pass: ${res.generatedPass}", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                                }
                            }
                        }
                    }
                }
                
                Button(
                    onClick = {
                        isGenerating = true
                        scope.launch {
                            try {
                                RetrofitClient.apiService.generateAutoSchedule("Bearer $token")
                                Toast.makeText(context, "Program Oluşturuldu!", Toast.LENGTH_LONG).show()
                                importResults = emptyList()
                                selectedExcelUri = null
                            } catch (e: Exception) {
                                Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isGenerating = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    if (isGenerating) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("Şimdi Akıllı Programı Hazırla")
                }

                TextButton(
                    onClick = {
                        importResults = emptyList()
                        selectedExcelUri = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Daha Sonra Yap (Kapat)")
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Dosya Hazır", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            try {
                                val inputStream = context.contentResolver.openInputStream(selectedExcelUri!!)
                                val bytes = inputStream?.readBytes()
                                inputStream?.close()
                                if (bytes != null) {
                                    val requestBody = bytes.toRequestBody("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".toMediaTypeOrNull())
                                    val filePart = MultipartBody.Part.createFormData("file", "data.xlsx", requestBody)
                                    val response = RetrofitClient.apiService.bulkImportExcel("Bearer $token", filePart)
                                    importResults = response.map { item ->
                                        ImportResult(
                                            course = item["course"] ?: "",
                                            lecturer = item["lecturer"] ?: "",
                                            generatedUser = item["generated_user"] ?: "",
                                            generatedPass = item["generated_pass"] ?: ""
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("Sisteme İşle")
                }
            }
        }
    }
}
