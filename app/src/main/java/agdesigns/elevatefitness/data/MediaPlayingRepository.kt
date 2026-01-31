package agdesigns.elevatefitness.data

import agdesigns.elevatefitness.shared.MEDIA_IMAGES_PATH
import agdesigns.elevatefitness.shared.bitmapArrayStore
import agdesigns.elevatefitness.shared.grpc.Media.MediaPlaying
import agdesigns.elevatefitness.service.NotificationListener
import agdesigns.elevatefitness.shared.WearBitmapArrayStore
import agdesigns.elevatefitness.utils.notificationAccessFlow
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.datastore.core.DataStore
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.ProtoDataStoreHelper.protoDataStore
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.data.apphelper.DataLayerAppHelper
import com.google.android.horologist.datalayer.phone.PhoneDataLayerAppHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

data class NowPlaying(
    val mediaId: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val artwork: Bitmap? = null,
    val isPlaying: Boolean = false,
    val hasAccess: Boolean = false,
    val hasSession: Boolean = false,
    val activityIntent: PendingIntent? = null,
    val packageName: String? = null,
    val appLabel: String? = null
)

data class SessionSummary(
    val packageName: String,
    val appLabel: String,
    val isSelected: Boolean,
    val title: String?,
    val artist: String?,
    val isPlaying: Boolean
)

@OptIn(ExperimentalHorologistApi::class)
class MediaPlayingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val registry: WearDataLayerRegistry,
    private val datalayerHelper: PhoneDataLayerAppHelper
) {
    private val secondaryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val wearImagesStoreDeferred = CompletableDeferred<WearBitmapArrayStore>()
    // FIXME: this should be urgent
    private val wearMediaStoreDeferred = CompletableDeferred<DataStore<MediaPlaying>>()

    init {
        secondaryScope.launch {
            if (datalayerHelper.isAvailable()) {
                wearMediaStoreDeferred.complete(
                    registry.protoDataStore<MediaPlaying>(
                        coroutineScope = secondaryScope,
                    )
                )
                wearImagesStoreDeferred.complete(
                    registry.bitmapArrayStore(
                        coroutineScope = secondaryScope,
                        path = MEDIA_IMAGES_PATH
                    )
                )
            }
        }
    }

    private val manager: MediaSessionManager =
        context.getSystemService(MediaSessionManager::class.java)

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val component = ComponentName(context, NotificationListener::class.java)

    // mainly used to recycle bitmap if song has not changed
    private var lastEmission: NowPlaying = NowPlaying()

    private val _nowPlaying = MutableStateFlow(
        NowPlaying()
    )
    val nowPlaying: StateFlow<NowPlaying> = _nowPlaying.asStateFlow()

    private val _sessions = MutableStateFlow<List<SessionSummary>>(emptyList())
    val sessions: StateFlow<List<SessionSummary>> = _sessions.asStateFlow()

    private var currentController: MediaController? = null
    private var controllerCallback: MediaController.Callback? = null
    private var selectedPackageName: String? = null
    private var hasAccess: Boolean = false


    fun start() {
        secondaryScope.launch {
            notificationAccessFlow(context).collect { newHasAccess ->
                hasAccess = newHasAccess
                if (!hasAccess) {
                    emitNoAccess()
                    return@collect
                }

                // Register listener (on main)
                manager.addOnActiveSessionsChangedListener(
                    sessionsChangedListener,
                    component,
                    mainHandler
                )

                // Seed with current list
                handleSessionsChanged(manager.getActiveSessions(component))
            }
        }
        // send now playing to wear
        secondaryScope.launch {
            nowPlaying.collect { media ->
                wearMediaStoreDeferred.await().updateData {
                    MediaPlaying.newBuilder()
                        .setTitle(media.title ?: "")
                        .setArtist(media.artist ?: "")
                        .setIsPlaying(media.isPlaying)
                        .build()
                }

                if (media.artwork != null) {
                    wearImagesStoreDeferred.await().updateData {
                        // avoid sending if same bitmap
                        if (it.getOrNull(0) == media.artwork)
                            it
                        else
                            listOf(media.artwork)
                    }
                }
            }
        }
    }

    fun stop() {
        try {
            manager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (_: Throwable) {}
        switchTo(null)
        scope.coroutineContext.cancelChildren()
    }

    // --- Controls ---
    fun play() = withController { it.transportControls.play() }
    fun pause() = withController { it.transportControls.pause() }
    fun togglePlayPause() = withController { c ->
        val playing = c.playbackState?.state == PlaybackState.STATE_PLAYING
        if (playing) c.transportControls.pause() else c.transportControls.play()
    }
    fun next() = withController { it.transportControls.skipToNext() }
    fun previous() = withController { it.transportControls.skipToPrevious() }

    // --- Session selection ---
    fun selectSession(packageName: String) {
        selectedPackageName = packageName
        val list = manager.getActiveSessions(component)
        val target = list.firstOrNull { it.packageName == packageName }
        switchTo(target)
        // Refresh summaries with new selection
        handleSessionsChanged(list)
    }

    fun selectNextSession() = rotateSelection(+1)
    fun selectPreviousSession() = rotateSelection(-1)

    // --- Internals ---

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            controllers?.let { handleSessionsChanged(it) }
        }

    private fun handleSessionsChanged(controllers: List<MediaController>) {
        if (!hasAccess) {
            emitNoAccess()
            return
        }

        // Derive summaries
        val summaries = controllers.map { c ->
            val md = c.metadata
            val pb = c.playbackState
            SessionSummary(
                packageName = c.packageName,
                appLabel = appLabel(c.packageName),
                isSelected = c.packageName == (currentController?.packageName ?: selectedPackageName),
                title = md?.description?.title?.toString(),
                artist = md?.description?.subtitle?.toString(),
                isPlaying = pb?.state == PlaybackState.STATE_PLAYING
            )
        }

        _sessions.value = summaries

        // pick first the best controller
        val bestController = controllers.maxByOrNull {
            var score = 0
            // first check if request package
            if (it.packageName == selectedPackageName)
                score += 10
            // look for common music app names
            if (it.packageName.contains("music", ignoreCase = true))
                score += 5
            if (it.packageName.contains("spotify", ignoreCase = true))
                score += 5
            if (it.packageName.contains("tidal", ignoreCase = true))
                score += 5
            if (it.packageName.contains("youtube", ignoreCase = true))
                score += 2
            if (it.metadata?.description?.title != null)
                score += 1
            // add 1 to current controller so that if same score, we prefer current controller
            if (it.sessionToken == currentController?.sessionToken)
                score += 1
            score
        }
        // Choose a controller if none selected
        if (currentController == null || bestController?.sessionToken != currentController!!.sessionToken) {
            switchTo(bestController)
        } else {
            // Ensure still valid; if gone, pick a new one
            val stillPresent = controllers.any { it.sessionToken == currentController?.sessionToken }
            if (!stillPresent) {
                switchTo(bestController)
            } else {
                // Update current NowPlaying fields from latest metadata
                emitFrom(currentController)
            }
        }
    }

    private fun switchTo(controller: MediaController?) {
        // Unregister old
        controllerCallback?.let { cb ->
            currentController?.unregisterCallback(cb)
        }
        controllerCallback = null
        currentController = controller
        selectedPackageName = controller?.packageName

        if (controller == null) {
            _nowPlaying.update {
                NowPlaying(
                    title = null,
                    artist = null,
                    artwork = null,
                    isPlaying = false,
                    hasAccess = hasAccess,
                    hasSession = false,
                    packageName = null,
                    appLabel = null
                )
            }
            return
        }

        controllerCallback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                emitFrom(controller)
            }

            override fun onMetadataChanged(metadata: MediaMetadata?) {
                emitFrom(controller)
            }

            override fun onSessionDestroyed() {
                // Current controller died; trigger refresh
                handleSessionsChanged(manager.getActiveSessions(component))
            }
        }.also { controller.registerCallback(it, mainHandler) }

        emitFrom(controller)
    }

    private fun emitFrom(controller: MediaController?) {
        if (controller == null) return
        val md = controller.metadata
        val pb = controller.playbackState

        // Identify the track robustly
        val mediaId = md?.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)
            ?: md?.description?.mediaId
            ?: buildString {
                append(md?.description?.title?.toString() ?: "")
                append("|")
                append(md?.description?.subtitle?.toString() ?: "")
            }
        val art = if (lastEmission.mediaId == mediaId && lastEmission.artwork != null)
            lastEmission.artwork
        else
            md?.description?.iconBitmap
                ?: md?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
                ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)

        _nowPlaying.update {
            NowPlaying(
                mediaId = mediaId,
                title = md?.description?.title?.toString(),
                artist = md?.description?.subtitle?.toString(),
                artwork = art,
                isPlaying = pb?.state == PlaybackState.STATE_PLAYING,
                hasAccess = hasAccess,
                hasSession = true,
                packageName = controller.packageName,
                activityIntent = controller.sessionActivity,
                appLabel = appLabel(controller.packageName)
            )
        }
        lastEmission = _nowPlaying.value
        // Also refresh session list flags (selected/playing) cheaply
        val currentPkg = controller.packageName
        _sessions.update { list ->
            list.map {
                if (it.packageName == currentPkg) {
                    it.copy(
                        isSelected = true,
                        title = _nowPlaying.value.title,
                        artist = _nowPlaying.value.artist,
                        isPlaying = _nowPlaying.value.isPlaying
                    )
                } else {
                    it.copy(isSelected = false)
                }
            }
        }
    }

    private fun emitNoAccess() {
        _nowPlaying.update {
            NowPlaying()
        }
        _sessions.value = emptyList()
        switchTo(null)
    }

    private fun withController(action: (MediaController) -> Unit) {
        val c = currentController ?: return
        // Many transport controls must be invoked on main
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action(c)
        } else {
            mainHandler.post { action(c) }
        }
    }

    private fun rotateSelection(step: Int) {
        val list = manager.getActiveSessions(component)
        if (list.isEmpty()) return
        val idx = list.indexOfFirst { it.sessionToken == currentController?.sessionToken }
        val nextIdx = if (idx == -1) 0 else (idx + step).floorMod(list.size)
        selectSession(list[nextIdx].packageName)
    }

    private fun Int.floorMod(m: Int): Int = ((this % m) + m) % m

    private fun appLabel(packageName: String): String =
        try {
            val ai = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(ai).toString()
        } catch (_: Throwable) {
            packageName
        }
}
