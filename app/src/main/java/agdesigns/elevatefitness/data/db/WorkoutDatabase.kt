package agdesigns.elevatefitness.data.db

import agdesigns.elevatefitness.data.dataStore
import agdesigns.elevatefitness.data.db.entity.Exercise
import agdesigns.elevatefitness.data.db.dao.ExerciseDao
import agdesigns.elevatefitness.data.db.entity.ExerciseRecord
import agdesigns.elevatefitness.data.db.dao.ExerciseRecordDao
import agdesigns.elevatefitness.data.db.entity.ProgramExercise
import agdesigns.elevatefitness.data.db.dao.ProgramExerciseDao
import agdesigns.elevatefitness.data.db.entity.WorkoutExercise
import agdesigns.elevatefitness.data.db.dao.WorkoutExerciseDao
import agdesigns.elevatefitness.data.db.entity.WorkoutPlan
import agdesigns.elevatefitness.data.db.dao.WorkoutPlanDao
import agdesigns.elevatefitness.data.db.entity.WorkoutProgram
import agdesigns.elevatefitness.data.db.dao.WorkoutProgramDao
import agdesigns.elevatefitness.data.db.dao.WorkoutRecordDao
import agdesigns.elevatefitness.data.db.entity.WorkoutRecord
import agdesigns.elevatefitness.data.db.entity.getVariation
import agdesigns.elevatefitness.utils.getLocalizedString
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale

@Database(
    entities =
    [
        WorkoutPlan::class,
        WorkoutProgram::class,
        ProgramExercise::class,
        ExerciseRecord::class,
        WorkoutRecord::class,
        WorkoutExercise::class,
        Exercise::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class WorkoutDatabase: RoomDatabase() {
    abstract val workoutPlanDao: WorkoutPlanDao
    abstract val workoutProgramDao: WorkoutProgramDao
    abstract val programExerciseDao: ProgramExerciseDao
    abstract val exerciseRecordDao: ExerciseRecordDao
    abstract val workoutRecordDao: WorkoutRecordDao
    abstract val exerciseDao: ExerciseDao
    abstract val workoutExerciseDao: WorkoutExerciseDao

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: WorkoutDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope): WorkoutDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context,
                    WorkoutDatabase::class.java,
                    "workout-database"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        //pre-populate data
                        scope.launch {
                            instance?.exerciseDao?.insertAll(
                                INITIAL_EXERCISE_DATA
                            )
                        }
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        // Check if migration is needed every time database opens
                        checkAndPerformDataMigration(context)
                    }
                }).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
        }

        private fun checkAndPerformDataMigration(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                val database = getInstance(context, this@launch)
                val dao = database.exerciseDao

                // Check if any exercises need data migration
                val pendingMigrationCount = dao.getMigrationPendingCount()
                if (pendingMigrationCount > 0) {
                    Log.d("ExerciseDatabase", "Found $pendingMigrationCount exercises needing migration")

                    // Perform data migration with context access
                    val migrator = ExerciseDataMigrator(context)
                    migrator.migrateExerciseData(database)
                }
            }
        }
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercise ADD COLUMN nameResKey TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE exercise ADD COLUMN imageResKey TEXT NOT NULL DEFAULT 'finish_workout'")
        db.execSQL("ALTER TABLE exercise ADD COLUMN descriptionResKey TEXT NOT NULL DEFAULT 'description_not_available'")
        db.execSQL("ALTER TABLE exercise ADD COLUMN variationsResKeys TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE exercise ADD COLUMN userDefined INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE exercise ADD COLUMN needsMigration INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE programexercise ADD COLUMN variationResKey TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE exerciserecord ADD COLUMN variationResKey TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE workoutexercise ADD COLUMN variationResKey TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE workoutexercise ADD COLUMN nameResKey TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE workoutexercise ADD COLUMN imageResKey TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE workoutexercise ADD COLUMN descriptionResKey TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE workoutexercise ADD COLUMN userDefined INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE workoutrecord ADD COLUMN intensityPercent REAL NOT NULL DEFAULT 50.0")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE WorkoutRecord ADD COLUMN workoutModifications TEXT NOT NULL DEFAULT ''"
        )
        db.execSQL(
            "ALTER TABLE ExerciseRecord ADD COLUMN extWorkoutExerciseId INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_ExerciseRecord_extWorkoutExerciseId`" +
                    "ON `ExerciseRecord` (`extWorkoutExerciseId`);"
        )
    }
}

class ExerciseDataMigrator(private val context: Context) {
    private val dbVersionKey = intPreferencesKey("Current db version")

    // map exercise name to exercise
    val resolvedExercises: Map<String, Exercise> = INITIAL_EXERCISE_DATA.associateBy {
        context.getLocalizedString(it.nameResource, Locale.ENGLISH)
    }
    val variations: Map<String, String> = INITIAL_EXERCISE_DATA.flatMap { exercise ->
        exercise.variationsResKeys
    }.associateBy {
        context.getLocalizedString(getVariation(it), Locale.ENGLISH)
    }


    suspend fun migrateExerciseData(db: WorkoutDatabase) {
        val dbVersion = context.dataStore.data.map {
            it[dbVersionKey] ?: 1
        }.first()
        // migration already done
        if (dbVersion > 1) {
            Log.d("ExerciseDataMigrator", "Already migrated")
            return
        }
        Log.d("ExerciseDataMigrator", "Found db version $dbVersion, proceeding with migration")
        // Start by migrating core exercises
        val existingExercises = db.exerciseDao.getExercisesForMigration()
        for (exercise in existingExercises) {
            val associatedExercise = resolvedExercises[exercise.name]
            val newExercise = if (associatedExercise != null) {
                // an exercise with the name was found, update with new stuff
                // remove needs migration flag
                associatedExercise.copy(
                    exerciseId = exercise.exerciseId,
                    needsMigration = false
                )
            } else {
                // either there was a problem in the migration or exercise is user defined
                // log but assume the latter
                Log.d("ExerciseDataMigrator", "No exercise found for ${exercise.name}")
                exercise.copy(
                    needsMigration = false,
                    userDefined = true
                )
            }
            db.exerciseDao.updateExercise(newExercise)
        }
        // now migrate program exercises
        db.programExerciseDao.getAll().forEach {
            if (it.variationResKey.isBlank()) {
                val variationResKey = variations[it.variation]
                if (variationResKey != null) {
                    db.programExerciseDao.update(
                        it.copy(
                            variation = "",
                            variationResKey = variationResKey
                        )
                    )
                }
            }
        }
        // now migrate exercise record
        db.exerciseRecordDao.getAll().forEach {
            if (it.variationResKey.isBlank()) {
                val variationResKey = variations[it.variation]
                if (variationResKey != null) {
                    db.exerciseRecordDao.update(
                        it.copy(
                            variation = "",
                            variationResKey = variationResKey
                        )
                    )
                }
            }
        }
        // migrate workout exercise
        db.workoutExerciseDao.getAll().forEach {
            val exercise = db.exerciseDao.getExercise(it.extExerciseId).first()
            val variationResKey = variations[it.variation] ?: ""
            val variation2write = if (variationResKey.isBlank()) it.variation else ""
            db.workoutExerciseDao.update(
                it.copy(
                    variationResKey = variationResKey,
                    nameResKey = exercise.nameResKey,
                    imageResKey = exercise.imageResKey,
                    descriptionResKey = exercise.descriptionResKey,
                    variation = variation2write,
                    userDefined = exercise.userDefined
                )
            )
        }
        // migrate dataStore (not best place but works)
        context.dataStore.edit {
            it[dbVersionKey] = 2
        }


        // update db version
        context.dataStore.edit {
            it[dbVersionKey] = 2
        }
    }
}