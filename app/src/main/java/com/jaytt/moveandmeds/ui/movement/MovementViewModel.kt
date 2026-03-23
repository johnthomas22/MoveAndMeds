package com.jaytt.moveandmeds.ui.movement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaytt.moveandmeds.alarm.AlarmScheduler
import com.jaytt.moveandmeds.data.model.MovementSettings
import com.jaytt.moveandmeds.data.repository.MovementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovementViewModel @Inject constructor(
    private val repository: MovementRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    val settings = repository.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MovementSettings())

    fun saveSettings(settings: MovementSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings)
            alarmScheduler.scheduleMovementAlarm(settings)
        }
    }
}
