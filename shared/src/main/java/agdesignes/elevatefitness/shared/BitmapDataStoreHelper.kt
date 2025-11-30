package agdesignes.elevatefitness.shared

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.TargetNodeId
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.data.store.impl.dataItemFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import androidx.core.graphics.scale

/*
 horologist only allows to send data items. With this extension, we allow bitmaps using the same mechanism
 */

@OptIn(ExperimentalHorologistApi::class)
fun WearDataLayerRegistry.bitmapArrayStore(
    coroutineScope: CoroutineScope,
    path: String
) = WearBitmapArrayStore(
        this,
        path,
        coroutineScope,
    )

@OptIn(ExperimentalHorologistApi::class)
fun WearDataLayerRegistry.bitmapFlow(
    node: TargetNodeId,
    path: String,
): Flow<List<Bitmap>> {
    val registry = this
    return flow {
        val nodeId = node.evaluate(registry)

        if (nodeId != null) {
            emitAll(registry.dataClient.bitmapArrayFlow(nodeId, path))
        }
    }
}
/**
 * DataStore-like interface for syncing bitmap arrays across devices.
 * Uses DataClient Assets for efficient binary transfer.
 */
@OptIn(ExperimentalHorologistApi::class)
class WearBitmapArrayStore(
    private val wearDataLayerRegistry: WearDataLayerRegistry,
    private val path: String,
    private val coroutineScope: CoroutineScope,
    started: SharingStarted = SharingStarted.Eagerly,
    private val compression: BitmapCompression = BitmapCompression(),
) {
    private val mutex = Mutex()

    private val nodeIdFlow = flow {
        val nodeId = TargetNodeId.ThisNodeId.evaluate(wearDataLayerRegistry)
        emit(
            NodeIdAndPath(
                nodeId = nodeId,
                fullPath = buildUri(nodeId, path),
            )
        )
    }.shareIn(coroutineScope, started = started, replay = 1)

    val dataClient: DataClient
        get() = wearDataLayerRegistry.dataClient

    /**
     * Flow of bitmap arrays. Emits whenever bitmaps change on either device.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val data: Flow<List<Bitmap>> = nodeIdFlow.flatMapLatest { (nodeId, _) ->
        dataClient.bitmapArrayFlow(nodeId, path)
    }.shareIn(coroutineScope, started = SharingStarted.Eagerly, replay = 1)

    /**
     * Update the bitmap array. Transform function receives current bitmaps.
     */
    suspend fun updateData(transform: suspend (current: List<Bitmap>) -> List<Bitmap>): List<Bitmap> =
        mutex.withLock {
            val nodeId = nodeIdFlow.first()

            val oldBitmaps = readExistingValue(nodeId)
            val newBitmaps = transform(oldBitmaps)

            if (newBitmaps.isEmpty() && oldBitmaps.isNotEmpty()) {
                // Delete if clearing all bitmaps
                dataClient.deleteDataItems(nodeId.fullPath)
            } else if (newBitmaps != oldBitmaps) {
                writeBitmaps(nodeId, newBitmaps)
            }

            return newBitmaps
        }

    /**
     * Set bitmaps directly without transform
     */
    suspend fun setBitmaps(bitmaps: List<Bitmap>) = updateData { bitmaps }

    /**
     * Clear all bitmaps
     */
    suspend fun clear() = updateData { emptyList() }

    private suspend fun writeBitmaps(nodeId: NodeIdAndPath, bitmaps: List<Bitmap>) {
        val assets = bitmaps.mapIndexed { index, bitmap ->
            val bytes = compression.compress(bitmap)
            "bitmap_$index" to Asset.createFromBytes(bytes)
        }.toMap()

        val request = PutDataMapRequest.create(path).apply {
            dataMap.putInt("count", bitmaps.size)
            dataMap.putLong("timestamp", System.currentTimeMillis())

            assets.forEach { (key, asset) ->
                dataMap.putAsset(key, asset)
            }
        }.asPutDataRequest().apply {
            setUrgent() // Prioritize image transfer
        }

        dataClient.putDataItem(request)
    }

    private suspend fun readExistingValue(nodeId: NodeIdAndPath): List<Bitmap> {
        return try {
            val dataItem = dataClient.getDataItem(buildUri(nodeId.nodeId, path)).await()

            if (dataItem != null) {
                val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                val count = dataMap.getInt("count", 0)

                if (count == 0) return emptyList()

                (0 until count).mapNotNull { index ->
                    val asset = dataMap.getAsset("bitmap_$index")
                    asset?.let { loadBitmapFromAsset(it) }
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.w("WearBitmapArrayStore", "Failed to read bitmaps", e)
            emptyList()
        }
    }

    private suspend fun loadBitmapFromAsset(asset: Asset): Bitmap? {
        return try {
            val inputStream = dataClient
                .getFdForAsset(asset)
                .await()
                .inputStream

            BitmapFactory.decodeStream(inputStream).also {
                inputStream.close()
            }
        } catch (e: Exception) {
            Log.e("WearBitmapArrayStore", "Failed to load bitmap from asset", e)
            null
        }
    }
}

/**
 * Internal helper matching Horologist's pattern
 */
private data class NodeIdAndPath(
    val nodeId: String,
    val fullPath: Uri,
)

private fun buildUri(nodeId: String, path: String): Uri {
    return Uri.Builder()
        .scheme(PutDataRequest.WEAR_URI_SCHEME)
        .path(path)
        .authority(nodeId)
        .build()
}

/**
 * Flow extension for bitmap arrays
 */
private fun DataClient.bitmapArrayFlow(
    nodeId: String,
    path: String,
): Flow<List<Bitmap>> = callbackFlow {
    val listener = DataClient.OnDataChangedListener { dataEvents ->
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                if (dataItem.uri.path == path) {
                    trySend(dataItem)
                }
            } else if (event.type == DataEvent.TYPE_DELETED) {
                trySend(null)
            }
        }
    }

    val uri = Uri.Builder()
        .scheme(PutDataRequest.WEAR_URI_SCHEME)
        .path(path)
        .authority(nodeId)
        .build()

    // Subscribe to updates first
    addListener(listener, uri, DataClient.FILTER_LITERAL).await()

    // Then get current value
    val item = this@bitmapArrayFlow.getDataItem(uri).await()
    trySend(item)

    awaitClose {
        removeListener(listener)
    }
}.mapNotNull { dataItem ->
    dataItem?.let { parseBitmapArray(this, it) } ?: emptyList()
}

private suspend fun parseBitmapArray(dataClient: DataClient, dataItem: DataItem): List<Bitmap> {
    return try {
        val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
        val count = dataMap.getInt("count", 0)

        if (count == 0) return emptyList()

        // Load assets in parallel for better performance
        coroutineScope {
            (0 until count).map { index ->
                async(Dispatchers.IO) {
                    val asset = dataMap.getAsset("bitmap_$index")
                    asset?.let { loadBitmapFromAssetSync(dataClient, it, dataItem) }
                }
            }.awaitAll().filterNotNull()
        }
    } catch (e: Exception) {
        Log.w("WearBitmapArrayStore", "Failed to parse bitmap array", e)
        emptyList()
    }
}

private suspend fun loadBitmapFromAssetSync(dataClient: DataClient, asset: Asset, dataItem: DataItem): Bitmap? {
    return try {
        dataClient
            .getFdForAsset(asset)
            .await()
            .inputStream
            .use { stream ->
                BitmapFactory.decodeStream(stream)
            }
    } catch (e: Exception) {
        null
    }
}

/**
 * Bitmap compression configuration
 */
data class BitmapCompression(
    val format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
    val quality: Int = 85,
    val maxWidth: Int = 1024,
    val maxHeight: Int = 1024,
) {
    fun compress(bitmap: Bitmap): ByteArray {
        val scaled = if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
            val ratio = minOf(
                maxWidth.toFloat() / bitmap.width,
                maxHeight.toFloat() / bitmap.height
            )
            bitmap.scale((bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt())
        } else {
            bitmap
        }

        return ByteArrayOutputStream().use { stream ->
            scaled.compress(format, quality, stream)
            if (scaled != bitmap) scaled.recycle()
            stream.toByteArray()
        }
    }
}