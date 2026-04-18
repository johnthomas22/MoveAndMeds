package com.jaytt.moveandmeds.ui.exercises

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.jaytt.moveandmeds.data.model.ExerciseWithTimes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
    onAddExercise: () -> Unit,
    onEditExercise: (Int) -> Unit,
    onScanClick: () -> Unit,
    onPdfScanResult: (encodedText: String) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: ExercisesViewModel = hiltViewModel()
) {
    val exercises by viewModel.exercises.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isProcessingPdf by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onScanClick()
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isProcessingPdf = true
            try {
                val encodedText = withContext(Dispatchers.IO) {
                    // Extract text directly from the PDF — no render/OCR needed
                    com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context)
                    val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                    val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputStream)
                    val text = com.tom_roush.pdfbox.text.PDFTextStripper().getText(document)

                    // Extract embedded images — one per exercise diagram
                    val pdfImagesDir = File(context.cacheDir, "pdf_images")
                    pdfImagesDir.deleteRecursively()
                    pdfImagesDir.mkdirs()
                    var imageIndex = 0
                    for (page in document.pages) {
                        for (name in page.resources.xObjectNames) {
                            val xObject = page.resources.getXObject(name)
                            if (xObject is com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject) {
                                val bmp: Bitmap? = xObject.image
                                if (bmp != null && bmp.width >= 60 && bmp.height >= 60) {
                                    FileOutputStream(File(pdfImagesDir, "$imageIndex.jpg")).use {
                                        bmp.compress(Bitmap.CompressFormat.JPEG, 85, it)
                                    }
                                    bmp.recycle()
                                    imageIndex++
                                }
                            }
                        }
                    }

                    document.close()
                    inputStream.close()

                    URLEncoder.encode(text, StandardCharsets.UTF_8.name())
                }
                if (encodedText != null) onPdfScanResult(encodedText)
            } catch (_: Exception) {
            } finally {
                isProcessingPdf = false
            }
        }
    }

    fun launchScanner() {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED -> onScanClick()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (isProcessingPdf) {
        Dialog(onDismissRequest = {}) {
            Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 6.dp) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Reading PDF…")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercises") },
                actions = {
                    IconButton(onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }) {
                        Icon(Icons.Default.Description, contentDescription = "Import PDF")
                    }
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
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
                    if (ewt.exercise.sets.isNotBlank() && ewt.exercise.reps.isNotBlank()) append(", ")
                    if (ewt.exercise.reps.isNotBlank()) append("repeat ${ewt.exercise.reps} times")
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
