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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
        capacity = Channel.UNLIMITED
    )
    val setCompletions: ReceiveChannel<Workout.SetCompleted> = _setCompletions

    private val _workoutCompletions = Channel<Float>(
        capacity = Channel.UNLIMITED
    )
    val workoutCompletions: ReceiveChannel<Float> = _workoutCompletions


    var ongoingWorkout: Boolean = false

    // Called by the gRPC service
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun handleSetCompleted(setCompleted: Workout.SetCompleted): Boolean {
        // Send to channel - will buffer if no one is collecting
        _setCompletions.send(setCompleted)
        // only return once it has been consumed
        for (i in 0..10) {
            delay(200)
            if (setCompletions.isEmpty) {
                return true
            }
        }
        return false
    }

    fun handleCompleteWorkout(intensity: Float) {
        _workoutCompletions.trySend(intensity)
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