package agdesigns.elevatefitness.data

import android.content.Context
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class WatchMessageReceiver @Inject constructor(
    @ApplicationContext private val context: Context
) : MessageClient.OnMessageReceivedListener {

    // set completion requests
    private val _setCompletionInfo = MutableSharedFlow<JSONObject>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val setCompletionInfo: SharedFlow<JSONObject> = _setCompletionInfo.asSharedFlow()

    // watch has requested to sync
    private val _syncRequest = MutableSharedFlow<Boolean>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val syncRequest: SharedFlow<Boolean> = _syncRequest.asSharedFlow()

    // keep track of watch to avoid sending useless data
    private val _watchHeartbeat = MutableSharedFlow<Long>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val watchHeartbeat: SharedFlow<Long> = _watchHeartbeat.asSharedFlow()


    init {
        Wearable.getMessageClient(context).addListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/watch2phone") {
            val msg = String(messageEvent.data, Charsets.UTF_8)
            val json = JSONObject(msg)
            _setCompletionInfo.tryEmit(json)
        } else if (messageEvent.path == "/request_sync") {
            _syncRequest.tryEmit(true)
        } else if (messageEvent.path == "/heartbeat") {
            _watchHeartbeat.tryEmit(System.currentTimeMillis())
            // watch replies to phone heartbeats but phone should avoid that
            // to avoid overloading the connection
        }
    }

    // FIXME: should be called
    fun cleanup() {
        Wearable.getMessageClient(context).removeListener(this)
    }
}