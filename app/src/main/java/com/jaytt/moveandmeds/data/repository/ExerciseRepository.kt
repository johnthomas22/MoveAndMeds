package com.jaytt.moveandmeds.data.repository

import com.jaytt.moveandmeds.data.db.ExerciseDao
import com.jaytt.moveandmeds.data.model.Exercise
import com.jaytt.moveandmeds.data.model.ExerciseTime
import com.jaytt.moveandmeds.data.model.ExerciseWithTimes
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(private val dao: ExerciseDao) {
    fun getAllExercisesWithTimes(): Flow<List<ExerciseWithTimes>> = dao.getAllExercisesWithTimes()

    suspend fun getExerciseWithTimes(id: Int): ExerciseWithTimes? = dao.getExerciseWithTimes(id)

    suspend fun saveExercise(exercise: Exercise, times: List<ExerciseTime>): Int =
        dao.upsertExerciseWithTimes(exercise, times)

    suspend fun deleteExercise(exercise: Exercise) = dao.deleteExercise(exercise)

    suspend fun deleteAll() {
        dao.deleteAllExerciseTimes()
        dao.deleteAllExercises()
    }
}
