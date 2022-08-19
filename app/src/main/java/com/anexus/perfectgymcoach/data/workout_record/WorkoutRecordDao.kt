package com.anexus.perfectgymcoach.data.workout_record

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutRecordDao {

    @Query(
        "SELECT * FROM workoutrecord"
    )
    fun getRecords(): Flow<List<WorkoutRecord>>

    @Insert
    suspend fun insert(workoutRecord: WorkoutRecord): Long

    @Update
    fun update(workoutRecord: WorkoutRecord)

    @Update(entity = WorkoutRecord::class)
    suspend fun updateFinish(workoutRecordFinish: WorkoutRecordFinish)
}