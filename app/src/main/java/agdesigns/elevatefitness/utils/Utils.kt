package agdesigns.elevatefitness.utils

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.Exercise
import android.content.Context
import android.content.res.Configuration
import android.provider.Settings
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.ui.unit.LayoutDirection
import com.agdesignes.shared.Equipment
import java.util.Locale

fun getMetFromIntensity(intensity: Float): Float {
    // input: intensity 0-100
    // output: a reasonable MET value for a workout at that intensity
    // assume workout met 3-8
    return 3f + 0.05f * intensity
}

fun getIntensityFromMet(met: Float): Float {
    // input: a reasonable MET value for a workout
    // output: intensity 0-100
    return (met - 3f) / 0.05f
}

operator fun PaddingValues.plus(paddingValues: PaddingValues): PaddingValues {
    return PaddingValues(
        top = this.calculateTopPadding() + paddingValues.calculateTopPadding(),
        bottom = this.calculateBottomPadding() + paddingValues.calculateBottomPadding(),
        start = this.calculateLeftPadding(LayoutDirection.Ltr) + paddingValues.calculateLeftPadding(LayoutDirection.Ltr),
        end = this.calculateRightPadding(LayoutDirection.Ltr) + paddingValues.calculateRightPadding(LayoutDirection.Ltr)
    )
}

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

