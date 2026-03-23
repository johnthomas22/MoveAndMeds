package com.jaytt.moveandmeds.ui.medicines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

// Regex patterns for medicine parsing
private val dosageRegex = Regex(
    """(\d+(?:\.\d+)?)\s*(mg|ml|mcg|g|tablet|capsule|tab|cap)s?""",
    RegexOption.IGNORE_CASE
)
private val timeRegex = Regex(
    """(?:once|twice|three times|four times|(?:\d+)\s*times?)(?:\s*(?:a|per)\s*(?:day|week))?|every\s+\d+\s*hours?|(?:morning|evening|night|bedtime|at bedtime|\d{1,2}:\d{2}(?:\s*[ap]m)?|\d{1,2}\s*[ap]m)""",
    RegexOption.IGNORE_CASE
)

// ALL-CAPS phrases that are definitely not medicine names
private val excludedAllCapsFragments = setOf(
    "PRIVATE", "CONFIDENTIAL", "MEDICATION", "DISCHARGE", "PRESCRIPTION",
    "TABLETS", "CAPSULES", "NOT", "APPLICABLE", "ALLERG", "ADVERSE",
    "REACTION", "CLINICAL", "SUMMARY", "INFORMATION", "WARD", "HOSPITAL",
    "HEALTHCARE", "NHS", "PATIENT", "DATE", "NONE", "DRUG", "OTHER",
    "PLEASE", "SPECIFY", "ROUTE", "QUANTITY", "PRESCRIBER", "SIGNATURE",
    "PHARMACIST", "VTE", "ADVICE", "REVIEW", "SUPPLY", "DIRECTIONS"
)

// Words and phrases that are form labels / boilerplate — not medicine names
private val excludedWords = setOf(
    "drug", "none", "date", "free", "tablets", "tablet", "capsule", "capsules",
    "clinical", "summary", "information", "given", "advice", "discharge", "please",
    "specify", "other", "location", "dose", "doses", "frequency", "route", "quantity",
    "prescribed", "prescriber", "signature", "pharmacist", "patient", "name", "ward",
    "hospital", "healthcare", "spire", "nhs", "medication", "prescription", "not",
    "this", "that", "your", "with", "food", "water", "before", "after", "take",
    "daily", "each", "the", "and", "for", "use", "only", "from", "into", "they",
    "were", "have", "been", "are", "you", "per", "vte", "leaflet", "allergy",
    "allergies", "review", "supply", "directions", "instructions", "once", "twice",
    "morning", "evening", "night", "reason", "brand", "generic", "strength", "form",
    "oral", "topical", "injection", "intravenous", "intramuscular", "subcutaneous",
    "npf", "sugar", "applicable", "infection", "status", "pharm", "suppli",
    "causative", "agents", "description", "reaction", "known", "new", "private",
    "confidential", "pollen"
)

data class MedicineScannedItem(val value: String, val section: MedicineScanSection)

enum class MedicineScanSection { NAME, DOSAGE, TIME, OTHER }

// Returns true if a line looks like OCR noise (too many non-alphabetic characters)
private fun isOcrNoise(text: String): Boolean {
    if (text.isBlank()) return true
    val alphaSpace = text.count { it.isLetter() || it.isWhitespace() }
    val ratio = alphaSpace.toFloat() / text.length
    return ratio < 0.6f  // more than 40% non-alpha = likely noise
}

// Returns true if a line is all-caps (likely a medicine name from a hospital sheet)
private fun isAllCaps(text: String): Boolean {
    val letters = text.filter { it.isLetter() }
    return letters.isNotEmpty() && letters.all { it.isUpperCase() }
}

// Returns true if the line is just a generic form label
private fun isFormLabel(text: String): Boolean {
    val words = text.trim().lowercase().split(Regex("\\s+"))
    return words.all { w ->
        val cleaned = w.replace(Regex("[^a-z]"), "")
        cleaned.isEmpty() || cleaned in excludedWords || cleaned.length <= 2
    }
}

fun parseMedicineText(raw: String): Map<MedicineScanSection, List<MedicineScannedItem>> {
    val dosageItems = mutableListOf<MedicineScannedItem>()
    val timeItems = mutableListOf<MedicineScannedItem>()
    val nameItems = mutableListOf<MedicineScannedItem>()
    val otherItems = mutableListOf<MedicineScannedItem>()

    val matchedRanges = mutableListOf<IntRange>()

    // 1. Find dosage matches in full text
    dosageRegex.findAll(raw).forEach { match ->
        dosageItems.add(MedicineScannedItem(match.value.trim(), MedicineScanSection.DOSAGE))
        matchedRanges.add(match.range)
    }

    // 2. Find time/frequency matches
    timeRegex.findAll(raw).forEach { match ->
        if (matchedRanges.none { it.intersects(match.range) }) {
            timeItems.add(MedicineScannedItem(match.value.trim(), MedicineScanSection.TIME))
            matchedRanges.add(match.range)
        }
    }

    // 3. Process lines — combine consecutive ALL-CAPS lines into single medicine name candidates
    val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
    var i = 0
    while (i < lines.size) {
        val line = lines[i]

        // Skip OCR noise
        if (isOcrNoise(line)) { i++; continue }

        // Combine consecutive ALL-CAPS lines (medicine names on hospital sheets are ALL CAPS)
        if (isAllCaps(line) && !isFormLabel(line) && line.length > 3) {
            val combined = StringBuilder(line)
            var j = i + 1
            while (j < lines.size && isAllCaps(lines[j]) && !isOcrNoise(lines[j]) && !isFormLabel(lines[j])) {
                combined.append(" ").append(lines[j])
                j++
            }
            val name = combined.toString().trim()
            // Skip if it contains any excluded ALL-CAPS fragment
            val containsExcluded = excludedAllCapsFragments.any { frag -> name.contains(frag) }
            if (!isFormLabel(name) && !containsExcluded) {
                nameItems.add(MedicineScannedItem(name, MedicineScanSection.NAME))
            }
            i = j
            continue
        }

        // Skip form labels and short entries
        if (isFormLabel(line) || line.length < 5) { i++; continue }
        // Skip entries containing a colon (form labels like "Infection Status:", "Date:")
        if (line.contains(':') && line.length < 40) { i++; continue }

        // Mixed-case lines go to Other text only if they're long enough to be meaningful
        val lineStart = raw.indexOf(line)
        if (lineStart >= 0) {
            val lineRange = lineStart until (lineStart + line.length)
            if (matchedRanges.none { it.intersects(lineRange) } && line.length > 15) {
                otherItems.add(MedicineScannedItem(line, MedicineScanSection.OTHER))
            }
        }
        i++
    }

    return buildMap {
        if (nameItems.isNotEmpty()) put(MedicineScanSection.NAME, nameItems.distinctBy { it.value })
        if (dosageItems.isNotEmpty()) put(MedicineScanSection.DOSAGE, dosageItems.distinctBy { it.value })
        if (timeItems.isNotEmpty()) put(MedicineScanSection.TIME, timeItems.distinctBy { it.value })
        if (otherItems.isNotEmpty()) put(MedicineScanSection.OTHER, otherItems.distinctBy { it.value })
    }
}

private fun IntRange.intersects(other: IntRange): Boolean =
    first <= other.last && last >= other.first

private fun MedicineScanSection.displayTitle() = when (this) {
    MedicineScanSection.NAME -> "Possible medicine name"
    MedicineScanSection.DOSAGE -> "Dosage information"
    MedicineScanSection.TIME -> "Time / frequency"
    MedicineScanSection.OTHER -> "Other text"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineScanResultScreen(
    encodedText: String,
    onBack: () -> Unit,
    onAddMedicine: (name: String, dosage: String, times: List<String>) -> Unit
) {
    val rawText = runCatching {
        URLDecoder.decode(encodedText, StandardCharsets.UTF_8.name())
    }.getOrDefault(encodedText)

    val sections = parseMedicineText(rawText)

    var selectedName by remember { mutableStateOf("") }
    var selectedDosage by remember { mutableStateOf("") }
    val selectedTimes = remember { mutableStateListOf<String>() }

    val hasSelection = selectedName.isNotBlank() || selectedDosage.isNotBlank() || selectedTimes.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medicine Scan Results") },
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
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (selectedName.isNotBlank())
                            Text("Name: $selectedName", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        if (selectedDosage.isNotBlank())
                            Text("Dosage: $selectedDosage", style = MaterialTheme.typography.bodySmall)
                        if (selectedTimes.isNotEmpty())
                            Text("Times: ${selectedTimes.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { onAddMedicine(selectedName, selectedDosage, selectedTimes.toList()) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedName.isNotBlank()
                        ) {
                            Text("Add Medicine")
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (sections.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No recognisable medicine information found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("Tap to select name, dosage and times, then tap Add Medicine.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val sectionOrder = listOf(
                    MedicineScanSection.NAME, MedicineScanSection.DOSAGE,
                    MedicineScanSection.TIME, MedicineScanSection.OTHER
                )
                sectionOrder.forEach { section ->
                    val sectionItems = sections[section] ?: return@forEach
                    item(key = "header_${section.name}") {
                        Text(section.displayTitle(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    }
                    items(sectionItems, key = { "${section.name}_${it.value}" }) { scannedItem ->
                        val isSelected = when (section) {
                            MedicineScanSection.NAME -> selectedName == scannedItem.value
                            MedicineScanSection.DOSAGE -> selectedDosage == scannedItem.value
                            MedicineScanSection.TIME -> selectedTimes.contains(scannedItem.value)
                            MedicineScanSection.OTHER -> false
                        }
                        MedicineScannedItemCard(
                            item = scannedItem,
                            isSelected = isSelected,
                            onClick = {
                                when (section) {
                                    MedicineScanSection.NAME ->
                                        selectedName = if (isSelected) "" else scannedItem.value
                                    MedicineScanSection.DOSAGE ->
                                        selectedDosage = if (isSelected) "" else scannedItem.value
                                    MedicineScanSection.TIME ->
                                        if (isSelected) selectedTimes.remove(scannedItem.value)
                                        else selectedTimes.add(scannedItem.value)
                                    MedicineScanSection.OTHER ->
                                        if (selectedName.isBlank()) selectedName = scannedItem.value
                                }
                            }
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun MedicineScannedItemCard(
    item: MedicineScannedItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isOther = item.section == MedicineScanSection.OTHER
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                isOther -> MaterialTheme.colorScheme.surfaceVariant
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
                style = if (isOther) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
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
