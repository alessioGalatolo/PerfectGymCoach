package agdesigns.elevatefitness.di

import agdesignes.elevatefitness.shared.MediaSerializer
import android.content.Context
import agdesignes.elevatefitness.shared.WorkoutDataDynamicSerializer
import agdesignes.elevatefitness.shared.WorkoutDataStaticSerializer
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.datalayer.phone.PhoneDataLayerAppHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@OptIn(ExperimentalHorologistApi::class)
object DatalayerModule {

//    @Singleton
//    @Provides
//    fun providesCoroutineScope(): CoroutineScope {
//        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
//    }

    @Singleton
    @Provides
    fun phoneDataLayerAppHelper(
        @ApplicationContext applicationContext: Context,
        wearDataLayerRegistry: WearDataLayerRegistry,
    ) = PhoneDataLayerAppHelper(
        context = applicationContext,
        registry = wearDataLayerRegistry,
    )

    @Singleton
    @Provides
    fun wearDataLayerRegistry(
        @ApplicationContext applicationContext: Context,
        coroutineScope: CoroutineScope,
    ): WearDataLayerRegistry = WearDataLayerRegistry.fromContext(
        application = applicationContext,
        coroutineScope = coroutineScope,
    ).apply {
        registerSerializer(WorkoutDataStaticSerializer)

        registerSerializer(WorkoutDataDynamicSerializer)

        registerSerializer(MediaSerializer)
    }
}