package com.anexus.perfectgymcoach.data.workout_plan

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.anexus.perfectgymcoach.data.exercise.*
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgramDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(
    entities =
    [
        WorkoutPlan::class,
        WorkoutProgram::class,
        WorkoutExercise::class,
        Exercise::class
    ],
    version = 1,
    exportSchema = true
)
abstract class WorkoutPlanDatabase: RoomDatabase() {
    abstract val workoutPlanDao: WorkoutPlanDao
    abstract val workoutProgramDao: WorkoutProgramDao
    abstract val workoutExerciseDao: WorkoutExerciseDao
    abstract val exerciseDao: ExerciseDao

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: WorkoutPlanDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope): WorkoutPlanDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context,
                    WorkoutPlanDatabase::class.java,
                    "workout-plan-database"
                ).addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        //pre-populate data
                        scope.launch {
                            instance?.let{it.exerciseDao.insertAll(INITIAL_EXERCISE_DATA)}
                        }
                    }
                })
                    .build()
                    .also { instance = it }
            }
        }
    }
}