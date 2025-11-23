package agdesigns.elevatefitness.presentation.screens.common

import android.app.PendingIntent
import android.graphics.Bitmap


data class MediaPlayingState(
    val title: String? = null,
    val artist: String? = null,
    val artwork: Bitmap? = null,
    val packageName: String? = null,
    val isPlaying: Boolean = false,
    val needsAccess: Boolean = false,
    val hasSession: Boolean = false,
    val canAskAccess: Boolean = false,
    val activityIntent: PendingIntent? = null
)