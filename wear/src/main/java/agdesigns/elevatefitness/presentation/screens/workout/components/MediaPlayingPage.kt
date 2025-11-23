package agdesigns.elevatefitness.presentation.screens.workout.components

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.presentation.screens.common.MediaPlayingState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.media.ui.components.MediaControlButtons
import com.google.android.horologist.media.ui.components.display.TextMediaDisplay
import com.google.android.horologist.media.ui.screens.player.PlayerScreen

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun MediaPlayingPage(
    mediaState: MediaPlayingState,
) {
    PlayerScreen(
        mediaDisplay = {
            TextMediaDisplay(
                title = mediaState.title ?: stringResource(R.string.no_title),
                subtitle = mediaState.artist ?: stringResource(R.string.no_artist),
            )
        },
        controlButtons = {
            MediaControlButtons(
                onPlayButtonClick = { /*TODO*/ },
                onPauseButtonClick = { /*TODO*/ },
                playPauseButtonEnabled = true,
                playing = true,
                onSeekToPreviousButtonClick = { /*TODO*/ },
                onSeekToNextButtonClick = { /*TODO*/ },
                seekToPreviousButtonEnabled = false,
                seekToNextButtonEnabled = false,
            )
        },
        buttons = {

        }
    )
}