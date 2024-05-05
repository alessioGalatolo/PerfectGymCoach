package agdesigns.elevatefitness.data.workout_program

import androidx.room.*
import agdesigns.elevatefitness.data.exercise.ProgramExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutProgramDao {

    @Query(
        "SELECT * FROM `program` " +
        "LEFT JOIN programexercise ON `program`.programId = programexercise.extProgramId " +
        "WHERE `program`.extPlanId LIKE :planId"
    )
    fun getProgramsMapExercises(planId: Long): Flow<Map<WorkoutProgram, List<ProgramExercise>>>

    @Query(
        "SELECT * FROM `program` " +
        "LEFT JOIN programexercise ON `program`.programId = programexercise.extProgramId " +
        "WHERE `program`.programId LIKE :programId"
    )
    fun getProgramMapExercises(programId: Long): Flow<Map<WorkoutProgram, List<ProgramExercise>>>

    @Query("SELECT * FROM `program` WHERE `program`.extPlanId LIKE :planId")
    fun getPrograms(planId: Long): Flow<List<WorkoutProgram>>

    @Insert
    suspend fun insert(program: WorkoutProgram): Long

    @Update(entity = WorkoutProgram::class)
    suspend fun updateName(workoutProgramRename: WorkoutProgramRename)

    @Update(entity = WorkoutProgram::class)
    suspend fun updateOrder(workoutProgramReorders: List<WorkoutProgramReorder>)

    @Update(entity = WorkoutProgram::class)
    suspend fun removeFromPlan(removePlan: RemovePlan)

}