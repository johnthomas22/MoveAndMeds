package com.jaytt.moveandmeds.ui.medicines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaytt.moveandmeds.alarm.AlarmScheduler
import com.jaytt.moveandmeds.data.model.MedicineWithTimes
import com.jaytt.moveandmeds.data.repository.MedicineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedicinesViewModel @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    val medicines: StateFlow<List<MedicineWithTimes>> = medicineRepository.getAllMedicinesWithTimes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteMedicine(mwt: MedicineWithTimes) {
        viewModelScope.launch {
            alarmScheduler.cancelMedicineAlarms(mwt.medicine.id)
            medicineRepository.deleteMedicine(mwt.medicine)
        }
    }
}
