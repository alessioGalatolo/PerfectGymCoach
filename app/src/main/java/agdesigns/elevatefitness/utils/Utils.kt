package agdesigns.elevatefitness.utils

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.ELEVATE_FITNESS_SHARE_EXTENSION
import agdesigns.elevatefitness.data.ELEVATE_FITNESS_SHARE_MIME_TYPE
import agdesigns.elevatefitness.data.db.entity.Exercise
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.IOException
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


// credits to ltp from stackoverflow when people still used stackoverflow
fun Context.getLocaleListFromXml(): LocaleListCompat {
    val tagsList = mutableListOf<CharSequence>()
    try {
        val xpp: XmlPullParser = resources.getXml(R.xml.locales_config)
        while (xpp.eventType != XmlPullParser.END_DOCUMENT) {
            if (xpp.eventType == XmlPullParser.START_TAG) {
                if (xpp.name == "locale") {
                    tagsList.add(xpp.getAttributeValue(0))
                }
            }
            xpp.next()
        }
    } catch (e: XmlPullParserException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    }

    return LocaleListCompat.forLanguageTags(tagsList.joinToString(","))
}


fun Context.getLangPreferenceDropdownEntries(): Map<String, String> {
    val localeList = getLocaleListFromXml()
    val map = mutableMapOf<String, String>()

    for (a in 0 until localeList.size()) {
        localeList[a].let {
            map.put(it?.toLanguageTag() ?: "und", it?.getDisplayName(it)?.replaceFirstChar { it.uppercase() } ?: "---")
        }
    }
    return map
}



fun notificationAccessFlow(context: Context) = callbackFlow {
    val uri: Uri = Settings.Secure.getUriFor("enabled_notification_listeners")
    val resolver = context.contentResolver
    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            trySend(Unit)
        }
    }
    // initial
    trySend(Unit)
    resolver.registerContentObserver(uri, false, observer)

    awaitClose { resolver.unregisterContentObserver(observer) }
}.map {
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}.distinctUntilChanged()

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


fun launchShareIntent(context: Context, text: String, title: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_TITLE, title)
    }
    context.startActivity(Intent.createChooser(sendIntent, title))
}

fun launchFileShareIntent(context: Context, fileName: String, json: String, title: String) {
    val safeName = fileName.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim().take(50)
    val cacheDir = File(context.cacheDir, "shared_plans").apply { mkdirs() }
    val file = File(cacheDir, "$safeName$ELEVATE_FITNESS_SHARE_EXTENSION")
    file.writeText(json)
    val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = ELEVATE_FITNESS_SHARE_MIME_TYPE
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, title))
}