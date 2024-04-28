package com.anexus.perfectgymcoach.ui

import com.anexus.perfectgymcoach.data.exercise.Exercise
import com.anexus.perfectgymcoach.data.exercise.ExerciseRecord
import kotlin.math.round


const val decimalPlaces = 100  // 2 decimal places

fun isPrimaryMuscle(muscle: Exercise.Muscle): Boolean {
    if (muscle == Exercise.Muscle.CHEST)
        return true
    if (muscle == Exercise.Muscle.BACK)
        return true
    if (muscle == Exercise.Muscle.SHOULDERS)
        return true
    if (muscle == Exercise.Muscle.LEGS)
        return true
    return false
}

fun exerciseIsCompound(exercise: Exercise): Boolean {
    if (exercise.secondaryMuscles.size > 1)
        return true
    if (exercise.name.lowercase().contains("squat")) // FIXME: not ideal
        return true
    return false
}

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