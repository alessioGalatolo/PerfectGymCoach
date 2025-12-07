package agdesigns.elevatefitness.di

import agdesigns.elevatefitness.data.datastore.PermissionStateDataStore
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
    fun provideRepository(permissionStateDataStore: PermissionStateDataStore, @ApplicationContext context: Context): WearRepository =
        WearRepository.getInstance(permissionStateDataStore, context)
}