package agdesigns.elevatefitness.presentation

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class WearRepository @Inject constructor(
    private val handler: WearDataHandler,
    @ApplicationContext private val context: Context
) {

    private var oldMessages: WearWorkout = WearWorkout()

    fun observeWearWorkout(): Flow<WearWorkout> =
        callbackFlow {
            // collect handler.messages
            // do not propagate null values instead recycle old ones
            // fixme: needs to be changed every time WearWorkout changes...
            handler.messages.collect {
                val newWearWorkout = WearWorkout(
                    exerciseName = it.exerciseName ?: oldMessages.exerciseName,
                    rest = it.rest ?: oldMessages.rest,
                    reps = it.reps ?: oldMessages.reps,
                    setsDone = it.setsDone ?: oldMessages.setsDone,
                    note = it.note ?: oldMessages.note,
                    weight = it.weight ?: oldMessages.weight,
                    restTimestamp = it.restTimestamp ?: oldMessages.restTimestamp,
                    exerciseIncrement = it.exerciseIncrement ?: oldMessages.exerciseIncrement,
                    nextExerciseName = it.nextExerciseName ?: oldMessages.nextExerciseName,
                )
                oldMessages = newWearWorkout
                trySend(newWearWorkout)
            }
        }

    fun observeWearImage(): Flow<Bitmap> =
        callbackFlow {
            // collect handler.messages
            // do not propagate null values instead recycle old ones
            handler.image.collect {
                trySend(it)
            }
        }

    fun completeSet(exerciseName: String, reps: Int, weight: Float) {
        // from a view model
        val message = JSONObject()
        message.put("exerciseName", exerciseName)
        message.put("reps", reps)
        message.put("weight", weight.toDouble())

        val nodes = Wearable.getNodeClient(context).connectedNodes
        nodes.addOnSuccessListener {
            for (node in it) {
                Wearable.getMessageClient(context)
                    .sendMessage(node.id, "/watch2phone", message.toString().toByteArray())
            }
        }
    }

    fun forceSync() {
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
        handler.cleanup()
    }

    fun reopen() {
        handler.reopen()
    }

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: WearRepository? = null

        fun getInstance(handler: WearDataHandler, context: Context) =
            instance ?: synchronized(this) {
                instance ?: WearRepository(handler, context).also { instance = it }
            }
    }
}
