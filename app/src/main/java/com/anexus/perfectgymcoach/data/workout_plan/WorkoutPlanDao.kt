package com.anexus.perfectgymcoach.data.workout_plan

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutPlanDao {

    @Query("SELECT * FROM `plan`")
    fun getPlans(): Flow<List<WorkoutPlan>>

    @Query(
        "SELECT * FROM `plan` " +
        "LEFT JOIN `program` ON `plan`.planId = `program`.extPlanId "
    )
    fun getPlanMapPrograms(): Flow<Map<WorkoutPlan, List<WorkoutProgram>>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: WorkoutPlan): Long

    // TODO: delete plan
}