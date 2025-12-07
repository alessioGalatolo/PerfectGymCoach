package agdesigns.elevatefitness.di

import agdesignes.elevatefitness.shared.MediaSerializer
import agdesignes.elevatefitness.shared.PairedWatch
import android.content.Context
import agdesignes.elevatefitness.shared.WorkoutDataDynamicSerializer
import agdesignes.elevatefitness.shared.WorkoutDataStaticSerializer
import agdesignes.elevatefitness.shared.grpc.WorkoutWearServiceGrpcKt
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.datalayer.grpc.GrpcExtensions.grpcClient
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

    @Singleton
    @Provides
    fun wearWorkoutService(
        wearDataLayerRegistry: WearDataLayerRegistry,
        coroutineScope: CoroutineScope,
    ): WorkoutWearServiceGrpcKt.WorkoutWearServiceCoroutineStub =
        wearDataLayerRegistry.grpcClient(
            nodeId = PairedWatch,
            coroutineScope = coroutineScope,
        ) {
            WorkoutWearServiceGrpcKt.WorkoutWearServiceCoroutineStub(it)
        }
}