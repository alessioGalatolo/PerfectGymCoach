package agdesigns.elevatefitness.presentation

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

    @Provides
    fun provideHandler(@ApplicationContext context: Context): WearDataHandler =
        WearDataHandler(context)

    @Singleton
    @Provides
    fun provideRepository(handler: WearDataHandler, @ApplicationContext context: Context): WearRepository =
        WearRepository.getInstance(handler, context)

}