package agdesigns.elevatefitness.presentation.screens.workout.components

import agdesigns.elevatefitness.R
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Timer
import kotlin.math.roundToInt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.OutlinedIconButton
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.ambient.AmbientAware
import com.google.android.horologist.compose.ambient.AmbientState
import com.google.android.horologist.compose.rotaryinput.accumulatedBehavior
import com.google.android.horologist.media.ui.components.ControlButtonLayout
import com.google.android.horologist.media.ui.screens.player.PlayerScreen

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun EndWorkoutPage(
    contentPadding: PaddingValues,
    ambientState: AmbientState,
    lastIntensity: Float?,
    workoutTime: String?,
    heartRate: Int?,
    calories: Double?,
    endWorkout: (Float) -> Unit,
    terminate: () -> Unit
) {
    var endedWorkout by remember { mutableStateOf(false) }
    var workoutIntensity by remember { mutableFloatStateOf(0.5f) }
    val haptics = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester, endedWorkout) {
        if (endedWorkout) {
            focusRequester.requestFocus()
        }
    }
    if (!endedWorkout) {
        PlayerScreen(
            mediaDisplay = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (workoutTime != null) {
                        val tabularTextStyle = MaterialTheme.typography.labelSmall.copy(
                            fontFeatureSettings = "tnum"
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                Icons.Filled.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                if (ambientState.isInteractive)
                                    workoutTime // this is "mm:ss"
                                else
                                    "${workoutTime.split(":").getOrNull(0) ?: "--"}:--",
                                style = tabularTextStyle
                            )
                        }
                    }

                    if (heartRate != null || calories != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            if (heartRate != null) {
                                val animation = rememberInfiniteTransition()
                                val size by animation.animateFloat(
                                    initialValue = 12.dp.value,
                                    targetValue = 16.dp.value,
                                    animationSpec = infiniteRepeatable(
                                        tween(1000),
                                        RepeatMode.Reverse
                                    )
                                )
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Favorite,
                                        contentDescription = null,
                                        modifier = Modifier.size(if (ambientState.isInteractive) size.dp else 12.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                Text(
                                    if (ambientState.isInteractive)
                                        "$heartRate bpm"
                                    else
                                        "-- bpm",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            if (calories != null) {
                                Text(
                                    "🔥 ${calories.roundToInt()} kcal",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            },
            controlButtons = {
                Text(
                    stringResource(R.string.end_workout),
                    style = MaterialTheme.typography.numeralExtraSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                )
            },
            buttons = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = terminate) {
                        Icon(
                            Icons.Default.Close,
                            stringResource(R.string.close)
                        )
//                        Spacer(Modifier.width(4.dp))
//                        Text(stringResource(R.string.close))
                    }
                    FilledTonalIconButton(
                        shapes = IconButtonDefaults.animatedShapes(
                            shape = MaterialTheme.shapes.small,
                            pressedShape = MaterialTheme.shapes.extraLarge
                        ),
                        onClick = { endedWorkout = true },
                        modifier = Modifier
                            .height(IconButtonDefaults.DefaultButtonSize)
                            .width(IconButtonDefaults.DefaultButtonSize.times(1.5f))
                    ) {
                        Row {
                            Icon(
                                Icons.Default.Done,
                                stringResource(R.string.end_workout)
                            )
                        }
                    }
                }
            }
        )
    } else {
        CircularProgressIndicator(
            progress = { workoutIntensity },
            startAngle = 100f,
            endAngle = 80f,
            strokeWidth = CircularProgressIndicatorDefaults.smallStrokeWidth,
            modifier = Modifier
                .rotaryScrollable(  // TODO: what about devices without rotary?
                    accumulatedBehavior { value ->
                        val delta = if (value > 0) 1 else -1
                        workoutIntensity += (delta * 5) / 100f
                        // clip workoutIntensity to 0..1
                        if (workoutIntensity in 0f..1f) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        }
                        workoutIntensity = workoutIntensity.coerceIn(0f, 1f)
                    },
                    focusRequester = focusRequester
                )
        )
        if (lastIntensity != null) {
            CircularProgressIndicator(
                progress = { lastIntensity },
                startAngle = 100f,
                endAngle = 80f,
                strokeWidth = CircularProgressIndicatorDefaults.smallStrokeWidth,
                colors = ProgressIndicatorDefaults.colors(
                    trackColor = Color.Transparent,
                    indicatorColor = MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier.alpha(0.5f)
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            PlayerScreen(
                mediaDisplay = {
                },
                controlButtons = {
                    Box(Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(R.string.how_intense_was_this_workout),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
                buttons = {
                    ControlButtonLayout(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 10.dp),
                        leftButton = {},
                        middleButton = {
                            IconButton(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .height(IconButtonDefaults.DefaultButtonSize * 1.2f)
                                    .width(IconButtonDefaults.DefaultButtonSize),
                                shapes = IconButtonDefaults.animatedShapes(),
                                colors = if (ambientState.isAmbient)
                                    IconButtonDefaults.outlinedIconButtonColors()
                                else
                                    IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                onClick = { endWorkout(workoutIntensity) },
                            ) {
                                Icon(Icons.Default.Done, "")
                            }
                        },
                        rightButton = {}
                    )
                }
            )
        }
    }
}