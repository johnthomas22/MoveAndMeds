package com.jaytt.moveandmeds.ui.info

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

private val phoneRegex = Regex("""[\+\d][\d\s\-\(\)]{6,20}\d""")
private val emailRegex = Regex("""[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}""")
private val urlRegex = Regex("""(https?://|www\.)[^\s]+""")

data class ScannedItem(val value: String, val type: String)

fun parseScannedText(raw: String): Map<String, List<ScannedItem>> {
    val phones = mutableListOf<ScannedItem>()
    val emails = mutableListOf<ScannedItem>()
    val urls = mutableListOf<ScannedItem>()
    val others = mutableListOf<ScannedItem>()

    val matchedRanges = mutableListOf<IntRange>()

    // Collect emails first (most specific)
    emailRegex.findAll(raw).forEach { match ->
        emails.add(ScannedItem(match.value.trim(), "email"))
        matchedRanges.add(match.range)
    }

    // URLs
    urlRegex.findAll(raw).forEach { match ->
        val alreadyCovered = matchedRanges.any { it.intersects(match.range) }
        if (!alreadyCovered) {
            urls.add(ScannedItem(match.value.trim(), "url"))
            matchedRanges.add(match.range)
        }
    }

    // Phone numbers — strip whitespace to normalise, deduplicate overlaps
    phoneRegex.findAll(raw).forEach { match ->
        val alreadyCovered = matchedRanges.any { it.intersects(match.range) }
        if (!alreadyCovered) {
            val normalised = match.value.trim()
            // Only include if it has at least 7 digits
            val digits = normalised.filter { it.isDigit() }
            if (digits.length >= 7) {
                phones.add(ScannedItem(normalised, "phone"))
                matchedRanges.add(match.range)
            }
        }
    }

    // Other text blocks — lines longer than 3 chars not already matched
    raw.lines().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.length > 3) {
            val lineStart = raw.indexOf(trimmed)
            val lineRange = lineStart until (lineStart + trimmed.length)
            val alreadyCovered = matchedRanges.any { it.intersects(lineRange) }
            if (!alreadyCovered) {
                others.add(ScannedItem(trimmed, "other"))
            }
        }
    }

    return buildMap {
        if (phones.isNotEmpty()) put("Phone numbers", phones.distinctBy { it.value })
        if (emails.isNotEmpty()) put("Email addresses", emails.distinctBy { it.value })
        if (urls.isNotEmpty()) put("Web addresses", urls.distinctBy { it.value })
        if (others.isNotEmpty()) put("Other text", others.distinctBy { it.value })
    }
}

private fun IntRange.intersects(other: IntRange): Boolean =
    first <= other.last && last >= other.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultScreen(
    encodedText: String,
    onBack: () -> Unit,
    onItemSelected: (value: String, type: String) -> Unit
) {
    val rawText = runCatching {
        URLDecoder.decode(encodedText, StandardCharsets.UTF_8.name())
    }.getOrDefault(encodedText)

    val sections = parseScannedText(rawText)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Results") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (sections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    "No recognisable contacts found.",
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
                sections.forEach { (sectionTitle, items) ->
                    item {
                        Text(
                            text = sectionTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(items) { scannedItem ->
                        ScannedItemCard(
                            item = scannedItem,
                            onClick = { onItemSelected(scannedItem.value, scannedItem.type) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Tap any item to pre-fill the Add Contact form.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ScannedItemCard(
    item: ScannedItem,
    onClick: () -> Unit
) {
    val isOther = item.type == "other"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isOther)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Text(
            text = item.value,
            style = if (isOther) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}
