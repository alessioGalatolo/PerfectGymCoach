package agdesigns.elevatefitness.utils

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.Exercise
import android.content.Context
import android.content.res.Configuration
import android.provider.Settings
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import com.agdesignes.shared.Equipment
import java.util.Locale

fun Context.getLocalizedString(
    @StringRes resId: Int,
    locale: Locale
): String {
    // Copy current configuration
    val config = resources.configuration

    // Create a new configuration with the desired locale
    val newConfig = Configuration(config)
    newConfig.setLocale(locale)

    // Create a localized context
    val localizedContext = createConfigurationContext(newConfig)

    return localizedContext.getString(resId)
}


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

fun isFreeWeight(equipment: Equipment): Boolean {
    return when (equipment) {
        Equipment.BARBELL,
        Equipment.DUMBBELL,
        Equipment.BODY_WEIGHT -> true
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
    return false
}


fun getStickyHeader(
    layoutInfo: LazyListLayoutInfo,
    id2StickyHeader: Map<Int, String>,
    lastVisibleKey: Int
): Pair<String?, Int> {

    val visibleItems = layoutInfo.visibleItemsInfo.filter {
        id2StickyHeader.contains(it.key)
    }

    // partially showing (TODO: this could come in handy when adding animations)
    val itemsStartingToDisappear = visibleItems.filter {
        it.offset < 0 && (it.offset + it.size >= 0)
    }
    // completely visible
    val itemsCompletelyVisible = visibleItems.filter {
        it.offset >= 0 && it.offset + it.size <= layoutInfo.viewportEndOffset
    }
    // items partially or completely visible
    val itemsVisible =
        itemsCompletelyVisible.toSet()

    // find key relative to highest value in stickyHeaders2Id
    val highestVisibleId: Int? =
        itemsVisible.maxByOrNull { it.key as Int }?.key as Int?
    val titleText = if (lastVisibleKey > (highestVisibleId ?: 0)) {
        id2StickyHeader[lastVisibleKey]!!
    } else if (highestVisibleId != null) {
        // call can be null if highest header is in list (opposed to being in the topappbar)
        id2StickyHeader[highestVisibleId + 1]
    } else null
    highestVisibleId ?: lastVisibleKey
    return Pair(titleText, (highestVisibleId ?: lastVisibleKey))
}

