package agdesigns.elevatefitness.ui

import agdesigns.elevatefitness.data.exercise.Exercise
import agdesigns.elevatefitness.data.exercise.ExerciseRecord
import android.content.Context
import android.provider.Settings
import kotlin.math.round


const val decimalPlaces = 100  // 2 decimal places

fun hasNotificationAccess(context: Context): Boolean {
    val contentResolver = context.contentResolver
    val enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
    val packageName = context.packageName

    return enabledNotificationListeners.isNotEmpty() && enabledNotificationListeners.contains(packageName)
}

fun isMajorMover(muscle: Exercise.Muscle): Boolean {
    return when (muscle) {
        Exercise.Muscle.CHEST,
        Exercise.Muscle.BACK,
        Exercise.Muscle.SHOULDERS,
        Exercise.Muscle.QUADRICEPS -> true
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
    if (exercise.name.lowercase().contains("squat")) // FIXME: not ideal But can now be fixed
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

fun barbellFromWeight(
    weight: Float,
    useImperial: Boolean,
    isRecord: Boolean,
    noWeight: Boolean = false  // will skip (20.0 kg) unless custom value
): String {
    if (weight == 0f && !isRecord)
        return ExerciseRecord.BarbellType.OTHER.barbellName + "..."
    var barbellName = ExerciseRecord.BarbellType.entries.find {
        it.weight[false] == weight ||
                it.weight[true] == maybeKgToLb(weight, true)
    }?.barbellName
    if (barbellName == null) {
        // return weight in any case
        barbellName = ExerciseRecord.BarbellType.OTHER.barbellName
        return barbellName + " (${maybeKgToLb(weight, useImperial)} ${if (useImperial) "lb" else "kg"})"
    }
    if (noWeight)
        return barbellName
    return barbellName + " (${maybeKgToLb(weight, useImperial)} ${if (useImperial) "lb" else "kg"})"
}