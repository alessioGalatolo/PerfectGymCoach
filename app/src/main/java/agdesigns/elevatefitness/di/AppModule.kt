package agdesigns.elevatefitness.di

import agdesigns.elevatefitness.data.BackupRepository
import agdesigns.elevatefitness.data.DatabaseBackupManager
import agdesigns.elevatefitness.data.MediaPlayingRepository
import agdesigns.elevatefitness.data.PhoneWorkoutRepository
import agdesigns.elevatefitness.service.NotificationService
import agdesigns.elevatefitness.data.PreferenceRepository
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.SearchesRepository
import agdesigns.elevatefitness.data.V1PrefsMigration
import agdesigns.elevatefitness.data.V2PrefsMigration
import agdesigns.elevatefitness.data.db.WorkoutDatabase
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.WearDataLayerRegistry
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

    private const val PREFS_FILE = "app_prefs"


    @Singleton
    @Provides
    fun providesCoroutineScope(): CoroutineScope {
        // Run this code when providing an instance of CoroutineScope
        return CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @Singleton
    @Provides
    fun provideWorkoutPlanDatabase(
        @ApplicationContext app: Context,
        scope: CoroutineScope
    ): WorkoutDatabase = WorkoutDatabase.getInstance(app, scope)

    @Singleton
    @Provides
    fun provideRepository(
        db: WorkoutDatabase,
        @ApplicationContext context: Context
    ): Repository = Repository.getInstance(db, context)


    @Singleton
    @Provides
    fun provideSearchesRepository(@ApplicationContext context: Context) = SearchesRepository(context)

    @Singleton
    @Provides
    fun provideBackupRepository(backupManager: DatabaseBackupManager) = BackupRepository(
        backupManager
    )

    @Singleton
    @Provides
    fun provideBackupManager(
        @ApplicationContext context: Context,
        dataStore: DataStore<Preferences>,
        database: WorkoutDatabase
    ): DatabaseBackupManager = DatabaseBackupManager(context, dataStore, database)

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
//            corruptionHandler = ReplaceFileCorruptionHandler(
//                produceNewData = { emptyPreferences() }
//            ),
            // Optional: SharedPreferences migration
            migrations = listOf(V1PrefsMigration(context), V2PrefsMigration(context)),
            produceFile = { context.preferencesDataStoreFile(PREFS_FILE) }
        )
    }

    @Provides
    @Singleton
    fun providePreferenceRepository(
        dataStore: DataStore<Preferences>,
        @ApplicationContext context: Context
    ): PreferenceRepository = PreferenceRepository(dataStore, context)

    @Provides
    @Singleton
    @OptIn(ExperimentalHorologistApi::class)
    fun provideMediaPlayingRepository(
        @ApplicationContext context: Context,
        registry: WearDataLayerRegistry
    ): MediaPlayingRepository = MediaPlayingRepository(context, registry)

    @Provides
    @Singleton
    fun provideNotificationService(
        @ApplicationContext context: Context
    ): NotificationService = NotificationService(context)

    @OptIn(ExperimentalHorologistApi::class)
    @Provides
    @Singleton
    fun phoneWorkoutRepository(
        registry: WearDataLayerRegistry
    ): PhoneWorkoutRepository = PhoneWorkoutRepository(
        registry
    )

}