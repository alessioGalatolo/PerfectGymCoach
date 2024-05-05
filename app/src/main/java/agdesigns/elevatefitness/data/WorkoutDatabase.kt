package agdesigns.elevatefitness.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import agdesigns.elevatefitness.data.exercise.*
import agdesigns.elevatefitness.data.workout_exercise.WorkoutExercise
import agdesigns.elevatefitness.data.workout_exercise.WorkoutExerciseDao
import agdesigns.elevatefitness.data.workout_record.WorkoutRecord
import agdesigns.elevatefitness.data.workout_record.WorkoutRecordDao
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlan
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlanDao
import agdesigns.elevatefitness.data.workout_program.WorkoutProgram
import agdesigns.elevatefitness.data.workout_program.WorkoutProgramDao
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