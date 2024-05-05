package agdesigns.elevatefitness.data.workout_exercise

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutExerciseDao {

    @Query("SELECT * FROM workoutexercise WHERE extWorkoutId LIKE :workoutId")
    fun getWorkoutExercises(workoutId: Long): Flow<List<WorkoutExercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: WorkoutExercise)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercises: List<WorkoutExercise>)

    @Query("DELETE FROM workoutexercise WHERE workoutExerciseId = :workoutExerciseId")
    suspend fun delete(workoutExerciseId: Long)

    @Update(entity = WorkoutExercise::class)
    suspend fun updateOrder(workoutProgramReorders: WorkoutExerciseReorder)
}