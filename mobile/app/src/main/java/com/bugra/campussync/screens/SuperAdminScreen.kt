package com.bugra.campussync.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugra.campussync.utils.LocalAppStrings
import com.bugra.campussync.viewmodels.SuperAdminViewModel

@Composable
fun SuperAdminScreen() {
    val strings = LocalAppStrings.current
    val viewModel: SuperAdminViewModel = viewModel()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(strings.superAdminInstitutions, strings.superAdminAdminAssign)

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }
        when (selectedTab) {
            0 -> InstitutionManagementTab(viewModel)
            1 -> AdminAssignmentTab(viewModel)
        }
    }
}

@Composable
fun InstitutionManagementTab(viewModel: SuperAdminViewModel) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val state by viewModel.state.collectAsState()
    val institutions = state.institutions
    val isSubmitting = state.isSubmittingInstitution

    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(strings.superAdminAddInstitution, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings.superAdminInstitutionName) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text(strings.superAdminInstitutionType) }, modifier = Modifier.fillMaxWidth(), singleLine = true)

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                when {
                    name.isBlank() -> Toast.makeText(context, strings.superAdminNameEmpty, Toast.LENGTH_SHORT).show()
                    type.isBlank() -> Toast.makeText(context, strings.superAdminTypeEmpty, Toast.LENGTH_SHORT).show()
                    else -> viewModel.createInstitution(
                        name = name, type = type,
                        onSuccess = {
                            Toast.makeText(context, strings.superAdminAdded, Toast.LENGTH_SHORT).show()
                            name = ""; type = ""
                        },
                        onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSubmitting
        ) {
            if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text(strings.add)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("${strings.superAdminExisting} (${institutions.size})", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (institutions.isEmpty()) {
            Text(strings.superAdminNone, color = androidx.compose.ui.graphics.Color.Gray, fontSize = 13.sp)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(institutions) { inst ->
                    ListItem(
                        headlineContent = { Text(inst["name"]?.toString() ?: "—", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text(inst["institution_type"]?.toString() ?: "—") },
                        leadingContent = { Icon(Icons.Default.Business, contentDescription = null) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAssignmentTab(viewModel: SuperAdminViewModel) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val state by viewModel.state.collectAsState()
    val institutions = state.institutions
    val isSubmitting = state.isSubmittingAdmin

    var selectedInstId by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }

    val canSubmit = selectedInstId.isNotEmpty() && username.isNotBlank() && password.isNotBlank() && firstName.isNotBlank() && !isSubmitting

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(strings.superAdminAssignAdmin, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))

        if (institutions.isEmpty()) {
            Text(strings.superAdminAddFirst, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        } else {
            Text(strings.superAdminSelectInstitution, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(institutions) { inst ->
                    val id = inst["id"]?.toString() ?: ""
                    FilterChip(
                        selected = selectedInstId == id,
                        onClick = { selectedInstId = id },
                        label = { Text(inst["name"]?.toString() ?: "—") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text(strings.settingsFirstName) }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text(strings.settingsLastName) }, modifier = Modifier.weight(1f), singleLine = true)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text(strings.loginUsername) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text(strings.loginPassword) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.createAdmin(
                    institutionId = selectedInstId, username = username,
                    password = password, firstName = firstName, lastName = lastName,
                    onSuccess = {
                        Toast.makeText(context, strings.superAdminCreated, Toast.LENGTH_SHORT).show()
                        username = ""; password = ""; firstName = ""; lastName = ""
                    },
                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                )
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = canSubmit
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.superAdminCreating)
            } else {
                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.superAdminCreateAdmin)
            }
        }
    }
}
