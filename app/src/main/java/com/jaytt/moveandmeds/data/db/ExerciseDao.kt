package com.jaytt.moveandmeds.data.db

import androidx.room.*
import com.jaytt.moveandmeds.data.model.Exercise
import com.jaytt.moveandmeds.data.model.ExerciseTime
import com.jaytt.moveandmeds.data.model.ExerciseWithTimes
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Transaction
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercisesWithTimes(): Flow<List<ExerciseWithTimes>>

    @Transaction
    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseWithTimes(id: Int): ExerciseWithTimes?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise): Long

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Delete
    suspend fun deleteExercise(exercise: Exercise)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimes(times: List<ExerciseTime>)

    @Query("DELETE FROM exercise_times WHERE exerciseId = :exerciseId")
    suspend fun deleteTimesForExercise(exerciseId: Int)

    @Query("DELETE FROM exercises")
    suspend fun deleteAllExercises()

    @Query("DELETE FROM exercise_times")
    suspend fun deleteAllExerciseTimes()

    @Transaction
    suspend fun upsertExerciseWithTimes(exercise: Exercise, times: List<ExerciseTime>): Int {
        val id = insertExercise(exercise).toInt()
        deleteTimesForExercise(id)
        insertTimes(times.map { it.copy(exerciseId = id) })
        return id
    }
}
