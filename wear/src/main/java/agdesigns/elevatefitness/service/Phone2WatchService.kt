package agdesigns.elevatefitness.service

import agdesignes.elevatefitness.shared.grpc.Workout
import agdesignes.elevatefitness.shared.grpc.WorkoutWearServiceGrpcKt
import agdesigns.elevatefitness.data.WearRepository
import android.content.Intent
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.datalayer.grpc.server.BaseGrpcDataService
import com.google.protobuf.Empty
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@OptIn(ExperimentalHorologistApi::class)
@AndroidEntryPoint
class Phone2WatchService: BaseGrpcDataService<WorkoutWearServiceGrpcKt.WorkoutWearServiceCoroutineImplBase>() {

    @Inject
    override lateinit var registry: WearDataLayerRegistry

    @Inject
    lateinit var wearRepository: WearRepository

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY // or START_NOT_STICKY depending on your needs
    }

    override fun buildService(): WorkoutWearServiceGrpcKt.WorkoutWearServiceCoroutineImplBase {
        return object : WorkoutWearServiceGrpcKt.WorkoutWearServiceCoroutineImplBase() {
            override suspend fun scrollToExercise(request: Workout.ExerciseToScrollTo): Empty {
                wearRepository.handleScrollToExercise(request.exerciseIndex)
                return Empty.newBuilder().build()
            }

            override suspend fun setRest(request: Workout.RestPhone2Watch): Empty {
                wearRepository.handleSetRest(request)
                return Empty.newBuilder().build()
            }
        }
    }
}