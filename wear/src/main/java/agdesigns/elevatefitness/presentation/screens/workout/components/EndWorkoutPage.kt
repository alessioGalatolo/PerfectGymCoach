package agdesigns.elevatefitness.presentation.screens.workout.components

import agdesigns.elevatefitness.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.ambient.AmbientAware
import com.google.android.horologist.compose.rotaryinput.accumulatedBehavior
import com.google.android.horologist.media.ui.components.ControlButtonLayout
import com.google.android.horologist.media.ui.screens.player.PlayerScreen

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun EndWorkoutPage(
    contentPadding: PaddingValues,
    lastIntensity: Float?,
    endWorkout: (Float) -> Unit
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
    AmbientAware { ambientState ->
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (!endedWorkout) {
                TextButton(
                    onClick = { endedWorkout = true }, modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxSize()
                ) {
                    Text(
                        stringResource(R.string.end_workout),
                        style = MaterialTheme.typography.numeralMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center
                    )
                }
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
}