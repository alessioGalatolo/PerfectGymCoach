package com.agdesignes.shared

import androidx.compose.ui.res.stringResource
import kotlin.math.round
import androidx.compose.runtime.Composable

const val decimalPlaces = 100  // 2 decimal places


fun maybeKgToLb(kg: Float, useImperial: Boolean): Float {
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