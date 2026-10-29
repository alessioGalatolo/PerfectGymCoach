package agdesigns.elevatefitness.service

import agdesigns.elevatefitness.data.SensorRecordingsRepository
import agdesigns.elevatefitness.shared.SENSOR_RECORDING_PATH
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService

/**
 * Receives the raw sensor recordings sent by the watch (developer option), stores them and
 * deletes the data item so that it does not linger in the data layer.
 */
class SensorRecordingListenerService : WearableListenerService() {
    // runs on a background thread, blocking calls are fine
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val dataClient = Wearable.getDataClient(this)
        dataEvents
            .filter {
                it.type == DataEvent.TYPE_CHANGED &&
                    it.dataItem.uri.path?.startsWith(SENSOR_RECORDING_PATH) == true
            }
            // the buffer is released when this method returns
            .map { it.dataItem.freeze() }
            .forEach { dataItem ->
                try {
                    val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                    val id = dataMap.getString("id") ?: return@forEach
                    val metadataAsset = dataMap.getAsset("metadata") ?: return@forEach
                    val samplesAsset = dataMap.getAsset("samples") ?: return@forEach
                    val metadata = Tasks.await(dataClient.getFdForAsset(metadataAsset))
                        .inputStream.use { it.readBytes().decodeToString() }
                    Tasks.await(dataClient.getFdForAsset(samplesAsset)).inputStream.use {
                        SensorRecordingsRepository.save(this, id, metadata, it)
                    }
                    Tasks.await(dataClient.deleteDataItems(dataItem.uri))
                    Log.d(TAG, "Saved sensor recording $id")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save sensor recording ${dataItem.uri}", e)
                }
            }
    }

    companion object {
        private const val TAG = "SensorRecordingListener"
    }
}
