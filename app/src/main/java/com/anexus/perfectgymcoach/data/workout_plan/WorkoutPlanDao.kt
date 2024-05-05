package com.anexus.perfectgymcoach.data.workout_plan

import androidx.room.*
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutPlanDao {

    @Query("SELECT * FROM `plan`")
    fun getPlans(): Flow<List<WorkoutPlan>>

    @Query("SELECT * FROM `plan` WHERE `plan`.planId LIKE :planId")
    fun getPlan(planId: Long): Flow<WorkoutPlan>

    @Query(
        "SELECT * FROM `plan` " +
        "LEFT JOIN `program` ON `plan`.planId = `program`.extPlanId "
    )
    fun getPlanMapPrograms(): Flow<Map<WorkoutPlan, List<WorkoutProgram>>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: WorkoutPlan): Long

    @Update(entity = WorkoutPlan::class)
    suspend fun updateCurrentProgram(workoutPlanUpdateProgram: WorkoutPlanUpdateProgram)

    @Update(entity = WorkoutPlan::class)
    suspend fun archivePlan(archiveWorkoutPlan: ArchiveWorkoutPlan)
}