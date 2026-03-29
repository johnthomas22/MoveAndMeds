package com.jaytt.moveandmeds.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaytt.moveandmeds.alarm.AlarmScheduler
import com.jaytt.moveandmeds.data.model.Exercise
import com.jaytt.moveandmeds.data.model.ExerciseTime
import com.jaytt.moveandmeds.data.model.ExerciseWithTimes
import com.jaytt.moveandmeds.data.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val repository: ExerciseRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    suspend fun getExercise(id: Int): ExerciseWithTimes? = repository.getExerciseWithTimes(id)

    fun saveExercise(
        id: Int?,
        name: String,
        sets: String,
        reps: String,
        notes: String,
        times: List<Pair<Int, Int>>,
        daysOfWeek: String,
        reminderType: String = "timed",
        intervalMinutes: Int = 60,
        intervalStartHour: Int = 8,
        intervalEndHour: Int = 22,
        imagePath: String? = null,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val exercise = Exercise(
                id = id ?: 0,
                name = name.trim(),
                sets = sets.trim(),
                reps = reps.trim(),
                notes = notes.trim(),
                daysOfWeek = daysOfWeek,
                reminderType = reminderType,
                intervalMinutes = intervalMinutes,
                intervalStartHour = intervalStartHour,
                intervalEndHour = intervalEndHour,
                imagePath = imagePath
            )
            if (id != null) alarmScheduler.cancelExerciseAlarms(id)
            val newId = repository.saveExercise(exercise, times.map { (h, m) ->
                ExerciseTime(exerciseId = 0, hour = h, minute = m)
            })
            val saved = repository.getExerciseWithTimes(newId)
            saved?.let { alarmScheduler.scheduleExerciseAlarms(it) }
            onDone()
        }
    }
}
