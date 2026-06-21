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
import agdesigns.elevatefitness.data.db.entity.UpdateExerciseRecordSetTypes
import agdesigns.elevatefitness.shared.SetType
import agdesigns.elevatefitness.data.db.entity.UpdateProgramExerciseSetTypes
import agdesigns.elevatefitness.data.db.entity.WorkoutExerciseUpdateSetTypes
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
    version = 14,
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
                }).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14
                )
                    .build()
                    .also { instance = it }
            }
        }

        private fun checkAndPerformDataMigration(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                val database = getInstance(context, this@launch)

                // Perform data migration with context access
                val migrator = ExerciseDataMigrator(context)
                migrator.migrateExerciseData(database)
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

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE Exercise ADD COLUMN isDurationBased INTEGER NOT NULL DEFAULT 0"
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE WorkoutExercise ADD COLUMN overriddenDurationBased INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE ProgramExercise ADD COLUMN overriddenDurationBased INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE ExerciseRecord ADD COLUMN overriddenDurationBased INTEGER NOT NULL DEFAULT 0"
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE WorkoutRecord ADD COLUMN healthRecordId TEXT"
        )
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE WorkoutRecord ADD COLUMN maxHeartRate INTEGER"
        )
        db.execSQL(
            "ALTER TABLE WorkoutRecord ADD COLUMN avgHeartRate INTEGER"
        )
        db.execSQL(
            "ALTER TABLE WorkoutRecord ADD COLUMN minHeartRate INTEGER"
        )
        db.execSQL(
            "ALTER TABLE WorkoutRecord ADD COLUMN heartRates TEXT"
        )
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE ProgramExercise ADD COLUMN setTypes TEXT"
        )
        db.execSQL(
            "ALTER TABLE WorkoutExercise ADD COLUMN setTypes TEXT"
        )
        db.execSQL(
            "ALTER TABLE ExerciseRecord ADD COLUMN setTypes TEXT"
        )
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE ExerciseRecord ADD COLUMN barbellTypeResKey TEXT NOT NULL DEFAULT ''"
        )
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE Exercise ADD COLUMN wearRepTrackable TEXT NOT NULL DEFAULT 'NOT_TRACKABLE'"
        )
        db.execSQL(
            "ALTER TABLE Exercise ADD COLUMN firstPhase TEXT NOT NULL DEFAULT 'AGAINST_GRAVITY'"
        )
        db.execSQL(
            "ALTER TABLE WorkoutExercise ADD COLUMN wearRepTrackable TEXT NOT NULL DEFAULT 'NOT_TRACKABLE'"
        )
        db.execSQL(
            "ALTER TABLE WorkoutExercise ADD COLUMN firstPhase TEXT NOT NULL DEFAULT 'AGAINST_GRAVITY'"
        )
        db.execSQL(
            "ALTER TABLE ExerciseRecord ADD COLUMN trackingResults TEXT NOT NULL DEFAULT ''"
        )


    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE Exercise ADD COLUMN healthExerciseSegmentType INTEGER NOT NULL DEFAULT 38"
        )
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE Exercise ADD COLUMN muscleRegion TEXT NOT NULL DEFAULT 'PRIMARY'"
        )
        db.execSQL(
            "ALTER TABLE Exercise ADD COLUMN suggestOnlyIfPerformed INTEGER NOT NULL DEFAULT 0"
        )
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE WorkoutRecord ADD COLUMN watchOffsetSeconds INTEGER"
        )
    }
}


class ExerciseDataMigrator(private val context: Context) {
    private val dbVersionKey = intPreferencesKey("Current db version")

    suspend fun migrateExerciseData(db: WorkoutDatabase) {
        val dbVersion = context.dataStore.data.map {
            it[dbVersionKey] ?: 1
        }.first()
        // migration already done
        if (dbVersion < 2) {
            Log.d(
                "ExerciseDataMigrator",
                "Found db version $dbVersion, proceeding with migration to v2"
            )
            migrateExercises1To2(db)
        }
        if (dbVersion < 5) {
            Log.d(
                "ExerciseDataMigrator",
                "Found db version $dbVersion, proceeding with migration to v5"
            )
            migrateExercises2To5(db)
        }
        if (dbVersion < 9) {
            Log.d(
                "ExerciseDataMigrator",
                "Found db version $dbVersion, proceeding with migration to v9"
            )
            migrateExercises5To9(db)
        }
        if (dbVersion < 11) {
            Log.d(
                "ExerciseDataMigrator",
                "Found db version $dbVersion, proceeding with migration to v11"
            )
            migrateExercises9To11(db)
        }
        if (dbVersion < 12) {
            Log.d(
                "ExerciseDataMigrator",
                "Found db version $dbVersion, proceeding with migration to v12"
            )
            migrateExercises11To12(db)
        }
        if (dbVersion < 13) {
            Log.d(
                "ExerciseDataMigrator",
                "Found db version $dbVersion, proceeding with migration to v13"
            )
            migrateExercises12To13(db)
        }
    }

    private suspend fun migrateExercises1To2(db: WorkoutDatabase) {
        // map exercise name to exercise
        val resolvedExercises: Map<String, Exercise> = INITIAL_EXERCISE_DATA.associateBy {
            context.getLocalizedString(it.nameResource, Locale.ENGLISH)
        }
        val variations: Map<String, String> = INITIAL_EXERCISE_DATA.flatMap { exercise ->
            exercise.variationsResKeys
        }.associateBy {
            context.getLocalizedString(getVariation(it), Locale.ENGLISH)
        }
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
    }

    private suspend fun migrateExercises2To5(db: WorkoutDatabase) {
        val dao = db.exerciseDao

        // Get current exercises keyed by nameResKey
        val existing = dao.getAllExercises()
            .first()
            .associateBy { it.nameResKey }

        INITIAL_EXERCISE_DATA.forEach { new ->
            val old = existing[new.nameResKey]
            if (old != null) {
                // Preserve the existing DB id so the row is updated, not duplicated.
                // Copy all fields from the new definition except keep the old primary key.
                dao.updateExercise(new.copy(exerciseId = old.exerciseId))
            } else {
                // Brand-new exercise — insert with whatever id the new data carries
                // (0 / auto-generate, or an explicit value if you assign them).
                dao.insert(new)
            }
        }
        context.dataStore.edit {
            it[dbVersionKey] = 5
        }
    }

    private suspend fun migrateExercises5To9(db: WorkoutDatabase) {
        // The following is a fix for a long-standing bug on old installs (bug is now fixed but db is messed up)
        // it doesn't really belong here but it's a good place
        // Bug: a program can have associate exercise whose "orderInProgram" has holes
        // e.g., ex1 -> orderInProgram = 1 (but should be 0), ex2 -> orderInProgram = 4 (but should be 2), etc.
        val programMapEx = db.workoutProgramDao.getAllProgramsMapExercises().first()
        for ((_, exercises) in programMapEx) {
            exercises.sortedBy {
                it.orderInProgram
            }.mapIndexed { index, exercise ->
                exercise.copy(
                    orderInProgram = index
                )
            }.forEach {
                db.programExerciseDao.update(it)
            }
        }

        // here we introduced setTypes, update workoutExercises and programExercises
        // to have a list of SetType.NORMAL that matches number of sets
        val workoutExercises = db.workoutExerciseDao.getAll()
        workoutExercises.filter { it.setTypes == null }.forEach {
            db.workoutExerciseDao.updateSetTypes(
                WorkoutExerciseUpdateSetTypes(
                    it.workoutExerciseId,
                    List(it.reps.size) { _ ->
                        SetType.NORMAL
                    }
                )
            )
        }
        val programExercises = db.programExerciseDao.getAll()
        programExercises.filter { it.setTypes == null }.forEach {
            db.programExerciseDao.updateSetTypes(
                UpdateProgramExerciseSetTypes(
                    it.programExerciseId,
                    List(it.reps.size) { _ ->
                        SetType.NORMAL
                    }
                )
            )
        }
        val exerciseRecords = db.exerciseRecordDao.getAll()
        exerciseRecords.filter { it.setTypes == null }.forEach {
            db.exerciseRecordDao.updateSetTypes(
                UpdateExerciseRecordSetTypes(
                    it.recordId,
                    List(it.reps.size) { _ ->
                        SetType.NORMAL
                    }
                )
            )
        }
        context.dataStore.edit {
            it[dbVersionKey] = 9
        }
    }

    private suspend fun migrateExercises9To11(db: WorkoutDatabase) {
        val dao = db.exerciseDao
        val existing = dao.getAllExercises().first().associateBy { it.nameResKey }
        INITIAL_EXERCISE_DATA.forEach { new ->
            val old = existing[new.nameResKey] ?: return@forEach
            dao.updateExercise(old.copy(wearRepTrackable = new.wearRepTrackable))
        }
        val records = db.exerciseRecordDao.getAll()
        records.forEach {
            db.exerciseRecordDao.update(
                it.copy(
                    trackingResults = List(it.reps.size) { _ -> null }
                )
            )
        }
        context.dataStore.edit {
            it[dbVersionKey] = 11
        }
    }

    private suspend fun migrateExercises11To12(db: WorkoutDatabase) {
        val dao = db.exerciseDao
        val existing = dao.getAllExercises().first().associateBy { it.nameResKey }
        INITIAL_EXERCISE_DATA.forEach { new ->
            val old = existing[new.nameResKey] ?: return@forEach
            dao.updateExercise(
                old.copy(
                    healthExerciseSegmentType = new.healthExerciseSegmentType,
                    // forgot to update this in the previous migration
                    firstPhase = new.firstPhase
                )
            )
        }
        context.dataStore.edit {
            it[dbVersionKey] = 12
        }
    }

    private suspend fun migrateExercises12To13(db: WorkoutDatabase) {
        val dao = db.exerciseDao
        val existing = dao.getAllExercises().first().associateBy { it.nameResKey }
        INITIAL_EXERCISE_DATA.forEach { new ->
            val old = existing[new.nameResKey] ?: return@forEach
            dao.updateExercise(
                new.copy(
                    exerciseId = old.exerciseId
                )
            )
        }
        context.dataStore.edit {
            it[dbVersionKey] = 13
        }
    }

}