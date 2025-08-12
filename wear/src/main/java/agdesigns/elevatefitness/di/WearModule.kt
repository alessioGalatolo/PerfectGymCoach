package agdesigns.elevatefitness.di

import agdesigns.elevatefitness.data.datastore.PermissionStateDataStore
import agdesigns.elevatefitness.data.datastore.WorkoutDataStore
import agdesigns.elevatefitness.data.phone.WearDataHandler
import agdesigns.elevatefitness.data.phone.WearMessageHandler
import agdesigns.elevatefitness.data.WearRepository
import android.content.Context
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
    fun providePermissionStateDataStore(@ApplicationContext context: Context): PermissionStateDataStore =
        PermissionStateDataStore(context)

    @Singleton
    @Provides
    fun provideWorkoutDataStore(@ApplicationContext context: Context): WorkoutDataStore =
        WorkoutDataStore(context)

    @Singleton
    @Provides
    fun provideDataHandler(@ApplicationContext context: Context): WearDataHandler =
        WearDataHandler(context)

    @Singleton
    @Provides
    fun provideMessageHandler(@ApplicationContext context: Context): WearMessageHandler =
        WearMessageHandler(context)

    @Singleton
    @Provides
    fun provideRepository(dataHandler: WearDataHandler, messageHandler: WearMessageHandler, dataStore: WorkoutDataStore, permissionStateDataStore: PermissionStateDataStore, @ApplicationContext context: Context): WearRepository =
        WearRepository.Companion.getInstance(dataHandler, messageHandler, dataStore, permissionStateDataStore, context)

}