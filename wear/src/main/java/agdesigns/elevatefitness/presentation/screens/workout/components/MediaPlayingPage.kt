package agdesigns.elevatefitness.presentation.screens.workout.components

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.presentation.screens.common.MediaPlayingState
import agdesigns.elevatefitness.presentation.screens.common.TextHeaderWithMarquee
import agdesigns.elevatefitness.presentation.screens.common.VignetteImage
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.AmbientMode
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.media.ui.components.MediaControlButtons
import com.google.android.horologist.media.ui.screens.player.PlayerScreen

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun MediaPlayingPage(
    mediaState: MediaPlayingState,
    ambientMode: AmbientMode,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    raiseVolume: () -> Unit,
    lowerVolume: () -> Unit
) {
    PlayerScreen(
        mediaDisplay = {
            if (mediaState.title == null) {
                TextHeaderWithMarquee(
                    title = stringResource(R.string.no_media_playing),
                    subtitle = stringResource(R.string.no_media_playing_desc),
                    ambientMode = ambientMode
                )
            } else {
                TextHeaderWithMarquee(
                    title = mediaState.title,
                    subtitle = mediaState.artist ?: stringResource(R.string.no_artist),
                    ambientMode = ambientMode
                )
            }
        },
        controlButtons = {
            MediaControlButtons(
                onPlayButtonClick = onPlayPause,
                onPauseButtonClick = onPlayPause,
                playPauseButtonEnabled = true,
                playing = mediaState.isPlaying,
                onSeekToPreviousButtonClick = onPrevious,
                onSeekToNextButtonClick = onNext,
                seekToPreviousButtonEnabled = true,
                seekToNextButtonEnabled = true,
            )
        },
        buttons = {
            Row {
                IconButton(
                    colors = IconButtonDefaults.outlinedIconButtonColors(),
                    border = if (ambientMode is AmbientMode.Interactive)
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    else null,
                    shapes = IconButtonDefaults.shapes(MaterialTheme.shapes.medium),
                    onClick = lowerVolume
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeDown,
                        stringResource(R.string.lower_audio_volume)
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    colors = IconButtonDefaults.outlinedIconButtonColors(),
                    border = if (ambientMode is AmbientMode.Interactive)
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    else null,
                    shapes = IconButtonDefaults.animatedShapes (
                        shape = MaterialTheme.shapes.extraLarge,
                        pressedShape = MaterialTheme.shapes.medium
                    ),
                    onClick = raiseVolume
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp,
                        stringResource(R.string.raise_audio_volume)
                    )
                }
            }
        },
        background = {
            if (mediaState.artwork != null && ambientMode is AmbientMode.Interactive) {
                VignetteImage(
                    mediaState.artwork.asImageBitmap(),
                    alpha = 0f
                )
            }
        }
    )
}