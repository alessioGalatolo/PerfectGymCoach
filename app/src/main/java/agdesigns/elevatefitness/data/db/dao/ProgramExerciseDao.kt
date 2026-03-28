package agdesigns.elevatefitness.data.db.dao

import agdesigns.elevatefitness.data.db.entity.ProgramExercise
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseAndInfo
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseReorder
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseWithExercise
import agdesigns.elevatefitness.data.db.entity.UpdateExerciseSuperset
import agdesigns.elevatefitness.data.db.entity.UpdateProgramExerciseSetTypes
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramExerciseDao {

    // get all exercises
    @Query("SELECT * FROM programexercise")
    fun getAll(): List<ProgramExercise>

    @Update
    suspend fun update(programExercise: ProgramExercise)

    @Query(
        "SELECT * FROM programexercise " +
                "WHERE extProgramId LIKE :programId")
    fun getExercises(programId: Long): Flow<List<ProgramExercise>>

    @Query("SELECT * FROM programexercise WHERE programExerciseId LIKE :programExerciseId")
    fun getProgramExercise(programExerciseId: Long): Flow<ProgramExercise>

    @Query("SELECT programexercise.*, exercise.image, exercise.imageResKey, exercise.equipment, exercise.name, exercise.nameResKey, exercise.description, exercise.descriptionResKey, exercise.userDefined " +
            "FROM programexercise " +
            "LEFT JOIN exercise ON programexercise.extExerciseId = exercise.exerciseId " +
            "WHERE programexercise.extProgramId = :programId")
    fun getExercisesAndInfo(programId: Long): Flow<List<ProgramExerciseAndInfo>>

    @Query("SELECT programexercise.*, exercise.* " +
            "FROM programexercise " +
            "LEFT JOIN exercise ON programexercise.extExerciseId = exercise.exerciseId " +
            "WHERE programexercise.extProgramId = :programId")
    fun getProgramExercisesWithExercise(programId: Long): Flow<List<ProgramExerciseWithExercise>>

    @Query("SELECT programexercise.*, exercise.image, exercise.imageResKey, exercise.equipment, exercise.name, exercise.nameResKey, exercise.description, exercise.descriptionResKey, exercise.userDefined " +
            "FROM programexercise " +
            "LEFT JOIN exercise ON programexercise.extExerciseId = exercise.exerciseId " +
            "WHERE programexercise.extProgramId IN (:programIds) "
    )
    fun getExercisesAndInfo(programIds: List<Long>): Flow<List<ProgramExerciseAndInfo>>

    @Update(entity = ProgramExercise::class)
    suspend fun updateOrder(programExerciseReorders: List<ProgramExerciseReorder>)

    @Update(entity = ProgramExercise::class)
    suspend fun updateSuperset(updateExerciseSupersets: List<UpdateExerciseSuperset>)

    @Update(entity = ProgramExercise::class)
    suspend fun updateSetTypes(updateProgramExerciseSetTypes: UpdateProgramExerciseSetTypes)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(plan: ProgramExercise): Long

    @Query("DELETE FROM programexercise WHERE programExerciseId = :programExerciseId")
    suspend fun delete(programExerciseId: Long)

}