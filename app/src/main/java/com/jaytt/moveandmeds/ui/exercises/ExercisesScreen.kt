package com.jaytt.moveandmeds.ui.exercises

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
import com.jaytt.moveandmeds.data.model.ExerciseWithTimes
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
    onAddExercise: () -> Unit,
    onEditExercise: (Int) -> Unit,
    onScanClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: ExercisesViewModel = hiltViewModel()
) {
    val exercises by viewModel.exercises.collectAsState()
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
                title = { Text("Exercises") },
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
            FloatingActionButton(onClick = onAddExercise) {
                Icon(Icons.Default.Add, contentDescription = "Add exercise")
            }
        }
    ) { padding ->
        if (exercises.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No exercises added yet. Tap + to add one.",
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
                items(exercises, key = { "exercise_${it.exercise.id}" }) { ewt ->
                    ExerciseCard(
                        ewt = ewt,
                        onEdit = { onEditExercise(ewt.exercise.id) },
                        onDelete = { viewModel.deleteExercise(ewt) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(ewt: ExerciseWithTimes, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${ewt.exercise.name}?") },
            text = { Text("This will remove the exercise and all its reminders.") },
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
                    ewt.exercise.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                val setsReps = buildString {
                    if (ewt.exercise.sets.isNotBlank()) append("${ewt.exercise.sets} sets")
                    if (ewt.exercise.sets.isNotBlank() && ewt.exercise.reps.isNotBlank()) append(" x ")
                    if (ewt.exercise.reps.isNotBlank()) append("${ewt.exercise.reps} reps")
                }
                if (setsReps.isNotBlank()) {
                    Text(
                        setsReps,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (ewt.exercise.reminderType == "interval") {
                    val mins = ewt.exercise.intervalMinutes
                    val label = if (mins < 60) "Every ${mins}m" else "Every ${mins / 60}h"
                    val startH = ewt.exercise.intervalStartHour
                    val endH = ewt.exercise.intervalEndHour
                    Text(
                        "$label, ${String.format(Locale.getDefault(), "%02d:00", startH)}–${String.format(Locale.getDefault(), "%02d:00", endH)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (ewt.times.isNotEmpty()) {
                    Text(
                        ewt.times.joinToString(", ") { t ->
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
