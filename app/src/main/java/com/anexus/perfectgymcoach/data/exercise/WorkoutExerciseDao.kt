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


    @Query("SELECT workoutexercise.*, exercise.image, exercise.equipment, exercise.name " +
            "FROM workoutexercise " +
            "LEFT JOIN exercise ON workoutexercise.extExerciseId = exercise.exerciseId " +
            "WHERE workoutexercise.extProgramId = :programId")
    fun getExercisesAndInfo(programId: Long): Flow<List<WorkoutExerciseAndInfo>>


    @Query("SELECT workoutexercise.*, exercise.image, exercise.equipment, exercise.name " +
            "FROM workoutexercise " +
            "LEFT JOIN exercise ON workoutexercise.extExerciseId = exercise.exerciseId " +
            "WHERE workoutexercise.extProgramId IN (:programIds) "
    )
    fun getExercisesAndInfo(programIds: List<Long>): Flow<List<WorkoutExerciseAndInfo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: WorkoutExercise)

}