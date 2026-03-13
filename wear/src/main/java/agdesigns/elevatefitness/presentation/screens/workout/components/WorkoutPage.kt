package agdesigns.elevatefitness.presentation.screens.workout.components

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.shared.R as sharedR
import agdesigns.elevatefitness.presentation.screens.common.MorphPolygonShape
import agdesigns.elevatefitness.presentation.screens.common.TextHeaderWithMarquee
import agdesigns.elevatefitness.presentation.screens.common.VignetteImage
import agdesigns.elevatefitness.presentation.screens.workout.ExercisesState
import agdesigns.elevatefitness.presentation.screens.workout.WorkoutState
import agdesigns.elevatefitness.shared.grpc.Workout
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Link
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.ambient.AmbientState
import com.google.android.horologist.media.ui.components.ControlButtonLayout
import com.google.android.horologist.media.ui.components.controls.MediaButton
import com.google.android.horologist.media.ui.components.controls.MediaButtonDefaults
import com.google.android.horologist.media.ui.screens.player.PlayerScreen
import com.google.android.horologist.media.ui.util.isLargeScreen
import kotlinx.coroutines.delay

@Composable
fun WorkoutPage(
    contentPadding: PaddingValues,
    exercisesState: ExercisesState,
    workoutState: WorkoutState,
    listState: ScalingLazyListState,
    ambientState: AmbientState,
    acceptModification: (Int) -> Unit,
    startRest: () -> Unit,
    resetRest: () -> Unit,
    onNextExercise: () -> Unit,
    onPreviousExercise: () -> Unit,
    onDismissHint: () -> Unit,
) {
    val setsDone = remember (workoutState.currentExerciseIndex, exercisesState.exercisesSetsDone) {
        exercisesState.exercisesSetsDone.getOrNull(workoutState.currentExerciseIndex) ?: 0
    }
    val currentImage = remember(workoutState.currentExerciseIndex, exercisesState.images) {
        exercisesState.images.getOrNull(workoutState.currentExerciseIndex)
    }
    if (currentImage != null && ambientState.isInteractive) {
        VignetteImage(
            currentImage.asImageBitmap(),
            alpha = 0.15f,
        )
    }
    if ((workoutState.ongoingRestProgression ?: 0f) > 0f && !workoutState.settingSetValues) {
        RestScreen(
            restProgression = workoutState.ongoingRestProgression ?: 1f,
            currentRestSeconds = workoutState.ongoingRestSecs ?: 0L,
            nextSetExerciseName = workoutState.nextSetExerciseName,
            ambientState = ambientState,
            hints = workoutState.inRestHints,
            skipRest = resetRest,
            onDismissHint = onDismissHint
        )
    } else if (!workoutState.settingSetValues) {
        ExercisePage(
            exerciseName = workoutState.currentExercise?.name ?: "",
            setsDone = setsDone,
            totalSets = workoutState.currentExercise?.restCount ?: 0,
            exerciseSubtitle = "${workoutState.currentReps} x ${workoutState.currentWeight} " +
                    if (exercisesState.imperialSystem)
                        stringResource(sharedR.string.lb)
                    else
                        stringResource(sharedR.string.kg),
            isSuperset = workoutState.currentExercise?.supersetExercise != 0L,
            bottomText = workoutState.currentExercise?.note ?: "",
            startRest = startRest,
            hasPrevious = workoutState.currentExerciseIndex > 0,
            hasNext = workoutState.currentExerciseIndex < exercisesState.exercises.size - 1,
            ambientState = ambientState,
            modification = if (exercisesState.suggestedModifications.getOrNull(workoutState.currentExerciseIndex-1)?.type == Workout.ProtoModificationType.EXERCISE_ADDED)
                exercisesState.suggestedModifications.getOrNull(workoutState.currentExerciseIndex-1)
            else if (exercisesState.suggestedModifications.getOrNull(workoutState.currentExerciseIndex)?.type != Workout.ProtoModificationType.EXERCISE_ADDED)
                exercisesState.suggestedModifications.getOrNull(workoutState.currentExerciseIndex)
            else null,
            acceptModification = {
                if (exercisesState.suggestedModifications.getOrNull(workoutState.currentExerciseIndex-1)?.type == Workout.ProtoModificationType.EXERCISE_ADDED)
                    acceptModification(workoutState.currentExerciseIndex-1)
                else if (exercisesState.suggestedModifications.getOrNull(workoutState.currentExerciseIndex)?.type != Workout.ProtoModificationType.EXERCISE_ADDED)
                    acceptModification(workoutState.currentExerciseIndex)
            },
            onNext = {
                if (workoutState.currentExerciseIndex < exercisesState.exercises.size - 1) {
                    onNextExercise()
                }
            },
            onPrevious = {
                if (workoutState.currentExerciseIndex > 0) {
                    onPreviousExercise()
                }
            },

        )
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun ExercisePage(
    exerciseName: String,
    setsDone: Int,
    totalSets: Int,
    exerciseSubtitle: String,
    isSuperset: Boolean,
    bottomText: String,
    hasPrevious: Boolean,
    hasNext: Boolean,
    ambientState: AmbientState,
    modification: Workout.ProtoSuggestedModification?,
    acceptModification: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    startRest: () -> Unit,
) {
    PlayerScreen(
        mediaDisplay = {
            TextHeaderWithMarquee(
                title = exerciseName + " (${setsDone + 1}/$totalSets)",
                subtitle = exerciseSubtitle,
                ambientState = ambientState
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
                        colors = if (ambientState.isInteractive)
                            MediaButtonDefaults.mediaButtonDefaultColors
                        else
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                        enabled = hasPrevious
                    )
                },
                rightButton = {
                    MediaButton(
                        onClick = onNext,
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        "",
                        modifier = Modifier.fillMaxSize(),
                        enabled = hasNext,
                        colors = if (ambientState.isInteractive)
                            MediaButtonDefaults.mediaButtonDefaultColors
                        else
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                    )
                },
                middleButton = {
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
                    val background = if (ambientState.isAmbient) {
                        Color.Transparent
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                    Box {
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
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = {
                                        startRest()
                                    }
                                )
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
                                    .size(
                                        if (LocalConfiguration.current.isLargeScreen)
                                            38.dp
                                        else
                                            32.dp
                                    )
                                    .align(Alignment.Center),
                                contentDescription = stringResource(agdesigns.elevatefitness.R.string.done_icon),
                                tint = if (ambientState.isAmbient)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        if (isSuperset) {
                            val buttonBackground = if (ambientState.isAmbient) {
                                Color.Transparent
                            } else {
                                MaterialTheme.colorScheme.secondary
                            }
                            Icon(
                                Icons.Default.Link,
                                stringResource(agdesigns.elevatefitness.R.string.superset),
                                tint = if (ambientState.isAmbient)
                                    MaterialTheme.colorScheme.secondary
                                else
                                    MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier
                                    .rotate(-45f)
                                    .align(Alignment.BottomEnd)
                                    .clip(MaterialTheme.shapes.extraLarge)
                                    .background(buttonBackground)
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
    // we only suggest modification is the user has not done any sets yet
    if (modification != null && setsDone == 0) {
        var dialogVisible by remember { mutableStateOf(false) }
        val haptics = LocalHapticFeedback.current
        LaunchedEffect(modification) {
            if (modification != null) {
                delay(2000)
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                dialogVisible = true
            }
        }
        AlertDialog(
            visible = dialogVisible,
            onDismissRequest = { dialogVisible = false },
            confirmButton = {
                AlertDialogDefaults.ConfirmButton(
                    onClick = {
                        dialogVisible = false
                        acceptModification()
                    }
                ) {
                    Icon(
                        Icons.Default.Done,
                        null
                    )
                }
            },
            title = {
                val text = when (modification.type) {
                    Workout.ProtoModificationType.EXERCISE_REPLACED -> stringResource(R.string.modification_title_replace)
                    Workout.ProtoModificationType.EXERCISE_ADDED -> stringResource(R.string.modification_title_add)
                    Workout.ProtoModificationType.EXERCISE_SKIPPED -> stringResource(R.string.modification_title_skip)
                    else -> ""
                }
                Text(
                    text,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            dismissButton = {
                AlertDialogDefaults.DismissButton(
                    onClick = { dialogVisible = false }
                ) {
                    Icon(
                        Icons.Default.Close,
                        null
                    )
                }
            },
            text = {
                val text = when (modification.type) {
                    Workout.ProtoModificationType.EXERCISE_REPLACED -> buildAnnotatedString {
                        append(stringResource(R.string.modification_desc_replace, exerciseName))
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(modification.targetExerciseName)
                        }
                        append(stringResource(R.string.modification_desc_do_it_again))
                    }
                    Workout.ProtoModificationType.EXERCISE_ADDED -> buildAnnotatedString {
                        // FIXME: add back name of previous exercise
                        append(stringResource(R.string.modification_desc_add))
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(modification.targetExerciseName)
                        }
                        append(stringResource(R.string.modification_desc_do_it_again))
                    }
                    Workout.ProtoModificationType.EXERCISE_SKIPPED -> buildAnnotatedString {
                        append(stringResource(R.string.modification_desc_skip, exerciseName))
                        append(stringResource(R.string.modification_desc_do_it_again))
                    }
                    else -> buildAnnotatedString { append("")  }
                }
                Text(text)
            },
        )
    }

}