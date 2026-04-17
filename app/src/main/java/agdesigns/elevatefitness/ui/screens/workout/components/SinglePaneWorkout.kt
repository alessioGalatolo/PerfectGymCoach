package agdesigns.elevatefitness.ui.screens.workout.components

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseAndInfo
import agdesigns.elevatefitness.data.db.entity.Theme
import agdesigns.elevatefitness.navigation.DestinationsNavigator
import agdesigns.elevatefitness.ui.common.FullScreenImageCard
import agdesigns.elevatefitness.ui.common.HorizontalPagerIndicator
import agdesigns.elevatefitness.ui.common.MediaPlayingState
import agdesigns.elevatefitness.ui.common.MediaViewModel
import agdesigns.elevatefitness.ui.common.SwipeableMediaPlaying
import agdesigns.elevatefitness.ui.common.SwipeableMediaPlayingDefaults
import agdesigns.elevatefitness.ui.screens.workout.CurrentExerciseState
import agdesigns.elevatefitness.ui.screens.workout.WorkoutEvent
import agdesigns.elevatefitness.ui.screens.workout.WorkoutPagesContent
import agdesigns.elevatefitness.ui.screens.workout.WorkoutState
import agdesigns.elevatefitness.ui.screens.workout.WorkoutViewModel
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.Top
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SharedTransitionScope.SinglePaneWorkout(
    animatedVisibilityScope: AnimatedVisibilityScope,
    navigator: DestinationsNavigator,
    previewExercise: ProgramExerciseAndInfo?,
    workoutState: WorkoutState,
    currentExerciseState: CurrentExerciseState,
    pagesContent: WorkoutPagesContent,
    mediaState: MediaPlayingState,
    pagerState: PagerState,
    sharedStateCard: SharedTransitionScope.SharedContentState,
    sharedStateImg: SharedTransitionScope.SharedContentState,
    containerTransitionFinished: Boolean,
    previewImageShouldDisappear: Boolean,
    mediaControlsDismissed: Boolean,
    setDismissMediaControl: (Boolean) -> Unit,
    mediaSwipeState: SwipeToDismissBoxState,
    snackbarHostState: SnackbarHostState,
    scrollState: ScrollState,
    title: @Composable (Modifier) -> Unit,
    completeWorkout: () -> Unit,
    completeSet: () -> Unit,
    onClose: () -> Unit,
    viewModel: WorkoutViewModel,
    mediaVM: MediaViewModel
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    var fabHeight by remember(mediaState.canAskAccess, mediaState.needsAccess, mediaControlsDismissed) {
        val visibleFabHeight = SwipeableMediaPlayingDefaults.totalHeight +
                16.dp // fab bottom padding
        val fabVisible = (!mediaState.needsAccess || mediaState.canAskAccess) && !mediaControlsDismissed
        mutableStateOf(if (fabVisible) visibleFabHeight else 0.dp)
    }

    // if bright image (i.e., white), change status bar icons to dark
    var brightImage by remember { mutableStateOf(false) }
    var imageHeight by remember { mutableStateOf(0.dp) }
    val systemTheme = isSystemInDarkTheme()
    val useDarkTheme by remember { derivedStateOf {
        when (workoutState.userTheme) {
            Theme.SYSTEM -> systemTheme
            Theme.LIGHT -> false
            Theme.DARK -> true
        }
    }}

    // title for top app bar, do not share bounds for animation
    val titleTopBar = @Composable {
        Text(
            currentExerciseState.exerciseTitle ?: stringResource(R.string.end_of_workout),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    FullScreenImageCard(
        animatedVisibilityScope = animatedVisibilityScope,
        sharedState = sharedStateCard,
        topAppBarNavigationIcon = { appBarShown ->
            AnimatedVisibility(
                visible = containerTransitionFinished && !currentExerciseState.isLoading,
                enter = scaleIn(MaterialTheme.motionScheme.fastSpatialSpec()),
                exit = scaleOut(MaterialTheme.motionScheme.fastSpatialSpec())
            ) {
                IconButton(
                    shapes = IconButtonDefaults.shapes(),
                    onClick = onClose,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close_icon),
                    )
                }
            }
        },
        topAppBarActions = { appBarShown ->
            AnimatedVisibility(
                visible = containerTransitionFinished
                        && !currentExerciseState.isLoading
                        && workoutState.workoutStarted,
                enter = EnterTransition.None,
                exit = ExitTransition.None
            ) {
                Row(verticalAlignment = CenterVertically) {
                    val needsDarkColor = (brightImage && !appBarShown) ||
                            (appBarShown && !useDarkTheme)
                    Text(
                        currentExerciseState.workoutTimeFormatted,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (needsDarkColor)
                            Color.Black
                        else
                            Color.White,
                        modifier = Modifier.animateEnterExit(
                            enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
                            exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec())
                        )
                    )
                    Spacer(Modifier.width(4.dp))
                    FilledIconButton(
                        onClick = {
                            completeWorkout()
                        }, shapes = IconButtonDefaults.shapes(
                            MaterialTheme.shapes.small,
                            MaterialTheme.shapes.extraLarge
                        ),
                        modifier = Modifier.animateEnterExit(
                            enter = scaleIn(MaterialTheme.motionScheme.fastSpatialSpec()),
                            exit = scaleOut(MaterialTheme.motionScheme.fastSpatialSpec())
                        )
                    ) {
                        Icon(
                            Icons.Default.DoneAll,
                            stringResource(R.string.complete_workout),
                        )
                    }
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = containerTransitionFinished && !pagerState.isScrollInProgress,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
            ) {
                WorkoutBottomBar(
                    workoutState = workoutState,
                    currentExerciseState = currentExerciseState,
                    contentPadding = WindowInsets.navigationBars.asPaddingValues() +
                            PaddingValues(horizontal = 16.dp),
                    containerColor = NavigationBarDefaults.containerColor,
                    hideMainAction = false,
                    startWorkout = {
                        scope.launch {
                            haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                        }
                        viewModel.onEvent(WorkoutEvent.StartWorkout)
                    },
                    completeWorkout = completeWorkout,
                    completeSet = completeSet,
                    addSet = {
                        scope.launch {
                            haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                        }
                        viewModel.onEvent(WorkoutEvent.AddSetToCurrentExercise)
                    },
                    goToNextExercise = {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                pagerState.currentPage + 1
                            )
                        }
                    },
                    updateReps = { value ->
                        viewModel.onEvent(WorkoutEvent.UpdateReps(value))
                    },
                    updateWeight = { value ->
                        viewModel.onEvent(
                            WorkoutEvent.UpdateWeight(
                                value
                            )
                        )
                    },
                    autoStepWeight = { newValue, equipment, decrement ->
                        viewModel.onEvent(
                            WorkoutEvent.AutoStepWeight(newValue, equipment, decrement)
                        )
                    }
                )
            }
        },
        title = titleTopBar,
        image = {
            val imageCorners = MaterialTheme.shapes.extraLarge.copy(
                topStart = ZeroCornerSize,
                topEnd = ZeroCornerSize
            )
            WorkoutExerciseImage(
                animatedVisibilityScope = animatedVisibilityScope,
                imageId = if (pagerState.currentPage == pagesContent.exercises.size)
                    R.drawable.finish_workout
                else currentExerciseState.currentExercise?.image  ?: R.drawable.finish_workout,
                sharedStateImg = sharedStateImg,
                imageCorners = imageCorners,
                previewImage = previewExercise?.image,
                previewImageShouldDisappear = previewImageShouldDisappear,
                canShowActualImage = containerTransitionFinished && !currentExerciseState.isLoading,
                showPagerIndicator = true,
                pagerState = pagerState,
                setImageIsBright = {
                    brightImage = it
                },
                setImageHeight = {
                    imageHeight = it
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        cardShape = MaterialTheme.shapes.extraLarge as RoundedCornerShape,
        floatingActionButton = {
            if (!mediaState.needsAccess || mediaState.canAskAccess) {
                AnimatedVisibility(
                    visible = containerTransitionFinished && !pagerState.isScrollInProgress && !mediaControlsDismissed,
                    enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                    exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec())
                ) {
                    SwipeableMediaPlaying(
                        onDismiss = { setDismissMediaControl(true) },
                        mediaState = mediaState,
                        swipeState = mediaSwipeState,
                        togglePlayPause = { mediaVM.togglePlayPause() },
                        playNext = { mediaVM.playNext() },
                        modifier = Modifier.padding(start = 32.dp), // weird padding as it pretends to be a fab
                        openPermissionDialog = {
                            viewModel.onEvent(WorkoutEvent.ToggleRequestNotificationAccessDialog)
                        }
                    )
                }
            }
        },
        imageHeight = imageHeight,
        brightImage = brightImage,
        darkTheme = useDarkTheme,
        scrollState = scrollState,
    ) { currentBottomPadding ->
        val progressAnim = animateFloatAsState(
            targetValue = currentExerciseState.restProgress,
            animationSpec = tween(
                500, // rest progress gets updated every 500 millis, slowly progress
                easing = LinearEasing
            ),
        )

        /*
        Bottom padding can become 0.dp when scrolling pager as it makes bottomBar disappear
        This would cause some content to shift abruptly and that is not desirable
        We thus only update currentBottomPadding is not basePadding (which should not happen normally as
        we always have a bottom bar in Workout)
         */
        var bottomPadding by remember { mutableStateOf(0.dp) }
        val basePadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LaunchedEffect(
            currentBottomPadding,
        ) {
            if (currentBottomPadding != basePadding) {
                bottomPadding = currentBottomPadding
            }
        }

        ExercisePages(
            navigator = navigator,
            horizontalPagerState = pagerState,
            currentExerciseState = currentExerciseState,
            pagesContent = pagesContent,
            previewExercise = previewExercise,
            workoutState = workoutState,
            bottomPadding = bottomPadding,
            fabHeight = fabHeight,
            restCounterProgress = progressAnim.value,
            showTitle = true,
            title = title,
            addSet = { viewModel.onEvent(WorkoutEvent.AddSetToCurrentExercise) },
            updateBottomBar = { rep, weight ->
                if (rep != null)
                    viewModel.onEvent(WorkoutEvent.UpdateReps(rep.toString()))
                else
                // this should never happen. Log it
                    Log.e("Workout", "updateBottomBar called with null rep")
                if (weight != null)
                    viewModel.onEvent(WorkoutEvent.UpdateWeight(weight.toString()))
            },
            updateValues = { a, b, c, d ->
                viewModel.onEvent(
                    WorkoutEvent.EditSetRecord(
                        reps = a,
                        weight = b,
                        exerciseInWorkout = c,
                        set = d
                    )
                )
            },
            deleteSet = { exerciseInWorkout, set ->
                viewModel.onEvent(
                    WorkoutEvent.DeleteSetRecord(
                        exerciseInWorkout,
                        set
                    )
                )
            },
            updateTare = { barbellType ->
                viewModel.onEvent(WorkoutEvent.UpdateTare(
                    barbellType.weight[false]!!,
                    barbellType
                ))
            },
            toggleOtherEquipment = { viewModel.onEvent(WorkoutEvent.ToggleOtherEquipmentDialog) },
            addExercise = { exerciseInWorkout, originalSize ->
                viewModel.onEvent(WorkoutEvent.AddExercise(exerciseInWorkout, originalSize))
            },
            changeExercise = { exerciseInWorkout, originalSize ->
                scope.launch {
                    viewModel.onEvent(
                        WorkoutEvent.ReplaceExercise(
                            exerciseInWorkout,
                            originalSize
                        )
                    )
                }
            },
            removeExercise = { viewModel.onEvent(WorkoutEvent.RemoveExercise(it)) },
            mediaControlsDismissed = !mediaState.canAskAccess || mediaControlsDismissed,
            resetMediaControlVisibility = {
                scope.launch {
                    mediaSwipeState.reset()
                    setDismissMediaControl(false)
                    mediaVM.resetCanRequestAccess()

                }
            },
            dontRequestOngoingWorkoutNotification = {
                viewModel.onEvent(
                    WorkoutEvent.DontRequestOngoingWorkoutNotification
                )
            },
            refreshPromotedNotificationAccess = {
                viewModel.onEvent(
                    WorkoutEvent.RefreshHasPromptedNotificationsAccess
                )
            },
            onAcceptSuggestion = {
                viewModel.onEvent(
                    WorkoutEvent.AcceptSuggestedModification(it)
                )
            },
            updateSetType = { page, set, type ->
                viewModel.onEvent(
                    WorkoutEvent.UpdateSetType(
                        page,
                        set,
                        type
                    )
                )
            },
            finishWorkout = completeWorkout
        )
    }
}