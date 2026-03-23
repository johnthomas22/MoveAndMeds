package com.jaytt.moveandmeds.ui.medicine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaytt.moveandmeds.alarm.AlarmScheduler
import com.jaytt.moveandmeds.data.model.Medicine
import com.jaytt.moveandmeds.data.model.MedicineTime
import com.jaytt.moveandmeds.data.model.MedicineWithTimes
import com.jaytt.moveandmeds.data.repository.MedicineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedicineViewModel @Inject constructor(
    private val repository: MedicineRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

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
