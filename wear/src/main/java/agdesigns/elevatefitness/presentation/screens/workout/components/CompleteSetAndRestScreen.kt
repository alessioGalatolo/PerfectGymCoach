package agdesigns.elevatefitness.presentation.screens.workout.components

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.presentation.screens.common.RoundedPolygonShape
import agdesigns.elevatefitness.presentation.screens.workout.ExercisesState
import agdesigns.elevatefitness.presentation.screens.workout.WorkoutState
import agdesigns.elevatefitness.presentation.screens.workout.WorkoutViewModel
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AnimatedText
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ButtonGroup
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.rememberAnimatedTextFontRegistry
import agdesigns.elevatefitness.shared.BarbellType
import agdesigns.elevatefitness.shared.Equipment
import agdesigns.elevatefitness.presentation.screens.common.TextHeaderWithMarquee
import agdesigns.elevatefitness.presentation.screens.workout.InRestHint
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.AlertDialogDefaults
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.ambient.AmbientAware
import com.google.android.horologist.compose.ambient.AmbientState
import com.google.android.horologist.compose.rotaryinput.accumulatedBehavior
import com.google.android.horologist.media.ui.components.ControlButtonLayout
import com.google.android.horologist.media.ui.components.display.TextMediaDisplay
import com.google.android.horologist.media.ui.screens.player.PlayerScreen
import com.google.android.horologist.media.ui.util.isLargeScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun CompleteSetAndRestScreen(
    restProgression: Float,
    currentRestSeconds: Long,
    nextSetExerciseName: String,
    exercisesState: ExercisesState,
    workoutState: WorkoutState,
    ambientState: AmbientState,
    changeReps: (Int) -> Unit,
    changeWeight: (Int) -> Unit,
    fineGrainedChangeWeight: (Int) -> Unit,
    changeTare: (Int) -> Unit,
    skipRest: () -> Unit,
    completeSet: () -> Unit,
    onDismissHint: () -> Unit
) {
    var previousRestProgression by remember { mutableFloatStateOf(restProgression) }

    // FIXME: may have weird behaviour when coming out of ambient state
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
    if (ambientState.isInteractive) {
        CircularProgressIndicator(
            progress = { animatedRestProgression.value },
            startAngle = CircularProgressIndicatorDefaults.StartAngle + 20f,  // allow for clock in up center
            endAngle = CircularProgressIndicatorDefaults.StartAngle - 20f,
            strokeWidth = CircularProgressIndicatorDefaults.smallStrokeWidth
        )
    }
    val totalPages = rememberSaveable(
        workoutState.restTimestamp,
        exercisesState.exercises,
        exercisesState.exercisesSetsDone,
        workoutState.currentExerciseIndex
    ) {
        val currentEx = exercisesState.exercises.getOrNull(workoutState.currentExerciseIndex)
        val setsDone = exercisesState.exercisesSetsDone.getOrNull(workoutState.currentExerciseIndex) ?: 0
        val equipment = Equipment.fromResKey(currentEx?.equipment)
        if (equipment == Equipment.BARBELL && setsDone == 0) // only ask about barbell during first set
            3 // we have a barbell selection page
        else
            2
    }
    // page: 0 -> select reps, 1 -> select weight, 2 -> select tare, 3 -> show rest
    var page by rememberSaveable(
        totalPages,
        workoutState.settingSetValues
    ) {
        mutableIntStateOf(
            if (workoutState.settingSetValues)
                0
            else
                totalPages
        )
    }
    AnimatedContent(
        targetState = page,
        transitionSpec = {
            (fadeIn(animationSpec = tween(300, delayMillis = 150)) +
                    scaleIn(initialScale = 0.5f, animationSpec = tween(300, delayMillis = 150)))
                .togetherWith(fadeOut(animationSpec = tween(150)))
        }
    ) { animatedPage ->
        when (animatedPage) {
            0 -> SelectValueScreen(
                title = stringResource(R.string.reps),
                subtitle = "",
                value = workoutState.currentReps.toString(),
                subValue = "",
                useArrowButtons = false,
                changeValue = changeReps,
                nextButtonText = stringResource(R.string.complete_set_next),
                ambientState = ambientState,
                onNext = {
                    page = 1
                },
            )
            1 -> SelectValueScreen(
                title = stringResource(R.string.weight),
                subtitle = "",
                value = "${workoutState.currentWeight.toInt()}",
                subValue = if ("%.2f".format(workoutState.currentWeight % 1) != "0.00")
                    "%.2f".format(workoutState.currentWeight % 1).substring(1)
                else
                    "",
                changeValue = changeWeight,
                fineGrainedChangeValue = fineGrainedChangeWeight,
                nextButtonText = if (page == totalPages-1)
                    stringResource(R.string.done_icon)
                else
                    stringResource(R.string.complete_set_next),
                useArrowButtons = false,
                ambientState = ambientState,
                onNext = {
                    page = 2
                    if (page == totalPages) {
                        completeSet()
                    }
                },
            )
            totalPages -> {
                ShowRestScreen(
                    nextSetExerciseName = nextSetExerciseName,
                    currentRestSeconds = currentRestSeconds,
                    ambientState = ambientState,
                    hints = workoutState.inRestHints,
                    onDismissHint = onDismissHint,
                    skipRest = skipRest
                )
            }
            2 -> {
                // TODO: move barbell selection to first place so that we can change the suggested
                //  weight if barbell changes
                // if barbell is other, use weight from user
                val type = BarbellType.entries[workoutState.tareIndex]
                val weight = if (type == BarbellType.OTHER)
                    workoutState.tareBarbell
                else
                    type.weight[exercisesState.imperialSystem] ?: 0f

                val weightMainValue = "%.0f".format(floor(weight))
                val weightSubValue = if ("%.2f".format(weight % 1) != "0.00")
                    "%.2f".format(weight % 1).substring(1)
                else
                    ""
                SelectValueScreen(
                    title = stringResource(R.string.barbell),
                    subtitle = stringResource(type.barbellResource),
                    value = weightMainValue,
                    subValue = weightSubValue + if (exercisesState.imperialSystem)
                        stringResource(agdesigns.elevatefitness.shared.R.string.lb)
                    else
                        stringResource(agdesigns.elevatefitness.shared.R.string.kg),
                    nextButtonText = stringResource(R.string.done_icon),
                    useArrowButtons = true,
                    ambientState = ambientState,
                    changeValue = changeTare
                ) {
                    // Do not manually change page, "completeSet" already causes to end up in rest screen
                    // also setting page here causes double animation
//                    page = totalPages
                    completeSet()
                }
            }
            else -> {}
        }
    }

}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun SelectValueScreen(
    title: String,
    subtitle: String,
    value: String,
    subValue: String,
    nextButtonText: String,
    useArrowButtons: Boolean,  // used for barbell selection
    ambientState: AmbientState,
    changeValue: (Int) -> Unit,
    fineGrainedChangeValue: (Int) -> Unit = changeValue,  // mainly used for weight
    onNext: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    // for rotary control of value
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    val scope = rememberCoroutineScope()
    // Scale down with length of value to show, this is roughly right but definitely not optimal
    val restTextStyle = when (value.length) {
        1 -> MaterialTheme.typography.numeralExtraLarge
        2 -> MaterialTheme.typography.numeralLarge
        3 -> MaterialTheme.typography.numeralMedium
        4 -> MaterialTheme.typography.numeralSmall
        else -> MaterialTheme.typography.numeralExtraSmall
    }
    val animatedTextFontRegistry =
        rememberAnimatedTextFontRegistry(
            // Variation axes at the start of the animation, width 10, weight 200
            startFontVariationSettings =
                FontVariation.Settings(FontVariation.weight(
                    restTextStyle.fontWeight?.weight ?: 700
                )),
            // Variation axes at the end of the animation, width 100, weight 500
            endFontVariationSettings =
                FontVariation.Settings(
                    FontVariation.weight(
                        (restTextStyle.fontWeight?.weight ?: 700).times(1.5f).toInt()
                    )
                ),
            startFontSize = restTextStyle.fontSize,
            endFontSize = restTextStyle.fontSize,
            textStyle = restTextStyle.copy(
                color = MaterialTheme.colorScheme.primary
            ),
        )
    val textAnimatable = remember { Animatable(0f) }
    PlayerScreen(
        mediaDisplay = {
            TextHeaderWithMarquee(
                title = title,
                subtitle = subtitle,
                ambientState = ambientState
            )
        },
        controlButtons = {
            ControlButtonLayout(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 10.dp),
                leftButton = {
                    IconButton(
                        modifier = Modifier
                            .height(IconButtonDefaults.DefaultButtonSize * 1.2f)
                            .width(IconButtonDefaults.DefaultButtonSize),
                        shapes = IconButtonDefaults.animatedShapes(),
                        colors = if (ambientState.isAmbient)
                            IconButtonDefaults.outlinedIconButtonColors()
                        else
                            IconButtonDefaults.iconButtonColors(
                                containerColor = if (!useArrowButtons)
                                    MaterialTheme.colorScheme.secondaryContainer
                                else
                                    Color.Transparent
                            ),
                        onClick = {
                            changeValue(-1)
                            scope.launch {
                                textAnimatable.animateTo(1f)
                                textAnimatable.animateTo(0f)
                            }
                        },
                    ) {
                        Icon(
                            if (useArrowButtons)
                                Icons.AutoMirrored.Filled.ArrowBack
                            else
                                Icons.Default.Remove,
                            // FIXME: contentDescription
                            contentDescription = stringResource(R.string.remove_icon_minus_reps)
                        )
                    }
                },
                middleButton = {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                            .rotaryScrollable(
                                accumulatedBehavior { value ->
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                    val delta = if (value > 0) 1 else -1
                                    fineGrainedChangeValue(delta)
                                    scope.launch {
                                        textAnimatable.animateTo(1f)
                                        textAnimatable.animateTo(0f)
                                    }
                                },
                                focusRequester = focusRequester
                            )
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            AnimatedText(
                                text = value,
                                fontRegistry = animatedTextFontRegistry,
                                progressFraction = { textAnimatable.value }
                            )
                        } else {
                            Text(
                                text = value,
                                style = restTextStyle,
                            )
                        }
                        if (subValue.isNotEmpty()) {
                            Text(
                                subValue,
                                textAlign = TextAlign.End,
                                style = MaterialTheme.typography.bodyExtraSmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 8.dp)
                                    .offset(y = (25).dp)
                            )
                        }
                    }
                },
                rightButton = {
                    IconButton(
                        modifier = Modifier
                            .height(IconButtonDefaults.DefaultButtonSize * 1.2f)
                            .width(IconButtonDefaults.DefaultButtonSize),
                        shapes = IconButtonDefaults.animatedShapes(),
                        colors = if (ambientState.isAmbient)
                            IconButtonDefaults.outlinedIconButtonColors()
                        else
                            IconButtonDefaults.iconButtonColors(
                                containerColor = if (!useArrowButtons)
                                    MaterialTheme.colorScheme.secondaryContainer
                                else
                                    Color.Transparent
                            ),
                        onClick = {
                            changeValue(1)
                            scope.launch {
                                textAnimatable.animateTo(1f)
                                textAnimatable.animateTo(0f)
                            }
                        }
                    ) {
                        Icon(
                            if (useArrowButtons)
                                Icons.AutoMirrored.Filled.ArrowForward
                            else
                                Icons.Default.Add,
                            // FIXME: contentDescription
                            contentDescription = stringResource(R.string.add_icon_plus_reps)
                        )
                    }
                }
            )

        },
        buttons = {
            EdgeButton(
                onClick = onNext,
                colors = if (ambientState.isInteractive)
                    ButtonDefaults.filledVariantButtonColors()
                else
                    ButtonDefaults.outlinedButtonColors(),
                border = if (ambientState.isInteractive)
                    null
                else
                    ButtonDefaults.outlinedButtonBorder(true),
                modifier = Modifier.fillMaxSize()
            ) {
                Text(text = nextButtonText)
            }
        }
    )
}


@OptIn(ExperimentalHorologistApi::class)
@Composable
fun ShowRestScreen(
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
        onDismissRequest = { showDialog = false },
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
                ambientState = ambientState
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