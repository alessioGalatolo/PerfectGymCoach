package agdesigns.elevatefitness.presentation.screens.select_values

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.presentation.screens.common.TextHeaderWithMarquee
import agdesigns.elevatefitness.presentation.screens.common.VignetteImage
import agdesigns.elevatefitness.presentation.screens.workout.WorkoutEvent
import agdesigns.elevatefitness.presentation.screens.workout.WorkoutViewModel
import agdesigns.elevatefitness.shared.BarbellType
import agdesigns.elevatefitness.shared.Equipment
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.AmbientMode
import androidx.wear.compose.foundation.LocalAmbientModeManager
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.AnimatedText
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.rememberAnimatedTextFontRegistry
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.rotaryinput.accumulatedBehavior
import com.google.android.horologist.media.ui.components.ControlButtonLayout
import com.google.android.horologist.media.ui.screens.player.PlayerScreen
import kotlinx.coroutines.launch
import kotlin.math.floor

/**
 * Shows the circular rest progress indicator and the value selection pages (reps, weight,
 * optionally barbell tare). Used as a standalone navigation destination so the user can
 * swipe back if they entered it by mistake. After confirming values, [onComplete] is called
 * and the caller is responsible for navigating back to the workout/rest screen.
 */
@OptIn(ExperimentalHorologistApi::class)
@Composable
fun SelectValuesScreen(
    onBack: () -> Unit,
    viewModel: WorkoutViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exercisesState by viewModel.exercisesState.collectAsStateWithLifecycle()
    var previousRestProgression by remember { mutableFloatStateOf(state.ongoingRestProgression ?: 0f) }

    val currentImage = remember(state.currentExerciseIndex, exercisesState.images) {
        exercisesState.images.getOrNull(state.currentExerciseIndex)
    }
    val isDurationBased = remember(state.currentExerciseIndex, exercisesState.exercises) {
        exercisesState.exercises.getOrNull(state.currentExerciseIndex)?.isDurationBased ?: false
    }

    val animatedRestProgression = animateFloatAsState(
        targetValue = state.ongoingRestProgression ?: 0f,
        animationSpec = if ((state.ongoingRestProgression ?: 0f) > previousRestProgression) {
            snap()
        } else {
            tween(
                WorkoutViewModel.TIME_REFRESH_DELAY_MILLIS.toInt(),
                easing = LinearEasing
            )
        }
    )
    LaunchedEffect(state.ongoingRestProgression) {
        previousRestProgression = state.ongoingRestProgression ?: 0f
    }
    var valuesCompleted by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose {
            if (!valuesCompleted) {
                viewModel.onEvent(WorkoutEvent.CancelSetValues)
            }
        }
    }
    val ambientModeManager = LocalAmbientModeManager.current
    val ambientMode = ambientModeManager?.currentAmbientMode ?: AmbientMode.Interactive

    ScreenScaffold(
        timeText = { TimeText() }
    ) { _ ->
        if (currentImage != null && ambientMode is AmbientMode.Interactive) {
            VignetteImage(
                currentImage.asImageBitmap(),
                alpha = 0.15f,
            )
        }

        // totalValuePages: number of value-selection pages (2 = reps+weight, 3 = reps+weight+barbell)
        val totalValuePages = rememberSaveable(
            exercisesState.exercises,
            exercisesState.exercisesSetsDone,
            state.currentExerciseIndex
        ) {
            val currentEx =
                exercisesState.exercises.getOrNull(state.currentExerciseIndex)
            val setsDone =
                exercisesState.exercisesSetsDone.getOrNull(state.currentExerciseIndex)
                    ?: 0
            val equipment = Equipment.fromResKey(currentEx?.equipment)
            if (equipment == Equipment.BARBELL && setsDone == 0) 3 else 2
        }

        // page: 0 -> select reps, 1 -> select weight, 2 -> select barbell tare (if applicable)
        var page by rememberSaveable(totalValuePages) { mutableIntStateOf(0) }

        AnimatedContent(
            targetState = page,
            transitionSpec = {
                (fadeIn(animationSpec = tween(300, delayMillis = 150)) +
                        scaleIn(
                            initialScale = 0.5f,
                            animationSpec = tween(300, delayMillis = 150)
                        ))
                    .togetherWith(fadeOut(animationSpec = tween(150)))
            }
        ) { animatedPage ->
            when (animatedPage) {
                0 -> SelectSingleValue(
                    title = if (isDurationBased)
                        stringResource(R.string.hold)
                    else
                        stringResource(R.string.reps),
                    subtitle = "",
                    value = state.currentReps.toString(),
                    subValue = "",
                    useArrowButtons = false,
                    changeValue = {
                        viewModel.onEvent(WorkoutEvent.ChangeReps(it))
                    },
                    nextButtonText = stringResource(R.string.complete_set_next),
                    ambientMode = ambientMode,
                    onNext = { page = 1 },
                )

                1 -> SelectSingleValue(
                    title = stringResource(R.string.weight),
                    subtitle = "",
                    value = "${state.currentWeight.toInt()}",
                    subValue = if ("%.2f".format(state.currentWeight % 1) != "0.00")
                        "%.2f".format(state.currentWeight % 1).substring(1)
                    else
                        "",
                    changeValue = {
                        viewModel.onEvent(WorkoutEvent.ChangeWeight(it))
                    },
                    fineGrainedChangeValue = {
                        viewModel.onEvent(WorkoutEvent.FineGrainedChangeWeight(it))
                    },
                    nextButtonText = if (totalValuePages == 2)
                        stringResource(R.string.done_icon)
                    else
                        stringResource(R.string.complete_set_next),
                    useArrowButtons = false,
                    ambientMode = ambientMode,
                    onNext = {
                        if (totalValuePages == 2) {
                            valuesCompleted = true
                            viewModel.onEvent(WorkoutEvent.CompleteSet)
                            onBack()
                        } else {
                            page = 2
                        }
                    },
                )

                2 -> {
                    // TODO: move barbell selection to first place so that we can change the suggested
                    //  weight if barbell changes
                    val type = BarbellType.entries[state.tareIndex]
                    val weight = if (type == BarbellType.OTHER)
                        state.tareBarbell
                    else
                        type.weight[exercisesState.imperialSystem] ?: 0f

                    val weightMainValue = "%.0f".format(floor(weight))
                    val weightSubValue = if ("%.2f".format(weight % 1) != "0.00")
                        "%.2f".format(weight % 1).substring(1)
                    else
                        ""
                    SelectSingleValue(
                        title = stringResource(R.string.barbell),
                        subtitle = stringResource(type.barbellResource),
                        value = weightMainValue,
                        subValue = weightSubValue + if (exercisesState.imperialSystem)
                            stringResource(agdesigns.elevatefitness.shared.R.string.lb)
                        else
                            stringResource(agdesigns.elevatefitness.shared.R.string.kg),
                        nextButtonText = stringResource(R.string.done_icon),
                        useArrowButtons = true,
                        ambientMode = ambientMode,
                        changeValue = {
                            viewModel.onEvent(WorkoutEvent.ChangeTare(it))
                        }
                    ) {
                        valuesCompleted = true
                        viewModel.onEvent(WorkoutEvent.CompleteSet)
                        onBack()
                    }
                }

                else -> {}
            }
        }
        if (ambientMode is AmbientMode.Interactive) {
            CircularProgressIndicator(
                progress = { animatedRestProgression.value },
                startAngle = CircularProgressIndicatorDefaults.StartAngle + 20f,
                endAngle = CircularProgressIndicatorDefaults.StartAngle - 20f,
                strokeWidth = CircularProgressIndicatorDefaults.smallStrokeWidth
            )
        }
    }
}


@OptIn(ExperimentalHorologistApi::class)
@Composable
fun SelectSingleValue(
    title: String,
    subtitle: String,
    value: String,
    subValue: String,
    nextButtonText: String,
    useArrowButtons: Boolean,  // used for barbell selection
    ambientMode: AmbientMode,
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
                ambientMode = ambientMode
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
                        colors = if (ambientMode is AmbientMode.Ambient)
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
                        modifier = Modifier
                            .fillMaxSize()
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
                        colors = if (ambientMode is AmbientMode.Ambient)
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
                colors = if (ambientMode is AmbientMode.Interactive)
                    ButtonDefaults.filledVariantButtonColors()
                else
                    ButtonDefaults.outlinedButtonColors(),
                border = if (ambientMode is AmbientMode.Interactive)
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