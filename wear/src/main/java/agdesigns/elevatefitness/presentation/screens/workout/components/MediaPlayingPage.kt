package agdesigns.elevatefitness.presentation.screens.workout.components

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.presentation.screens.common.MediaPlayingState
import agdesigns.elevatefitness.presentation.screens.common.TextHeaderWithMarquee
import agdesigns.elevatefitness.presentation.screens.common.VignetteImage
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.media.ui.components.MediaControlButtons
import com.google.android.horologist.media.ui.components.display.TextMediaDisplay
import com.google.android.horologist.media.ui.screens.player.PlayerScreen

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun MediaPlayingPage(
    mediaState: MediaPlayingState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    PlayerScreen(
        mediaDisplay = {
            TextHeaderWithMarquee(
                title = mediaState.title ?: stringResource(R.string.no_title),
                subtitle = mediaState.artist ?: stringResource(R.string.no_artist),
            )
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

        },
        background = {
            if (mediaState.artwork != null) {
                VignetteImage(
                    mediaState.artwork.asImageBitmap(),
                    alpha = 0f
                )
            }
        }
    )
}