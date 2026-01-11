package agdesigns.elevatefitness.shared

import android.net.Uri
import androidx.compose.ui.res.stringResource
import kotlin.math.round
import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.ProtoDataStoreHelper.protoDataStore
import com.google.android.horologist.data.TargetNodeId
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.data.store.impl.WearLocalDataStore
import com.google.protobuf.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

const val decimalPlaces = 100  // 2 decimal places


fun maybeKgToLb(kg: Float, useImperial: Boolean): Float {
    if (!useImperial)
        return round(kg * decimalPlaces) / decimalPlaces
    return round(kg * 2.20462f * decimalPlaces) / decimalPlaces
}

fun maybeKgToLb(kg: Double, useImperial: Boolean): Double {
    if (!useImperial)
        return round(kg * decimalPlaces) / decimalPlaces
    return round(kg * 2.20462f * decimalPlaces) / decimalPlaces
}


fun maybeLbToKg(weight: Float, useImperial: Boolean): Float {
    if (!useImperial)
        return weight
    return weight / 2.20462f
}

fun barbellResFromWeight(
    weight: Float,
): Int {
    var barbellResource = BarbellType.entries.find {
        it.weight[false] == weight ||
                it.weight[true] == maybeKgToLb(weight, true)
    }?.barbellResource
    if (barbellResource == null) {
        // return weight in any case
        barbellResource = BarbellType.OTHER.barbellResource
    }
    return barbellResource
}

fun barbellIndexFromWeight(
    weight: Float,
): Int {
    val index = BarbellType.entries.indexOfFirst {
        it.weight[false] == weight ||
                it.weight[true] == maybeKgToLb(weight, true)
    }
    if (index == -1) {
        return BarbellType.OTHER.ordinal
    }
    return index
}


@Composable
fun weightAndUnit(
    weight: Float,  // weight in kg
    useImperial: Boolean,
    inParenthesis: Boolean = false
): String {
    val displayWeight = maybeKgToLb(weight, useImperial)
    val unit = if (useImperial) stringResource(R.string.lb) else stringResource(R.string.kg)
    return if (inParenthesis)
        "($displayWeight $unit)"
    else
        "$displayWeight $unit"
}

fun ZonedDateTime?.toProtoTimestamp(): Timestamp {
    val millis = this?.toInstant()?.toEpochMilli()
    return if (millis != null)
        Timestamp.newBuilder()
            .setSeconds(millis / 1000)
            .setNanos((millis % 1000).toInt() * 1000000)
            .build()
    else
        Timestamp.newBuilder()
            .setSeconds(0L)
            .setNanos(0)
            .build()
}

fun Timestamp.toZonedDateTime(): ZonedDateTime? {
    if (this.seconds == 0L && this.nanos == 0)
        return null
    val millis = this.seconds * 1000 + this.nanos / 1000000
    return ZonedDateTime.ofInstant(
        Instant.ofEpochMilli(millis),
        ZoneId.systemDefault()
    )
}

// horologist has a TargetNodeId for paired phone but not for paired watch
public object PairedWatch : TargetNodeId {
    @OptIn(ExperimentalHorologistApi::class)
    override suspend fun evaluate(dataLayerRegistry: WearDataLayerRegistry): String? {
        val capabilitySearch = dataLayerRegistry.capabilityClient.getCapability(
            TargetNodeId.HOROLOGIST_WATCH,
            CapabilityClient.FILTER_ALL,
        ).await()

        return capabilitySearch.nodes.singleOrNull()?.id
    }
}

data class PlateChange(
    val add: Map<Float, Int>,
    val remove: Map<Float, Int>
)


fun getPlates(weight: Float): Map<Float, Int> {
    // Standard barbell plates in kg (or lbs)
    val availablePlates = listOf(25f, 20f, 15f, 10f, 5f, 2.5f, 1.25f)

    val plates = mutableMapOf<Float, Int>()
    var remaining = weight

    for (plate in availablePlates) {
        val count = (remaining / plate).toInt()
        if (count > 0) {
            plates[plate] = count
            remaining -= count * plate
        }
    }

    return plates
}

fun calculatePlateChange(oldWeight: Float, newWeight: Float): PlateChange {
    val oldPlates = getPlates(oldWeight)
    val newPlates = getPlates(newWeight)

    val add = mutableMapOf<Float, Int>()
    val remove = mutableMapOf<Float, Int>()

    // Find plates to add
    newPlates.forEach { (plate, newCount) ->
        val oldCount = oldPlates[plate] ?: 0
        if (newCount > oldCount) {
            add[plate] = newCount - oldCount
        }
    }

    // Find plates to remove
    oldPlates.forEach { (plate, oldCount) ->
        val newCount = newPlates[plate] ?: 0
        if (oldCount > newCount) {
            remove[plate] = oldCount - newCount
        }
    }

    return PlateChange(add, remove)
}

/*
 * Same as Horologist's WearLocalDataStore but with urgent flag to send to wear immediately
 */
@ExperimentalHorologistApi
class UrgentWearLocalDataStore<T>(
    private val delegate: WearLocalDataStore<T>,
    private val serializer: Serializer<T>,
    private val path: String,
) : DataStore<T> by delegate {

    private val mutex = Mutex()

    override suspend fun updateData(transform: suspend (t: T) -> T): T = mutex.withLock {
        // Read current value
        val oldT = delegate.data.first()
        val newT = transform(oldT)

        if (newT == null) {
            delegate.dataClient.deleteDataItems(buildUri(path))
                .await()
        } else if (newT != oldT) {
            val request = PutDataRequest.create(path).apply {
                data = writeBytes(newT)
            }

            delegate.dataClient.putDataItem(request).await()
        }

        return newT
    }

    suspend fun urgentUpdateData(transform: suspend (t: T) -> T): T = mutex.withLock {
        // Read current value
        val oldT = delegate.data.first()
        val newT = transform(oldT)

        if (newT == null) {
            delegate.dataClient.deleteDataItems(buildUri(path))
                .await()
        } else if (newT != oldT) {
            val request = PutDataRequest.create(path).apply {
                data = writeBytes(newT)
                setUrgent() // Add urgency flag
            }

            delegate.dataClient.putDataItem(request).await()
        }

        return newT
    }

    private suspend fun writeBytes(t: T): ByteArray {
        return ByteArrayOutputStream().apply {
            serializer.writeTo(t, this)
        }.toByteArray()
    }

    private fun buildUri(path: String): Uri {
        return Uri.Builder()
            .scheme(PutDataRequest.WEAR_URI_SCHEME)
            .path(path)
            .build()
    }
}

@OptIn(ExperimentalHorologistApi::class)
inline fun <reified T : Any> WearDataLayerRegistry.urgentProtoDataStore(coroutineScope: CoroutineScope) =
    UrgentWearLocalDataStore(
        WearLocalDataStore(
            this,
            coroutineScope = coroutineScope,
            serializer = serializers.serializerForType<T>(),
            path = WearDataLayerRegistry.dataStorePath(T::class),
        ),
        serializers.serializerForType<T>(),
        WearDataLayerRegistry.dataStorePath(T::class),
    )