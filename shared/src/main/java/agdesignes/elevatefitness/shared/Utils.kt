package agdesignes.elevatefitness.shared

import androidx.compose.ui.res.stringResource
import kotlin.math.round
import androidx.compose.runtime.Composable
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.TargetNodeId
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.protobuf.Timestamp
import kotlinx.coroutines.tasks.await
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