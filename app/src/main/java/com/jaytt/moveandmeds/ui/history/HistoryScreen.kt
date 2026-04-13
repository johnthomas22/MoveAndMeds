package com.jaytt.moveandmeds.ui.history

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    itemType: String,
    itemId: Int,
    itemName: String,
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val historyState = remember(itemType, itemId) {
        viewModel.getHistoryForItem(itemType, itemId)
    }
    val history by historyState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.pruneOldHistory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History: $itemName") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { exportHistory(context, history) }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                }
            )
        }
    ) { padding ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No history yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history, key = { it.id }) { record ->
                    HistoryItem(record)
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(record: ReminderHistory) {
    val dateFormat = remember { SimpleDateFormat("EEE dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val (statusLabel, bgColor, labelColor) = when (record.status) {
        "taken_medicine" -> Triple("Taken", Color(0xFF4CAF50).copy(alpha = 0.15f), Color(0xFF2E7D32))
        "taken_exercise" -> Triple("Done", Color(0xFF4CAF50).copy(alpha = 0.15f), Color(0xFF2E7D32))
        "skipped_medicine", "skipped_exercise" -> Triple("Skipped", Color(0xFFFF6F00).copy(alpha = 0.15f), Color(0xFFE65100))
        "dismissed" -> Triple("Dismissed", Color(0xFF9E9E9E).copy(alpha = 0.15f), Color(0xFF616161))
        "missed" -> Triple("Missed", Color(0xFFF44336).copy(alpha = 0.15f), Color(0xFFC62828))
        else -> Triple("Fired", Color(0xFF4CAF50).copy(alpha = 0.15f), Color(0xFF2E7D32))
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dateFormat.format(Date(record.scheduledTime)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    record.itemName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(shape = MaterialTheme.shapes.small, color = bgColor) {
                Text(
                    text = statusLabel,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = labelColor
                )
            }
        }
    }
}

private fun exportHistory(context: Context, history: List<ReminderHistory>) {
    val uri = CsvExporter.exportHistory(context, history)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Export history"))
}
