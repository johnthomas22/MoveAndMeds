package com.jaytt.moveandmeds.ui.medicines

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.jaytt.moveandmeds.data.model.MedicineWithTimes
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicinesScreen(
    onAddMedicine: () -> Unit,
    onEditMedicine: (Int) -> Unit,
    onScanClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: MedicinesViewModel = hiltViewModel()
) {
    val medicines by viewModel.medicines.collectAsState()
    val context = LocalContext.current

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onScanClick()
    }

    fun launchScanner() {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED -> onScanClick()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medicines") },
                actions = {
                    IconButton(onClick = { launchScanner() }) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = "Scan")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMedicine) {
                Icon(Icons.Default.Add, contentDescription = "Add medicine")
            }
        }
    ) { padding ->
        if (medicines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No medicines added yet. Tap + to add one.",
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
                items(medicines, key = { "medicine_${it.medicine.id}" }) { mwt ->
                    MedicineCard(
                        mwt = mwt,
                        onEdit = { onEditMedicine(mwt.medicine.id) },
                        onDelete = { viewModel.deleteMedicine(mwt) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MedicineCard(mwt: MedicineWithTimes, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${mwt.medicine.name}?") },
            text = { Text("This will remove the medicine and all its reminders.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    mwt.medicine.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (mwt.medicine.dosage.isNotBlank()) {
                    Text(
                        mwt.medicine.dosage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (mwt.times.isNotEmpty()) {
                    Text(
                        mwt.times.joinToString(", ") { t ->
                            String.format(Locale.getDefault(), "%02d:%02d", t.hour, t.minute)
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit") }
            IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, "Delete") }
        }
    }
}
