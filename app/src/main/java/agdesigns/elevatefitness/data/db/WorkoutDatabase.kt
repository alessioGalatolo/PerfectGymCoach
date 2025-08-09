package agdesigns.elevatefitness.data.db

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
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
    version = 1,
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
                            instance?.exerciseDao?.insertAll(INITIAL_EXERCISE_DATA)
                        }
                    }
                })
                    .build()
                    .also { instance = it }
            }
        }
    }
}