package com.anexus.perfectgymcoach.ui

import com.anexus.perfectgymcoach.data.exercise.ExerciseRecord
import kotlin.math.round


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

fun barbellFromWeight(weight: Float, useImperial: Boolean, isRecord: Boolean): String {
    if (weight == 0f && !isRecord)
        return ExerciseRecord.BarbellType.OTHER.barbellName + "..."
    return (ExerciseRecord.BarbellType.values().find {
        it.weight[false] == weight ||
                it.weight[true] == maybeKgToLb(weight, true)
    }?.barbellName
        ?: ExerciseRecord.BarbellType.OTHER.barbellName) +
            " (${maybeKgToLb(weight, useImperial)} ${if (useImperial) "lb" else "kg"})"
}