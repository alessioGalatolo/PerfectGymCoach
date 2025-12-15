package agdesigns.elevatefitness.service

import agdesigns.elevatefitness.shared.grpc.Media
import agdesigns.elevatefitness.shared.grpc.MediaServiceGrpcKt
import agdesigns.elevatefitness.shared.grpc.Workout
import agdesigns.elevatefitness.shared.grpc.WorkoutServiceGrpcKt
import agdesigns.elevatefitness.data.MediaPlayingRepository
import agdesigns.elevatefitness.data.PhoneWorkoutRepository
import android.util.Log
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.datalayer.grpc.server.BaseGrpcDataService
import com.google.protobuf.Empty
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@OptIn(ExperimentalHorologistApi::class)
class WearMediaDataService: BaseGrpcDataService<MediaServiceGrpcKt.MediaServiceCoroutineImplBase>() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface MediaServiceEntryPoint {
        fun registry(): WearDataLayerRegistry
        fun mediaRepository(): MediaPlayingRepository
    }

    private val entryPoint: MediaServiceEntryPoint by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            MediaServiceEntryPoint::class.java
        )
    }

    override val registry: WearDataLayerRegistry by lazy {
        entryPoint.registry()
    }

    override fun buildService(): MediaServiceGrpcKt.MediaServiceCoroutineImplBase {
        val repository = entryPoint.mediaRepository()

        return object : MediaServiceGrpcKt.MediaServiceCoroutineImplBase() {

            override suspend fun playPause(request: Empty): Media.setMediaResponse {
                repository.togglePlayPause()
                return Media.setMediaResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Media toggled")
                    .build()
            }

            override suspend fun next(request: Empty): Media.setMediaResponse {
                repository.next()
                return Media.setMediaResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Media next")
                    .build()
            }

            override suspend fun previous(request: Empty): Media.setMediaResponse {
                repository.previous()
                return Media.setMediaResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Media previous")
                    .build()
            }

        }
    }

}