package com.anexus.perfectgymcoach.data.workout_plan

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.anexus.perfectgymcoach.data.exercise.Exercise
import com.anexus.perfectgymcoach.data.exercise.ExerciseDao
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import com.anexus.perfectgymcoach.data.exercise.WorkoutExerciseDao
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgramDao

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

        fun getInstance(context: Context): WorkoutPlanDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context,
                    WorkoutPlanDatabase::class.java,
                    "workout-plan-database"
                ).createFromAsset("workout-plan-database.db").build()
                    .also { instance = it }
            }
        }
    }
}