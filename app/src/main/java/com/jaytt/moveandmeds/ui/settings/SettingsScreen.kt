package com.jaytt.moveandmeds.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jaytt.moveandmeds.data.model.MovementSettings
import com.jaytt.moveandmeds.ui.movement.MovementViewModel
import com.jaytt.moveandmeds.util.RecoveryHelper
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    viewModel: MovementViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    var localSettings by remember(settings) { mutableStateOf(settings ?: MovementSettings()) }

    val context = LocalContext.current
    var recoveryStartDate by remember {
        mutableStateOf(RecoveryHelper.getRecoveryStartDate(context))
    }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = recoveryStartDate
                ?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    val date = millis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    RecoveryHelper.setRecoveryStartDate(context, date)
                    recoveryStartDate = date
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Movement Reminders section header
            Text(
                "Movement Reminders",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable movement reminders", modifier = Modifier.weight(1f))
                Switch(
                    checked = localSettings.isEnabled,
                    onCheckedChange = { localSettings = localSettings.copy(isEnabled = it) }
                )
            }

            Text("Remind me every:", style = MaterialTheme.typography.labelLarge)
            val intervals = listOf(15, 20, 30, 45, 60, 90, 120)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                intervals.forEach { mins ->
                    FilterChip(
                        selected = localSettings.intervalMinutes == mins,
                        onClick = { localSettings = localSettings.copy(intervalMinutes = mins) },
                        label = { Text(if (mins < 60) "${mins}m" else "${mins / 60}h") }
                    )
                }
            }

            Text("Active hours:", style = MaterialTheme.typography.labelLarge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("From")
                HourDropdown(localSettings.startHour) { localSettings = localSettings.copy(startHour = it) }
                Text("to")
                HourDropdown(localSettings.endHour) { localSettings = localSettings.copy(endHour = it) }
            }

            Text("Skip reminder if I've taken at least X steps:", style = MaterialTheme.typography.labelLarge)
            val stepOptions = listOf(25, 50, 100, 200, 500)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                stepOptions.forEach { steps ->
                    FilterChip(
                        selected = localSettings.stepThreshold == steps,
                        onClick = { localSettings = localSettings.copy(stepThreshold = steps) },
                        label = { Text("$steps") }
                    )
                }
            }

            Button(
                onClick = { viewModel.saveSettings(localSettings) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Movement Settings")
            }

            HorizontalDivider()

            // Recovery section
            Text(
                "Recovery",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Set your operation or recovery start date to see daily progress and motivational messages.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (recoveryStartDate != null) {
                val formatter = DateTimeFormatter.ofPattern("d MMM yyyy")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Started: ${recoveryStartDate!!.format(formatter)}")
                    }
                    IconButton(onClick = {
                        RecoveryHelper.setRecoveryStartDate(context, null)
                        recoveryStartDate = null
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear recovery date")
                    }
                }
                Text(
                    RecoveryHelper.getMotivationalMessage(recoveryStartDate!!),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Set Recovery Start Date")
                }
            }

            HorizontalDivider()

            // About section header
            Text(
                "About",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                onClick = onPrivacyPolicy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Privacy Policy",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HourDropdown(selectedHour: Int, onSelect: (Int) -> Unit) {
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
