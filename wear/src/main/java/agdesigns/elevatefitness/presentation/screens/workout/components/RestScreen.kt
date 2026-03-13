package agdesigns.elevatefitness.presentation.screens.workout.components

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.presentation.screens.common.RoundedPolygonShape
import agdesigns.elevatefitness.presentation.screens.workout.WorkoutViewModel
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import agdesigns.elevatefitness.presentation.screens.common.TextHeaderWithMarquee
import agdesigns.elevatefitness.presentation.screens.workout.InRestHint
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.mutableFloatStateOf
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Icon
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.ambient.AmbientState
import com.google.android.horologist.media.ui.screens.player.PlayerScreen
import com.google.android.horologist.media.ui.util.isLargeScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun RestScreen(
    restProgression: Float,
    currentRestSeconds: Long,
    nextSetExerciseName: String,
    ambientState: AmbientState,
    hints: List<InRestHint>,
    onDismissHint: () -> Unit,
    skipRest: () -> Unit,
) {
    var previousRestProgression by remember { mutableFloatStateOf(restProgression) }

    val animatedRestProgression = animateFloatAsState(
        targetValue = restProgression,
        animationSpec = if (restProgression > previousRestProgression) {
            // Resetting to full - use snap (no animation)
            snap()
        } else {
            tween(
                WorkoutViewModel.TIME_REFRESH_DELAY_MILLIS.toInt(),
                easing = LinearEasing
            )
        }
    )

    LaunchedEffect(restProgression) {
        previousRestProgression = restProgression
    }

    Rest(
        nextSetExerciseName = nextSetExerciseName,
        currentRestSeconds = currentRestSeconds,
        ambientState = ambientState,
        hints = hints,
        onDismissHint = onDismissHint,
        skipRest = skipRest
    )
    if (ambientState.isInteractive) {
        CircularProgressIndicator(
            progress = { animatedRestProgression.value },
            startAngle = CircularProgressIndicatorDefaults.StartAngle + 20f,  // allow for clock in up center
            endAngle = CircularProgressIndicatorDefaults.StartAngle - 20f,
            strokeWidth = CircularProgressIndicatorDefaults.smallStrokeWidth
        )
    }
}


@OptIn(ExperimentalHorologistApi::class)
@Composable
fun Rest(
    nextSetExerciseName: String,
    currentRestSeconds: Long,
    ambientState: AmbientState,
    hints: List<InRestHint>,
    onDismissHint: () -> Unit,
    skipRest: () -> Unit,
) {
    val nextThingString = if (nextSetExerciseName.isNotBlank())
        stringResource(R.string.next_thing)
    else
        stringResource(R.string.all_done)  // we are likely at the end of workout
    val middleSize = if (LocalConfiguration.current.isLargeScreen) 88.dp else 72.dp
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Dialog state and helpful message generation
    var showDialog by remember { mutableStateOf(false) }
    LaunchedEffect(hints) {
        if (hints.isNotEmpty()) {
            delay(5000)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            showDialog = true
        }
    }
    val hint = hints.firstOrNull()

    // Show dialog only if there's rest time and we have a helpful message
    AlertDialog(
        visible = hint != null && showDialog && currentRestSeconds > 0 && hints.isNotEmpty(),
        onDismissRequest = {
            showDialog = false
            scope.launch {
                // wait for the dialog to disappear, then dismiss hint
                // otherwise a new hint will briefly appear before the dialog disappears
                delay(1000)
                onDismissHint()
            }
        },
        icon = {

        },
        title = {
            if (hint != null) {
                Text(stringResource(hint.titleResId))
            }
        },
        text = {
            if (hint != null) {
                Text(
                    stringResource(hint.descResId, *hint.descVarArgs.toTypedArray())
                )
            }
        },
        edgeButton = {
            AlertDialogDefaults.EdgeButton(
                onClick = {
                    // Perform confirm action here
                    showDialog = false
                    scope.launch {
                        // wait for the dialog to disappear, then dismiss hint
                        // otherwise a new hint will briefly appear before the dialog disappears
                        delay(1000)
                        onDismissHint()
                    }
                }
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = stringResource(R.string.done_icon)
                )
            }

        }
    )

    // Reset dialog when rest ends
    LaunchedEffect(currentRestSeconds) {
        if (currentRestSeconds == 0L) {
            showDialog = true // Reset for next rest period
        }
    }

    LaunchedEffect(Unit) {
        delay(5000)
        showDialog = true
    }

    PlayerScreen(
        mediaDisplay = {
            TextHeaderWithMarquee(
                title = nextThingString,
                subtitle = nextSetExerciseName,
                ambientState = ambientState,
                modifier = Modifier.padding(CircularProgressIndicatorDefaults.FullScreenPadding)
            )
        },
        controlButtons = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(middleSize),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Absolute.Center,
            ) {
                val poly = remember {
                    RoundedPolygon.star(
                        6,
                        rounding = CornerRounding(0.2f)
                    )
                }
                val clipShape = remember(poly) {
                    RoundedPolygonShape(polygon = poly)
                }

                // Infinite rotation animation
                val infiniteTransition = rememberInfiniteTransition(label = "rotation")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 18000,
                            easing = LinearEasing
                        ),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotation"
                )
                val background = if (ambientState.isAmbient) {
                    Color.Transparent
                } else {
                    MaterialTheme.colorScheme.primary
                }

                Box(
                    modifier = Modifier
                        .size(middleSize)
                        .rotate(if (ambientState.isAmbient) 0f else rotation) // Apply rotation here
                        .clip(clipShape)
                        // this will only be visible in ambient mode as the background becomes transparent
                        .border(1.dp, MaterialTheme.colorScheme.primary, clipShape)
                        .background(background),
                    contentAlignment = Alignment.Center
                ) {
                    val minutes = currentRestSeconds.floorDiv(60)
                    val seconds = currentRestSeconds.mod(60)
                    Text(
                        text = if (ambientState.isInteractive)
                            "%02d:%02d".format(minutes, seconds)
                        else
                            "%02d:--".format(minutes),
                        style = MaterialTheme.typography.numeralExtraSmall,
                        color = if (ambientState.isAmbient) {
                            Color.White
                        } else
                            MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .rotate(
                                if (ambientState.isAmbient)
                                    0f
                                else -rotation // Counter-rotate text to keep it upright
                            )
                    )
                }
            }
        },
        buttons = {
            TextButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    skipRest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text(text = stringResource(R.string.skip_rest), textAlign = TextAlign.Center)
            }
        }
    )
}