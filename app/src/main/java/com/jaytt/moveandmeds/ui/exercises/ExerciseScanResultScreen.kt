package com.jaytt.moveandmeds.ui.exercises

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// ── Data ──────────────────────────────────────────────────────────────────────

data class ParsedExerciseBlock(
    val name: String,
    val reps: String,
    val sets: String,
    val notes: String,
    val reminderType: String,   // "timed" or "interval"
    val intervalMinutes: Int,   // used when type == "interval"
    val timesPerDay: Int        // used when type == "timed"
)

// ── Regex / signal patterns ───────────────────────────────────────────────────

private val numberedLineRegex = Regex("""^\s*\d+[\s.]+(.+)$""")
private val repsValueRegex    = Regex("""(\d+)\s*(?:[–\-]|to)?\s*(\d*)\s*(?:reps?|repetitions?)""", RegexOption.IGNORE_CASE)
private val setsValueRegex    = Regex("""(\d{1,2})\s*(?:sets?)\b""", RegexOption.IGNORE_CASE)
private val timesPerDayRegex  = Regex("""(\d+)\s*(?:times?\s+a\s+day|x\s+a\s+day)""", RegexOption.IGNORE_CASE)
private val everyXHoursRegex  = Regex("""every\s+(\d+)\s+hours?""", RegexOption.IGNORE_CASE)
private val everyXMinsRegex   = Regex("""every\s+(\d+)\s+minutes?""", RegexOption.IGNORE_CASE)

private val intervalSignals = listOf(
    Regex("""frequently""",         RegexOption.IGNORE_CASE),
    Regex("""throughout the day""", RegexOption.IGNORE_CASE),
    Regex("""hourly""",             RegexOption.IGNORE_CASE),
    Regex("""every\s+\d+\s+hours?""", RegexOption.IGNORE_CASE),
    Regex("""every\s+\d+\s+minutes?""", RegexOption.IGNORE_CASE),
    Regex("""regularly""",          RegexOption.IGNORE_CASE),
)
private val timedSignals = listOf(
    Regex("""\d+\s*(?:times?\s+a\s+day|x\s+a\s+day)""", RegexOption.IGNORE_CASE),
    Regex("""once\s+a\s+day""",  RegexOption.IGNORE_CASE),
    Regex("""twice\s+a\s+day""", RegexOption.IGNORE_CASE),
    Regex("""once\s+daily""",    RegexOption.IGNORE_CASE),
    Regex("""daily""",           RegexOption.IGNORE_CASE),
)

// Lines that are document boilerplate, not exercise names or schedule info.
// Keep only document-format noise here (applies to any physio sheet).
// Do NOT add condition/procedure/hospital-specific terms — use structural guards instead.
private val boilerplatePatterns = listOf(
    Regex("""how often""",                  RegexOption.IGNORE_CASE),
    Regex("""ref:""",                       RegexOption.IGNORE_CASE),
    Regex("""^issue\s+\d+$""",             RegexOption.IGNORE_CASE), // footer "Issue 3"
    Regex("""page\s+[a-z\d]+\s+of\s+\d""", RegexOption.IGNORE_CASE), // "Page 1 of 4" / "Page l of 4"
    Regex("""issued?\s+by""",               RegexOption.IGNORE_CASE),
    Regex("""issue\s+date""",               RegexOption.IGNORE_CASE),
    Regex("""review\s+date""",              RegexOption.IGNORE_CASE),
    Regex("""department""",                 RegexOption.IGNORE_CASE),
    Regex("""©"""),
    Regex("""ph?[yv]s""",                   RegexOption.IGNORE_CASE), // Physiotools OCR garbles: phys/phvs/pys/pvs/OPysotoots etc.
    Regex("""post.{0,5}op(erative)?""",     RegexOption.IGNORE_CASE), // "post operative", "post-op" section headers
    Regex("""consultant""",                 RegexOption.IGNORE_CASE),
    Regex("""contact\s+number""",           RegexOption.IGNORE_CASE),
    Regex("""caution""",                    RegexOption.IGNORE_CASE),
    Regex("""^\d+$"""),                     // bare number-only lines
)

// Instruction/advice lines and description lines that look like exercise names but aren't
private val instructionLinePattern = Regex(
    """^(avoid|do not|don't|listen|remember|unless|if your|keep|place|always|never|ensure|start with|you can|you might|you should|breathe|clear|gently|have a|squeeze|lift your|lift the|stand and|stand with|stand on|bend your|bend the|flex your|hold your|hold the|return to|bring your|bring the|face the|slide your|slide the|raise your|lower your|step |walk |sit |swing |lie on|lie flat|lying |tighten|straighten|cross your|turn your|push your|pull your)""",
    RegexOption.IGNORE_CASE
)

// Words that cannot be a complete exercise name on their own
private val nonNameWords = setOf(
    "reps", "rep", "repetitions", "repetition", "sets", "set",
    "times", "seconds", "minutes", "hours", "daily", "day"
)

// ── Parser ────────────────────────────────────────────────────────────────────

// ML Kit reads two-column physio sheets left-column-first, then right-column.
// So exercise names/descriptions come before their HOW OFTEN data.
// Strategy: Pass 1 – find exercise names in order.
//           Pass 2 – find schedule groups in document order.
//           Pair them up by index.

private data class ParsedSchedule(
    val reminderType: String,
    val intervalMinutes: Int,
    val timesPerDay: Int,
    val reps: String
)

fun parseExerciseBlocks(raw: String): List<ParsedExerciseBlock> {
    val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }

    // If the document contains schedule/rep signals anywhere it's clearly an exercise
    // sheet — allow title-case name detection from the start rather than requiring
    // a numbered line first (some physio sheets don't number their exercises).
    val documentHasScheduleSignals = lines.any { line ->
        (intervalSignals + timedSignals).any { it.containsMatchIn(line) } ||
        repsValueRegex.containsMatchIn(line) ||
        setsValueRegex.containsMatchIn(line)
    }

    // ── Pass 1: find exercise name candidates ─────────────────────────────────
    data class NameEntry(val lineIdx: Int, val name: String)
    val names = mutableListOf<NameEntry>()
    // When the document has no schedule signals, gate un-numbered names behind a
    // numbered exercise to avoid picking up document titles and hospital headers.
    var seenNumberedExercise = documentHasScheduleSignals

    for ((i, line) in lines.withIndex()) {
        if (boilerplatePatterns.any { it.containsMatchIn(line) }) continue
        if (line.length < 3) continue
        if (instructionLinePattern.containsMatchIn(line)) continue

        // Numbered line — strip any HOW OFTEN column text merged onto same line
        val numbered = numberedLineRegex.find(line)
        if (numbered != null) {
            val candidate = stripScheduleSuffix(numbered.groupValues[1].trim())
            if (isValidExerciseName(candidate) && !instructionLinePattern.containsMatchIn(candidate)) {
                names.add(NameEntry(i, candidate))
                seenNumberedExercise = true
                continue
            }
        }

        // Non-numbered short title-case line — require 2+ words to filter
        // single-word OCR artefacts (garbled logos, watermarks, etc.)
        if (seenNumberedExercise
            && line.first().isUpperCase()
            && line.length in 5..70
            && line.split(Regex("\\s+")).size in 2..6
            && !isScheduleOrReps(line)
        ) {
            val candidate = stripScheduleSuffix(line)
            if (isValidExerciseName(candidate)) {
                names.add(NameEntry(i, candidate))
            }
        }
    }

    if (names.isEmpty()) return emptyList()

    // ── Pass 2: extract schedule groups from the whole document ───────────────
    val scheduleGroups = extractScheduleGroups(lines)

    // ── Pass 3: pair names with schedule groups and build blocks ──────────────
    return names.mapIndexed { idx, entry ->
        val nextNameIdx = names.getOrNull(idx + 1)?.lineIdx ?: lines.size
        val bodyLines = lines
            .subList(entry.lineIdx + 1, nextNameIdx)
            .filter { !boilerplatePatterns.any { p -> p.containsMatchIn(it) } }
        val schedule = scheduleGroups.getOrNull(idx)
        buildBlockFromSchedule(entry.name, bodyLines, schedule)
    }
}

// A name is valid if it has at least one non-measurement content word
private fun isValidExerciseName(candidate: String): Boolean {
    if (candidate.length !in 3..70) return false
    // Lines ending in "Exercises" (plural) are section headers, not individual exercises
    if (candidate.trimEnd().endsWith("Exercises", ignoreCase = true)) return false
    val words = candidate.lowercase().split(Regex("\\s+"))
        .map { it.replace(Regex("[^a-z]"), "") }
        .filter { it.isNotEmpty() }
    return words.isNotEmpty() &&
        !words.all { it in nonNameWords || it.all(Char::isDigit) }
}

// Strip schedule/reps text that OCR merged from the HOW OFTEN column onto the name line
private fun stripScheduleSuffix(text: String): String {
    val allSignals = intervalSignals + timedSignals + listOf(repsValueRegex, setsValueRegex)
    var earliest = text.length
    allSignals.forEach { regex ->
        val match = regex.find(text)
        if (match != null && match.range.first < earliest) earliest = match.range.first
    }
    return text.substring(0, earliest).trim()
}

// Each frequency signal starts a new schedule group; reps following belong to it
private fun extractScheduleGroups(lines: List<String>): List<ParsedSchedule> {
    val result = mutableListOf<ParsedSchedule>()
    var freqLines = mutableListOf<String>()
    var reps = ""
    var inGroup = false

    for (line in lines) {
        if (boilerplatePatterns.any { it.containsMatchIn(line) }) continue

        val isFreq = intervalSignals.any { it.containsMatchIn(line) } ||
                     timedSignals.any { it.containsMatchIn(line) }
        val isReps = repsValueRegex.containsMatchIn(line)

        when {
            isFreq && !inGroup -> {
                freqLines = mutableListOf(line); reps = ""; inGroup = true
            }
            isFreq && inGroup -> {
                result.add(buildSchedule(freqLines.joinToString(" "), reps))
                freqLines = mutableListOf(line); reps = ""
            }
            isReps && inGroup
                && !line.contains("start with", ignoreCase = true)
                && !line.contains("build up", ignoreCase = true) -> {
                val r = extractReps(line)
                if (r.isNotBlank()) reps = r
            }
        }
    }
    if (inGroup) result.add(buildSchedule(freqLines.joinToString(" "), reps))
    return result
}

private fun buildSchedule(freqText: String, reps: String): ParsedSchedule {
    val hasInterval = intervalSignals.any { it.containsMatchIn(freqText) }
    val hasTimed    = timedSignals.any { it.containsMatchIn(freqText) }
    val reminderType = if (hasInterval && !hasTimed) "interval" else "timed"
    val intervalMinutes = when {
        everyXHoursRegex.containsMatchIn(freqText) ->
            (everyXHoursRegex.find(freqText)!!.groupValues[1].toIntOrNull() ?: 1) * 60
        everyXMinsRegex.containsMatchIn(freqText) ->
            everyXMinsRegex.find(freqText)!!.groupValues[1].toIntOrNull() ?: 60
        Regex("""hourly""", RegexOption.IGNORE_CASE).containsMatchIn(freqText) -> 60
        else -> 60
    }.coerceIn(15, 480)
    return ParsedSchedule(
        reminderType    = reminderType,
        intervalMinutes = intervalMinutes,
        timesPerDay     = extractTimesPerDay(freqText),
        reps            = reps
    )
}

private fun buildBlockFromSchedule(
    name: String,
    bodyLines: List<String>,
    schedule: ParsedSchedule?
): ParsedExerciseBlock {
    val notes = bodyLines
        .filter { !isScheduleOrReps(it) && it.length > 5 && !it.matches(Regex("""^[\d\s\-–.]+$""")) }
        .joinToString(" ")
        .trim()

    return if (schedule != null) {
        ParsedExerciseBlock(
            name            = name,
            reps            = schedule.reps,
            sets            = "",
            notes           = notes,
            reminderType    = schedule.reminderType,
            intervalMinutes = schedule.intervalMinutes,
            timesPerDay     = schedule.timesPerDay
        )
    } else {
        val bodyText = bodyLines.joinToString(" ")
        ParsedExerciseBlock(
            name            = name,
            reps            = extractReps(bodyText),
            sets            = extractSets(bodyText),
            notes           = notes,
            reminderType    = if (intervalSignals.any { it.containsMatchIn(bodyText) } &&
                                  timedSignals.none { it.containsMatchIn(bodyText) }) "interval" else "timed",
            intervalMinutes = 60,
            timesPerDay     = extractTimesPerDay(bodyText)
        )
    }
}

private fun isScheduleOrReps(line: String): Boolean =
    intervalSignals.any { it.containsMatchIn(line) } ||
    timedSignals.any { it.containsMatchIn(line) } ||
    repsValueRegex.containsMatchIn(line) ||
    setsValueRegex.containsMatchIn(line)

private fun extractReps(text: String): String {
    val match = repsValueRegex.find(text) ?: return ""
    val lo = match.groupValues[1]
    val hi = match.groupValues[2]
    return if (hi.isNotBlank()) "$lo-$hi" else lo
}

private fun extractSets(text: String): String {
    val match = setsValueRegex.find(text) ?: return ""
    val num = match.groupValues[1].toIntOrNull() ?: return ""
    val after = text.substring((match.range.last + 1).coerceAtMost(text.length - 1)).take(15)
    if (Regex("""a\s+day""", RegexOption.IGNORE_CASE).containsMatchIn(after)) return ""
    return if (num in 1..20) num.toString() else ""
}

private fun extractTimesPerDay(text: String): Int {
    timesPerDayRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()
        ?.let { return it.coerceIn(1, 10) }
    if (Regex("""twice\s+a\s+day""", RegexOption.IGNORE_CASE).containsMatchIn(text)) return 2
    if (Regex("""once\s+a\s+day|once\s+daily""", RegexOption.IGNORE_CASE).containsMatchIn(text)) return 1
    return 3
}

fun generateEvenlySpacedTimes(timesPerDay: Int): List<Pair<Int, Int>> {
    if (timesPerDay <= 1) return listOf(Pair(9, 0))
    val startMins = 8 * 60
    val endMins   = 20 * 60
    val step = (endMins - startMins) / (timesPerDay - 1)
    return (0 until timesPerDay).map { i ->
        val total = startMins + i * step
        Pair(total / 60, total % 60)
    }
}

// ── Composable ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseScanResultScreen(
    encodedText: String,
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: ExerciseScanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val rawText = remember(encodedText) {
        runCatching { URLDecoder.decode(encodedText, StandardCharsets.UTF_8.name()) }
            .getOrDefault(encodedText)
    }
    val blocks = remember(rawText) { parseExerciseBlocks(rawText) }
    var selected by remember(blocks) { mutableStateOf(blocks.indices.toSet()) }
    var isSaving by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    // Per-exercise image crops from the scanned page
    var exerciseCrops by remember { mutableStateOf<List<Bitmap?>>(emptyList()) }
    var cropsLoading by remember { mutableStateOf(true) }
    LaunchedEffect(blocks) {
        if (blocks.isEmpty()) { exerciseCrops = emptyList(); cropsLoading = false; return@LaunchedEffect }
        val cacheFile = File(context.cacheDir, "exercise_scan_temp.jpg")
        if (!cacheFile.exists()) { exerciseCrops = List(blocks.size) { null }; return@LaunchedEffect }
        try {
            // Load bitmap with EXIF rotation applied
            val raw = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(cacheFile.absolutePath) }
                ?: run { exerciseCrops = List(blocks.size) { null }; return@LaunchedEffect }
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    val exif = ExifInterface(cacheFile.absolutePath)
                    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    val matrix = Matrix()
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90  -> matrix.postRotate(90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    }
                    Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
                }.getOrDefault(raw)
            }
            // Re-run ML Kit to get text block positions in this image
            val inputImage = InputImage.fromFilePath(context, Uri.fromFile(cacheFile))
            val visionText = suspendCancellableCoroutine { cont ->
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(inputImage)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
            val allLines = visionText.textBlocks.flatMap { it.lines }

            // Layout: diagram (left column) | exercise name + instructions (middle) | HOW OFTEN (right)
            // The diagram sits beside the text at the same Y position — there is no
            // vertical text-free gap to detect.  Instead, crop everything to the LEFT
            // of the exercise name's X position, for the exercise's full Y range.
            data class NameBounds(val top: Int, val nameLeft: Int)
            val nameBoundsMap = blocks.map { block ->
                val line = allLines.firstOrNull { line ->
                    line.text.contains(block.name, ignoreCase = true) ||
                    block.name.contains(line.text.trim(), ignoreCase = true)
                }
                NameBounds(line?.boundingBox?.top ?: -1, line?.boundingBox?.left ?: -1)
            }

            exerciseCrops = withContext(Dispatchers.Default) {
                blocks.indices.map { i ->
                    val (nameTop, nameLeft) = nameBoundsMap[i]
                    // nameLeft must be at least 20% of image width — rules out sheets
                    // where the exercise name starts at the left edge (no diagram column)
                    if (nameTop < 0 || nameLeft < bitmap.width / 5) return@map null

                    val exerciseEnd = nameBoundsMap.drop(i + 1).firstOrNull { it.top > nameTop }?.top
                        ?: if (nameBoundsMap.size > 1) {
                            // Estimate row height from the spacing between exercise names
                            val avgRowHeight = (nameBoundsMap.last().top - nameBoundsMap.first().top) /
                                (nameBoundsMap.size - 1)
                            (nameTop + avgRowHeight).coerceAtMost(bitmap.height)
                        } else bitmap.height

                    val cropTop    = nameTop.coerceIn(0, bitmap.height - 1)
                    val cropHeight = (exerciseEnd - nameTop).coerceIn(1, bitmap.height - cropTop)
                    val cropWidth  = nameLeft.coerceIn(1, bitmap.width)

                    val crop = Bitmap.createBitmap(bitmap, 0, cropTop, cropWidth, cropHeight)
                    if (crop.width > 1200) {
                        val scale = 1200f / crop.width
                        Bitmap.createScaledBitmap(crop, 1200, (crop.height * scale).toInt(), true)
                    } else crop
                }
            }
        } catch (_: Exception) {
            exerciseCrops = List(blocks.size) { null }
        } finally {
            cropsLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Results") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString(rawText))
                        copied = true
                    }) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = if (copied) "Copied!" else "Copy raw text"
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (blocks.isNotEmpty()) {
                Surface(tonalElevation = 4.dp) {
                    Button(
                        onClick = {
                            isSaving = true
                            viewModel.saveSelected(
                                blocks = blocks,
                                selectedIndices = selected,
                                crops = exerciseCrops,
                                onDone = onDone
                            )
                        },
                        enabled = selected.isNotEmpty() && !isSaving && !cropsLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                if (selected.isEmpty()) "Select exercises to add"
                                else "Add ${selected.size} exercise${if (selected.size == 1) "" else "s"}"
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (blocks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No exercises detected.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Make sure the exercise name and frequency (e.g. \"3x a day\" or \"10 reps\") are clearly visible in the frame.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "${blocks.size} exercise${if (blocks.size == 1) "" else "s"} found — tap to deselect any you don't want to add.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(blocks.indices.toList(), key = { it }) { idx ->
                    ExerciseBlockCard(
                        block = blocks[idx],
                        cropBitmap = exerciseCrops.getOrNull(idx),
                        cropsLoading = cropsLoading,
                        isSelected = idx in selected,
                        onToggle = {
                            selected = if (idx in selected) selected - idx else selected + idx
                        }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun ExerciseBlockCard(
    block: ParsedExerciseBlock,
    cropBitmap: Bitmap?,
    cropsLoading: Boolean,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .padding(top = 2.dp, end = 10.dp)
                    .size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = block.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (block.reps.isNotBlank()) {
                        SummaryChip("${block.reps} reps")
                    }
                    if (block.sets.isNotBlank()) {
                        SummaryChip("${block.sets} sets")
                    }
                    val schedLabel = if (block.reminderType == "interval")
                        "Every ${block.intervalMinutes} min"
                    else
                        "${block.timesPerDay}× daily"
                    SummaryChip(schedLabel)
                }
                if (block.notes.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = block.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                if (cropsLoading) {
                    Spacer(Modifier.height(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else if (cropBitmap != null) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            bitmap = cropBitmap.asImageBitmap(),
                            contentDescription = "Exercise illustration",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(label: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 1.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
