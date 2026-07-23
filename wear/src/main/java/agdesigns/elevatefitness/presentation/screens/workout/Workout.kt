package agdesigns.elevatefitness.presentation.screens.workout

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.presentation.screens.workout.components.CalibrationScreen
import agdesigns.elevatefitness.presentation.screens.workout.components.EndWorkoutPage
import agdesigns.elevatefitness.presentation.screens.workout.components.LoadingWorkoutScreen
import agdesigns.elevatefitness.presentation.screens.workout.components.MediaPlayingPage
import agdesigns.elevatefitness.presentation.screens.workout.components.RepsTempoPage
import agdesigns.elevatefitness.presentation.screens.workout.components.WorkoutPage
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.compose.foundation.AmbientMode
import androidx.wear.compose.foundation.LocalAmbientModeManager
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.VerticalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ConfirmationDialogDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.FailureConfirmationDialog
import androidx.wear.compose.material3.HorizontalPageIndicator
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.OpenOnPhoneDialog
import androidx.wear.compose.material3.OpenOnPhoneDialogDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.confirmationDialogCurvedText
import androidx.wear.compose.material3.openOnPhoneDialogCurvedText
import androidx.wear.remote.interactions.RemoteActivityHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun Workout(
    onBack: () -> Unit,
    navigateToSelectValues: () -> Unit,
    terminate: () -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val listState = rememberScalingLazyListState()
    val exercisesState by viewModel.exercisesState.collectAsState()
    val state by viewModel.state.collectAsState()
    val mediaState by viewModel.mediaState.collectAsState()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    DisposableEffect(Unit) {
        onDispose {
            if (!state.settingSetValues) {
                scope.launch {
                    viewModel.onEvent(WorkoutEvent.StopActivity)
                    delay(500L)
                    terminate()
                }
            }
        }
    }
    var nonRetriableErrorDialogShown by rememberSaveable { mutableStateOf(false) }
    var retriableErrorDialogShown by rememberSaveable { mutableStateOf(false) }

    val curvedTextStyle = ConfirmationDialogDefaults.curvedTextStyle
    val text = stringResource(R.string.non_retriable_error_with_phone)
    FailureConfirmationDialog(
        visible = nonRetriableErrorDialogShown,
        onDismissRequest = { nonRetriableErrorDialogShown = false },
        curvedText = { confirmationDialogCurvedText(
            text = text,
            style = curvedTextStyle
        )},
    )
    AlertDialog(
        visible = retriableErrorDialogShown,
        onDismissRequest = { retriableErrorDialogShown = false },
        icon = {
            ConfirmationDialogDefaults.ConnectionFailureIcon()
        },
        title = { Text(stringResource(R.string.retriable_error_title)) },
        text = { Text(stringResource(R.string.retriable_error_info)) },
        edgeButton = {
            AlertDialogDefaults.EdgeButton(
                onClick = {
                    // Perform confirm action here
                    viewModel.onEvent(WorkoutEvent.RetrySendSetCompleted)
                    retriableErrorDialogShown = false
                },
                content = {
                    Text(stringResource(R.string.retriable_error_retry))
                }
            )
        },
    )
    AlertDialog(
        visible = state.showOtherAppExerciseDialog,
        onDismissRequest = { viewModel.onEvent(WorkoutEvent.DismissOtherAppDialog) },
        title = { Text(stringResource(R.string.other_app_exercise_title)) },
        text = { Text(stringResource(R.string.other_app_exercise_info)) },
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(
                onClick = { viewModel.onEvent(WorkoutEvent.ConfirmKillOtherApp) },
            ) {
                Icon(
                    Icons.Default.Done,
                    stringResource(R.string.other_app_exercise_confirm)
                )
            }
        },
        dismissButton = {
            AlertDialogDefaults.DismissButton(
                onClick = { viewModel.onEvent(WorkoutEvent.DismissOtherAppDialog) },
            ) {
                Icon(
                    Icons.Default.Close,
                    stringResource(R.string.close)
                )
            }
        }
    )

    // listen for VM effects
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is WorkoutEffect.RetriableError -> {
                    retriableErrorDialogShown = true
                }
                is WorkoutEffect.NonRetriableError -> {
                    nonRetriableErrorDialogShown = true
                }
                is WorkoutEffect.NavigateToSelectValues -> {
                    navigateToSelectValues()
                }
            }
        }
    }
    LaunchedEffect(exercisesState.activeWorkout) {
        if (!exercisesState.activeWorkout) {
            terminate()
        }
    }
    val showCalibrationOverlay = state.needsCalibration || state.calibrationInProgress || state.calibrationComplete
    if (exercisesState.exercises.isNotEmpty()) {
        LaunchedEffect(Unit) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        val mainPagerState = rememberPagerState(initialPage = 1) { 3 }
        val setTrackingPagerState = rememberPagerState {
            if (exercisesState.tempoRomTrackingEnabled)
                2
            else
                1
        }

        // hint at user that we have a set tracking page, but only once
        var hasHintedAtSetTrackingPager by rememberSaveable {
            mutableStateOf(false)
        }
        val isInRest = state.ongoingRestSecs?.let { it > 0L } ?: false
        LaunchedEffect(isInRest, hasHintedAtSetTrackingPager) {
            if (!hasHintedAtSetTrackingPager && setTrackingPagerState.pageCount > 1) {
                if (isInRest) {
                    delay(2000)
                    setTrackingPagerState.animateScrollToPage(1, -0.5f)
                    setTrackingPagerState.animateScrollToPage(0)
                    hasHintedAtSetTrackingPager = true
                }
            }
        }
        val text = OpenOnPhoneDialogDefaults.text
        val style = OpenOnPhoneDialogDefaults.curvedTextStyle
        var openOnPhone by remember { mutableStateOf(false) }
        val ambientModeManager = LocalAmbientModeManager.current
        val ambientMode = ambientModeManager?.currentAmbientMode ?: AmbientMode.Interactive
        OpenOnPhoneDialog(
            visible = openOnPhone,
            onDismissRequest = { openOnPhone = false },
            curvedText = { openOnPhoneDialogCurvedText(text = text, style = style) }
        )

        val hint = state.inRestHints.firstOrNull()

        AlertDialog(
            visible = state.showHintDialog && hint != null,
            onDismissRequest = {
                viewModel.onEvent(WorkoutEvent.DismissHint)
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
                EdgeButton(
                    colors = if (ambientMode is AmbientMode.Interactive)
                        ButtonDefaults.buttonColors()
                    else
                        ButtonDefaults.outlinedButtonColors(),
                    onClick = {
                        viewModel.onEvent(WorkoutEvent.DismissHint)
                    },
                    buttonSize = EdgeButtonSize.Medium,
                    border = if (ambientMode is AmbientMode.Interactive)
                        null
                    else
                        ButtonDefaults.outlinedButtonBorder(true)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = stringResource(R.string.done_icon)
                    )
                }

            }
        )
        ScreenScaffold(
            modifier = Modifier.background(Color.Transparent),
            scrollState = listState,
            timeText = {
                if (mainPagerState.currentPage != 0) {
                    TimeText()
                }
            },
        ) { contentPadding ->
            HorizontalPagerScaffold(
                pagerState = mainPagerState,
                pageIndicator = { HorizontalPageIndicator(pagerState = mainPagerState) },
            ) {
                HorizontalPager(
                    mainPagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> EndWorkoutPage(
                            contentPadding,
                            ambientMode = ambientMode,
                            lastIntensity = exercisesState.lastIntensity,
                            workoutTime = state.workoutTime,
                            heartRate = state.currentHeartRate,
                            calories = state.totalCalories,
                            endWorkout = { intensity ->
                                scope.launch {
                                    viewModel.onEvent(WorkoutEvent.EndWorkout(workoutIntensity = intensity))
                                    openOnPhone = true
                                    val openAppIntent = Intent(Intent.ACTION_VIEW).apply {
                                        addCategory(Intent.CATEGORY_BROWSABLE)
                                        data = "elevatefitness://bringtoforeground".toUri()
                                    }
                                    val remoteActivityHelper = RemoteActivityHelper(context)
                                    remoteActivityHelper.startRemoteActivity(
                                        openAppIntent,
                                        context.packageName
                                    )
                                    delay(OpenOnPhoneDialogDefaults.DurationMillis)
                                    delay(500L)
                                    terminate()
                                }
                            },
                            terminate = {
                                scope.launch {
                                    viewModel.onEvent(WorkoutEvent.StopActivity)
                                    delay(500L)
                                    terminate()
                                }
                            }
                        )

                        1 -> VerticalPager(
                            state = setTrackingPagerState,
                        ) { verticalPage ->
                            when (verticalPage) {
                                1 -> RepsTempoPage(
                                    state,
                                    setTrackingPagerState,
                                    setResult = state.lastSetResult
                                )

                                else -> WorkoutPage(
                                    contentPadding,
                                    workoutState = state,
                                    exercisesState = exercisesState,
                                    listState = listState,
                                    ambientMode = ambientMode,
                                    acceptModification = {
                                        viewModel.onEvent(WorkoutEvent.AcceptModification(it))
                                    },
                                    dismissModification = {
                                        viewModel.onEvent(WorkoutEvent.DismissModification(it))
                                    },
                                    resetRest = {
                                        viewModel.onEvent(WorkoutEvent.ResetRest)
                                    },
                                    startRest = {
                                        viewModel.onEvent(WorkoutEvent.StartRest)
                                    },
                                    onNextExercise = {
                                        viewModel.onEvent(WorkoutEvent.NextExercise)
                                    },
                                    onPreviousExercise = {
                                        viewModel.onEvent(WorkoutEvent.PreviousExercise)
                                    },
                                    onDismissHint = {
                                        viewModel.onEvent(WorkoutEvent.DismissHint)
                                    },
                                    onAddSet = {
                                        viewModel.onEvent(WorkoutEvent.AddSetToCurrentExercise)
                                    },
                                    onExtendRest = {
                                        viewModel.onEvent(WorkoutEvent.ExtendRest())
                                    },
                                )
                            }
                        }

                        2 -> MediaPlayingPage(
                            mediaState,
                            ambientMode = ambientMode,
                            onPlayPause = {
                                viewModel.onEvent(WorkoutEvent.PlayPauseMedia)
                            }, onNext = {
                                viewModel.onEvent(WorkoutEvent.NextMedia)
                            }, onPrevious = {
                                viewModel.onEvent(WorkoutEvent.PreviousMedia)
                            }, raiseVolume = {
                                viewModel.onEvent(WorkoutEvent.RaiseVolume)
                            }, lowerVolume = {
                                viewModel.onEvent(WorkoutEvent.LowerVolume)
                            }
                        )
                    }
                }
            }
        }
    } else {
        LoadingWorkoutScreen(
            onBack
        )
    }
    if (showCalibrationOverlay) {
        CalibrationScreen(
            isCalibrating = state.calibrationInProgress,
            progress = state.calibrationProgress,
            isComplete = state.calibrationComplete,
            onStart = { viewModel.onEvent(WorkoutEvent.StartCalibration) },
            onDismiss = { viewModel.onEvent(WorkoutEvent.DismissCalibration) },
        )
    }
}