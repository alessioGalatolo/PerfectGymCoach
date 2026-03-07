package agdesigns.elevatefitness.presentation.screens.workout

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.datastore.ShownRationaleStatus
import agdesigns.elevatefitness.presentation.screens.home.HomeViewModel
import agdesigns.elevatefitness.presentation.screens.home.components.PermissionRequiredScreen
import agdesigns.elevatefitness.presentation.screens.workout.components.EndWorkoutPage
import agdesigns.elevatefitness.presentation.screens.workout.components.LoadingWorkoutScreen
import agdesigns.elevatefitness.presentation.screens.workout.components.MediaPlayingPage
import agdesigns.elevatefitness.presentation.screens.workout.components.WorkoutPage
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.ConfirmationDialogDefaults
import androidx.wear.compose.material3.FailureConfirmationDialog
import androidx.wear.compose.material3.HorizontalPageIndicator
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.OpenOnPhoneDialog
import androidx.wear.compose.material3.OpenOnPhoneDialogDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.confirmationDialogCurvedText
import androidx.wear.compose.material3.openOnPhoneDialogCurvedText
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.horologist.compose.ambient.AmbientAware
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess


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

    DisposableEffect(Unit) {
        onDispose {
            if (!state.settingSetValues) {
                viewModel.onEvent(WorkoutEvent.StopActivity)
                terminate()
            }
        }
    }
    var nonRetriableErrorDialogShown by rememberSaveable { mutableStateOf(false) }
    var retriableErrorDialogShown by rememberSaveable { mutableStateOf(false) }

    val curvedTextStyle = ConfirmationDialogDefaults.curvedTextStyle
    val text = stringResource(R.string.non_retriable_error_with_phone)
    // FIXME: compose bug? where curved text is not entirely shown
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
            }
        }
    }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(exercisesState.activeWorkout) {
        if (!exercisesState.activeWorkout) {
            onBack()
        }
    }
    if (exercisesState.exercises.isNotEmpty()) {
        LaunchedEffect(Unit) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        val pagerState = rememberPagerState(initialPage = 1) {
            if (mediaState.title == null) 2 else 3
        }
        val text = OpenOnPhoneDialogDefaults.text
        val style = OpenOnPhoneDialogDefaults.curvedTextStyle
        var openOnPhone by remember { mutableStateOf(false) }
        AmbientAware { ambientState ->
            OpenOnPhoneDialog(
                visible = openOnPhone,
                onDismissRequest = { openOnPhone = false },
                curvedText = { openOnPhoneDialogCurvedText(text = text, style = style) }
            )
            ScreenScaffold(
                modifier = Modifier.background(Color.Transparent),
                scrollState = listState,
                timeText = {
                    if (pagerState.currentPage != 0) {
                        TimeText()
                    }
                },
            ) { contentPadding ->
                HorizontalPagerScaffold(
                    pagerState = pagerState,
                    pageIndicator = { HorizontalPageIndicator(pagerState = pagerState) },
                ) {
                    HorizontalPager(
                        pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> EndWorkoutPage(
                                contentPadding,
                                exercisesState.lastIntensity,
                                endWorkout = { intensity ->
                                    scope.launch {
                                        viewModel.onEvent(WorkoutEvent.EndWorkout(workoutIntensity = intensity))
                                        openOnPhone = true
                                        delay(OpenOnPhoneDialogDefaults.DurationMillis)
                                        delay(500L)
                                        exitProcess(0) // FIXME
                                    }
                                }
                            )

                            1 -> WorkoutPage(
                                contentPadding,
                                workoutState = state,
                                exercisesState = exercisesState,
                                listState = listState,
                                ambientState = ambientState,
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
                                navigateToSelectValues = navigateToSelectValues
                            )

                            2 -> MediaPlayingPage(
                                mediaState,
                                ambientState = ambientState,
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
        }
    } else {
        LoadingWorkoutScreen(
            onBack
        )
    }
}