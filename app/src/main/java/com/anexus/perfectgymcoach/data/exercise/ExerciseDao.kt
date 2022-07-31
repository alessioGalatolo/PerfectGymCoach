package com.anexus.perfectgymcoach.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query(
        "SELECT * FROM exercise " +
        "WHERE primaryMuscle LIKE :muscle")
    fun getExercises(muscle: Exercise.Muscle): Flow<List<Exercise>>

    @Query("SELECT * FROM exercise")
    fun getAllExercises(): Flow<List<Exercise>>

    @Insert(onConflict = REPLACE)
    suspend fun insert(exercise: Exercise)

    @Insert(onConflict = REPLACE)
    fun insertAll(exercises: List<Exercise>)

    // TODO: delete plan
}