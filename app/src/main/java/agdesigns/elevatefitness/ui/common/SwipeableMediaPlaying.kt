package agdesigns.elevatefitness.ui.common

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.ui.screens.workout.WorkoutEvent
import android.app.ActivityOptions
import android.media.session.PlaybackState.STATE_PLAYING
import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage


object SwipeableMediaPlayingDefaults{
    internal val artworkSize = 48.dp
    internal val innerPadding = 16.dp

    val totalHeight = artworkSize + innerPadding*2
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SwipeableMediaPlaying(
    onDismiss: () -> Unit,
    state: MediaPlayingState,
    togglePlayPause: () -> Unit,
    playNext: () -> Unit,
    modifier: Modifier = Modifier,
    openPermissionDialog: () -> Unit = {}
) {
    val shouldTeaseMediaAccess = state.needsAccess && state.canAskAccess
    val mediaTitle = if (shouldTeaseMediaAccess) {
        stringResource(R.string.tease_media_access_prompt)
    } else state.title ?: stringResource(R.string.no_music_playing)
    val mediaArtist = if (shouldTeaseMediaAccess) {
        stringResource(R.string.tease_media_access_learn_more)
    } else state.artist ?: stringResource(R.string.no_music_playing_info)
    val context = LocalContext.current

    SwipeToDismissBox(
        state = rememberSwipeToDismissBoxState(),
        onDismiss = {
            onDismiss()
        },
        backgroundContent = {}
        ) {
            ElevatedCard(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface
                ),
                modifier = modifier
                    .clickable {
                        if (shouldTeaseMediaAccess) {
                            openPermissionDialog()
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                val opts = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) ActivityOptions
                                    .makeBasic()
                                    .setPendingIntentBackgroundActivityStartMode(
                                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE
                                    )
                                    .toBundle() else ActivityOptions
                                    .makeBasic()
                                    .setPendingIntentBackgroundActivityStartMode(
                                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                                    )
                                    .toBundle()
                                state.activityIntent?.send(context, 0, null, null, null, null, opts)
                            } else {
                                state.activityIntent?.send()
                            }
                        }
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    // big blurred artwork as background
                    if (state.artwork != null) {
                        AsyncImage(
                            state.artwork,
                            stringResource(R.string.song_artwork),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .matchParentSize()
                                .blur(16.dp)
                        )
                        // Dimming scrim (dark overlay)
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.3f))
                        )
                    }
                    Column (Modifier.padding(SwipeableMediaPlayingDefaults.innerPadding)) {
                        Row(
                            verticalAlignment = CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            if (state.artwork != null) {
                                AsyncImage(
                                    state.artwork,
                                    stringResource(R.string.song_artwork),
                                    Modifier
                                        .size(SwipeableMediaPlayingDefaults.artworkSize)
                                        .clip(
                                            RoundedCornerShape(8.dp)
                                        )
                                )
                            } else {
                                Icon(
                                    Icons.Default.MusicNote,
                                    stringResource(R.string.no_song_artwork_available),
                                    Modifier.size(SwipeableMediaPlayingDefaults.artworkSize)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    mediaTitle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    mediaArtist,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            // if we are just teasing, gain space by removing buttons
                            if (!shouldTeaseMediaAccess) {
                                Spacer(Modifier.width(8.dp))
                                FilledIconToggleButton(
                                    checked = state.isPlaying,
                                    onCheckedChange = { togglePlayPause() },
                                    shapes = IconButtonDefaults.toggleableShapes(),
                                    modifier = Modifier.size(IconButtonDefaults.smallContainerSize(
                                        IconButtonDefaults.IconButtonWidthOption.Wide))
                                ) {
                                    if (state.isPlaying) {
                                        Icon(Icons.Default.Pause,
                                            stringResource(R.string.pause_icon),
                                        )
                                    } else {
                                        Icon(Icons.Default.PlayArrow,
                                            stringResource(R.string.play_icon)
                                        )
                                    }
                                }
                                FilledTonalIconButton(
                                    shapes = IconButtonDefaults.shapes(),
                                    onClick = playNext
                                ) {
                                    Icon(Icons.Default.SkipNext,
                                        stringResource(R.string.skipnext_icon_track)
                                    )
                                }
                            }
                        }
                    }
                }
            }
    }
}

@Preview
@Composable
fun SwipeableMediaPlayingPreview() {
    Column {
        SwipeableMediaPlaying(
            onDismiss = {},
            state = MediaPlayingState(
                title = "Song title",
                artist = "Artist name",
                isPlaying = true
            ),
            togglePlayPause = {},
            playNext = {}
        )

        SwipeableMediaPlaying(
            onDismiss = {},
            state = MediaPlayingState(
                title = "Song title",
                artist = "Artist name",
                isPlaying = false
            ),
            togglePlayPause = {},
            playNext = {}
        )
        SwipeableMediaPlaying(
            onDismiss = {},
            state = MediaPlayingState(
                title = null,
                artist = null,
                isPlaying = false,
                needsAccess = true,
                canAskAccess = true
            ),
            togglePlayPause = {},
            playNext = {}
        )
    }
}