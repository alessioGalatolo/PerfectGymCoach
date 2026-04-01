package agdesigns.elevatefitness.ui.common

import agdesigns.elevatefitness.data.MediaPlayingRepository
import agdesigns.elevatefitness.data.PreferenceRepository
import android.app.PendingIntent
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


data class MediaPlayingState(
    val title: String? = null,
    val artist: String? = null,
    val artwork: Bitmap? = null,
    val packageName: String? = null,
    val isPlaying: Boolean = false,
    val needsAccess: Boolean = true,
    val hasSession: Boolean = false,
    val canAskAccess: Boolean = false,
    val activityIntent: PendingIntent? = null
)

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val medias: MediaPlayingRepository,
    private val preferences: PreferenceRepository
) : ViewModel() {

    val state: StateFlow<MediaPlayingState> =
        combine(
            medias.nowPlaying,
            preferences.getDontWantNotificationAccess()
        ) { np, dontWantAccess ->
            Log.d("MediaViewModel", "Received $np")
            MediaPlayingState(
                title = np.title,
                artist = np.artist,
                artwork = np.artwork,
                packageName = np.packageName,
                isPlaying = np.isPlaying,
                needsAccess = !np.hasAccess,
                canAskAccess = !dontWantAccess,
                hasSession = np.hasSession,
                activityIntent = np.activityIntent
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            MediaPlayingState()
        )

    init {
        medias.start()
    }

    override fun onCleared() {
        medias.stop()
        super.onCleared()
    }

    fun togglePlayPause() {
        medias.togglePlayPause()
    }

    fun playNext() {
        medias.next()
    }

    fun resetCanRequestAccess() {
        viewModelScope.launch {
            preferences.setDontWantNotificationAccess(false)
        }
    }
}
