package com.jaytt.moveandmeds.ui.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

// Regex patterns for exercise parsing
private val setsRegex = Regex(
    """(\d{1,2})\s*(?:sets?|x\b)""",
    RegexOption.IGNORE_CASE
)
private val repsRegex = Regex(
    """(\d{1,3})\s*(?:reps?|repetitions?|times?)""",
    RegexOption.IGNORE_CASE
)
private val exerciseCommonWords = setOf(
    "the", "and", "for", "use", "only", "not", "this", "that", "your", "you",
    "are", "have", "been", "from", "into", "they", "were", "sets", "reps",
    "repetitions", "times", "exercise", "workout", "with", "each", "per"
)

// Interval detection patterns
private val intervalPatterns = listOf(
    Regex("""frequently""", RegexOption.IGNORE_CASE),
    Regex("""regularly""", RegexOption.IGNORE_CASE),
    Regex("""throughout the day""", RegexOption.IGNORE_CASE),
    Regex("""hourly""", RegexOption.IGNORE_CASE),
    Regex("""every\s+\d+\s+hours?""", RegexOption.IGNORE_CASE),
    Regex("""every\s+\d+\s+minutes?""", RegexOption.IGNORE_CASE)
)
private val timedPatterns = listOf(
    Regex("""\d+\s*times?\s+a\s+day""", RegexOption.IGNORE_CASE),
    Regex("""\d+\s*x\s+a\s+day""", RegexOption.IGNORE_CASE),
    Regex("""once\s+a\s+day""", RegexOption.IGNORE_CASE),
    Regex("""twice\s+a\s+day""", RegexOption.IGNORE_CASE)
)
// Match "every X hours/minutes" to extract the number
private val everyXHoursRegex = Regex("""every\s+(\d+)\s+hours?""", RegexOption.IGNORE_CASE)
private val everyXMinsRegex = Regex("""every\s+(\d+)\s+minutes?""", RegexOption.IGNORE_CASE)

data class ExerciseScannedItem(val value: String, val section: ExerciseScanSection)

enum class ExerciseScanSection { NAME, SETS, REPS, NOTES }

data class SuggestedReminderType(
    val type: String,       // "timed" or "interval"
    val intervalMinutes: Int = 60
)

fun suggestReminderType(raw: String): SuggestedReminderType {
    val hasInterval = intervalPatterns.any { it.containsMatchIn(raw) }
    val hasTimed = timedPatterns.any { it.containsMatchIn(raw) }

    if (hasInterval && !hasTimed) {
        // Try to extract interval minutes
        val hoursMatch = everyXHoursRegex.find(raw)
        val minsMatch = everyXMinsRegex.find(raw)
        val intervalMins = when {
            hoursMatch != null -> (hoursMatch.groupValues[1].toIntOrNull() ?: 1) * 60
            minsMatch != null -> minsMatch.groupValues[1].toIntOrNull() ?: 60
            else -> 60
        }
        return SuggestedReminderType("interval", intervalMins.coerceIn(15, 240))
    }
    return SuggestedReminderType("timed")
}

fun parseExerciseText(raw: String): Map<ExerciseScanSection, List<ExerciseScannedItem>> {
    val nameItems = mutableListOf<ExerciseScannedItem>()
    val setsItems = mutableListOf<ExerciseScannedItem>()
    val repsItems = mutableListOf<ExerciseScannedItem>()
    val notesItems = mutableListOf<ExerciseScannedItem>()

    val matchedRanges = mutableListOf<IntRange>()

    // Find sets matches
    setsRegex.findAll(raw).forEach { match ->
        val num = match.groupValues[1].toIntOrNull() ?: 0
        if (num in 1..20) {
            setsItems.add(ExerciseScannedItem(match.groupValues[1], ExerciseScanSection.SETS))
            matchedRanges.add(match.range)
        }
    }

    // Find reps matches
    repsRegex.findAll(raw).forEach { match ->
        val alreadyCovered = matchedRanges.any { it.intersects(match.range) }
        if (!alreadyCovered) {
            val num = match.groupValues[1].toIntOrNull() ?: 0
            if (num in 1..1000) {
                repsItems.add(ExerciseScannedItem(match.groupValues[1], ExerciseScanSection.REPS))
                matchedRanges.add(match.range)
            }
        }
    }

    // Other text blocks become name candidates or notes
    raw.lines().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.length > 3) {
            val lineStart = raw.indexOf(trimmed)
            if (lineStart >= 0) {
                val lineRange = lineStart until (lineStart + trimmed.length)
                val alreadyCovered = matchedRanges.any { it.intersects(lineRange) }
                if (!alreadyCovered) {
                    val words = trimmed.split(Regex("\\s+"))
                    val isNameCandidate = words.any { word ->
                        val cleaned = word.lowercase().replace(Regex("[^a-z]"), "")
                        cleaned.length > 3 &&
                            !cleaned.all { it.isDigit() } &&
                            cleaned !in exerciseCommonWords
                    }
                    if (isNameCandidate && trimmed.length <= 60) {
                        nameItems.add(ExerciseScannedItem(trimmed, ExerciseScanSection.NAME))
                    } else if (trimmed.length > 3) {
                        notesItems.add(ExerciseScannedItem(trimmed, ExerciseScanSection.NOTES))
                    }
                }
            }
        }
    }

    return buildMap {
        if (nameItems.isNotEmpty()) put(ExerciseScanSection.NAME, nameItems.distinctBy { it.value })
        if (setsItems.isNotEmpty()) put(ExerciseScanSection.SETS, setsItems.distinctBy { it.value })
        if (repsItems.isNotEmpty()) put(ExerciseScanSection.REPS, repsItems.distinctBy { it.value })
        if (notesItems.isNotEmpty()) put(ExerciseScanSection.NOTES, notesItems.distinctBy { it.value })
    }
}

private fun IntRange.intersects(other: IntRange): Boolean =
    first <= other.last && last >= other.first

private fun ExerciseScanSection.displayTitle() = when (this) {
    ExerciseScanSection.NAME -> "Possible exercise name"
    ExerciseScanSection.SETS -> "Sets"
    ExerciseScanSection.REPS -> "Reps"
    ExerciseScanSection.NOTES -> "Notes / instructions"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseScanResultScreen(
    encodedText: String,
    onBack: () -> Unit,
    onAddExercise: (name: String, sets: String, reps: String, notes: String, reminderType: String, intervalMinutes: Int) -> Unit
) {
    val rawText = runCatching {
        URLDecoder.decode(encodedText, StandardCharsets.UTF_8.name())
    }.getOrDefault(encodedText)

    val sections = parseExerciseText(rawText)
    val suggested = remember(rawText) { suggestReminderType(rawText) }

    var selectedReminderType by remember(suggested) { mutableStateOf(suggested.type) }
    var selectedName by remember { mutableStateOf("") }
    var selectedSets by remember { mutableStateOf("") }
    var selectedReps by remember { mutableStateOf("") }
    val selectedNotes = remember { mutableStateListOf<String>() }

    val hasSelection = selectedName.isNotBlank() || selectedSets.isNotBlank() ||
        selectedReps.isNotBlank() || selectedNotes.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercise Scan Results") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (hasSelection) {
                Surface(tonalElevation = 4.dp) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (selectedName.isNotBlank())
                            Text("Name: $selectedName", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        if (selectedSets.isNotBlank())
                            Text("Sets: $selectedSets", style = MaterialTheme.typography.bodySmall)
                        if (selectedReps.isNotBlank())
                            Text("Reps: $selectedReps", style = MaterialTheme.typography.bodySmall)
                        if (selectedNotes.isNotEmpty())
                            Text("Notes: ${selectedNotes.joinToString("; ")}", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                onAddExercise(
                                    selectedName,
                                    selectedSets,
                                    selectedReps,
                                    selectedNotes.joinToString("\n"),
                                    selectedReminderType,
                                    suggested.intervalMinutes
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedName.isNotBlank()
                        ) {
                            Text("Add Exercise")
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (sections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No recognisable exercise information found.",
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "Tap to select name, sets, reps and notes, then tap Add Exercise.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Suggested reminder type section
                item {
                    Text(
                        "Suggested reminder type",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedReminderType == "timed",
                            onClick = { selectedReminderType = "timed" },
                            label = { Text("At set times") }
                        )
                        FilterChip(
                            selected = selectedReminderType == "interval",
                            onClick = { selectedReminderType = "interval" },
                            label = { Text("At intervals") }
                        )
                    }
                }

                val sectionOrder = listOf(
                    ExerciseScanSection.NAME,
                    ExerciseScanSection.SETS,
                    ExerciseScanSection.REPS,
                    ExerciseScanSection.NOTES
                )
                sectionOrder.forEach { section ->
                    val sectionItems = sections[section] ?: return@forEach
                    item(key = "header_${section.name}") {
                        Text(
                            text = section.displayTitle(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(sectionItems, key = { "${section.name}_${it.value}" }) { scannedItem ->
                        val isSelected = when (section) {
                            ExerciseScanSection.NAME -> selectedName == scannedItem.value
                            ExerciseScanSection.SETS -> selectedSets == scannedItem.value
                            ExerciseScanSection.REPS -> selectedReps == scannedItem.value
                            ExerciseScanSection.NOTES -> selectedNotes.contains(scannedItem.value)
                        }
                        ExerciseScannedItemCard(
                            item = scannedItem,
                            isSelected = isSelected,
                            onClick = {
                                when (section) {
                                    ExerciseScanSection.NAME ->
                                        selectedName = if (isSelected) "" else scannedItem.value
                                    ExerciseScanSection.SETS ->
                                        selectedSets = if (isSelected) "" else scannedItem.value
                                    ExerciseScanSection.REPS ->
                                        selectedReps = if (isSelected) "" else scannedItem.value
                                    ExerciseScanSection.NOTES ->
                                        if (isSelected) selectedNotes.remove(scannedItem.value)
                                        else selectedNotes.add(scannedItem.value)
                                }
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun ExerciseScannedItemCard(
    item: ExerciseScannedItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isNotes = item.section == ExerciseScanSection.NOTES
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                isNotes -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.value,
                style = if (isNotes) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
