package com.jaytt.moveandmeds.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class MedicineWithTimes(
    @Embedded val medicine: Medicine,
    @Relation(
        parentColumn = "id",
        entityColumn = "medicineId"
    )
    val times: List<MedicineTime>
)
