package agdesigns.elevatefitness.ui.screens.workout.components

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseAndInfo
import agdesigns.elevatefitness.navigation.DestinationsNavigator
import agdesigns.elevatefitness.ui.common.MediaPlayingState
import agdesigns.elevatefitness.ui.common.MediaViewModel
import agdesigns.elevatefitness.ui.common.SwipeableMediaPlaying
import agdesigns.elevatefitness.ui.common.SwipeableMediaPlayingDefaults
import agdesigns.elevatefitness.ui.screens.workout.CurrentExerciseState
import agdesigns.elevatefitness.ui.screens.workout.WorkoutEvent
import agdesigns.elevatefitness.ui.screens.workout.WorkoutPagesContent
import agdesigns.elevatefitness.ui.screens.workout.WorkoutState
import agdesigns.elevatefitness.ui.screens.workout.WorkoutViewModel
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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.FloatingToolbarExitDirection
import androidx.compose.material3.FloatingToolbarHorizontalFabPosition
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFloatingActionButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3AdaptiveApi::class,
)
@Composable
fun SharedTransitionScope.DoublePaneWorkout(
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
    title: @Composable () -> Unit,
    completeWorkout: () -> Unit,
    completeSet: () -> Unit,
    onClose: () -> Unit,
    viewModel: WorkoutViewModel,
    mediaVM: MediaViewModel
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val restProgressAnim by animateFloatAsState(
        targetValue = currentExerciseState.restProgress,
        animationSpec = tween(500, easing = LinearEasing),
    )

    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator()

    NavigableListDetailPaneScaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer),
        navigator = scaffoldNavigator,
        paneExpansionDragHandle = { state ->
            val interactionSource = remember { MutableInteractionSource() }
            VerticalDragHandle(
                modifier =
                    Modifier.paneExpansionDraggable(
                        state,
                        LocalMinimumInteractiveComponentSize.current,
                        interactionSource,
                    ),
                interactionSource = interactionSource,
            )
        },
        listPane = {
            AnimatedPane {
                // ---- LEFT PANE: exercise image + list + media ----
                Column(
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .fillMaxHeight()
                ) {
                    val currentImageId = if (pagerState.currentPage == pagesContent.exercises.size)
                        R.drawable.finish_workout
                    else
                        pagesContent.exercises.getOrNull(pagerState.currentPage)?.image
                            ?: R.drawable.finish_workout
                    var fabHeight by remember { mutableStateOf(0.dp) }
                    ElevatedCard(
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .statusBarsPadding()
                            .fillMaxWidth()
                    ) {
                        Box {
                            WorkoutExerciseImage(
                                animatedVisibilityScope = animatedVisibilityScope,
                                imageId = currentImageId,
                                sharedStateImg = sharedStateImg,
                                imageCorners = MaterialTheme.shapes.extraLarge,
                                previewImage = previewExercise?.image,
                                previewImageShouldDisappear = previewImageShouldDisappear,
                                canShowActualImage = containerTransitionFinished && !currentExerciseState.isLoading,
                                showPagerIndicator = false,
                                pagerState = pagerState,
                                setImageIsBright = {},
                                setImageHeight = {}
                            )
                            this@Column.AnimatedVisibility(
                                visible = containerTransitionFinished && !currentExerciseState.isLoading,
                                enter = scaleIn(MaterialTheme.motionScheme.fastSpatialSpec()),
                                exit = scaleOut(MaterialTheme.motionScheme.fastSpatialSpec()),
                                modifier = Modifier.padding(4.dp)
                            ) {
                                IconButton(
                                    shapes = IconButtonDefaults.shapes(),
                                    onClick = onClose,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                    )
                                ) {
                                    Icon(Icons.Filled.Close, stringResource(R.string.close_icon))
                                }
                            }
                        }
                    }
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        ProvideTextStyle(
                            value = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center)
                        ) {
                            CompositionLocalProvider(
                                content = title
                            )
                        }
                    }

                    Box {
                        LazyColumn(
                            Modifier
                                .padding(4.dp)
                                .clip(
                                    MaterialTheme.shapes.large.copy(
                                        bottomStart = CornerSize(0.dp),
                                        bottomEnd = CornerSize(0.dp)
                                    )
                                )
                                .background(MaterialTheme.colorScheme.surface)
                                .fillMaxWidth()
                        ) {
                            item {
                                Spacer(Modifier.height(16.dp))
                            }
                            itemsIndexed(pagesContent.exercises) { page, ex ->
                                val selected = page == pagerState.currentPage
                                ExerciseListItem(
                                    name = ex.name,
                                    imageModel = ex.image,
                                    selected = selected,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(page) } }
                                )
                            }
                            if (workoutState.workoutStarted) {
                                item {
                                    val selected =
                                        pagerState.currentPage == pagesContent.exercises.size
                                    Card(
                                        onClick = {
                                            scope.launch {
                                                pagerState.animateScrollToPage(pagesContent.exercises.size)
                                            }
                                        },
                                        colors = if (selected)
                                            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                        else
                                            CardDefaults.cardColors(),
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                            .fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = CenterVertically,
                                            modifier = Modifier.padding(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DoneAll,
                                                contentDescription = null,
                                                tint = if (selected)
                                                    MaterialTheme.colorScheme.onSecondaryContainer
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .padding(16.dp)
                                            )
                                            Text(
                                                stringResource(R.string.end_of_workout),
                                                modifier = Modifier
                                                    .padding(horizontal = 12.dp)
                                                    .weight(1f),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (selected)
                                                    MaterialTheme.colorScheme.onSecondaryContainer
                                                else
                                                    MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                            if (fabHeight > 0.dp) {
                                item {
                                    Spacer(Modifier.height(fabHeight))
                                }
                            }
                            item {
                                Spacer(Modifier.navigationBarsPadding())
                            }
                        }

                        // Media widget pinned at the bottom of the left pane
                        if (!mediaState.needsAccess || mediaState.canAskAccess) {
                            val visibleFabHeight = SwipeableMediaPlayingDefaults.totalHeight +
                                    16.dp // fab bottom padding
                            fabHeight = if (mediaControlsDismissed) 0.dp else visibleFabHeight
                            this@Column.AnimatedVisibility(
                                visible = containerTransitionFinished && !mediaControlsDismissed,
                                enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                                exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
                                modifier = Modifier.navigationBarsPadding().padding(8.dp).align(Alignment.BottomCenter)
                            ) {
                                SwipeableMediaPlaying(
                                    onDismiss = { setDismissMediaControl(true) },
                                    mediaState = mediaState,
                                    swipeState = mediaSwipeState,
                                    togglePlayPause = { mediaVM.togglePlayPause() },
                                    playNext = { mediaVM.playNext() },
                                    openPermissionDialog = {
                                        viewModel.onEvent(WorkoutEvent.ToggleRequestNotificationAccessDialog)
                                    },
                                    modifier = Modifier.navigationBarsPadding().padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }, detailPane = {
            AnimatedPane {
                var toolbarHeight by remember { mutableStateOf(96.dp) }
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        if (workoutState.workoutStarted) {
                            TopAppBar(
                                title = {
                                    Text(currentExerciseState.workoutTimeFormatted)
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                                actions = {
                                    AnimatedVisibility(
                                        visible = containerTransitionFinished
                                                && !currentExerciseState.isLoading
                                                && workoutState.workoutStarted,
                                        enter = EnterTransition.None,
                                        exit = ExitTransition.None
                                    ) {
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
                                                stringResource(R.string.finish),
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    },
                    floatingActionButton = {
                        AnimatedVisibility(
                            visible = containerTransitionFinished && !pagerState.isScrollInProgress,
                            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                            modifier = Modifier.padding(start = 32.dp) // only necessary when put as a fab
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                // normally, floating toolbar is 64.dp and fap is 56.dp but our toolbar
                                // is larger, so maintain that ratio
                                val density = LocalDensity.current
                                // if currentExercise is null, only show fab to complete workout
                                if (currentExerciseState.currentExercise != null && workoutState.workoutStarted) {
                                    HorizontalFloatingToolbar(
                                        expanded = false,
                                        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                                        modifier = Modifier.weight(1f)
                                            .onGloballyPositioned {
                                                with(density) {
                                                    toolbarHeight = it.size.height.toDp()
                                                }
                                            },
                                    ) {
                                        WorkoutBottomBar(
                                            workoutState = workoutState,
                                            currentExerciseState = currentExerciseState,
                                            contentPadding = PaddingValues(),
                                            containerColor = Color.Transparent,
                                            hideMainAction = true,
                                            startWorkout = {
                                                scope.launch {
                                                    haptics.performHapticFeedback(
                                                        HapticFeedbackType.ToggleOn
                                                    )
                                                }
                                                viewModel.onEvent(WorkoutEvent.StartWorkout)
                                            },
                                            completeWorkout = completeWorkout,
                                            completeSet = completeSet,
                                            addSet = {
                                                scope.launch {
                                                    haptics.performHapticFeedback(
                                                        HapticFeedbackType.ToggleOn
                                                    )
                                                }
                                                viewModel.onEvent(WorkoutEvent.AddSetToCurrentExercise)
                                            },
                                            goToNextExercise = {
                                                scope.launch {
                                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                                }
                                            },
                                            updateReps = { value ->
                                                viewModel.onEvent(
                                                    WorkoutEvent.UpdateReps(
                                                        value
                                                    )
                                                )
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
                                                    WorkoutEvent.AutoStepWeight(
                                                        newValue,
                                                        equipment,
                                                        decrement
                                                    )
                                                )
                                            }
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                }
                                if (!workoutState.workoutStarted) {
                                    // complete workout
                                    LargeFloatingActionButton({
                                        scope.launch {
                                            haptics.performHapticFeedback(
                                                HapticFeedbackType.ToggleOn
                                            )
                                        }
                                        viewModel.onEvent(WorkoutEvent.StartWorkout)
                                    }) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            stringResource(R.string.start_workout)
                                        )
                                    }
                                } else if (currentExerciseState.currentExercise == null) {
                                    // complete workout
                                    LargeFloatingActionButton(
                                        onClick = completeWorkout
                                    ) {
                                        Icon(
                                            Icons.Default.DoneAll,
                                            stringResource(R.string.complete_workout)
                                        )
                                    }
                                } else if (currentExerciseState.setsDone >= currentExerciseState.currentExercise.reps.size) {
                                    // next exercise button
                                    FloatingActionButton(
                                        modifier = Modifier.size(toolbarHeight * 56 / 64),
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        onClick = {
                                            scope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.SkipNext,
                                            stringResource(R.string.next_exercise)
                                        )
                                    }
                                } else {
                                    // complete set button
                                    FloatingActionButton(
                                        modifier = Modifier.size(toolbarHeight * 56 / 64),
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        onClick = completeSet
                                    ) {
                                        Icon(
                                            Icons.Default.Done,
                                            stringResource(R.string.complete_set)
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    // ---- RIGHT PANE: exercise content + bottom bar ----
                    Column(
                        Modifier
                            .fillMaxHeight()
                            .verticalScroll(scrollState)
                    ) {
                        Spacer(Modifier.height(innerPadding.calculateTopPadding()))
                        ExercisePages(
                            navigator = navigator,
                            horizontalPagerState = pagerState,
                            currentExerciseState = currentExerciseState,
                            pagesContent = pagesContent,
                            previewExercise = previewExercise,
                            workoutState = workoutState,
                            bottomPadding = innerPadding.calculateBottomPadding(),
                            fabHeight = toolbarHeight + 16.dp,
                            restCounterProgress = restProgressAnim,
                            showTitle = false,
                            title = title,
                            addSet = { viewModel.onEvent(WorkoutEvent.AddSetToCurrentExercise) },
                            updateBottomBar = { rep, weight ->
                                if (rep != null)
                                    viewModel.onEvent(WorkoutEvent.UpdateReps(rep.toString()))
                                else
                                    Log.e("Workout", "updateBottomBar called with null rep")
                                if (weight != null)
                                    viewModel.onEvent(WorkoutEvent.UpdateWeight(weight.toString()))
                            },
                            updateValues = { a, b, c, d ->
                                viewModel.onEvent(
                                    WorkoutEvent.EditSetRecord(
                                        a,
                                        b,
                                        c,
                                        d
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
                            updateTare = { tare ->
                                viewModel.onEvent(
                                    WorkoutEvent.UpdateTare(
                                        tare
                                    )
                                )
                            },
                            toggleOtherEquipment = {
                                viewModel.onEvent(WorkoutEvent.ToggleOtherEquipmentDialog)
                            },
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
                            removeExercise = {
                                viewModel.onEvent(
                                    WorkoutEvent.RemoveExercise(
                                        it
                                    )
                                )
                            },
                            mediaControlsDismissed = !mediaState.canAskAccess || mediaControlsDismissed,
                            resetMediaControlVisibility = {
                                scope.launch {
                                    mediaSwipeState.reset()
                                    setDismissMediaControl(false)
                                    mediaVM.resetCanRequestAccess()
                                }
                            },
                            dontRequestOngoingWorkoutNotification = {
                                viewModel.onEvent(WorkoutEvent.DontRequestOngoingWorkoutNotification)
                            },
                            refreshPromotedNotificationAccess = {
                                viewModel.onEvent(WorkoutEvent.RefreshHasPromptedNotificationsAccess)
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
                            }
                        )
                    }
                }
            }
        }
    )
}

/** A single row in the left-pane exercise list. Extracted for reuse. */
@Composable
fun ExerciseListItem(
    name: String,
    imageModel: Any?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = if (selected)
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        else
            CardDefaults.cardColors(),
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.large)
                )
                if (selected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Text(
                name,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected)
                    MaterialTheme.colorScheme.onSecondaryContainer
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
