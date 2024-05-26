package agdesigns.elevatefitness.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query(
        "SELECT * FROM exercise " +
                "WHERE primaryMuscle LIKE :muscle")
    fun getExercises(muscle: Exercise.Muscle): Flow<List<Exercise>>

    @Query(
        "SELECT * FROM exercise " +
        "WHERE exerciseId LIKE :exerciseId")
    fun getExercise(exerciseId: Long): Flow<Exercise>

    @Query("SELECT * FROM exercise")
    fun getAllExercises(): Flow<List<Exercise>>

    @Insert(onConflict = REPLACE)
    suspend fun insert(exercise: Exercise)

    @Insert(onConflict = REPLACE)
    fun insertAll(exercises: List<Exercise>)

    @Query("UPDATE exercise SET probability = :newProbability WHERE exerciseId = :exerciseId")
    suspend fun resetProbability(exerciseId: Long, newProbability: Double = 1.0)

    @Query("UPDATE exercise SET probability = :newProbability")
    suspend fun resetAllProbabilities(newProbability: Double = 1.0)

}