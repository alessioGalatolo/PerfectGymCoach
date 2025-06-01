package agdesigns.elevatefitness.presentation

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class WearRepository @Inject constructor(
    private val dataHandler: WearDataHandler,
    private val messageHandler: WearMessageHandler,
    @ApplicationContext private val context: Context
) {
    private var lastHeartbeat = System.currentTimeMillis()
    private val _isPhoneAlive = MutableStateFlow(true)

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

    fun observeWearWorkout(): Flow<WearWorkout> = dataHandler.workoutData

    fun observeWearImage(): Flow<Bitmap> = dataHandler.image

    fun observeWorkoutInterrupted(): Flow<Boolean> = dataHandler.workoutInterrupted

    fun isPhoneAlive(): Flow<Boolean> = _isPhoneAlive.asStateFlow()

    fun completeSet(exerciseName: String, reps: Int, weight: Float, tare: Float) {
        // from a view model
        val message = JSONObject()
        message.put("exerciseName", exerciseName)
        message.put("reps", reps)
        message.put("weight", weight.toDouble())
        message.put("tare", tare.toDouble())

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

    init {
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                val alive = System.currentTimeMillis() - lastHeartbeat < 2000
                _isPhoneAlive.tryEmit(alive)
                delay(1000)
            }
        }
    }

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: WearRepository? = null

        fun getInstance(dataHandler: WearDataHandler, messageHandler: WearMessageHandler, context: Context) =
            instance ?: synchronized(this) {
                instance ?: WearRepository(dataHandler, messageHandler, context).also { instance = it }
            }
    }
}
