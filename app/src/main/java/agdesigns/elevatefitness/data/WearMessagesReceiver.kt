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
class WearMessagesReceiver @Inject constructor(
    @ApplicationContext private val context: Context
) : MessageClient.OnMessageReceivedListener {

    private val _messages = MutableSharedFlow<JSONObject>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messages: SharedFlow<JSONObject> = _messages.asSharedFlow()

    private val _syncRequest = MutableSharedFlow<Boolean>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val syncRequest: SharedFlow<Boolean> = _syncRequest.asSharedFlow()

    init {
        Wearable.getMessageClient(context).addListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/watch2phone") {
            val msg = String(messageEvent.data, Charsets.UTF_8)
            val json = JSONObject(msg)
            _messages.tryEmit(json)
        } else if (messageEvent.path == "/request_sync") {
            _syncRequest.tryEmit(true)
        }
    }

    // FIXME: should be called
    fun cleanup() {
        Wearable.getMessageClient(context).removeListener(this)
    }
}