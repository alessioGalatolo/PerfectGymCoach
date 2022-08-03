package com.anexus.perfectgymcoach.data.workout_program

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutProgramDao {

    @Query(
        "SELECT * FROM `program` " +
        "LEFT JOIN workoutexercise ON `program`.programId = workoutexercise.extProgramId " +
        "WHERE `program`.extPlanId LIKE :planId"
    )
    fun getPrograms(planId: Long): Flow<Map<WorkoutProgram, List<WorkoutExercise>>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(program: WorkoutProgram)

    // TODO: delete plan
}