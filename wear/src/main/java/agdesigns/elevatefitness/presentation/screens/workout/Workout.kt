package agdesigns.elevatefitness.presentation.screens.workout

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.presentation.screens.workout.components.EndWorkoutPage
import agdesigns.elevatefitness.presentation.screens.workout.components.LoadingWorkoutScreen
import agdesigns.elevatefitness.presentation.screens.workout.components.MediaPlayingPage
import agdesigns.elevatefitness.presentation.screens.workout.components.WorkoutPage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.HorizontalPageIndicator
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import kotlin.system.exitProcess


@Composable
fun Workout(
    onBack: () -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    DisposableEffect(Unit) {
        onDispose {
            viewModel.onEvent(WorkoutEvent.StopActivity)
        }
    }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val workoutState by viewModel.state.collectAsState()
    val mediaState by viewModel.mediaState.collectAsState()
    val listState = rememberScalingLazyListState()
    LaunchedEffect(workoutState.workoutEnded) {
        if (workoutState.workoutEnded) {
            onBack()
        }
    }
    if (workoutState.exerciseName.isNotEmpty()) {
        LaunchedEffect(Unit) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        val pagerState = rememberPagerState(initialPage = 1) {
            if (mediaState.title == null) 2 else 3
        }
        ScreenScaffold(
            modifier = Modifier.background(Color.Transparent),
            scrollState = listState,
            timeText = { TimeText() },
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
                            endWorkout = {
                                viewModel.onEvent(WorkoutEvent.StopActivity)
                                scope.launch {
                                    delay(1000L)
                                    exitProcess(0) // FIXME
                                }
                            }
                        )
                        1 -> WorkoutPage(
                            contentPadding,
                            workoutState = workoutState,
                            listState = listState,
                            changeWeight = {
                                viewModel.onEvent(WorkoutEvent.ChangeWeight(it))
                            },
                            changeReps = {
                                viewModel.onEvent(WorkoutEvent.ChangeReps(it))
                            },
                            changeTare = {
                                viewModel.onEvent(WorkoutEvent.ChangeTare(it))
                            },
                            resetRest = {
                                viewModel.onEvent(WorkoutEvent.ResetRest)
                            },
                            startRest = {
                                viewModel.onEvent(WorkoutEvent.StartRest)
                            },
                            completeSet = {
                                viewModel.onEvent(WorkoutEvent.CompleteSet)
                            }
                        )
                        2 -> MediaPlayingPage(mediaState)
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