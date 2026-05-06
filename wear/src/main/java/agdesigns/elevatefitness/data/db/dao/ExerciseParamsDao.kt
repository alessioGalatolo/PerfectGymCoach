package agdesigns.elevatefitness.data.db.dao

import agdesigns.elevatefitness.data.db.entity.ExerciseParamsEntity
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ExerciseParamsDao {
    @Query("SELECT * FROM exercise_params WHERE exerciseId = :exerciseId")
    suspend fun getById(exerciseId: Long): ExerciseParamsEntity?

    @Upsert
    suspend fun upsert(entity: ExerciseParamsEntity)
}