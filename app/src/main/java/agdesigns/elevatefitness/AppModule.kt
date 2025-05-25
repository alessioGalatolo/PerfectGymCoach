package agdesigns.elevatefitness

import android.content.Context
import agdesigns.elevatefitness.data.WorkoutDatabase
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.WearMessagesReceiver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun providesCoroutineScope(): CoroutineScope {
        // Run this code when providing an instance of CoroutineScope
        return CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @Singleton // Tell Dagger-Hilt to create a singleton accessible everywhere in ApplicationCompenent (i.e. everywhere in the application)
    @Provides
    fun provideWorkoutPlanDatabase(
        @ApplicationContext app: Context,
        scope: CoroutineScope
    ): WorkoutDatabase = WorkoutDatabase.getInstance(app, scope)

    @Singleton
    @Provides
    fun provideRepository(db: WorkoutDatabase, wearMessagesReceiver: WearMessagesReceiver, @ApplicationContext context: Context
    ): Repository = Repository.getInstance(db, wearMessagesReceiver, context)

    @Singleton
    @Provides
    fun provideWearMessageReceiver(@ApplicationContext context: Context
    ): WearMessagesReceiver = WearMessagesReceiver(context)
}