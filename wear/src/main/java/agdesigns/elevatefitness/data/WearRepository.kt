package agdesigns.elevatefitness.data

import agdesigns.elevatefitness.data.datastore.PermissionStateDataStore
import agdesigns.elevatefitness.data.datastore.WorkoutDataStore
import agdesigns.elevatefitness.data.phone.WearDataHandler
import agdesigns.elevatefitness.data.phone.WearMessageHandler
import agdesigns.elevatefitness.data.phone.WearWorkout
import agdesigns.elevatefitness.service.WorkoutService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.IBinder
import android.util.Log
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearRepository @Inject constructor(
    private val dataHandler: WearDataHandler,
    private val messageHandler: WearMessageHandler,
    private val workoutDataStore: WorkoutDataStore,
    val permissionStateDataStore: PermissionStateDataStore,
    @ApplicationContext private val context: Context
) {
    private var lastHeartbeat = System.currentTimeMillis()
    private val _isPhoneAlive = MutableStateFlow(true)
    // The remaining variables are related to the binding/monitoring/interacting with the
    // service that gathers all the data to calculate walking points.
    private var foregroundOnlyServiceBound = false
    private val _service = MutableStateFlow<WorkoutService?>(null)
    val service: StateFlow<WorkoutService?> = _service

    var foregroundOnlyWalkingWorkoutService: WorkoutService? = null
        private set

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as WorkoutService.LocalBinder
            foregroundOnlyWalkingWorkoutService = binder.workoutService
            foregroundOnlyServiceBound = true
            _service.value = foregroundOnlyWalkingWorkoutService
        }
        override fun onServiceDisconnected(name: ComponentName) {
            foregroundOnlyWalkingWorkoutService = null
            foregroundOnlyServiceBound = false
            _service.value = null
        }
    }

    fun bindForegroundOnlyService() {
        val intent = Intent(context, WorkoutService::class.java)
        // If it's a foreground service that must actually run, start it as well:
        // ContextCompat.startForegroundService(context, intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun stopForegroundOnlyService() {
        if (foregroundOnlyServiceBound) {
            context.unbindService(connection)
            foregroundOnlyServiceBound = false
            _service.value = null
        }
    }

    val activeWorkoutFlow: Flow<Boolean> = workoutDataStore.activeWorkoutFlow

    init {
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                val alive = System.currentTimeMillis() - lastHeartbeat < 2000
                _isPhoneAlive.tryEmit(alive)
                delay(1000)
            }
        }
        CoroutineScope(Dispatchers.Default).launch {
            messageHandler.phoneHeartbeat.collect {
                lastHeartbeat = System.currentTimeMillis()
            }
        }
    }

    fun observeWorkoutActive(): Flow<Boolean> = dataHandler.workoutActive

    fun observeWearWorkout(): Flow<WearWorkout> = dataHandler.workoutData

    fun observeWearImage(): Flow<Bitmap> = dataHandler.image

    fun observeWorkoutInterrupted(): Flow<Boolean> = dataHandler.workoutInterrupted

    fun isPhoneAlive(): Flow<Boolean> = _isPhoneAlive.asStateFlow()

    fun completeSet(
        exerciseName: String,
        reps: Int,
        weight: Float,
        tare: Float,
        restTimestamp: ZonedDateTime?
    ) {
        // from a view model
        val message = JSONObject()
        message.put("exerciseName", exerciseName)
        message.put("reps", reps)
        message.put("weight", weight.toDouble())
        message.put("tare", tare.toDouble())
        message.put("restTimestamp", restTimestamp?.toInstant()?.toEpochMilli() ?: 0L)

        val nodes = Wearable.getNodeClient(context).connectedNodes
        nodes.addOnSuccessListener {
            for (node in it) {
                Wearable.getMessageClient(context)
                    .sendMessage(node.id, "/watch2phone", message.toString().toByteArray())
            }
        }
    }

    fun forceSync() {
        // TODO: move to message handler
        val nodes = Wearable.getNodeClient(context).connectedNodes
        nodes.addOnSuccessListener {
            for (node in it) {
                Wearable.getMessageClient(context)
                    .sendMessage(node.id, "/request_sync", System.currentTimeMillis().toString().toByteArray())
                    .addOnSuccessListener {
                        Log.d("WearRepository", "Sync request sent to ${node.displayName}")
                    }
            }
        }
    }


    fun close() {
        dataHandler.cleanup()
        messageHandler.cleanup()
    }

    fun reopen() {
        dataHandler.reopen()
        messageHandler.reopen()
    }

    /*
     * Ongoing activity stuff
     */
    suspend fun setActiveWorkout(activeWorkout: Boolean) = workoutDataStore.setActiveWorkout(activeWorkout)


    companion object {

        // For Singleton instantiation
        @Volatile private var instance: WearRepository? = null

        fun getInstance(dataHandler: WearDataHandler, messageHandler: WearMessageHandler, workoutDataStore: WorkoutDataStore, permissionStateDataStore: PermissionStateDataStore, context: Context) =
            instance ?: synchronized(this) {
                instance ?: WearRepository(dataHandler, messageHandler, workoutDataStore, permissionStateDataStore, context).also { instance = it }
            }
    }
}