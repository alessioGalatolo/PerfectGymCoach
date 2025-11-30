package agdesigns.elevatefitness.presentation.screens.workout.components

import agdesigns.elevatefitness.presentation.screens.common.MorphPolygonShape
import agdesigns.elevatefitness.presentation.screens.common.RoundedPolygonShape
import agdesigns.elevatefitness.presentation.screens.common.VignetteImage
import agdesigns.elevatefitness.presentation.screens.workout.ExercisesState
import agdesigns.elevatefitness.presentation.screens.workout.WorkoutState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
    exercisesState: ExercisesState,
    workoutState: WorkoutState,
    listState: ScalingLazyListState,
    changeWeight: (Int) -> Unit,
    changeReps: (Int) -> Unit,
    changeTare: (Int) -> Unit,
    startRest: () -> Unit,
    resetRest: () -> Unit,
    completeSet: () -> Unit,
    onNextExercise: () -> Unit,
    onPreviousExercise: () -> Unit,
) {
    val currentExercise = remember(workoutState.currentExerciseIndex, exercisesState.exercises) {
        exercisesState.exercises.getOrNull(workoutState.currentExerciseIndex)
            ?: exercisesState.exercises.lastOrNull()
    }
    val setsDone = remember (workoutState.currentExerciseIndex, exercisesState.exercisesSetsDone) {
        exercisesState.exercisesSetsDone.getOrNull(workoutState.currentExerciseIndex) ?: 0
    }
    val currentImage = remember(workoutState.currentExerciseIndex, exercisesState.images) {
        exercisesState.images.getOrNull(workoutState.currentExerciseIndex)
    }
    if (currentImage != null) {
        VignetteImage(currentImage.asImageBitmap(), alpha = 0.15f)
    }
    if ((workoutState.ongoingRestProgression ?: 0f) > 0f || workoutState.settingSetValues) {
        CompleteSetAndRestScreen(
            restProgression = workoutState.ongoingRestProgression ?: 1f,
            currentRestSeconds = workoutState.ongoingRestSecs ?: 0L,
            nextSetExerciseName = if (currentExercise?.let { setsDone < it.restCount } ?: false )
                currentExercise.name ?: ""
            else
                exercisesState.exercises.getOrNull(workoutState.currentExerciseIndex + 1)?.name ?: "",
            // FIXME: doesn't make much sense to pass states and values above explicitly, remove states
            workoutState = workoutState,
            exercisesState = exercisesState,
            changeReps = changeReps,
            changeWeight = changeWeight,
            changeTare = changeTare,
            skipRest = resetRest,
            completeSet = completeSet
        )
    } else {
        val exerciseName = remember(
            workoutState.currentExerciseIndex,
            exercisesState.exercises,
            exercisesState.exercisesSetsDone
        ) {
            (currentExercise?.name ?: "") + " (${setsDone + 1}/${currentExercise?.restCount ?: 0})"
        }

        ExercisePage(
            exerciseTitle = exerciseName,
            exerciseSubtitle = "${workoutState.currentReps} x ${workoutState.currentWeight} " +
                    if (exercisesState.imperialSystem)
                        stringResource(agdesignes.elevatefitness.shared.R.string.lb)
                    else
                        stringResource(agdesignes.elevatefitness.shared.R.string.kg),
            bottomText = currentExercise?.note ?: "",
            startRest = startRest,
            hasPrevious = workoutState.currentExerciseIndex > 0,
            hasNext = workoutState.currentExerciseIndex < exercisesState.exercises.size-1,
            onNext = {
                if (workoutState.currentExerciseIndex < exercisesState.exercises.size - 1) {
                    onNextExercise()
                }
            },
            onPrevious = {
                if (workoutState.currentExerciseIndex > 0) {
                    onPreviousExercise()
                }
            }

        )
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun ExercisePage(
    exerciseTitle: String,
    exerciseSubtitle: String,
    bottomText: String,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    startRest: () -> Unit
) {
    PlayerScreen(
        mediaDisplay = {
            TextMediaDisplay(
                title = exerciseTitle,
                subtitle = exerciseSubtitle  // TODO: test rendering
            )
        },
        controlButtons = {
            ControlButtonLayout(
                leftButton = {
                    MediaButton(
                        onClick = onPrevious,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        "",
                        modifier = Modifier.fillMaxSize(),
                        enabled = hasPrevious
                    )
                },
                rightButton = {
                    MediaButton(
                        onClick = onNext,
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        "",
                        modifier = Modifier.fillMaxSize(),
                        enabled = hasNext
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
            if (bottomText.isNotBlank()) {
                Text(
                    bottomText,
                    modifier = Modifier.basicMarquee()
                )
            }
        },
    )
}