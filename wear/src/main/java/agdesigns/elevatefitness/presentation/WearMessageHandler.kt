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

    private val _phoneHeartbeat = MutableSharedFlow<Long>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val phoneHeartbeat: SharedFlow<Long> = _phoneHeartbeat.asSharedFlow()

    init {
        Wearable.getMessageClient(context).addListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/heartbeat") {
            val msg = String(messageEvent.data, Charsets.UTF_8)
            _phoneHeartbeat.tryEmit(System.currentTimeMillis())
            if (msg == "ping" || msg == "pong") {
                val reply = if (msg == "ping") "pong" else "ping"
                sendMessage("/heartbeat", reply)
            }
        }
    }

    fun sendMessage(path: String, message: String) {
        val nodes = Wearable.getNodeClient(context).connectedNodes
        nodes.addOnSuccessListener {
            for (node in it) {
                Wearable.getMessageClient(context)
                    .sendMessage(node.id, path, message.toByteArray())
                    .addOnFailureListener {
                        Log.d("WearMessageHandler", "Failed to send message '$message' to ${node.displayName}")
                    }
            }
        }
    }

    fun cleanup() {
        Wearable.getMessageClient(context).removeListener(this)
    }

    fun reopen() {
        Wearable.getMessageClient(context).addListener(this)
    }
}