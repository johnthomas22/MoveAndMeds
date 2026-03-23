package com.jaytt.moveandmeds.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class ExerciseWithTimes(
    @Embedded val exercise: Exercise,
    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseId"
    )
    val times: List<ExerciseTime>
)
