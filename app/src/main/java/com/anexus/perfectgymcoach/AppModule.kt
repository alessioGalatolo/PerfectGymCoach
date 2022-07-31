package com.anexus.perfectgymcoach

import android.content.Context
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlanDatabase
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlanRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton // Tell Dagger-Hilt to create a singleton accessible everywhere in ApplicationCompenent (i.e. everywhere in the application)
    @Provides
    fun provideWorkoutPlanDatabase(
        @ApplicationContext app: Context
    ): WorkoutPlanDatabase = WorkoutPlanDatabase.getInstance(app)

    @Singleton
    @Provides
    fun provideWorkoutPlanRepository(db: WorkoutPlanDatabase):WorkoutPlanRepository = WorkoutPlanRepository.getInstance(db)
}