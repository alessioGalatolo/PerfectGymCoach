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
                "WHERE extWorkoutId LIKE :workoutId")
    fun getByWorkout(workoutId: Long): Flow<List<ExerciseRecord>>

    @Query(
        "SELECT exerciserecord.*, exercise.name, exercise.image, programexercise.rest, programexercise.variation " +
        "FROM exerciserecord " +
        "INNER JOIN exercise ON exerciserecord.extExerciseId = exercise.exerciseId " +
        "INNER JOIN programexercise ON exerciserecord.extExerciseId = programexercise.extExerciseId " +
        "WHERE exerciserecord.extWorkoutId LIKE :workoutId")
    fun getByWorkoutWithInfo(workoutId: Long): Flow<List<ExerciseRecordAndInfo>>

    @Query(
        "DELETE FROM exerciserecord WHERE exerciserecord.extWorkoutId LIKE :workoutId"
    )
    suspend fun deleteByWorkout(workoutId: Long)

    @Query(
        "SELECT * FROM exerciserecord " +
        "WHERE extExerciseId LIKE :exerciseId")
    fun getRecords(exerciseId: Long): Flow<List<ExerciseRecord>>

    @Query(
        "SELECT * FROM exerciserecord " +
        "WHERE extExerciseId IN (:exerciseIds)")
    fun getRecords(exerciseIds: List<Long>): Flow<List<ExerciseRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: ExerciseRecord): Long

}