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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Singleton

@OptIn(ExperimentalHorologistApi::class)
@Singleton
class PhoneWorkoutRepository(
    private val registry: WearDataLayerRegistry,
    private val phoneToWatchService: WorkoutWearServiceGrpcKt.WorkoutWearServiceCoroutineStub,
    private val datalayerHelper: PhoneDataLayerAppHelper,
    @ApplicationContext private val context: Context
) {
    private val secondaryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // split static data (e.g., exercises) with data that is frequently changing to avoid too many messages
    class WearWorkoutStores(
        val static: UrgentWearLocalDataStore<Workout.WorkoutStaticData>,
        val dynamic: UrgentWearLocalDataStore<Workout.WorkoutDynamicData>,
        val images: WearBitmapArrayStore
    )

    private val wearStoresMutex = Mutex()
    private var wearStores: WearWorkoutStores? = null

    /**
     * The stores synced with the watch, null if the Wear API is not available (yet).
     * Created on first use, so a watch paired after the app started still gets synced.
     */
    suspend fun wearWorkoutStores(): WearWorkoutStores? = wearStoresMutex.withLock {
        wearStores ?: if (datalayerHelper.isAvailable()) {
            WearWorkoutStores(
                static = registry.urgentProtoDataStore<Workout.WorkoutStaticData>(coroutineScope = secondaryScope),
                dynamic = registry.urgentProtoDataStore<Workout.WorkoutDynamicData>(coroutineScope = secondaryScope),
                images = registry.bitmapArrayStore(
                    coroutineScope = secondaryScope,
                    path = WORKOUT_IMAGES_PATH
                )
            ).also { wearStores = it }
        } else null
    }


    /**
     * A set completed on the watch, waiting to be processed by the phone.
     * Either the consumer or a timeout claims it first: a timed out set is reported as failed to
     * the watch (which will retry it) and must then be skipped by the consumer.
     */
    class PendingSetCompletion(val request: Workout.SetCompleted) {
        private val claimed = AtomicBoolean(false)
        internal val processed = CompletableDeferred<Unit>()

        /** Returns false if the set timed out and must not be processed. */
        fun tryClaim(): Boolean = claimed.compareAndSet(false, true)

        fun markProcessed() {
            processed.complete(Unit)
        }

        internal fun tryCancel(): Boolean = claimed.compareAndSet(false, true)
    }

    // Channel - only ONE subscriber, all events delivered
    private val _setCompletions = Channel<PendingSetCompletion>(
        capacity = Channel.UNLIMITED
    )
    val setCompletions: ReceiveChannel<PendingSetCompletion> = _setCompletions

    private val _workoutCompletions = Channel<Workout.CompleteWorkout>(
        capacity = Channel.UNLIMITED
    )
    val workoutCompletions: ReceiveChannel<Workout.CompleteWorkout> = _workoutCompletions

    private val _acceptedModifications = Channel<Int>(
        capacity = Channel.UNLIMITED
    )
    val acceptedModifications: ReceiveChannel<Int> = _acceptedModifications

    private val _addSetRequests = Channel<Workout.AddSetRequest>(
        capacity = Channel.UNLIMITED
    )
    val addSetRequests: ReceiveChannel<Workout.AddSetRequest> = _addSetRequests

    private val _extendRestRequests = Channel<Long>(
        capacity = Channel.UNLIMITED
    )
    val extendRestRequests: ReceiveChannel<Long> = _extendRestRequests


    var ongoingWorkout: Boolean = false

    suspend fun apiIsAvailable() = datalayerHelper.isAvailable()

    // Called by the gRPC service
    suspend fun handleSetCompleted(setCompleted: Workout.SetCompleted): Boolean {
        val pending = PendingSetCompletion(setCompleted)
        // Send to channel - will buffer if no one is collecting
        _setCompletions.send(pending)
        // only return once it has been processed
        if (withTimeoutOrNull(SET_COMPLETION_TIMEOUT_MILLIS) { pending.processed.await() } != null)
            return true
        // not processed in time: cancel it so that the watch's retry is not recorded twice
        if (pending.tryCancel())
            return false
        // consumer is processing it right now
        pending.processed.await()
        return true
    }

    fun handleCompleteWorkout(completion: Workout.CompleteWorkout) {
        _workoutCompletions.trySend(completion)
    }

    fun handleAcceptModification(modificationIndex: Int) {
        _acceptedModifications.trySend(modificationIndex)
    }

    fun handleAddSet(request: Workout.AddSetRequest) {
        _addSetRequests.trySend(request)
    }

    fun handleExtendRest(additionalSeconds: Long) {
        _extendRestRequests.trySend(additionalSeconds)
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
            wearWorkoutStores()?.static?.urgentUpdateData {
                Workout.WorkoutStaticData.newBuilder()
                    .setActiveWorkout(false)
                    .build()
            }
        }
    }

    companion object {
        private const val SET_COMPLETION_TIMEOUT_MILLIS = 2_200L
    }
}