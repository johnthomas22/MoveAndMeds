package com.jaytt.moveandmeds.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaytt.moveandmeds.alarm.AlarmScheduler
import com.jaytt.moveandmeds.data.repository.ExerciseRepository
import com.jaytt.moveandmeds.data.repository.MedicineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val exerciseRepository: ExerciseRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    fun deleteAllMedicines() {
        viewModelScope.launch {
            medicineRepository.getAllMedicinesWithTimes().first().forEach { mwt ->
                alarmScheduler.cancelMedicineAlarms(mwt.medicine.id)
            }
            medicineRepository.deleteAll()
        }
    }

    fun deleteAllExercises() {
        viewModelScope.launch {
            exerciseRepository.getAllExercisesWithTimes().first().forEach { ewt ->
                alarmScheduler.cancelExerciseAlarms(ewt.exercise.id)
            }
            exerciseRepository.deleteAll()
        }
    }
}
