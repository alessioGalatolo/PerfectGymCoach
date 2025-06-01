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
    fun provideRepository(dataHandler: WearDataHandler, messageHandler: WearMessageHandler, @ApplicationContext context: Context): WearRepository =
        WearRepository.getInstance(dataHandler, messageHandler, context)

}