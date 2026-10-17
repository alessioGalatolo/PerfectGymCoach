package agdesigns.elevatefitness.data.db.dao

import agdesigns.elevatefitness.data.db.entity.WorkoutRecord
import agdesigns.elevatefitness.data.db.entity.WorkoutRecordAndName
import agdesigns.elevatefitness.data.db.entity.WorkoutRecordFinish
import agdesigns.elevatefitness.data.db.entity.WorkoutRecordHealthId
import agdesigns.elevatefitness.data.db.entity.WorkoutRecordStart
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime

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
        "SELECT workoutrecord.*, `program`.name, `program`.extPlanId, `plan`.name AS planName, " +
        // reps are stored comma-separated, one entry per set
        "(SELECT COALESCE(SUM(CASE WHEN reps = '' THEN 0 " +
        "ELSE LENGTH(reps) - LENGTH(REPLACE(reps, ',', '')) + 1 END), 0) " +
        "FROM exerciserecord WHERE exerciserecord.extWorkoutId = workoutrecord.workoutId) AS totalSets " +
        "FROM workoutrecord " +
        "LEFT JOIN `program` ON workoutrecord.extProgramId = `program`.programId " +
        "LEFT JOIN `plan` ON `program`.extPlanId = `plan`.planId "
    )
    fun getRecordsAndName(): Flow<List<WorkoutRecordAndName>>

    @Query("""
    SELECT * FROM WorkoutRecord 
    WHERE startDate >= :startDate AND startDate <= :endDate
""")
    fun getWorkoutsBetween(
        startDate: ZonedDateTime,
        endDate: ZonedDateTime
    ): Flow<List<WorkoutRecord>>

    @Insert
    suspend fun insert(workoutRecord: WorkoutRecord): Long

    @Update
    fun update(workoutRecord: WorkoutRecord)

    @Update(entity = WorkoutRecord::class)
    suspend fun updateStart(workoutRecordStart: WorkoutRecordStart)

    @Update(entity = WorkoutRecord::class)
    suspend fun updateFinish(workoutRecordFinish: WorkoutRecordFinish)

    @Update(entity = WorkoutRecord::class)
    suspend fun updateHealthId(workoutRecordHealthId: WorkoutRecordHealthId)
}