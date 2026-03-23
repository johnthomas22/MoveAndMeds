package com.jaytt.moveandmeds.ui.info

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.jaytt.moveandmeds.data.model.ContactInfo
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(
    viewModel: InfoViewModel = hiltViewModel(),
    onScanClick: () -> Unit = {},
    prefillValue: String? = null,
    prefillType: String? = null
) {
    val contacts by viewModel.contacts.collectAsState()
    val context = LocalContext.current

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<ContactInfo?>(null) }
    var pendingPrefillValue by remember { mutableStateOf<String?>(null) }
    var pendingPrefillType by remember { mutableStateOf<String?>(null) }

    // Handle incoming prefill from scan result — open dialog once
    LaunchedEffect(prefillValue, prefillType) {
        if (!prefillValue.isNullOrBlank() && prefillType != null && prefillType != "other") {
            val decoded = runCatching {
                URLDecoder.decode(prefillValue, StandardCharsets.UTF_8.name())
            }.getOrDefault(prefillValue)
            pendingPrefillValue = decoded
            pendingPrefillType = prefillType
            editingContact = null
            showAddEditDialog = true
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onScanClick()
        }
    }

    fun launchScanner() {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED -> onScanClick()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (showAddEditDialog) {
        ContactDialog(
            contact = editingContact,
            prefillValue = if (editingContact == null) pendingPrefillValue else null,
            prefillType = if (editingContact == null) pendingPrefillType else null,
            onDismiss = {
                showAddEditDialog = false
                editingContact = null
                pendingPrefillValue = null
                pendingPrefillType = null
            },
            onConfirm = { description, value, type ->
                viewModel.saveContact(editingContact?.id, description, value, type)
                showAddEditDialog = false
                editingContact = null
                pendingPrefillValue = null
                pendingPrefillType = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contact Information") },
                actions = {
                    IconButton(onClick = { launchScanner() }) {
                        Icon(
                            Icons.Default.DocumentScanner,
                            contentDescription = "Scan document"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingContact = null
                pendingPrefillValue = null
                pendingPrefillType = null
                showAddEditDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add contact")
            }
        }
    ) { padding ->
        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No contacts yet. Tap + to add one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(contacts, key = { it.id }) { contact ->
                    ContactCard(
                        contact = contact,
                        onAction = {
                            val intent = when (contact.type) {
                                "email" -> Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contact.value}"))
                                "address" -> Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(contact.value)}"))
                                "url" -> Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(if (contact.value.startsWith("http")) contact.value else "https://${contact.value}")
                                )
                                else -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.value}"))
                            }
                            context.startActivity(intent)
                        },
                        onEdit = {
                            editingContact = contact
                            pendingPrefillValue = null
                            pendingPrefillType = null
                            showAddEditDialog = true
                        },
                        onDelete = { viewModel.deleteContact(contact) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactCard(
    contact: ContactInfo,
    onAction: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete contact?") },
            text = { Text("Remove \"${contact.description}\" from the list?") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    val typeIcon = when (contact.type) {
        "email" -> Icons.Default.Email
        "address" -> Icons.Default.LocationOn
        "url" -> Icons.Default.Language
        else -> Icons.Default.Phone
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = typeIcon,
                contentDescription = contact.type,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    contact.description,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    contact.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onAction) {
                Icon(typeIcon, contentDescription = "Open ${contact.description}")
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactDialog(
    contact: ContactInfo?,
    prefillValue: String? = null,
    prefillType: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (description: String, value: String, type: String) -> Unit
) {
    var description by remember { mutableStateOf(contact?.description ?: "") }
    var value by remember { mutableStateOf(contact?.value ?: prefillValue ?: "") }
    var selectedType by remember {
        mutableStateOf(
            contact?.type ?: when (prefillType) {
                "email", "url", "phone", "address" -> prefillType
                else -> "phone"
            }
        )
    }

    val typeOptions = listOf(
        "phone" to "Phone",
        "email" to "Email",
        "address" to "Address",
        "url" to "URL"
    )

    val valueLabel = when (selectedType) {
        "email" -> "Email address"
        "address" -> "Postal address"
        "url" -> "Web address"
        else -> "Phone number"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (contact == null) "Add Contact" else "Edit Contact") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    typeOptions.chunked(2).forEach { rowItems ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowItems.forEach { (typeKey, typeLabel) ->
                                FilterChip(
                                    selected = selectedType == typeKey,
                                    onClick = { selectedType = typeKey },
                                    label = { Text(typeLabel) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(valueLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(description.trim(), value.trim(), selectedType) },
                enabled = description.isNotBlank() && value.isNotBlank()
            ) {
                Text(if (contact == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
