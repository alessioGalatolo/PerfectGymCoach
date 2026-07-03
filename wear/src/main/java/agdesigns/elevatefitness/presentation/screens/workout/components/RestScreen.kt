package agdesigns.elevatefitness.presentation.screens.workout.components

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.presentation.screens.common.MorphPolygonShape
import agdesigns.elevatefitness.presentation.screens.workout.WorkoutViewModel
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.PlusOne
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.wear.compose.foundation.AmbientMode
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import agdesigns.elevatefitness.presentation.screens.common.TextHeaderWithMarquee
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.media.ui.components.controls.MediaButton
import com.google.android.horologist.media.ui.components.controls.MediaButtonDefaults
import com.google.android.horologist.media.ui.screens.player.PlayerScreen
import com.google.android.horologist.media.ui.util.isLargeScreen

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun RestScreen(
    restProgression: Float,
    currentRestSeconds: Long,
    nextSetExerciseName: String,
    ambientMode: AmbientMode,
    isLastSet: Boolean,
    heartRate: Int?,
    skipRest: () -> Unit,
    onAddSet: () -> Unit,
    onExtendRest: () -> Unit,
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
        ambientMode = ambientMode,
        isLastSet = isLastSet,
        heartRate = heartRate,
        skipRest = skipRest,
        onAddSet = onAddSet,
        onExtendRest = onExtendRest,
    )
    if (ambientMode is AmbientMode.Interactive) {
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
    ambientMode: AmbientMode,
    isLastSet: Boolean,
    heartRate: Int?,
    skipRest: () -> Unit,
    onAddSet: () -> Unit,
    onExtendRest: () -> Unit,
) {
    val nextThingString = if (nextSetExerciseName.isNotBlank())
        stringResource(R.string.next_thing)
    else
        stringResource(R.string.all_done)
    val middleSize = if (LocalConfiguration.current.isLargeScreen) 88.dp else 72.dp
    val haptics = LocalHapticFeedback.current

    var confirmingSkip by remember { mutableStateOf(false) }
    var outerBoxCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var timerBoxCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    LaunchedEffect(ambientMode) {
        if (ambientMode is AmbientMode.Ambient) confirmingSkip = false
    }

    val poly = remember { RoundedPolygon.star(6, rounding = CornerRounding(0.2f)) }
    val circlePoly = remember { RoundedPolygon.star(4, rounding = CornerRounding(0.35f)) }
    val morph = remember { Morph(poly, circlePoly) }
    val morphProgress by animateFloatAsState(
        targetValue = if (confirmingSkip) 1f else 0f,
        animationSpec = tween(300),
        label = "morph"
    )
    val morphShape = remember(morphProgress) { MorphPolygonShape(morph, morphProgress) }

    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val background by animateColorAsState(
        targetValue = when {
            ambientMode is AmbientMode.Ambient -> Color.Transparent
            confirmingSkip -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(300),
        label = "bgColor"
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            ambientMode is AmbientMode.Ambient -> Color.White
            confirmingSkip -> MaterialTheme.colorScheme.onError
            else -> MaterialTheme.colorScheme.onPrimary
        },
        animationSpec = tween(300),
        label = "contentColor"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            ambientMode is AmbientMode.Ambient -> MaterialTheme.colorScheme.primary
            confirmingSkip -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(300),
        label = "borderColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { outerBoxCoords = it }
            .then(
                if (confirmingSkip) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val outer = outerBoxCoords
                            val timer = timerBoxCoords
                            if (outer != null && timer != null && outer.isAttached && timer.isAttached) {
                                val timerTopLeft = outer.localPositionOf(timer, Offset.Zero)
                                val inBox = down.position.x >= timerTopLeft.x &&
                                        down.position.x <= timerTopLeft.x + timer.size.width &&
                                        down.position.y >= timerTopLeft.y &&
                                        down.position.y <= timerTopLeft.y + timer.size.height
                                if (!inBox) confirmingSkip = false
                            } else {
                                confirmingSkip = false
                            }
                        }
                    }
                } else Modifier
            )
    ) {
        PlayerScreen(
            mediaDisplay = {
                TextHeaderWithMarquee(
                    title = nextThingString,
                    subtitle = nextSetExerciseName,
                    ambientMode = ambientMode,
                    modifier = Modifier.padding(CircularProgressIndicatorDefaults.FullScreenPadding)
                )
            },
            controlButtons = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(middleSize),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (isLastSet) {
                        MediaButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                confirmingSkip = false
                                onAddSet()
                            },
                            icon = Icons.Default.PlusOne,
                            contentDescription = stringResource(R.string.add_set),
                            modifier = Modifier.weight(1f),
                            colors = if (ambientMode is AmbientMode.Interactive)
                                MediaButtonDefaults.mediaButtonDefaultColors
                            else
                                // TODO: transition away from this (old material lib)
                                androidx.wear.compose.material.ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Box(
                        modifier = Modifier
                            .size(middleSize)
                            .onGloballyPositioned { timerBoxCoords = it }
                            .rotate(if (ambientMode is AmbientMode.Ambient || confirmingSkip) 0f else rotation)
                            .clip(morphShape)
                            // only visible in ambient mode when background is transparent
                            .border(1.dp, borderColor, morphShape)
                            .background(background)
                            .clickable(enabled = ambientMode is AmbientMode.Interactive) {
                                if (confirmingSkip) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    skipRest()
                                } else {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    confirmingSkip = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (confirmingSkip) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.skip_rest),
                                tint = contentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            val minutes = currentRestSeconds.floorDiv(60)
                            val seconds = currentRestSeconds.mod(60)
                            Text(
                                text = if (ambientMode is AmbientMode.Interactive)
                                    "%02d:%02d".format(minutes, seconds)
                                else
                                    "%02d:--".format(minutes),
                                style = MaterialTheme.typography.numeralExtraSmall,
                                color = contentColor,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.rotate(
                                    if (ambientMode is AmbientMode.Ambient) 0f else -rotation
                                )
                            )
                        }
                    }

                    MediaButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            confirmingSkip = false
                            onExtendRest()
                        },
                        icon = Icons.Filled.MoreTime,
                        contentDescription = stringResource(R.string.extend_rest),
                        modifier = Modifier.weight(1f),
                        colors = if (ambientMode is AmbientMode.Interactive)
                            MediaButtonDefaults.mediaButtonDefaultColors
                        else
                            // TODO: transition away from this
                            androidx.wear.compose.material.ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                    )
                }
            },
            buttons = {
                HeartRate(
                    heartRate,
                    ambientMode
                )
            }
        )
    }
}