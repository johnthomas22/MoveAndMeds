package com.jaytt.moveandmeds.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dosage: String,
    val notes: String = "",
    val isEnabled: Boolean = true,
    val daysOfWeek: String = "1,2,3,4,5,6,7"
)
