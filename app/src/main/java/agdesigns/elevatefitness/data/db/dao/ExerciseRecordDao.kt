package agdesigns.elevatefitness.data.db.dao

import agdesigns.elevatefitness.data.db.entity.ExerciseRecord
import agdesigns.elevatefitness.data.db.entity.ExerciseRecordAndEquipment
import agdesigns.elevatefitness.data.db.entity.ExerciseRecordAndInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime

@Dao
interface ExerciseRecordDao {

    @Query(
        "SELECT * FROM exerciserecord " +
                "WHERE extWorkoutId LIKE :workoutId")
    fun getByWorkout(workoutId: Long): Flow<List<ExerciseRecord>>

    @Query(
        "SELECT exerciserecord.*, exercise.name, exercise.image, exercise.equipment " +
        "FROM exerciserecord " +
        "INNER JOIN exercise ON exerciserecord.extExerciseId = exercise.exerciseId " +
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

    @Query(
        "SELECT exerciserecord.*, exercise.equipment " +
                "FROM exerciserecord " +
                "INNER JOIN exercise ON exerciserecord.extExerciseId = exercise.exerciseId " +
                "WHERE exerciserecord.extExerciseId IN (:exerciseIds)")
    fun getRecordsWithEquipment(exerciseIds: List<Long>): Flow<List<ExerciseRecordAndEquipment>>

    @Query(
        "SELECT exerciserecord.*, exercise.equipment " +
                "FROM exerciserecord " +
                "INNER JOIN exercise ON exerciserecord.extExerciseId = exercise.exerciseId " +
                "WHERE exerciserecord.extExerciseId LIKE :exerciseId")
    fun getRecordsWithEquipment(exerciseId: Long): Flow<List<ExerciseRecordAndEquipment>>


    @Query(
        "SELECT exerciserecord.*, exercise.equipment " +
        "FROM exerciserecord " +
        "INNER JOIN exercise ON exerciserecord.extExerciseId = exercise.exerciseId ")
    fun getAllRecordsWithEquipment(): Flow<List<ExerciseRecordAndEquipment>>

    @Query(
        """
        SELECT * FROM exerciserecord
        WHERE date >= :startDate AND date <= :endDate"""
    )
    fun getRecordsInRange(startDate: ZonedDateTime, endDate: ZonedDateTime): Flow<List<ExerciseRecord>>


    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(plan: ExerciseRecord): Long

}