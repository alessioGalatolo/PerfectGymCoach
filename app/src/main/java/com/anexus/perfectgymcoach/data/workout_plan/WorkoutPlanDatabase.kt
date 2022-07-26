package com.anexus.perfectgymcoach.data.workout_plan

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WorkoutPlan::class],
    version = 1
)
abstract class WorkoutPlanDatabase: RoomDatabase() {
    abstract val workoutPlanDao: WorkoutPlanDao

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
                ).build()
                    .also { instance = it }
            }
        }
    }
}