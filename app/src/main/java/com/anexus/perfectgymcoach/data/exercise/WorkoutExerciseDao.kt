package com.anexus.perfectgymcoach.data.exercise

import androidx.room.*
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgramReorder
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutExerciseDao {

    @Query(
        "SELECT * FROM workoutexercise " +
                "WHERE extProgramId LIKE :programId")
    fun getExercises(programId: Long): Flow<List<WorkoutExercise>>

    @Query("SELECT * FROM workoutexercise WHERE workoutExerciseId LIKE :workoutExerciseId")
    fun getWorkoutExercise(workoutExerciseId: Long): Flow<WorkoutExercise>

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

    @Update(entity = WorkoutExercise::class)
    suspend fun updateOrder(workoutExerciseReorders: List<WorkoutExerciseReorder>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: WorkoutExercise)

    @Query("DELETE FROM workoutexercise WHERE workoutExerciseId = :workoutExerciseId")
    suspend fun delete(workoutExerciseId: Long)

}