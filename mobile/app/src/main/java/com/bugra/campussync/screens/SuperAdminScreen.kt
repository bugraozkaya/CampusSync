package com.bugra.campussync.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugra.campussync.network.RetrofitClient
import com.bugra.campussync.utils.TokenManager
import kotlinx.coroutines.launch

@Composable
fun SuperAdminScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Üniversiteler", "Admin Atama")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> InstitutionManagementTab()
            1 -> AdminAssignmentTab()
        }
    }
}

@Composable
fun InstitutionManagementTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    val token = tokenManager.getToken() ?: ""

    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var institutions by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    fun refreshList() {
        scope.launch {
            try {
                institutions = RetrofitClient.apiService.getInstitutions("Bearer $token")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit) { refreshList() }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Yeni Üniversite Ekle", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Üniversite Adı") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Kurum Tipi") }, modifier = Modifier.fillMaxWidth())
        
        Button(
            onClick = {
                scope.launch {
                    try {
                        RetrofitClient.apiService.createInstitution("Bearer $token", mapOf("name" to name, "institution_type" to type))
                        Toast.makeText(context, "Üniversite eklendi!", Toast.LENGTH_SHORT).show()
                        name = ""; type = ""; refreshList()
                    } catch (e: Exception) { 
                        Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show() 
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) { Text("Ekle") }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Mevcut Üniversiteler", fontWeight = FontWeight.Bold)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(institutions) { inst ->
                ListItem(
                    headlineContent = { Text(inst["name"]?.toString() ?: "") },
                    supportingContent = { Text(inst["institution_type"]?.toString() ?: "") },
                    leadingContent = { Icon(Icons.Default.Business, contentDescription = null) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAssignmentTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    val token = tokenManager.getToken() ?: ""

    var institutions by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedInstId by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try { institutions = RetrofitClient.apiService.getInstitutions("Bearer $token") } catch (e: Exception) {}
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Üniversiteye Admin Ata", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(institutions) { inst ->
                val id = inst["id"]?.toString() ?: ""
                FilterChip(
                    selected = selectedInstId == id,
                    onClick = { selectedInstId = id },
                    label = { Text(inst["name"]?.toString() ?: "Bilinmiyor") }
                )
            }
        }

        OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("Admin Adı Soyadı") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Kullanıcı Adı") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Şifre") }, modifier = Modifier.fillMaxWidth())

        Button(
            onClick = {
                scope.launch {
                    try {
                        RetrofitClient.apiService.createAdmin("Bearer $token", mapOf(
                            "institution_id" to selectedInstId,
                            "username" to username,
                            "password" to password,
                            "first_name" to firstName
                        ))
                        Toast.makeText(context, "Admin atandı!", Toast.LENGTH_SHORT).show()
                        username = ""; password = ""; firstName = ""
                    } catch (e: Exception) {
                        Toast.makeText(context, "Hata oluştu", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            enabled = selectedInstId.isNotEmpty()
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Admin Hesabı Oluştur")
        }
    }
}
