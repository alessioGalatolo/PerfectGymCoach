package agdesigns.elevatefitness.data.db.dao

import agdesigns.elevatefitness.data.db.entity.WorkoutAchievement
import agdesigns.elevatefitness.data.db.entity.WorkoutAchievementAndInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutAchievementDao {

    @Query(
        "SELECT workoutachievement.*, exercise.name, exercise.nameResKey, exercise.userDefined, exercise.equipment " +
        "FROM workoutachievement " +
        "LEFT JOIN exercise ON workoutachievement.extExerciseId = exercise.exerciseId " +
        "WHERE workoutachievement.extWorkoutId = :workoutId " +
        "ORDER BY workoutachievement.achievementId"
    )
    fun getByWorkoutWithInfo(workoutId: Long): Flow<List<WorkoutAchievementAndInfo>>

    @Query(
        "SELECT workoutachievement.*, exercise.name, exercise.nameResKey, exercise.userDefined, exercise.equipment " +
        "FROM workoutachievement " +
        "LEFT JOIN exercise ON workoutachievement.extExerciseId = exercise.exerciseId " +
        "ORDER BY workoutachievement.achievementId"
    )
    fun getAllWithInfo(): Flow<List<WorkoutAchievementAndInfo>>

    @Query("SELECT DISTINCT extWorkoutId FROM workoutachievement")
    suspend fun getWorkoutIds(): List<Long>

    @Query("SELECT COUNT(*) FROM workoutachievement WHERE extWorkoutId = :workoutId")
    suspend fun countByWorkout(workoutId: Long): Int

    @Insert
    suspend fun insertAll(achievements: List<WorkoutAchievement>)

    /** Stores the achievements of a workout, unless it already has some (they are never recomputed). */
    @Transaction
    suspend fun insertIfAbsent(workoutId: Long, achievements: List<WorkoutAchievement>) {
        if (countByWorkout(workoutId) == 0)
            insertAll(achievements)
    }
}
