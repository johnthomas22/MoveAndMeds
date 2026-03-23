package com.jaytt.moveandmeds.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaytt.moveandmeds.alarm.AlarmScheduler
import com.jaytt.moveandmeds.data.model.ExerciseWithTimes
import com.jaytt.moveandmeds.data.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExercisesViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    val exercises: StateFlow<List<ExerciseWithTimes>> = exerciseRepository.getAllExercisesWithTimes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteExercise(ewt: ExerciseWithTimes) {
        viewModelScope.launch {
            alarmScheduler.cancelExerciseAlarms(ewt.exercise.id)
            exerciseRepository.deleteExercise(ewt.exercise)
        }
    }
}
