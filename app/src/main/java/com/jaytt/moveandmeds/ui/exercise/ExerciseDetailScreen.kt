package com.jaytt.moveandmeds.ui.exercise

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jaytt.moveandmeds.ui.medicine.DaysOfWeekSelector
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exerciseId: Int?,
    onBack: () -> Unit,
    onHistory: ((Int) -> Unit)? = null,
    prefillName: String? = null,
    prefillSets: String? = null,
    prefillReps: String? = null,
    prefillReminderType: String? = null,
    prefillIntervalMinutes: Int? = null,
    viewModel: ExerciseViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf(prefillName ?: "") }
    var sets by remember { mutableStateOf(prefillSets ?: "") }
    var reps by remember { mutableStateOf(prefillReps ?: "") }
    var notes by remember { mutableStateOf("") }
    var times by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }
    var daysOfWeek by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) }
    var loaded by remember { mutableStateOf(false) }

    // Interval state
    var reminderType by remember { mutableStateOf(prefillReminderType ?: "timed") }
    var intervalMinutes by remember { mutableStateOf(prefillIntervalMinutes ?: 60) }
    var intervalStartHour by remember { mutableStateOf(8) }
    var intervalEndHour by remember { mutableStateOf(22) }

    val context = LocalContext.current

    LaunchedEffect(exerciseId) {
        if (exerciseId != null && !loaded) {
            val ewt = viewModel.getExercise(exerciseId)
            ewt?.let {
                name = it.exercise.name
                sets = it.exercise.sets
                reps = it.exercise.reps
                notes = it.exercise.notes
                times = it.times.map { t -> t.hour to t.minute }
                daysOfWeek = it.exercise.daysOfWeek.split(",")
                    .mapNotNull { d -> d.trim().toIntOrNull() }.toSet()
                reminderType = it.exercise.reminderType
                intervalMinutes = it.exercise.intervalMinutes
                intervalStartHour = it.exercise.intervalStartHour
                intervalEndHour = it.exercise.intervalEndHour
            }
            loaded = true
        } else if (exerciseId == null) {
            loaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (exerciseId == null) "Add Exercise" else "Edit Exercise") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (exerciseId != null && onHistory != null) {
                        IconButton(onClick = { onHistory(exerciseId) }) {
                            Icon(Icons.Default.DateRange, contentDescription = "History")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = sets,
                        onValueChange = { sets = it },
                        label = { Text("Sets (e.g. 3)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it },
                        label = { Text("Reps (e.g. 10)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
            item {
                Text("Active days", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                DaysOfWeekSelector(
                    selectedDays = daysOfWeek,
                    onSelectionChanged = { daysOfWeek = it }
                )
            }

            // Reminder type toggle
            item {
                Text("Reminder type", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = reminderType == "timed",
                        onClick = { reminderType = "timed" },
                        label = { Text("At set times") }
                    )
                    FilterChip(
                        selected = reminderType == "interval",
                        onClick = { reminderType = "interval" },
                        label = { Text("At intervals") }
                    )
                }
            }

            if (reminderType == "timed") {
                item {
                    Text("Reminder times", style = MaterialTheme.typography.labelLarge)
                }
                itemsIndexed(times) { index, (hour, minute) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(context, { _, h, m ->
                                    times = times.toMutableList().also { it[index] = h to m }
                                }, hour, minute, true).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(String.format(Locale.getDefault(), "%02d:%02d", hour, minute))
                        }
                        IconButton(onClick = {
                            times = times.toMutableList().also { it.removeAt(index) }
                        }) {
                            Icon(Icons.Default.Delete, "Remove time")
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = {
                            val now = Calendar.getInstance()
                            times = times + (now.get(Calendar.HOUR_OF_DAY) to now.get(Calendar.MINUTE))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add time")
                    }
                }
            } else {
                // Interval mode
                item {
                    Text("Remind me every:", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    val intervalOptions = listOf(30, 60, 120, 180, 240)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        intervalOptions.forEach { mins ->
                            FilterChip(
                                selected = intervalMinutes == mins,
                                onClick = { intervalMinutes = mins },
                                label = { Text(if (mins < 60) "${mins}m" else "${mins / 60}h") }
                            )
                        }
                    }
                }
                item {
                    Text("Active hours:", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("From")
                        ExerciseHourDropdown(intervalStartHour) { intervalStartHour = it }
                        Text("to")
                        ExerciseHourDropdown(intervalEndHour) { intervalEndHour = it }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val daysStr = daysOfWeek.sorted().joinToString(",")
                            viewModel.saveExercise(
                                id = exerciseId,
                                name = name,
                                sets = sets,
                                reps = reps,
                                notes = notes,
                                times = times,
                                daysOfWeek = daysStr,
                                reminderType = reminderType,
                                intervalMinutes = intervalMinutes,
                                intervalStartHour = intervalStartHour,
                                intervalEndHour = intervalEndHour,
                                onDone = onBack
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank()
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun ExerciseHourDropdown(selectedHour: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(String.format("%02d:00", selectedHour))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (0..23).forEach { hour ->
                DropdownMenuItem(
                    text = { Text(String.format("%02d:00", hour)) },
                    onClick = { onSelect(hour); expanded = false }
                )
            }
        }
    }
}
