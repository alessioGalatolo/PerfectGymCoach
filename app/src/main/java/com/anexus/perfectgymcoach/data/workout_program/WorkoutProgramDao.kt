package com.anexus.perfectgymcoach.data.workout_program

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutProgramDao {

    @Query(
        "SELECT * FROM `program`" +
        "WHERE planId LIKE :planId"
    )
    fun getPrograms(planId: Int): Flow<List<WorkoutProgram>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(program: WorkoutProgram)

    // TODO: delete plan
}