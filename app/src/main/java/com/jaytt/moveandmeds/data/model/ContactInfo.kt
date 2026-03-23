package com.jaytt.moveandmeds.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactInfo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val value: String,
    val type: String = "phone"
)
