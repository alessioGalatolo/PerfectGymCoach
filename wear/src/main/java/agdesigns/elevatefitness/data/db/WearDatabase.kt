package agdesigns.elevatefitness.data.db

import agdesigns.elevatefitness.data.db.dao.ExerciseParamsDao
import agdesigns.elevatefitness.data.db.entity.ExerciseParamsEntity
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ExerciseParamsEntity::class], version = 1, exportSchema = true)
abstract class WearDatabase : RoomDatabase() {
    abstract fun exerciseParamsDao(): ExerciseParamsDao
}