package agdesigns.elevatefitness.presentation.screens.workout.components

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.shared.R as sharedR
import agdesigns.elevatefitness.presentation.screens.common.MorphPolygonShape
import agdesigns.elevatefitness.presentation.screens.common.TextHeaderWithMarquee
import agdesigns.elevatefitness.presentation.screens.common.VignetteImage
import agdesigns.elevatefitness.presentation.screens.workout.ExercisesState
import agdesigns.elevatefitness.presentation.screens.workout.WorkoutState
import agdesigns.elevatefitness.presentation.screens.workout.WorkoutViewModel
import agdesigns.elevatefitness.shared.SetType
import agdesigns.elevatefitness.shared.grpc.Workout
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.wear.compose.foundation.AmbientMode
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.google.android.horologist.annotations.ExperimentalHorologistApi
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
    ambientMode: AmbientMode,
    acceptModification: (Int) -> Unit,
    dismissModification: (Int) -> Unit,
    startRest: () -> Unit,
    resetRest: () -> Unit,
    startExerciseTimer: () -> Unit,
    stopExerciseTimer: () -> Unit,
    onNextExercise: () -> Unit,
    onPreviousExercise: () -> Unit,
    onDismissHint: () -> Unit,
    onAddSet: () -> Unit,
    onExtendRest: () -> Unit,
) {
    val setsDone = remember (workoutState.currentExerciseIndex, exercisesState.exercisesSetsDone) {
        exercisesState.exercisesSetsDone.getOrNull(workoutState.currentExerciseIndex) ?: 0
    }
    val currentImage = remember(workoutState.currentExerciseIndex, exercisesState.images) {
        exercisesState.images.getOrNull(workoutState.currentExerciseIndex)
    }
    val currentSetType = workoutState.currentExercise?.setTypesList?.getOrNull(setsDone)?.let {
        SetType.fromResKey(it)
    } ?: SetType.NORMAL
    if (currentImage != null && ambientMode is AmbientMode.Interactive) {
        VignetteImage(
            currentImage.asImageBitmap(),
            alpha = 0.15f,
        )
    }
    if ((workoutState.ongoingRestProgression ?: 0f) > 0f && workoutState.successfullySetValues) {
        RestScreen(
            restProgression = workoutState.ongoingRestProgression ?: 1f,
            currentRestSeconds = workoutState.ongoingRestSecs ?: 0L,
            nextSetExerciseName = workoutState.nextSetExerciseName,
            ambientMode = ambientMode,
            isLastSet = workoutState.isLastSet,
            heartRate = workoutState.currentHeartRate,
            skipRest = resetRest,
            onAddSet = onAddSet,
            onExtendRest = onExtendRest,
        )
    } else {
        ExercisePage(
            heartRate = workoutState.currentHeartRate,
            exerciseName = workoutState.currentExercise?.name ?: "",
            setsDone = setsDone,
            totalSets = workoutState.currentExercise?.restCount ?: 0,
            exerciseSubtitle = if (
                currentSetType != SetType.NORMAL
            ) {
                stringResource(currentSetType.displayRes).first().uppercase() + ": "
            } else { "" }
                + "${workoutState.currentReps}" +
                    if (workoutState.currentExercise?.isDurationBased == true) {
                        "s "
                    } else { " " }
                +
                    "x ${workoutState.currentWeight}" +
                    if (exercisesState.imperialSystem)
                        stringResource(sharedR.string.lb)
                    else
                        stringResource(sharedR.string.kg),
            isSuperset = workoutState.currentExercise?.supersetExercise != 0L && exercisesState.exercises.any {
                it.programExerciseId == workoutState.currentExercise?.supersetExercise
            },
            bottomText = workoutState.currentExercise?.note ?: "",
            startRest = startRest,
            isDurationBased = workoutState.currentExercise?.isDurationBased == true,
            ongoingExercisePrepSecs = workoutState.ongoingExercisePrepSecs,
            exerciseTimerTotalSecs = workoutState.exerciseTimerTotalSecs,
            ongoingExerciseTimerSecs = workoutState.ongoingExerciseTimerSecs,
            ongoingExerciseTimerProgression = workoutState.ongoingExerciseTimerProgression,
            ongoingExerciseStopwatchSecs = workoutState.ongoingExerciseStopwatchSecs,
            startExerciseTimer = startExerciseTimer,
            stopExerciseTimer = stopExerciseTimer,
            hasPrevious = workoutState.currentExerciseIndex > 0,
            hasNext = workoutState.currentExerciseIndex < exercisesState.exercises.size - 1,
            ambientMode = ambientMode,
            modification = if (exercisesState.suggestedModifications.getOrNull(workoutState.currentExerciseIndex-1)?.type == Workout.ProtoModificationType.EXERCISE_ADDED)
                exercisesState.suggestedModifications.getOrNull(workoutState.currentExerciseIndex-1)
            else if (exercisesState.suggestedModifications.getOrNull(workoutState.currentExerciseIndex)?.type != Workout.ProtoModificationType.EXERCISE_ADDED)
                exercisesState.suggestedModifications.getOrNull(workoutState.currentExerciseIndex)
            else null,
            // this is really ugly, should compute modification in VM instead
            modificationDismissedMap = workoutState.modificationIsDismissed,
            acceptModification = {
                if (exercisesState.suggestedModifications.getOrNull(workoutState.currentExerciseIndex-1)?.type == Workout.ProtoModificationType.EXERCISE_ADDED)
                    acceptModification(workoutState.currentExerciseIndex-1)
                else if (exercisesState.suggestedModifications.getOrNull(workoutState.currentExerciseIndex)?.type != Workout.ProtoModificationType.EXERCISE_ADDED)
                    acceptModification(workoutState.currentExerciseIndex)
            },
            dismissModification = {
                if (exercisesState.suggestedModifications.getOrNull(workoutState.currentExerciseIndex-1)?.type == Workout.ProtoModificationType.EXERCISE_ADDED)
                    dismissModification(workoutState.currentExerciseIndex-1)
                else if (exercisesState.suggestedModifications.getOrNull(workoutState.currentExerciseIndex)?.type != Workout.ProtoModificationType.EXERCISE_ADDED)
                    dismissModification(workoutState.currentExerciseIndex)
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
    heartRate: Int?,
    exerciseName: String,
    setsDone: Int,
    totalSets: Int,
    exerciseSubtitle: String,
    isSuperset: Boolean,
    bottomText: String,
    hasPrevious: Boolean,
    hasNext: Boolean,
    ambientMode: AmbientMode,
    modification: Workout.ProtoSuggestedModification?,
    acceptModification: () -> Unit,
    dismissModification: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    startRest: () -> Unit,
    isDurationBased: Boolean,
    ongoingExercisePrepSecs: Long?,
    exerciseTimerTotalSecs: Long?,
    ongoingExerciseTimerSecs: Long?,
    ongoingExerciseTimerProgression: Float?,
    ongoingExerciseStopwatchSecs: Long?,
    startExerciseTimer: () -> Unit,
    stopExerciseTimer: () -> Unit,
    modificationDismissedMap: Map<Workout.ProtoSuggestedModification, Boolean>,
) {
    val isPreparing = ongoingExercisePrepSecs != null
    val timerStarted = ongoingExerciseTimerSecs != null
    val timerRunning = (ongoingExerciseTimerSecs ?: 0L) > 0L
    val stopwatchActive = ongoingExerciseStopwatchSecs != null

    var previousExerciseTimerProgression by remember {
        mutableFloatStateOf(ongoingExerciseTimerProgression ?: 1f)
    }
    val animatedExerciseTimerProgression = animateFloatAsState(
        targetValue = ongoingExerciseTimerProgression ?: 0f,
        animationSpec = if ((ongoingExerciseTimerProgression ?: 0f) > previousExerciseTimerProgression) {
            // Resetting to full, snap with no animation
            snap()
        } else {
            tween(
                WorkoutViewModel.TIME_REFRESH_DELAY_MILLIS.toInt(),
                easing = LinearEasing
            )
        }
    )
    LaunchedEffect(ongoingExerciseTimerProgression) {
        previousExerciseTimerProgression = ongoingExerciseTimerProgression ?: previousExerciseTimerProgression
    }

    Box(modifier = Modifier.fillMaxSize()) {
    PlayerScreen(
        mediaDisplay = {
            TextHeaderWithMarquee(
                title = exerciseName + " (${setsDone + 1}/$totalSets)",
                subtitle = exerciseSubtitle,
                ambientMode = ambientMode
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
                        colors = if (ambientMode is AmbientMode.Interactive)
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
                        colors = if (ambientMode is AmbientMode.Interactive)
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
                    val background = if (ambientMode is AmbientMode.Ambient) {
                        Color.Transparent
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                    Box {
                        if (isDurationBased && (timerRunning || isPreparing || stopwatchActive))  {
                            val borderColor = if (stopwatchActive){
                                Color.Green
                            } else {
                                Color.Yellow
                            }

                            if (isPreparing) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = ongoingExercisePrepSecs.toString(),
                                        style = MaterialTheme.typography.numeralExtraLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                    )
                                }

                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(background)
                                        .clickable {
                                            stopExerciseTimer()
                                            startRest()
                                        }
                                        .border(
                                            2.dp,
                                            borderColor,
                                            MaterialTheme.shapes.medium
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val displaySecs = if (stopwatchActive) {
                                        (exerciseTimerTotalSecs
                                            ?: 0L) + (ongoingExerciseStopwatchSecs ?: 0L)
                                    } else {
                                        ongoingExerciseTimerSecs ?: 0L
                                    }
                                    val minutes = displaySecs.floorDiv(60)
                                    val seconds = displaySecs.mod(60)
                                    Text(
                                        text = if (ambientMode is AmbientMode.Interactive)
                                            "%02d:%02d".format(minutes, seconds)
                                        else
                                            "%02d:--".format(minutes),
                                        style = MaterialTheme.typography.numeralExtraSmall,
                                        color = if (ambientMode is AmbientMode.Ambient)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        } else {
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
                                            if (isDurationBased && !timerStarted && !isPreparing) {
                                                startExerciseTimer()
                                            } else if (isDurationBased && stopwatchActive) {
                                                // should not get here, other branch should catch this
                                                stopExerciseTimer()
                                            } else if (!isDurationBased) {
                                                startRest()
                                            }
                                        }
                                    )
                                    .background(background),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    if (isDurationBased && !timerStarted) Icons.Default.Timer else Icons.Default.Done,
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
                                    contentDescription = stringResource(
                                        if (isDurationBased && !timerStarted)
                                            R.string.start_exercise_timer
                                        else
                                            R.string.done_icon
                                    ),
                                    tint = if (ambientMode is AmbientMode.Ambient)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                        if (isSuperset) {
                            val buttonBackground = if (ambientMode is AmbientMode.Ambient) {
                                Color.Transparent
                            } else {
                                MaterialTheme.colorScheme.secondary
                            }
                            Icon(
                                Icons.Default.Link,
                                stringResource(R.string.superset),
                                tint = if (ambientMode is AmbientMode.Ambient)
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
            val ambientAwareModifier = if (ambientMode is AmbientMode.Ambient) {
                Modifier
            } else {
                Modifier.basicMarquee()
            }
            if (isDurationBased && stopwatchActive) {
                // doing an exercise with a timer, e.g., plank
                // user is not stopping the timer, tell them how to do that
                Text(
                    stringResource(R.string.stop_timer_prompt),
                    modifier = ambientAwareModifier
                )
            } else if (bottomText.isNotBlank()) {
                Text(
                    bottomText,
                    modifier = ambientAwareModifier
                )
            } else {
                HeartRate(
                    heartRate,
                    ambientMode
                )
            }
        },
    )
        if (isDurationBased && timerStarted && ambientMode is AmbientMode.Interactive) {
            CircularProgressIndicator(
                progress = { animatedExerciseTimerProgression.value },
                startAngle = CircularProgressIndicatorDefaults.StartAngle + 20f,  // allow for clock in up center
                endAngle = CircularProgressIndicatorDefaults.StartAngle - 20f,
                strokeWidth = CircularProgressIndicatorDefaults.smallStrokeWidth
            )
        }
    }
    // we only suggest modification is the user has not done any sets yet
    if (modification != null && setsDone == 0) {
        var dialogVisible by rememberSaveable(modification) { mutableStateOf(false) }
        val haptics = LocalHapticFeedback.current
        LaunchedEffect(modification) {
            if (modificationDismissedMap[modification] != true) {
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
                    onClick = {
                        dialogVisible = false
                        dismissModification()
                    }
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