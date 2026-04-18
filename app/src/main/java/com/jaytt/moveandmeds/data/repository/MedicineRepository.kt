package com.jaytt.moveandmeds.data.repository

import com.jaytt.moveandmeds.data.db.MedicineDao
import com.jaytt.moveandmeds.data.model.Medicine
import com.jaytt.moveandmeds.data.model.MedicineTime
import com.jaytt.moveandmeds.data.model.MedicineWithTimes
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicineRepository @Inject constructor(private val dao: MedicineDao) {
    fun getAllMedicinesWithTimes(): Flow<List<MedicineWithTimes>> = dao.getAllMedicinesWithTimes()

    suspend fun getMedicineWithTimes(id: Int): MedicineWithTimes? = dao.getMedicineWithTimes(id)

    suspend fun saveMedicine(medicine: Medicine, times: List<MedicineTime>): Int =
        dao.upsertMedicineWithTimes(medicine, times)

    suspend fun deleteMedicine(medicine: Medicine) = dao.deleteMedicine(medicine)

    suspend fun deleteAll() {
        dao.deleteAllMedicineTimes()
        dao.deleteAllMedicines()
    }
}
