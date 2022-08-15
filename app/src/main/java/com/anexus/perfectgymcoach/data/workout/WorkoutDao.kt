package com.anexus.perfectgymcoach.data.workout

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Query(
        "SELECT * FROM `program` " +
        "LEFT JOIN workoutexercise ON `program`.programId = workoutexercise.extProgramId " +
        "WHERE `program`.extPlanId LIKE :planId"
    )
    fun getPrograms(planId: Long): Flow<Map<Workout, List<WorkoutExercise>>>

    @Insert
    suspend fun insert(program: Workout): Long

    @Insert
    fun update(program: Workout)
    // TODO: delete plan
}