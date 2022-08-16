package com.anexus.perfectgymcoach.data.workout_record

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutRecordDao {

//    @Query(
//        "SELECT * FROM workoutrecord " +
//        "LEFT JOIN workoutexercise ON `program`.programId = workoutexercise.extProgramId " +
//        "WHERE `program`.extPlanId LIKE :planId"
//    )
//    fun getPrograms(planId: Long): Flow<Map<WorkoutRecord, List<WorkoutExercise>>>

    @Insert
    suspend fun insert(program: WorkoutRecord): Long

    @Insert
    fun update(program: WorkoutRecord)
}