package com.jaytt.moveandmeds.ui.history

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jaytt.moveandmeds.data.model.ReminderHistory
import com.jaytt.moveandmeds.util.CsvExporter
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val allHistory by viewModel.allHistory.collectAsState()

    LaunchedEffect(Unit) { viewModel.pruneOldHistory() }

    // Group by day (descending), then by type within each day
    val grouped: List<Pair<String, Map<String, List<ReminderHistory>>>> = remember(allHistory) {
        val dayFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy")
        allHistory
            .groupBy { record ->
                Instant.ofEpochMilli(record.scheduledTime)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
            .entries
            .sortedByDescending { it.key }
            .map { (date, records) ->
                val label = dayFormatter.format(date)
                val byType = records
                    .groupBy { it.itemType }
                    .entries
                    .sortedBy { typeOrder(it.key) }
                    .associate { it.key to it.value.sortedByDescending { r -> r.scheduledTime } }
                label to byType
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Log") },
                actions = {
                    IconButton(onClick = { exportAll(context, allHistory) }) {
                        Icon(Icons.Default.Share, contentDescription = "Export all")
                    }
                }
            )
        }
    ) { padding ->
        if (grouped.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No activity recorded yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                grouped.forEach { (dayLabel, byType) ->
                    item(key = "day_$dayLabel") {
                        Text(
                            dayLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    byType.forEach { (type, records) ->
                        item(key = "type_${dayLabel}_$type") {
                            Text(
                                typeLabel(type),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                            )
                        }
                        items(records, key = { it.id }) { record ->
                            LogEntry(record)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntry(record: ReminderHistory) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(record.itemName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    timeFormat.format(Date(record.scheduledTime)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when (record.status) {
                "taken_medicine", "taken_exercise" -> Icon(
                    Icons.Default.Check,
                    contentDescription = "Taken",
                    tint = Color(0xFF4CAF50)
                )
                "skipped_medicine", "skipped_exercise" -> Surface(
                    shape = MaterialTheme.shapes.small,
                    color = Color(0xFFFF6F00).copy(alpha = 0.12f)
                ) {
                    Text(
                        "Skipped",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                }
                "dismissed" -> Icon(
                    Icons.Default.Check,
                    contentDescription = "Dismissed",
                    tint = Color(0xFF9E9E9E)
                )
                "missed" -> Surface(shape = MaterialTheme.shapes.small, color = Color(0xFFF44336).copy(alpha = 0.12f)) {
                    Text(
                        "Missed",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                }
            }
        }
    }
}

private fun typeOrder(type: String) = when (type) {
    "medicine" -> 0
    "exercise" -> 1
    else -> 2
}

private fun typeLabel(type: String) = when (type) {
    "medicine" -> "Medicines"
    "exercise" -> "Exercises"
    else -> "Movement"
}

private fun exportAll(context: Context, history: List<ReminderHistory>) {
    val uri = CsvExporter.exportHistory(context, history)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Export log"))
}
