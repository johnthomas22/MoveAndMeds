package com.jaytt.moveandmeds.data.db

import androidx.room.*
import com.jaytt.moveandmeds.data.model.Medicine
import com.jaytt.moveandmeds.data.model.MedicineTime
import com.jaytt.moveandmeds.data.model.MedicineWithTimes
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Transaction
    @Query("SELECT * FROM medicines ORDER BY name ASC")
    fun getAllMedicinesWithTimes(): Flow<List<MedicineWithTimes>>

    @Transaction
    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getMedicineWithTimes(id: Int): MedicineWithTimes?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: Medicine): Long

    @Update
    suspend fun updateMedicine(medicine: Medicine)

    @Delete
    suspend fun deleteMedicine(medicine: Medicine)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimes(times: List<MedicineTime>)

    @Query("DELETE FROM medicine_times WHERE medicineId = :medicineId")
    suspend fun deleteTimesForMedicine(medicineId: Int)

    @Transaction
    suspend fun upsertMedicineWithTimes(medicine: Medicine, times: List<MedicineTime>): Int {
        val id = insertMedicine(medicine).toInt()
        deleteTimesForMedicine(id)
        insertTimes(times.map { it.copy(medicineId = id) })
        return id
    }
}
