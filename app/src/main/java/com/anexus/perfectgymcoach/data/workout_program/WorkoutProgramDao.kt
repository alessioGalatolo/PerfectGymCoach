package com.anexus.perfectgymcoach.data.workout_program

import androidx.room.*
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import com.anexus.perfectgymcoach.data.workout_record.WorkoutRecord
import com.anexus.perfectgymcoach.data.workout_record.WorkoutRecordFinish
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutProgramDao {

    @Query(
        "SELECT * FROM `program` " +
        "LEFT JOIN workoutexercise ON `program`.programId = workoutexercise.extProgramId " +
        "WHERE `program`.extPlanId LIKE :planId"
    )
    fun getProgramMapExercises(planId: Long): Flow<Map<WorkoutProgram, List<WorkoutExercise>>>

    @Query("SELECT * FROM `program` WHERE `program`.extPlanId LIKE :planId")
    fun getPrograms(planId: Long): Flow<List<WorkoutProgram>>

    @Insert
    suspend fun insert(program: WorkoutProgram): Long

    @Update(entity = WorkoutProgram::class)
    suspend fun updateName(workoutProgramRename: WorkoutProgramRename)

    @Update(entity = WorkoutProgram::class)
    suspend fun updateOrder(workoutProgramReorders: List<WorkoutProgramReorder>)

    @Query("DELETE FROM `program` WHERE programId = :programId")
    suspend fun delete(programId: Long)
}