package agdesigns.elevatefitness.data.exercise

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramExerciseDao {

    @Query(
        "SELECT * FROM programexercise " +
                "WHERE extProgramId LIKE :programId")
    fun getExercises(programId: Long): Flow<List<ProgramExercise>>

    @Query("SELECT * FROM programexercise WHERE programExerciseId LIKE :programExerciseId")
    fun getProgramExercise(programExerciseId: Long): Flow<ProgramExercise>

    @Query("SELECT programexercise.*, exercise.image, exercise.equipment, exercise.name, exercise.description " +
            "FROM programexercise " +
            "LEFT JOIN exercise ON programexercise.extExerciseId = exercise.exerciseId " +
            "WHERE programexercise.extProgramId = :programId")
    fun getExercisesAndInfo(programId: Long): Flow<List<ProgramExerciseAndInfo>>


    @Query("SELECT programexercise.*, exercise.image, exercise.equipment, exercise.name, exercise.description " +
            "FROM programexercise " +
            "LEFT JOIN exercise ON programexercise.extExerciseId = exercise.exerciseId " +
            "WHERE programexercise.extProgramId IN (:programIds) "
    )
    fun getExercisesAndInfo(programIds: List<Long>): Flow<List<ProgramExerciseAndInfo>>

    @Update(entity = ProgramExercise::class)
    suspend fun updateOrder(programExerciseReorders: List<ProgramExerciseReorder>)

    @Update(entity = ProgramExercise::class)
    suspend fun updateSuperset(updateExerciseSupersets: List<UpdateExerciseSuperset>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: ProgramExercise)

    @Query("DELETE FROM programexercise WHERE programExerciseId = :programExerciseId")
    suspend fun delete(programExerciseId: Long)

}