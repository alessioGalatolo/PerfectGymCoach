package agdesigns.elevatefitness.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class WearDataHandler @Inject constructor(
    @ApplicationContext private val context: Context
) : DataClient.OnDataChangedListener {

    private val _messages = MutableSharedFlow<WearWorkout>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messages: SharedFlow<WearWorkout> = _messages.asSharedFlow()

    private val _image = MutableSharedFlow<Bitmap>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val image: SharedFlow<Bitmap> = _image.asSharedFlow()

    init {
        Wearable.getDataClient(context)
            .addListener(this)
        // TODO get all queued items on start, remove timestamp
//        Wearable.getDataClient(context).getDataItems("/image2watch".toUri()).addOnSuccessListener {
//            it.forEach { item ->
//                ...
//        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            Log.d("WearDataHandler", "onDataChanged: ${event.type}")
            if (event.type == DataEvent.TYPE_CHANGED) {
                val item = event.dataItem
                if (item.uri.path == "/phone2watch") {
                    val dataMap = DataMapItem.fromDataItem(item).dataMap
                    var wearWorkout = WearWorkout()
                    if (dataMap.containsKey("setsDone"))
                        wearWorkout = wearWorkout.copy(setsDone = dataMap.getInt("setsDone"))
                    if (dataMap.containsKey("exerciseName"))
                        wearWorkout = wearWorkout.copy(exerciseName = dataMap.getString("exerciseName"))
                    if (dataMap.containsKey("rest"))
                        wearWorkout = wearWorkout.copy(rest = dataMap.getIntegerArrayList("rest"))
                    if (dataMap.containsKey("reps"))
                        wearWorkout = wearWorkout.copy(reps = dataMap.getIntegerArrayList("reps"))
                    if (dataMap.containsKey("weight"))
                        wearWorkout = wearWorkout.copy(weight = dataMap.getFloat("weight"))
                    if (dataMap.containsKey("note"))
                        wearWorkout = wearWorkout.copy(note = dataMap.getString("note"))
                    if (dataMap.containsKey("restTimestamp"))
                        wearWorkout = wearWorkout.copy(restTimestamp = dataMap.getLong("restTimestamp"))
                    if (dataMap.containsKey("exerciseIncrement"))
                        wearWorkout = wearWorkout.copy(exerciseIncrement = dataMap.getFloat("exerciseIncrement"))
                    if (dataMap.containsKey("nextExerciseName"))
                        wearWorkout = wearWorkout.copy(nextExerciseName = dataMap.getString("nextExerciseName"))
                    // image should be last check
                    if (dataMap.containsKey("image")) {
                        Log.d("WearDataHandler", "onDataChanged: received image")
                        val imageAsset = dataMap.getAsset("image")

                        imageAsset?.let { asset ->
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val assetFd = Tasks.await(Wearable.getDataClient(context).getFdForAsset(asset))
                                    val inputStream = assetFd?.inputStream
                                    val imageBitmap = inputStream?.use { BitmapFactory.decodeStream(it) }

                                    if (imageBitmap != null) {
                                        _image.tryEmit(imageBitmap)
                                    }
                                } catch (e: Exception) {
                                    Log.e("WearDataHandler", "Failed to load asset", e)
                                }
                            }
                            return@forEach // emit will happen inside coroutine
                        }
                    } else {
                        Log.d("WearDataHandler", "emitting $wearWorkout")
                        _messages.tryEmit(wearWorkout)
                    }
                } else if (item.uri.path == "/image2watch") {
                    Log.d("WearDataHandler", "onDataChanged: received image on new uri ")
                    DataMapItem.fromDataItem(item).dataMap.getAsset("image")?.let { asset ->
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val assetFd = Tasks.await(Wearable.getDataClient(context).getFdForAsset(asset))
                                val inputStream = assetFd?.inputStream
                                val imageBitmap = inputStream?.use { BitmapFactory.decodeStream(it) }

                                if (imageBitmap != null) {
                                    Log.d("WearDataHandler", "emitting image")
                                    _image.tryEmit(imageBitmap)
                                }
                            } catch (e: Exception) {
                                Log.e("WearDataHandler", "Failed to load asset", e)
                            }
                        }
                    }
                }
            }
        }
    }

    fun cleanup() {
        Wearable.getDataClient(context)
            .removeListener(this)
    }

    fun reopen() {
        Wearable.getDataClient(context)
            .addListener(this)
    }
}
