package agdesigns.elevatefitness.di

import agdesigns.elevatefitness.data.datastore.CalibrationDataStore
import agdesigns.elevatefitness.data.datastore.PermissionStateDataStore
import agdesigns.elevatefitness.data.WearRepository
import agdesigns.elevatefitness.data.db.WearDatabase
import agdesigns.elevatefitness.data.db.dao.ExerciseParamsDao
import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WearModule {
    @Singleton
    @Provides
    fun provideCalibrationDataStore(@ApplicationContext context: Context): CalibrationDataStore =
        CalibrationDataStore(context)

    @Singleton
    @Provides
    fun providePermissionStateDataStore(@ApplicationContext context: Context): PermissionStateDataStore =
        PermissionStateDataStore(context)

    @Singleton
    @Provides
    fun provideRepository(permissionStateDataStore: PermissionStateDataStore, calibrationDataStore: CalibrationDataStore, @ApplicationContext context: Context): WearRepository =
        WearRepository.getInstance(permissionStateDataStore, calibrationDataStore, context)

    @Singleton
    @Provides
    fun provideWearDatabase(@ApplicationContext context: Context): WearDatabase =
        Room.databaseBuilder(context, WearDatabase::class.java, "wear_database").build()

    @Singleton
    @Provides
    fun provideExerciseParamsDao(db: WearDatabase): ExerciseParamsDao = db.exerciseParamsDao()
}