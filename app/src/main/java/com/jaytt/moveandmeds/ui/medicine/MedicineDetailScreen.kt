package com.jaytt.moveandmeds.ui.medicine

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
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineDetailScreen(
    medicineId: Int?,
    onBack: () -> Unit,
    onHistory: ((Int) -> Unit)? = null,
    prefillName: String? = null,
    prefillDosage: String? = null,
    viewModel: MedicineViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf(prefillName ?: "") }
    var dosage by remember { mutableStateOf(prefillDosage ?: "") }
    var notes by remember { mutableStateOf("") }
    var times by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }
    var daysOfWeek by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) }
    var loaded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(medicineId) {
        if (medicineId != null && !loaded) {
            val mwt = viewModel.getMedicine(medicineId)
            mwt?.let {
                name = it.medicine.name
                dosage = it.medicine.dosage
                notes = it.medicine.notes
                times = it.times.map { t -> t.hour to t.minute }
                daysOfWeek = it.medicine.daysOfWeek.split(",")
                    .mapNotNull { d -> d.trim().toIntOrNull() }.toSet()
            }
            loaded = true
        } else if (medicineId == null) {
            loaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (medicineId == null) "Add Medicine" else "Edit Medicine") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (medicineId != null && onHistory != null) {
                        IconButton(onClick = { onHistory(medicineId) }) {
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
                    label = { Text("Medicine name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosage (e.g. 1 tablet, 5ml)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
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
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val daysStr = daysOfWeek.sorted().joinToString(",")
                            viewModel.saveMedicine(medicineId, name, dosage, notes, times, daysStr, onBack)
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
fun DaysOfWeekSelector(
    selectedDays: Set<Int>,
    onSelectionChanged: (Set<Int>) -> Unit
) {
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        dayLabels.forEachIndexed { index, label ->
            val day = index + 1
            val isSelected = day in selectedDays
            FilterChip(
                selected = isSelected,
                onClick = {
                    val newDays = if (isSelected) {
                        selectedDays - day
                    } else {
                        selectedDays + day
                    }
                    onSelectionChanged(newDays)
                },
                label = { Text(label) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
