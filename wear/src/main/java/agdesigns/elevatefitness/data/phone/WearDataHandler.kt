package agdesigns.elevatefitness.data.phone

import agdesigns.elevatefitness.data.phone.WearWorkout
import agdesigns.elevatefitness.presentation.screens.common.MediaPlayingState
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.net.toUri
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearDataHandler @Inject constructor(
    @ApplicationContext private val context: Context
) : DataClient.OnDataChangedListener {
    // observed from home to check workout status
    private val _workoutActive = MutableSharedFlow<Boolean>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val workoutActive: SharedFlow<Boolean> = _workoutActive.asSharedFlow()

    // messages receives and emits workout data
    private val _workoutData = MutableSharedFlow<WearWorkout>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val workoutData: SharedFlow<WearWorkout> = _workoutData.asSharedFlow()

    // image receives and emits image of workout exercise
    private val _image = MutableSharedFlow<Bitmap>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val image: SharedFlow<Bitmap> = _image.asSharedFlow()

    private val _mediaState = MutableStateFlow(MediaPlayingState())
    val mediaState: StateFlow<MediaPlayingState> = _mediaState.asStateFlow()

    private val _workoutInterrupted: MutableSharedFlow<Boolean> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val workoutInterrupted: SharedFlow<Boolean> = _workoutInterrupted.asSharedFlow()

    init {
        Wearable.getDataClient(context)
            .addListener(this)
        // TODO get all queued items on start, remove timestamp
        Wearable.getDataClient(context).getDataItems("/image2watch".toUri()).addOnSuccessListener {
            var queuedAsset: Asset? = null

            it.forEach { item ->
                val dataMap = DataMapItem.fromDataItem(item).dataMap
                if (dataMap.containsKey("image")) {
                    val asset = dataMap.getAsset("image")
                    if (asset != null) {
                        queuedAsset = asset
                    }
                }
            }
            if (queuedAsset != null) {
                Log.d("WearDataHandler", "Found some queued image on init")
                decodeImageAndEmit(queuedAsset)
                _workoutActive.tryEmit(true)
            }
        }
        Wearable.getDataClient(context).getDataItems("/phone2watch".toUri()).addOnSuccessListener {
            var queuedWorkout = WearWorkout()
            it.forEach { item ->
                val dataMap = DataMapItem.fromDataItem(item).dataMap
                queuedWorkout = updateWorkout(dataMap, queuedWorkout)
            }
            _workoutActive.tryEmit(true)
            _workoutData.tryEmit(queuedWorkout)
        }
        Wearable.getDataClient(context).getDataItems("/now_playing".toUri()).addOnSuccessListener {
            var nowPlayingState = MediaPlayingState()
            var queuedAsset: Asset? = null

            Log.d("WearDataHandler", "Found some queued now playing on init")
            it.forEach { item ->
                Log.d("WearDataHandler", "Found some queued now playing on init")
                val dataMap = DataMapItem.fromDataItem(item).dataMap
                if (dataMap.containsKey("title"))
                    nowPlayingState = nowPlayingState.copy(title = dataMap.getString("title"))
                if (dataMap.containsKey("artist"))
                    nowPlayingState = nowPlayingState.copy(artist = dataMap.getString("artist"))
                if (dataMap.containsKey("isPlaying"))
                    nowPlayingState = nowPlayingState.copy(isPlaying = dataMap.getBoolean("isPlaying"))
                if (dataMap.containsKey("artwork")) {
                    val asset = dataMap.getAsset("artwork")
                    if (asset != null) {
                        queuedAsset = asset
                    }
                }
            }
            _mediaState.update {
                nowPlayingState
            }
            if (queuedAsset != null) {
                Log.d("WearDataHandler", "Found some queued artwork on init")
                decodeArtworkAndEmit(queuedAsset)
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            Log.d("WearDataHandler", "onDataChanged: ${event.type}")
            if (event.type == DataEvent.TYPE_CHANGED) {
                val item = event.dataItem
                if (item.uri.path == "/phone2watch") {
                    val dataMap = DataMapItem.fromDataItem(item).dataMap
                    var wearWorkout = WearWorkout()
                    wearWorkout = updateWorkout(dataMap, wearWorkout)

                    _workoutData.tryEmit(wearWorkout)
                    _workoutActive.tryEmit(true)
                } else if (item.uri.path == "/image2watch") {
                    Log.d("WearDataHandler", "onDataChanged: received image on new uri ")
                    DataMapItem.fromDataItem(item).dataMap.getAsset("image")?.let { asset ->
                        decodeImageAndEmit(asset)
                        _workoutActive.tryEmit(true)
                    }
                } else if (item.uri.path == "/stop_workout") {
                    _workoutInterrupted.tryEmit(true)
                    _workoutActive.tryEmit(false)
                } else if (item.uri.path == "/now_playing") {
                    val dataMap = DataMapItem.fromDataItem(item).dataMap
                    _mediaState.update {
                        var nowPlayingState = it
                        if (dataMap.containsKey("title"))
                            nowPlayingState = nowPlayingState.copy(title = dataMap.getString("title"))
                        if (dataMap.containsKey("artist"))
                            nowPlayingState = nowPlayingState.copy(artist = dataMap.getString("artist"))
                        if (dataMap.containsKey("isPlaying"))
                            nowPlayingState = nowPlayingState.copy(isPlaying = dataMap.getBoolean("isPlaying"))
                        nowPlayingState
                    }
                    if (dataMap.containsKey("artwork")) {
                        val asset = dataMap.getAsset("artwork")
                        if (asset != null) {
                            decodeArtworkAndEmit(asset)
                        }
                    }
                }
            }
        }
    }

        fun decodeImageAndEmit(asset: Asset) {
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

        fun decodeArtworkAndEmit(asset: Asset) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val assetFd = Tasks.await(Wearable.getDataClient(context).getFdForAsset(asset))
                    val inputStream = assetFd?.inputStream
                    val imageBitmap = inputStream?.use { BitmapFactory.decodeStream(it) }

                    if (imageBitmap != null) {
                        Log.d("WearDataHandler", "emitting artwork")
                        _mediaState.update {
                            it.copy(
                                artwork = imageBitmap
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WearDataHandler", "Failed to load asset", e)
                }
            }
        }

    // updates workout with non-null values in dataMap
    fun updateWorkout(dataMap: DataMap, workout: WearWorkout): WearWorkout {
        var updatedWorkout = workout
        // we are checking all the keys individually because not all of them give
        // null when not present e.g., getInt returns 0 if not present
        if (dataMap.containsKey("setsDone"))
            updatedWorkout = updatedWorkout.copy(setsDone = dataMap.getInt("setsDone"))
        if (dataMap.containsKey("exerciseName"))
            updatedWorkout = updatedWorkout.copy(exerciseName = dataMap.getString("exerciseName"))
        if (dataMap.containsKey("rest"))
            updatedWorkout = updatedWorkout.copy(rest = dataMap.getIntegerArrayList("rest"))
        if (dataMap.containsKey("reps"))
            updatedWorkout = updatedWorkout.copy(reps = dataMap.getIntegerArrayList("reps"))
        if (dataMap.containsKey("weight"))
            updatedWorkout = updatedWorkout.copy(weight = dataMap.getFloat("weight"))
        if (dataMap.containsKey("note"))
            updatedWorkout = updatedWorkout.copy(note = dataMap.getString("note"))
        if (dataMap.containsKey("restTimestamp"))
            updatedWorkout = updatedWorkout.copy(restTimestamp = dataMap.getLong("restTimestamp"))
        if (dataMap.containsKey("currentRestSeconds"))
            updatedWorkout = updatedWorkout.copy(currentRestSeconds = dataMap.getLong("currentRestSeconds"))
        if (dataMap.containsKey("exerciseIncrement"))
            updatedWorkout = updatedWorkout.copy(exerciseIncrement = dataMap.getFloat("exerciseIncrement"))
        if (dataMap.containsKey("nextExerciseName"))
            updatedWorkout = updatedWorkout.copy(nextExerciseName = dataMap.getString("nextExerciseName"))
        if (dataMap.containsKey("equipmentResKey"))
            updatedWorkout = updatedWorkout.copy(equipmentResKey = dataMap.getString("equipmentResKey"))
        if (dataMap.containsKey("imperialSystem"))
            updatedWorkout = updatedWorkout.copy(imperialSystem = dataMap.getBoolean("imperialSystem"))
        if (dataMap.containsKey("tareBarbell"))
            updatedWorkout = updatedWorkout.copy(tareBarbell = dataMap.getFloat("tareBarbell"))

        return updatedWorkout
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
