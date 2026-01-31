package agdesigns.elevatefitness.data

import agdesigns.elevatefitness.shared.UrgentWearLocalDataStore
import agdesigns.elevatefitness.shared.WORKOUT_IMAGES_PATH
import agdesigns.elevatefitness.shared.WearBitmapArrayStore
import agdesigns.elevatefitness.shared.bitmapArrayStore
import agdesigns.elevatefitness.shared.grpc.Workout
import agdesigns.elevatefitness.shared.grpc.WorkoutWearServiceGrpcKt
import agdesigns.elevatefitness.shared.urgentProtoDataStore
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.datalayer.phone.PhoneDataLayerAppHelper
import com.google.protobuf.Empty
import dagger.hilt.android.qualifiers.ApplicationContext
import io.grpc.StatusException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import javax.inject.Singleton

@OptIn(ExperimentalHorologistApi::class)
@Singleton
class PhoneWorkoutRepository(
    registry: WearDataLayerRegistry,
    private val phoneToWatchService: WorkoutWearServiceGrpcKt.WorkoutWearServiceCoroutineStub,
    private val datalayerHelper: PhoneDataLayerAppHelper,
    @ApplicationContext private val context: Context
) {
    private val secondaryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _workoutDatastoreDeferred = CompletableDeferred<UrgentWearLocalDataStore<Workout.WorkoutStaticData>>()

    // split static data (e.g., exercises) with data that is frequently changing to avoid too many messages
    val wearWorkoutStaticDeferred = CompletableDeferred<UrgentWearLocalDataStore<Workout.WorkoutStaticData>>()
    val wearWorkoutDynamicDeferred = CompletableDeferred<UrgentWearLocalDataStore<Workout.WorkoutDynamicData>>()
    val wearWorkoutImagesDeferred = CompletableDeferred<WearBitmapArrayStore>()


    // Channel - only ONE subscriber, all events delivered
    private val _setCompletions = Channel<Workout.SetCompleted>(
        capacity = Channel.UNLIMITED // Or use a specific buffer size
    )
    val setCompletions: ReceiveChannel<Workout.SetCompleted> = _setCompletions

    var ongoingWorkout: Boolean = false

    suspend fun apiIsAvailable() = datalayerHelper.isAvailable()

    // Called by the gRPC service
    suspend fun handleSetCompleted(setCompleted: Workout.SetCompleted) {
        // Send to channel - will buffer if no one is collecting
        _setCompletions.send(setCompleted)

        // Optional: persist immediately as backup
    }

    fun openWearWorkout() {
        // maybe open wear os app
//        if (!datalayerHelper.isAvailable())
//            return
        val openWearIntent = Intent(Intent.ACTION_VIEW).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            data = "elevatefitnesswear://startworkout".toUri()
        }
        val remoteActivityHelper = RemoteActivityHelper(context)
        remoteActivityHelper.startRemoteActivity(openWearIntent)
    }

    fun startOngoingWorkout() {
        ongoingWorkout = true
    }

    fun stopOngoingWorkout() {
        ongoingWorkout = false
        secondaryScope.launch {
            if (!datalayerHelper.isAvailable())
                return@launch
            try {
                phoneToWatchService.stopWorkout(Empty.getDefaultInstance())
            } catch (e: StatusException) {
                Log.e("PhoneWorkoutRepository", "Error stopping ongoing workout", e)
            }
            _workoutDatastoreDeferred.await().urgentUpdateData {
                Workout.WorkoutStaticData.newBuilder()
                    .setActiveWorkout(false)
                    .build()
            }
        }
    }

    init {
        secondaryScope.launch {
            if (datalayerHelper.isAvailable()) {
                _workoutDatastoreDeferred.complete(
                    registry.urgentProtoDataStore<Workout.WorkoutStaticData>(
                        coroutineScope = secondaryScope
                    )
                )
                wearWorkoutStaticDeferred.complete(
                    registry.urgentProtoDataStore<Workout.WorkoutStaticData>(
                        coroutineScope = secondaryScope
                    )
                )
                wearWorkoutDynamicDeferred.complete(
                    registry.urgentProtoDataStore<Workout.WorkoutDynamicData>(
                        coroutineScope = secondaryScope
                    )
                )
                wearWorkoutImagesDeferred.complete(
                    registry.bitmapArrayStore(
                        coroutineScope = secondaryScope,
                        path = WORKOUT_IMAGES_PATH
                    )
                )
            }
        }
    }
}