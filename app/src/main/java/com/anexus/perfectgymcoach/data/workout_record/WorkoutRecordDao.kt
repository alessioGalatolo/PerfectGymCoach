package com.anexus.perfectgymcoach.data.workout_record

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutRecordDao {

    @Query(
        "SELECT * FROM workoutrecord"
    )
    fun getRecords(): Flow<List<WorkoutRecord>>


    @Query(
        "SELECT * FROM workoutrecord WHERE workoutrecord.extProgramId LIKE :programId"
    )
    fun getRecordsByProgram(programId: Long): Flow<List<WorkoutRecord>>

    @Query(
        "SELECT * FROM workoutrecord WHERE workoutrecord.workoutId LIKE :workoutId"
    )
    fun getRecord(workoutId: Long): Flow<WorkoutRecord>


    @Query(
        "SELECT workoutrecord.*, `program`.name " +
        "FROM workoutrecord " +
        "LEFT JOIN `program` ON workoutrecord.extProgramId = `program`.programId "
    )
    fun getRecordsAndName(): Flow<List<WorkoutRecordAndName>>

    @Insert
    suspend fun insert(workoutRecord: WorkoutRecord): Long

    @Update
    fun update(workoutRecord: WorkoutRecord)

    @Update(entity = WorkoutRecord::class)
    suspend fun updateFinish(workoutRecordFinish: WorkoutRecordFinish)
}