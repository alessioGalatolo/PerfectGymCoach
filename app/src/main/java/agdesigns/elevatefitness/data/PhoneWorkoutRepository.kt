package agdesigns.elevatefitness.data

import agdesigns.elevatefitness.shared.grpc.Workout
import agdesigns.elevatefitness.shared.grpc.WorkoutWearServiceGrpcKt
import agdesigns.elevatefitness.shared.urgentProtoDataStore
import android.util.Log
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.ProtoDataStoreHelper.protoDataStore
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.protobuf.Empty
import io.grpc.StatusException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Singleton

@OptIn(ExperimentalHorologistApi::class)
@Singleton
class PhoneWorkoutRepository(
    registry: WearDataLayerRegistry,
    private val phoneToWatchService: WorkoutWearServiceGrpcKt.WorkoutWearServiceCoroutineStub
) {
    private val secondaryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val workoutDatastore = registry.urgentProtoDataStore<Workout.WorkoutStaticData>(
        coroutineScope = secondaryScope
    )

    // Channel - only ONE subscriber, all events delivered
    private val _setCompletions = Channel<Workout.SetCompleted>(
        capacity = Channel.UNLIMITED // Or use a specific buffer size
    )
    val setCompletions: ReceiveChannel<Workout.SetCompleted> = _setCompletions

    var ongoingWorkout: Boolean = false

    // Called by the gRPC service
    suspend fun handleSetCompleted(setCompleted: Workout.SetCompleted) {
        // Send to channel - will buffer if no one is collecting
        _setCompletions.send(setCompleted)

        // Optional: persist immediately as backup
    }

    fun startOngoingWorkout() {
        ongoingWorkout = true
    }

    fun stopOngoingWorkout() {
        ongoingWorkout = false
        secondaryScope.launch {
            try {
                phoneToWatchService.stopWorkout(Empty.getDefaultInstance())
            } catch (e: StatusException) {
                Log.e("PhoneWorkoutRepository", "Error stopping ongoing workout", e)
            }
            workoutDatastore.urgentUpdateData {
                Workout.WorkoutStaticData.newBuilder()
                    .setActiveWorkout(false)
                    .build()
            }
        }
    }
}