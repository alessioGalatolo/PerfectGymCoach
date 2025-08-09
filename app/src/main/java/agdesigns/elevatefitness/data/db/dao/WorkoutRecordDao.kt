package agdesigns.elevatefitness.data.db.dao

import agdesigns.elevatefitness.data.db.entity.WorkoutRecord
import agdesigns.elevatefitness.data.db.entity.WorkoutRecordAndName
import agdesigns.elevatefitness.data.db.entity.WorkoutRecordFinish
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
        "SELECT workoutrecord.*, `program`.name " +
        "FROM workoutrecord " +
        "LEFT JOIN `program` ON workoutrecord.extProgramId = `program`.programId "
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
}