package com.anexus.perfectgymcoach.data.workout_exercise

import androidx.room.*
import com.anexus.perfectgymcoach.data.exercise.ProgramExerciseAndInfo
import com.anexus.perfectgymcoach.data.exercise.ProgramExerciseReorder
import com.anexus.perfectgymcoach.data.exercise.UpdateExerciseSuperset
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgramReorder
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutExerciseDao {

    @Query("SELECT * FROM workoutexercise WHERE extWorkoutId LIKE :workoutId")
    fun getWorkoutExercises(workoutId: Long): Flow<List<WorkoutExercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: WorkoutExercise)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercises: List<WorkoutExercise>)

    @Query("DELETE FROM workoutexercise WHERE workoutExerciseId = :workoutExerciseId")
    suspend fun delete(workoutExerciseId: Long)

    @Update(entity = WorkoutExercise::class)
    suspend fun updateOrder(workoutProgramReorders: WorkoutExerciseReorder)
}