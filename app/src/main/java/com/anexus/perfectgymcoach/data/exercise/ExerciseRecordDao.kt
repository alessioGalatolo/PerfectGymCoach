package com.anexus.perfectgymcoach.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseRecordDao {

    @Query(
        "SELECT * FROM exerciserecord " +
        "WHERE extExerciseId LIKE :exerciseId")
    fun getRecords(exerciseId: Long): Flow<List<ExerciseRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: ExerciseRecord): Long

}