package agdesigns.elevatefitness.data.db.dao

import agdesigns.elevatefitness.data.db.entity.ArchiveWorkoutPlan
import agdesigns.elevatefitness.data.db.entity.WorkoutPlan
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanRename
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanUpdateProgram
import agdesigns.elevatefitness.data.db.entity.WorkoutProgram
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutPlanDao {

    @Query("SELECT * FROM `plan`")
    fun getPlans(): Flow<List<WorkoutPlan>>

    @Query("SELECT * FROM `plan` WHERE `plan`.planId LIKE :planId")
    fun getPlan(planId: Long): Flow<WorkoutPlan?>

    @Query(
        "SELECT * FROM `plan` " +
        "LEFT JOIN `program` ON `plan`.planId = `program`.extPlanId "
    )
    fun getPlanMapPrograms(): Flow<Map<WorkoutPlan, List<WorkoutProgram>>>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(plan: WorkoutPlan): Long

    @Update(entity = WorkoutPlan::class)
    suspend fun updateCurrentProgram(workoutPlanUpdateProgram: WorkoutPlanUpdateProgram)

    @Update(entity = WorkoutPlan::class)
    suspend fun archivePlan(archiveWorkoutPlan: ArchiveWorkoutPlan)

    @Update(entity = WorkoutPlan::class)
    suspend fun updateName(workoutPlanRename: WorkoutPlanRename)
}