package com.bugra.campussync.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
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

@Composable
fun UserManagementScreen() {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val token = tokenManager.getToken() ?: ""
    
    var userList by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            userList = RetrofitClient.apiService.getUsers("Bearer $token")
        } catch (e: Exception) {
            Toast.makeText(context, "Kullanıcılar yüklenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Üniversite Personeli",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Sisteme kayıtlı hoca ve yöneticileri görüntüleyin.",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (userList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Kayıtlı kullanıcı bulunamadı.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(userList) { user ->
                    UserCard(user)
                }
            }
        }
    }
}

@Composable
fun UserCard(user: Map<String, Any>) {
    val firstName = user["first_name"]?.toString() ?: ""
    val lastName = user["last_name"]?.toString() ?: ""
    val fullName = if (firstName.isEmpty() && lastName.isEmpty()) user["username"].toString() else "$firstName $lastName"
    val role = user["role"]?.toString() ?: ""
    val dept = user["department_name"]?.toString() ?: "Bölüm Belirtilmemiş"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (role == "ADMIN") Icons.Default.School else Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (role == "ADMIN") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = fullName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = "@${user["username"]}", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                SuggestionChip(
                    onClick = { },
                    label = { Text(dept, fontSize = 11.sp) },
                    enabled = false
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Badge(
                containerColor = if (role == "ADMIN") Color(0xFFE3F2FD) else Color(0xFFF1F8E9),
                contentColor = if (role == "ADMIN") Color(0xFF1976D2) else Color(0xFF388E3C)
            ) {
                Text(if (role == "ADMIN") "Yönetici" else "Hoca", modifier = Modifier.padding(4.dp))
            }
        }
    }
}
