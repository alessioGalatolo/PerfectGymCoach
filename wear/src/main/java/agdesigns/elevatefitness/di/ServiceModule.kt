package agdesigns.elevatefitness.di

import agdesignes.elevatefitness.shared.MediaSerializer
import agdesigns.elevatefitness.service.WearDataLayerAppHelper
import android.content.Context
import android.util.Log
import agdesignes.elevatefitness.shared.WorkoutDataDynamicSerializer
import agdesignes.elevatefitness.shared.WorkoutDataStaticSerializer
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.data.store.ProtoDataListener
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(ServiceComponent::class)
@OptIn(ExperimentalHorologistApi::class)
object ServiceModule {

    @ServiceScoped
    @Provides
    fun coroutineScope(): CoroutineScope {
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e(
                "ElevateFitnessWear",
                "Uncaught exception thrown by a service: ${throwable.message}",
                throwable,
            )
        }
        return CoroutineScope(Dispatchers.IO + SupervisorJob() + coroutineExceptionHandler)
    }

    @ServiceScoped
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

    @ServiceScoped
    @Provides
    fun wearDataLayerAppHelper(
        @ApplicationContext applicationContext: Context,
        wearDataLayerRegistry: WearDataLayerRegistry,
        coroutineScope: CoroutineScope,
    ) = WearDataLayerAppHelper(
        context = applicationContext,
        registry = wearDataLayerRegistry,
        scope = coroutineScope,
    )
}