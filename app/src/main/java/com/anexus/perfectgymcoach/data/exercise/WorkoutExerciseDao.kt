package com.anexus.perfectgymcoach.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutExerciseDao {

    @Query(
        "SELECT * FROM workoutexercise " +
        "WHERE extProgramId LIKE :programId")
    fun getExercises(programId: Long): Flow<List<WorkoutExercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: WorkoutExercise)

    // TODO: delete plan
}