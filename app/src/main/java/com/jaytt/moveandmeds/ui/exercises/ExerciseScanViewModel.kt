package com.jaytt.moveandmeds.ui.exercises

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaytt.moveandmeds.alarm.AlarmScheduler
import com.jaytt.moveandmeds.data.model.Exercise
import com.jaytt.moveandmeds.data.model.ExerciseTime
import com.jaytt.moveandmeds.data.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ExerciseScanViewModel @Inject constructor(
    private val repository: ExerciseRepository,
    private val alarmScheduler: AlarmScheduler,
    @ApplicationContext private val context: Context
) : ViewModel() {

    fun saveSelected(
        blocks: List<ParsedExerciseBlock>,
        selectedIndices: Set<Int>,
        crops: List<Bitmap?>,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            blocks.forEachIndexed { idx, block ->
                if (idx !in selectedIndices) return@forEachIndexed
                val crop = crops.getOrNull(idx)
                val imagePath = crop?.let { bitmap ->
                    withContext(Dispatchers.IO) {
                        runCatching {
                            val imageDir = File(context.filesDir, "exercise_images")
                            imageDir.mkdirs()
                            val imageFile = File(imageDir, "${UUID.randomUUID()}.jpg")
                            FileOutputStream(imageFile).use { out ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                            }
                            imageFile.absolutePath
                        }.getOrNull()
                    }
                }
                val exercise = Exercise(
                    name = block.name,
                    sets = block.sets,
                    reps = block.reps,
                    notes = block.notes,
                    reminderType = block.reminderType,
                    intervalMinutes = block.intervalMinutes,
                    imagePath = imagePath
                )
                val times: List<ExerciseTime> = if (block.reminderType == "timed") {
                    generateEvenlySpacedTimes(block.timesPerDay).map { (h, m) ->
                        ExerciseTime(exerciseId = 0, hour = h, minute = m)
                    }
                } else {
                    emptyList()
                }
                val newId = repository.saveExercise(exercise, times)
                val saved = repository.getExerciseWithTimes(newId)
                saved?.let { alarmScheduler.scheduleExerciseAlarms(it) }
            }
            onDone()
        }
    }
}
