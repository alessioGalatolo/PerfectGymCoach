package agdesigns.elevatefitness.di

import agdesigns.elevatefitness.ui.navigation.DestinationsNavigator
import agdesigns.elevatefitness.ui.navigation.HomeDestination
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
object NavigatorModule {

    @Provides
    @ActivityRetainedScoped
    fun provideNavigator() = DestinationsNavigator(HomeDestination)

}