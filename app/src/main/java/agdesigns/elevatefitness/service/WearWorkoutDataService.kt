package agdesigns.elevatefitness.service

import agdesigns.elevatefitness.shared.grpc.Workout
import agdesigns.elevatefitness.shared.grpc.WorkoutServiceGrpcKt
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
class WearWorkoutDataService: BaseGrpcDataService<WorkoutServiceGrpcKt.WorkoutServiceCoroutineImplBase>() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkoutServiceEntryPoint {
        fun registry(): WearDataLayerRegistry
        fun workoutRepository(): PhoneWorkoutRepository
    }

    private val entryPoint: WorkoutServiceEntryPoint by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            WorkoutServiceEntryPoint::class.java
        )
    }

    override val registry: WearDataLayerRegistry by lazy {
        entryPoint.registry()
    }

    override fun buildService(): WorkoutServiceGrpcKt.WorkoutServiceCoroutineImplBase {
        val repository = entryPoint.workoutRepository()

        return object : WorkoutServiceGrpcKt.WorkoutServiceCoroutineImplBase() {

            override suspend fun setCompleted(request: Workout.SetCompleted): Workout.SetCompletedResponse {
                Log.d("WearWorkoutDataService", "Received set completed request: $request")
                repository.handleSetCompleted(request)
                return Workout.SetCompletedResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Set recorded")
                    .build()
            }

            override suspend fun workoutActive(request: Empty): Workout.WorkoutActiveResponse {
                return Workout.WorkoutActiveResponse.newBuilder()
                    .setActive(repository.ongoingWorkout)
                    .build()
            }

            override suspend fun completeWorkout(request: Workout.CompleteWorkout): Empty {
                repository.handleCompleteWorkout(request.intensity)
                return Empty.newBuilder().build()
            }
        }
    }

}