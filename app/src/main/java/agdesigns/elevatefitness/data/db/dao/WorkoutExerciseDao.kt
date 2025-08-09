package agdesigns.elevatefitness.data.db.dao

import agdesigns.elevatefitness.data.db.entity.WorkoutExercise
import agdesigns.elevatefitness.data.db.entity.WorkoutExerciseReorder
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutExerciseDao {

    @Query("SELECT * FROM workoutexercise WHERE extWorkoutId LIKE :workoutId")
    fun getWorkoutExercises(workoutId: Long): Flow<List<WorkoutExercise>>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(exercise: WorkoutExercise)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(exercises: List<WorkoutExercise>)

    @Query("DELETE FROM workoutexercise WHERE workoutExerciseId = :workoutExerciseId")
    suspend fun delete(workoutExerciseId: Long)

    @Update(entity = WorkoutExercise::class)
    suspend fun updateOrder(workoutProgramReorders: WorkoutExerciseReorder)
}