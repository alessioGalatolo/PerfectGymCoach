package agdesigns.elevatefitness.presentation

import kotlinx.coroutines.flow.SharedFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class WearRepository @Inject constructor(
    private val handler: WearMessageHandler
) {

    val incomingMessages: SharedFlow<JSONObject> = handler.messages

    fun close() {
        handler.cleanup()
    }

    fun reopen() {
        handler.reopen()
    }

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: WearRepository? = null

        fun getInstance(handler: WearMessageHandler) =
            instance ?: synchronized(this) {
                instance ?: WearRepository(handler).also { instance = it }
            }
    }
}
