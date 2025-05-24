package agdesigns.elevatefitness.presentation

import android.content.Context
import android.util.Log
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
class WearMessageHandler @Inject constructor(
    @ApplicationContext private val context: Context
) : MessageClient.OnMessageReceivedListener {

    private val _messages = MutableSharedFlow<JSONObject>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messages: SharedFlow<JSONObject> = _messages.asSharedFlow()

    init {
        Wearable.getMessageClient(context).addListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/phone2watch") {
            val msg = String(messageEvent.data, Charsets.UTF_8)
            val json = JSONObject(msg)
            _messages.tryEmit(json)
        }
    }

    fun cleanup() {
        Wearable.getMessageClient(context).removeListener(this)
    }

    fun reopen() {
        Wearable.getMessageClient(context).addListener(this)
    }
}
