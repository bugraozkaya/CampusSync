package com.bugra.campussync.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugra.campussync.utils.LocalAppStrings
import com.bugra.campussync.viewmodels.UserManagementViewModel

@Composable
fun UserManagementScreen() {
    val strings = LocalAppStrings.current
    val viewModel: UserManagementViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val userList = state.users
    val isLoading = state.isLoading
    val errorMessage = state.error ?: ""

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(strings.usersTitle, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(strings.usersSubtitle, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearch(it) },
            placeholder = { Text(strings.usersSearch) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge
        )

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(strings.loading, fontSize = 13.sp, color = Color.Gray)
                    }
                }
            }
            errorMessage.isNotEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMessage, color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.load() }) { Text(strings.retry) }
                    }
                }
            }
            userList.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Group, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            if (searchQuery.isBlank()) strings.usersNone else "\"$searchQuery\" ${strings.usersNoResults}",
                            color = Color.Gray
                        )
                    }
                }
            }
            else -> {
                Text("${userList.size} ${strings.people}", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(userList) { user -> UserCard(user) }
                }
            }
        }
    }
}

@Composable
fun UserCard(user: Map<String, Any>) {
    val strings = LocalAppStrings.current
    val firstName = user["first_name"]?.toString() ?: ""
    val lastName = user["last_name"]?.toString() ?: ""
    val username = user["username"]?.toString() ?: ""
    val fullName = if (firstName.isEmpty() && lastName.isEmpty()) username else "$firstName $lastName".trim()
    val role = user["role"]?.toString()?.uppercase() ?: ""
    val dept = user["department_name"]?.toString() ?: user["department"]?.toString() ?: ""

    val roleLabel = when (role) {
        "ADMIN"      -> strings.roleAdmin
        "LECTURER"   -> strings.roleLecturer
        "SUPERADMIN" -> strings.roleSuperAdmin
        "SUPER"      -> strings.roleSuperAdmin
        "STAFF"      -> strings.roleStaff
        "IT"         -> strings.roleIT
        "STUDENT"    -> strings.roleStudent
        else         -> role.ifEmpty { strings.roleUnknown }
    }

    val roleColor = when (role) {
        "SUPER"    -> Color(0xFF7B1FA2) to Color(0xFFF3E5F5)
        "ADMIN"    -> Color(0xFF1976D2) to Color(0xFFE3F2FD)
        "LECTURER" -> Color(0xFF388E3C) to Color(0xFFF1F8E9)
        "IT"       -> Color(0xFF0288D1) to Color(0xFFE1F5FE)
        else       -> Color(0xFF616161) to Color(0xFFF5F5F5)
    }

    val roleIcon = when (role) {
        "SUPER" -> Icons.Default.SupervisorAccount
        "ADMIN" -> Icons.Default.AdminPanelSettings
        "IT"    -> Icons.Default.School
        else    -> Icons.Default.Person
    }

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = roleIcon, contentDescription = null, modifier = Modifier.size(38.dp), tint = roleColor.first)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(text = "@$username", fontSize = 11.sp, color = Color.Gray)
                if (dept.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = dept, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(color = roleColor.second, shape = MaterialTheme.shapes.small) {
                Text(
                    text = roleLabel,
                    color = roleColor.first,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
