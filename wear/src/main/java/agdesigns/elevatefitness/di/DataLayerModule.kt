package agdesigns.elevatefitness.di

import agdesigns.elevatefitness.shared.MediaSerializer
import agdesigns.elevatefitness.service.WearDataLayerAppHelper
import android.content.Context
import agdesigns.elevatefitness.shared.WorkoutDataDynamicSerializer
import agdesigns.elevatefitness.shared.WorkoutDataStaticSerializer
import agdesigns.elevatefitness.shared.grpc.MediaServiceGrpcKt
import agdesigns.elevatefitness.shared.grpc.PhoneInfoServiceGrpcKt
import agdesigns.elevatefitness.shared.grpc.WorkoutServiceGrpcKt
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.ProtoDataStoreHelper.registerProtoDataListener
import com.google.android.horologist.data.TargetNodeId
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.data.store.ProtoDataListener
import com.google.android.horologist.datalayer.grpc.GrpcExtensions.grpcClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.ActivityRetainedLifecycle
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow

@Module
@InstallIn(ActivityRetainedComponent::class)
@OptIn(ExperimentalHorologistApi::class)
object DataLayerModule {

    @ActivityRetainedScoped
    @Provides
    fun coroutineScope(
        activityRetainedLifecycle: ActivityRetainedLifecycle,
    ): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default).also {
            activityRetainedLifecycle.addOnClearedListener {
                it.cancel()
            }
        }
    }


    @ActivityRetainedScoped
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

    @ActivityRetainedScoped
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

    @ActivityRetainedScoped
    @Provides
    fun workoutService(
        wearDataLayerRegistry: WearDataLayerRegistry,
        coroutineScope: CoroutineScope,
    ): WorkoutServiceGrpcKt.WorkoutServiceCoroutineStub =
        wearDataLayerRegistry.grpcClient(
            nodeId = TargetNodeId.PairedPhone,
            coroutineScope = coroutineScope,
        ) {
            WorkoutServiceGrpcKt.WorkoutServiceCoroutineStub(it)
        }


    @ActivityRetainedScoped
    @Provides
    fun mediaService(
        wearDataLayerRegistry: WearDataLayerRegistry,
        coroutineScope: CoroutineScope,
    ): MediaServiceGrpcKt.MediaServiceCoroutineStub =
        wearDataLayerRegistry.grpcClient(
            nodeId = TargetNodeId.PairedPhone,
            coroutineScope = coroutineScope,
        ) {
            MediaServiceGrpcKt.MediaServiceCoroutineStub(it)
        }

    @ActivityRetainedScoped
    @Provides
    fun phoneInfoService(
        wearDataLayerRegistry: WearDataLayerRegistry,
        coroutineScope: CoroutineScope,
    ): PhoneInfoServiceGrpcKt.PhoneInfoServiceCoroutineStub =
        wearDataLayerRegistry.grpcClient(
            nodeId = TargetNodeId.PairedPhone,
            coroutineScope = coroutineScope,
        ) {
            PhoneInfoServiceGrpcKt.PhoneInfoServiceCoroutineStub(it)
        }
}