package com.jaytt.moveandmeds.ui.medicine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaytt.moveandmeds.alarm.AlarmScheduler
import com.jaytt.moveandmeds.data.model.Medicine
import com.jaytt.moveandmeds.data.model.MedicineTime
import com.jaytt.moveandmeds.data.model.MedicineWithTimes
import com.jaytt.moveandmeds.data.repository.MedicineRepository
import com.jaytt.moveandmeds.data.repository.MovementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedicineViewModel @Inject constructor(
    private val repository: MedicineRepository,
    private val alarmScheduler: AlarmScheduler,
    private val movementRepository: MovementRepository
) : ViewModel() {

    private val movementSettings = movementRepository.getSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun generateDefaultTimes(count: Int): List<Pair<Int, Int>> {
        if (count <= 0) return emptyList()
        val settings = movementSettings.value
        val startMinutes = (settings?.startHour ?: 8) * 60
        val endMinutes = (settings?.endHour ?: 22) * 60
        if (count == 1) return listOf(startMinutes / 60 to 0)
        val total = endMinutes - startMinutes
        return (0 until count).map { i ->
            val m = startMinutes + total * i / (count - 1)
            (m / 60) to (m % 60)
        }
    }

    suspend fun getMedicine(id: Int): MedicineWithTimes? = repository.getMedicineWithTimes(id)

    fun saveMedicine(
        id: Int?,
        name: String,
        dosage: String,
        notes: String,
        times: List<Pair<Int, Int>>,
        daysOfWeek: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val medicine = Medicine(
                id = id ?: 0,
                name = name.trim(),
                dosage = dosage.trim(),
                notes = notes.trim(),
                daysOfWeek = daysOfWeek
            )
            // Cancel old alarms before save
            if (id != null) alarmScheduler.cancelMedicineAlarms(id)
            val newId = repository.saveMedicine(medicine, times.map { (h, m) ->
                MedicineTime(medicineId = 0, hour = h, minute = m)
            })
            val saved = repository.getMedicineWithTimes(newId)
            saved?.let { alarmScheduler.scheduleMedicineAlarms(it) }
            onDone()
        }
    }
}
