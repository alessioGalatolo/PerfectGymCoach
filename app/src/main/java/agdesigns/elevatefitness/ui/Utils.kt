package agdesigns.elevatefitness.ui

import agdesigns.elevatefitness.data.exercise.Exercise
import agdesigns.elevatefitness.data.exercise.ExerciseRecord
import kotlin.math.round


const val decimalPlaces = 100  // 2 decimal places

fun isMajorMover(muscle: Exercise.Muscle): Boolean {
    return when (muscle) {
        Exercise.Muscle.CHEST,
        Exercise.Muscle.BACK,
        Exercise.Muscle.SHOULDERS,
        Exercise.Muscle.LEGS -> true
        else -> false
    }
}

fun isFreeWeight(equipment: Exercise.Equipment): Boolean {
    return when (equipment) {
        Exercise.Equipment.BARBELL,
        Exercise.Equipment.DUMBBELL,
        Exercise.Equipment.BODY_WEIGHT -> true
        else -> false
    }
}

fun exerciseIsCompound(exercise: Exercise): Boolean {
    if (!isFreeWeight(exercise.equipment))
        return false
    if (!isMajorMover(exercise.primaryMuscle))
        return false
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
    return (ExerciseRecord.BarbellType.entries.find {
        it.weight[false] == weight ||
                it.weight[true] == maybeKgToLb(weight, true)
    }?.barbellName
        ?: ExerciseRecord.BarbellType.OTHER.barbellName) +
            " (${maybeKgToLb(weight, useImperial)} ${if (useImperial) "lb" else "kg"})"
}