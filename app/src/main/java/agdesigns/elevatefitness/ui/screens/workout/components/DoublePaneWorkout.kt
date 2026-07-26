package agdesigns.elevatefitness.ui.screens.workout.components

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseAndInfo
import agdesigns.elevatefitness.ui.navigation.AddExerciseDialogDestination
import agdesigns.elevatefitness.ui.navigation.CreateExerciseDialogDestination
import agdesigns.elevatefitness.ui.navigation.DestinationsNavigator
import agdesigns.elevatefitness.ui.navigation.ExerciseStatsDestination
import agdesigns.elevatefitness.ui.navigation.ExercisesByMuscleDestination
import agdesigns.elevatefitness.ui.navigation.InPaneNavigator
import agdesigns.elevatefitness.ui.navigation.ViewExercisesDestination
import agdesigns.elevatefitness.ui.common.MediaPlayingState
import agdesigns.elevatefitness.ui.common.MediaViewModel
import agdesigns.elevatefitness.ui.common.SwipeableMediaPlaying
import agdesigns.elevatefitness.ui.common.SwipeableMediaPlayingDefaults
import agdesigns.elevatefitness.ui.screens.add_exercise.AddExerciseDialog
import agdesigns.elevatefitness.ui.screens.create_exercise.CreateExerciseDialog
import agdesigns.elevatefitness.ui.screens.statistics.ExerciseStats
import agdesigns.elevatefitness.ui.screens.view_exercises.ExercisesByMuscle
import agdesigns.elevatefitness.ui.screens.view_exercises.ViewExercises
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import agdesigns.elevatefitness.ui.screens.workout.CurrentExerciseState
import agdesigns.elevatefitness.ui.screens.workout.WorkoutEvent
import agdesigns.elevatefitness.ui.screens.workout.WorkoutPagesContent
import agdesigns.elevatefitness.ui.screens.workout.WorkoutState
import agdesigns.elevatefitness.ui.screens.workout.WorkoutViewModel
import agdesigns.elevatefitness.utils.largeLandscapeDirective
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
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
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalSharedTransitionApi::class,
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
    title: @Composable (Modifier) -> Unit,
    completeWorkout: () -> Unit,
    completeSet: () -> Unit,
    onClose: () -> Unit,
    viewModel: WorkoutViewModel,
    mediaVM: MediaViewModel
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    // Right-pane navigation stack. When non-empty the top entry replaces the normal exercise
    // content. InPaneNavigator intercepts pane-eligible destinations (ExercisesByMuscle,
    // ViewExercises, AddExerciseDialog) and pushes/pops this stack instead of the global one.
    val rightPaneStack = remember { mutableStateListOf<Any>() }
    val inPaneNavigator = remember { InPaneNavigator(rightPaneStack, navigator) }

    BackHandler(enabled = rightPaneStack.isNotEmpty()) {
        inPaneNavigator.navigateUp()
    }

    val restProgressAnim by animateFloatAsState(
        targetValue = currentExerciseState.restProgress,
        animationSpec = tween(500, easing = LinearEasing),
    )
    // TODO: why not have this in ExercisePages instead?
    val exerciseTimerProgressAnim by animateFloatAsState(
        targetValue = currentExerciseState.exerciseTimerProgress,
        animationSpec = tween(500, easing = LinearEasing),
    )

    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator(
        scaffoldDirective = largeLandscapeDirective(
            currentWindowAdaptiveInfoV2()
        )
    )

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
                // The header (image + title) sits behind an opaque, bottom-sheet-like exercise
                // list. The list starts right below the header, but scrolling it up collapses
                // the header via nested scroll, letting the sheet slide on top of the image
                // instead of the image always staying pinned above the list.
                Box(
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .fillMaxHeight()
                        .clipToBounds()
                ) {
                    val currentImageId = if (pagerState.currentPage == pagesContent.exercises.size)
                        R.drawable.finish_workout
                    else
                        pagesContent.exercises.getOrNull(pagerState.currentPage)?.image
                            ?: R.drawable.finish_workout
                    var fabHeight by remember { mutableStateOf(0.dp) }
                    var headerHeightPx by remember { mutableFloatStateOf(0f) }
                    // 0f = sheet resting right below the header; -headerHeightPx = sheet fully
                    // collapsed on top of the header/image.
                    var sheetOffsetPx by remember { mutableFloatStateOf(0f) }
                    val nestedScrollConnection = remember {
                        object : NestedScrollConnection {
                            override fun onPreScroll(
                                available: Offset,
                                source: NestedScrollSource
                            ): Offset {
                                if (headerHeightPx <= 0f || available.y >= 0f) return Offset.Zero
                                val newOffset = (sheetOffsetPx + available.y).coerceIn(-headerHeightPx, 0f)
                                val consumed = newOffset - sheetOffsetPx
                                sheetOffsetPx = newOffset
                                return Offset(0f, consumed)
                            }

                            override fun onPostScroll(
                                consumed: Offset,
                                available: Offset,
                                source: NestedScrollSource
                            ): Offset {
                                if (headerHeightPx <= 0f || available.y <= 0f) return Offset.Zero
                                val newOffset = (sheetOffsetPx + available.y).coerceIn(-headerHeightPx, 0f)
                                val consumed2 = newOffset - sheetOffsetPx
                                sheetOffsetPx = newOffset
                                return Offset(0f, consumed2)
                            }
                        }
                    }

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned {
                                headerHeightPx = it.size.height.toFloat()
                            }
                    ) {
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
                                        onClick = {
                                            scope.launch {
                                                // we may have some inPaneNavigation, close all the onClose
                                                inPaneNavigator.popAllRightPanes()
                                                delay(200L)
                                                onClose()
                                            }
                                        },
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
                                    content = { title(Modifier) }
                                )
                            }
                        }
                    }

                    Box(
                        Modifier
                            .fillMaxSize()
                            .nestedScroll(nestedScrollConnection)
                    ) {
                        val sheetOffset = with(LocalDensity.current) { sheetOffsetPx.toDp() }
                        val headerHeight = with(LocalDensity.current) { headerHeightPx.toDp() }
                        LazyColumn(
                            Modifier
                                .fillMaxSize()
                                .offset(y = headerHeight + sheetOffset)
                                .clip(
                                    MaterialTheme.shapes.large.copy(
                                        bottomStart = CornerSize(0.dp),
                                        bottomEnd = CornerSize(0.dp)
                                    )
                                )
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            item {
                                Spacer(Modifier.height(16.dp))
                            }
                            exercisesOverviewItems(
                                exercises = pagesContent.exercises,
                                exerciseSetsDone = pagesContent.exerciseSetsDone,
                                currentPage = pagerState.currentPage,
                                workoutStarted = workoutState.workoutStarted,
                                onExerciseClick = { page ->
                                    scope.launch {
                                        inPaneNavigator.popAllRightPanes()
                                        pagerState.animateScrollToPage(page)
                                    }
                                },
                                onFinishClick = {
                                    inPaneNavigator.popAllRightPanes()
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagesContent.exercises.size)
                                    }
                                }
                            )
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
                            AnimatedVisibility(
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
                AnimatedContent(
                    targetState = rightPaneStack.lastOrNull(),
                    transitionSpec = { slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        MotionScheme.expressive().slowSpatialSpec()
                    ) + fadeIn(
                        MotionScheme.expressive().slowEffectsSpec()
                    ) togetherWith
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                MotionScheme.expressive().slowSpatialSpec()
                            ) + fadeOut(
                        MotionScheme.expressive().slowEffectsSpec()
                    )
                 },
                    label = "RightPaneContent"
                ) { topScreen ->
                    when (topScreen) {
                        is ExercisesByMuscleDestination -> ExercisesByMuscle(
                            animatedVisibilityScope = this@AnimatedContent,
                            navigator = inPaneNavigator,
                            programName = topScreen.programName,
                            workoutId = topScreen.workoutId,
                            returnAfterAdding = true,
                            insertAtPosition = topScreen.insertAtPosition,
                        )
                        is ViewExercisesDestination -> ViewExercises(
                            animatedVisibilityScope = this@AnimatedContent,
                            navigator = inPaneNavigator,
                            programId = topScreen.programId,
                            workoutId = topScreen.workoutId,
                            muscleOrdinal = topScreen.muscleOrdinal,
                            focusSearch = topScreen.focusSearch,
                            programName = topScreen.programName,
                            returnAfterAdding = topScreen.returnAfterAdding,
                            insertAtPosition = topScreen.insertAtPosition,
                            viewModel = hiltViewModel(key = topScreen.toString())
                        )
                        is AddExerciseDialogDestination -> AddExerciseDialog(
                            animatedVisibilityScope = this@AnimatedContent,
                            navigator = inPaneNavigator,
                            previewExercise = topScreen.previewExercise,
                            programId = topScreen.programId,
                            workoutId = topScreen.workoutId,
                            programExerciseId = topScreen.programExerciseId,
                            returnAfterAdding = topScreen.returnAfterAdding,
                            insertAtPosition = topScreen.insertAtPosition,
                            continueAdding = topScreen.continueAdding,
                            viewModel = hiltViewModel(key = topScreen.toString())
                        )
                        is CreateExerciseDialogDestination -> CreateExerciseDialog(
                            navigator = inPaneNavigator,
                            muscleOrdinal = topScreen.muscleOrdinal,
                            filterEquipment = topScreen.filterEquipment,
                            viewModel = hiltViewModel(key = topScreen.toString())
                        )
                        is ExerciseStatsDestination -> ExerciseStats(
                            navigator = inPaneNavigator,
                            exerciseId = topScreen.exerciseId,
                            viewModel = hiltViewModel(key = topScreen.toString())
                        )
                        else -> {
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
                                                            stringResource(R.string.complete_workout),
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
                                                        completeSet = completeSet,
                                                        startWorkout = {
                                                            scope.launch {
                                                                haptics.performHapticFeedback(
                                                                    HapticFeedbackType.ToggleOn
                                                                )
                                                            }
                                                            viewModel.onEvent(WorkoutEvent.StartWorkout)
                                                        },
                                                        completeWorkout = completeWorkout,
                                                        startExerciseTimer = {
                                                            viewModel.onEvent(WorkoutEvent.StartExerciseTimer)
                                                        },
                                                        stopExerciseTimer = {
                                                            viewModel.onEvent(WorkoutEvent.StopExerciseTimer)
                                                        },
                                                        resetExerciseTimer = {
                                                            viewModel.onEvent(WorkoutEvent.ResetExerciseTimer)
                                                        },
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
                                    val currentWorkoutString = stringResource(R.string.current_workout)
                                    ExercisePages(
                                        navigator = inPaneNavigator,
                                        horizontalPagerState = pagerState,
                                        currentExerciseState = currentExerciseState,
                                        pagesContent = pagesContent,
                                        previewExercise = previewExercise,
                                        workoutState = workoutState,
                                        bottomPadding = innerPadding.calculateBottomPadding(),
                                        fabHeight = toolbarHeight + 16.dp,
                                        restCounterProgress = restProgressAnim,
                                        exerciseTimerCounterProgress = exerciseTimerProgressAnim,
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
                                        updateTare = { barbellType ->
                                            viewModel.onEvent(
                                                WorkoutEvent.UpdateTare(
                                                    barbellType.weight[false]!!,
                                                    barbellType
                                                )
                                            )
                                        },
                                        toggleOtherEquipment = {
                                            viewModel.onEvent(WorkoutEvent.ToggleOtherEquipmentDialog)
                                        },
                                        addExercise = { exerciseInWorkout, originalSize ->
                                            viewModel.onEvent(WorkoutEvent.AddExercise(exerciseInWorkout, originalSize))
                                            navigator.navigate(
                                                ExercisesByMuscleDestination(
                                                    programName = currentWorkoutString,
                                                    workoutId = workoutState.workoutId,
                                                    returnAfterAdding = true,
                                                    insertAtPosition = exerciseInWorkout+1
                                                )
                                            )
                                        },
                                        changeExercise = { exerciseInWorkout, originalSize ->
                                            viewModel.onEvent(
                                                WorkoutEvent.ReplaceExercise(
                                                    exerciseInWorkout,
                                                    originalSize
                                                )
                                            )
                                            navigator.navigate(
                                                ExercisesByMuscleDestination(
                                                    programName = currentWorkoutString,
                                                    workoutId = workoutState.workoutId,
                                                    returnAfterAdding = true
                                                )
                                            )
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
                                        },
                                        finishWorkout = completeWorkout,
                                        updateUserWeight = {
                                            viewModel.onEvent(
                                                WorkoutEvent.UpdateUserWeight(
                                                    it
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
