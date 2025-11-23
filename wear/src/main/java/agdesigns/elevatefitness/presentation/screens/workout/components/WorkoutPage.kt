package agdesigns.elevatefitness.presentation.screens.workout.components

import agdesigns.elevatefitness.presentation.screens.common.MorphPolygonShape
import agdesigns.elevatefitness.presentation.screens.common.RoundedPolygonShape
import agdesigns.elevatefitness.presentation.screens.common.VignetteImage
import agdesigns.elevatefitness.presentation.screens.workout.WorkoutState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.ambient.AmbientAware
import com.google.android.horologist.media.ui.components.ControlButtonLayout
import com.google.android.horologist.media.ui.components.controls.MediaButton
import com.google.android.horologist.media.ui.components.display.TextMediaDisplay
import com.google.android.horologist.media.ui.screens.player.PlayerScreen
import com.google.android.horologist.media.ui.util.isLargeScreen

@Composable
fun WorkoutPage(
    contentPadding: PaddingValues,
    workoutState: WorkoutState,
    listState: ScalingLazyListState,
    changeWeight: (Int) -> Unit,
    changeReps: (Int) -> Unit,
    changeTare: (Int) -> Unit,
    startRest: () -> Unit,
    resetRest: () -> Unit,
    completeSet: () -> Unit,
) {
    if (workoutState.imageBitmap != null) {
        VignetteImage(workoutState.imageBitmap.asImageBitmap())
    }
    if ((workoutState.ongoingRestProgression ?: 0f) > 0f || workoutState.settingSetValues) {
        CompleteSetAndRestScreen(
            restProgression = workoutState.ongoingRestProgression ?: 0f,
            currentRestSeconds = workoutState.ongoingRestSecs ?: 0L,
            nextSetExerciseName = if (workoutState.setsDone < workoutState.rest.size)
                workoutState.exerciseName
            else
                workoutState.nextExerciseName,
            workoutState = workoutState,
            changeReps = changeReps,
            changeWeight = changeWeight,
            changeTare = changeTare,
            skipRest = resetRest,
            completeSet = completeSet
        )
    } else {
        ExercisePage(
            workoutState,
            startRest = startRest
        )
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun ExercisePage(
    workoutState: WorkoutState,
    startRest: () -> Unit
) {
    PlayerScreen(
        mediaDisplay = {
            TextMediaDisplay(
                title = workoutState.exerciseName + " (${workoutState.setsDone + 1}/${workoutState.rest.size})",
                subtitle = workoutState.note
            )
        },
        controlButtons = {
            ControlButtonLayout(
                leftButton = {
                    MediaButton(
                        onClick = {},
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        "",
                        modifier = Modifier.fillMaxSize()
                    )
                },
                rightButton = {
                    MediaButton(
                        onClick = {},
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        "",
                        modifier = Modifier.fillMaxSize()
                    )
                },
                middleButton = {
                    AmbientAware { ambient ->
                        val shapeA = remember {
                            RoundedPolygon.star(
                                numVerticesPerRadius = 4,
                                radius = 2f,
                                innerRadius = 0.352f * 2f, // multiply by radius
                                rounding = CornerRounding(0.32f * 2f),
                            )
                        }
                        val shapeB = remember {
                            RoundedPolygon(
                                4,
                                radius = 1.4f,
                                rounding = CornerRounding(0.5f)
                            )
                        }
                        val morph = remember {
                            Morph(shapeA, shapeB)
                        }
                        val interactionSource = remember {
                            MutableInteractionSource()
                        }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val animatedProgress = animateFloatAsState(
                            targetValue = if (isPressed) 1f else 0f,
                            label = "progress",
                            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                        )
                        val background = if (ambient.isAmbient) {
                            Color.Transparent
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(45f)
                                .clip(
                                    MorphPolygonShape(morph, animatedProgress.value)
                                )
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary,
                                    MorphPolygonShape(morph, animatedProgress.value)
                                )
                                .rotate(-45f)
                                .clickable(interactionSource = interactionSource, indication = null, onClick = startRest)
                                .background(background),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Done,
                                modifier = Modifier
                                    .defaultMinSize(
                                        minWidth = ButtonDefaults.DefaultButtonSize,
                                        minHeight = ButtonDefaults.DefaultButtonSize,
                                    )
                                    .size(if (LocalConfiguration.current.isLargeScreen)
                                        38.dp
                                    else
                                        32.dp
                                    )
                                    .align(Alignment.Center),
                                contentDescription = "", // FIXME
                                tint = if (ambient.isAmbient)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            )
        },
        buttons = {
            Text("${workoutState.currentReps} x ${workoutState.weight} " +
                    if (workoutState.imperialSystem)
                        stringResource(com.agdesignes.shared.R.string.lb)
                    else
                        stringResource(com.agdesignes.shared.R.string.kg)
            )
        },
    )
}