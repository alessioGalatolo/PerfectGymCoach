package agdesigns.elevatefitness.ui.screens.workout

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.shared.maybeLbToKg
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseAndInfo
import agdesigns.elevatefitness.navigation.DestinationsNavigator
import agdesigns.elevatefitness.navigation.ExercisesByMuscleDestination
import agdesigns.elevatefitness.navigation.WorkoutRecapDestination
import agdesigns.elevatefitness.ui.common.CancelWorkoutDialog
import agdesigns.elevatefitness.ui.common.EmptyScreenInfo
import agdesigns.elevatefitness.ui.common.InputOtherEquipmentDialog
import agdesigns.elevatefitness.ui.common.MediaViewModel
import agdesigns.elevatefitness.ui.common.RequestNotificationAccessDialog
import agdesigns.elevatefitness.ui.common.SharedElementKey
import agdesigns.elevatefitness.ui.common.SharedElementType
import agdesigns.elevatefitness.ui.screens.workout.components.DoublePaneWorkout
import agdesigns.elevatefitness.ui.screens.workout.components.EnterIntensityAndFinishDialog
import agdesigns.elevatefitness.ui.screens.workout.components.SinglePaneWorkout
import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun SharedTransitionScope.Workout(
    animatedVisibilityScope: AnimatedVisibilityScope,
    navigator: DestinationsNavigator,
    programId: Long = 0L,
    previewExercise: ProgramExerciseAndInfo? = null, // preview of the first exercise, used for transition
    quickStart: Boolean = false,
    resumeWorkout: Boolean = false,
    viewModel: WorkoutViewModel = hiltViewModel(),
    mediaVM: MediaViewModel = hiltViewModel()
) {
    val workoutState by viewModel.workoutState.collectAsState()
    val pagesContent by viewModel.pagesContent.collectAsState()
    val currentExerciseState by viewModel.currentExerciseState.collectAsState()
    val mediaState by mediaVM.state.collectAsStateWithLifecycle()

    // Init VM
    LaunchedEffect(programId, resumeWorkout, quickStart) {
        viewModel.onEvent(
            WorkoutEvent.InitWorkout(
                programId,
                resumeWorkout,
                quickStart
            )
        )
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current

    // for container transform animation
    val sharedStateCard = rememberSharedContentState(
        SharedElementKey(
            "Workout",
            SharedElementType.Bounds,
            idLong = programId
        )
    )
    val sharedStateImg = rememberSharedContentState(
        SharedElementKey(
            "Workout",
            SharedElementType.Image,
            idLong = programId
        )
    )
    val sharedStateTitle = rememberSharedContentState(
        SharedElementKey(
            "Workout",
            SharedElementType.Title,
            idLong = programId
        )
    )

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val onClose: () -> Unit = {
        backDispatcher?.onBackPressed()
    }
    // used to animate dialog alpha for predictive back
    var cancelWorkoutDialogProgress by rememberSaveable { mutableFloatStateOf(0f) }
    PredictiveBackHandler(
        enabled = (workoutState.startDate != null) && cancelWorkoutDialogProgress < 0.5f,
    ) { backFlow ->
        try {
            backFlow.collect { back ->
                cancelWorkoutDialogProgress = back.progress
            }
            cancelWorkoutDialogProgress = 1f
        } catch (_: CancellationException) {
            cancelWorkoutDialogProgress = 0f
        }
    }

    // FIXME: should use WorkoutEffect
    LaunchedEffect (workoutState.shutDown){
        if (workoutState.shutDown) {
            navigator.navigateUp()
            navigator.navigate(
                WorkoutRecapDestination(workoutId = workoutState.workoutId)
            )
        }
    }

    RequestNotificationAccessDialog(
        dialogIsOpen = workoutState.requestNotificationAccessDialogOpen,
        toggleDialog = { viewModel.onEvent(WorkoutEvent.ToggleRequestNotificationAccessDialog) },
        openPermissionRequest = {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            context.startActivity(intent)
        },
        dontAskAgain = {
            viewModel.onEvent(WorkoutEvent.DontRequestNotificationAgain)
        }
    )
    CancelWorkoutDialog(
        dialogueOpenProgress = cancelWorkoutDialogProgress,
        dismissDialog = { cancelWorkoutDialogProgress = 0f },
        cancelWorkout = {
            viewModel.onEvent(WorkoutEvent.CancelWorkout)
            navigator.navigateUp()
        },
        deleteData = { viewModel.onEvent(WorkoutEvent.DeleteCurrentRecords) },
        hasRecords = workoutState.hasRecordedExercise
    )
    InputOtherEquipmentDialog(
        dialogIsOpen = workoutState.otherEquipmentDialogOpen,
        toggleDialog = { viewModel.onEvent(WorkoutEvent.ToggleOtherEquipmentDialog) },
        weightUnit = if (workoutState.imperialSystem) stringResource(R.string.lb) else stringResource(
            R.string.kg
        ),
        updateTare = { tare ->
            viewModel.onEvent(
                WorkoutEvent.UpdateTare(
                    maybeLbToKg(
                        tare,
                        workoutState.imperialSystem
                    )
                )
            )
        }
    )

    val pagerState = rememberPagerState(
        initialPage = previewExercise?.orderInProgram ?: 0,
        pageCount = {
            if (workoutState.startDate != null)
                pagesContent.exercises.size+1
            else
                pagesContent.exercises.size
        }
    )

    // Collect viewModel's one-off effects without causing recomposition loops
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is WorkoutEffect.NavigateBack -> navigator.navigateUp()
                is WorkoutEffect.ShowMessage -> snackbarHostState.showSnackbar(context.getString(effect.message))
                is WorkoutEffect.ShowErrorAndBack -> {
                    snackbarHostState.showSnackbar(context.getString(effect.message))
                    navigator.navigateUp()
                }
                is WorkoutEffect.AdvancePage -> {
                    pagerState.animateScrollToPage(effect.page)
                }
            }
        }
    }

    /*
    If pager is scrolled to the top, topappbar is opaque. If pager is scrolled to a new page where
    the topappbar should be transparent, we need to manually reset the scroll.
    Otherwise the topappbar will stay opaque
     */
    val scrollState = rememberScrollState()
    // communicate with viewModel so that it know current exercise
    LaunchedEffect(pagerState.currentPage) {
        viewModel.onEvent(WorkoutEvent.UpdateCurrentPage(pagerState.currentPage))
        scrollState.animateScrollTo(0)
    }

    // title below image, share bounds for animation
    val titleModifier = if(previewExercise != null && currentExerciseState.isLoading)
        Modifier.sharedElement(
            sharedStateTitle,
            animatedVisibilityScope,
            boundsTransform = { _, _ ->
                MotionScheme.expressive().slowSpatialSpec()
            }
        )
    else Modifier

    val title = @Composable { Text(
        if (previewExercise != null && currentExerciseState.isLoading)
            previewExercise.name
        else
            currentExerciseState.exerciseTitle ?: stringResource(R.string.end_of_workout),
        overflow = TextOverflow.Ellipsis,
        maxLines = 3,
        modifier = titleModifier  // FIXME: misbehaves
    ) }

    EnterIntensityAndFinishDialog(
        dialogIsOpen = workoutState.enterIntensityDialogOpen,
        lastIntensity = workoutState.lastWorkoutIntensity,
        dismissDialog = { viewModel.onEvent(WorkoutEvent.ToggleEnterIntensityDialog) },
        completeWorkout = {
            scope.launch {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            viewModel.onEvent(WorkoutEvent.FinishWorkout(it))
        }
    )
    val completeWorkout: () -> Unit = {
        scope.launch {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(200)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        viewModel.onEvent(WorkoutEvent.ToggleEnterIntensityDialog)
    }
    val completeSet: () -> Unit = {
        scope.launch {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        }
        viewModel.onEvent(WorkoutEvent.CompleteSet)
        if ((currentExerciseState.currentExercise?.supersetExercise ?: 0L) != 0L) {
            val superExercise = pagesContent.exercises.find {
                it.extProgramExerciseId == currentExerciseState.currentExercise?.supersetExercise
            }
            if (superExercise != null) {
                if (pagesContent.exercises.indexOf(superExercise) >
                    pagesContent.exercises.indexOf(currentExerciseState.currentExercise)
                ) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }
            }
        }
    }


    var mediaControlsDismissed by rememberSaveable { mutableStateOf(false) }
    val mediaSwipeState = rememberSwipeToDismissBoxState()

    // should only show preview when transitioning *into* the workout
    var containerTransitionFinished by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(this.isTransitionActive) {
        if (!this@Workout.isTransitionActive) {
            containerTransitionFinished = true
        }
    }
    // wait a bit, then disappear preview
    var previewImageShouldDisappear by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(containerTransitionFinished) {
        if (containerTransitionFinished) {
            delay(500)
            previewImageShouldDisappear = true
        }
    }
    if (pagesContent.exercises.isNotEmpty() || (currentExerciseState.isLoading && previewExercise != null)) {
        val windowSize = currentWindowAdaptiveInfo().windowSizeClass
        if (windowSize.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)) {
            DoublePaneWorkout(
                animatedVisibilityScope = animatedVisibilityScope,
                navigator = navigator,
                previewExercise = previewExercise,
                workoutState = workoutState,
                currentExerciseState = currentExerciseState,
                pagesContent = pagesContent,
                mediaState = mediaState,
                pagerState = pagerState,
                sharedStateCard = sharedStateCard,
                sharedStateImg = sharedStateImg,
                containerTransitionFinished = containerTransitionFinished,
                previewImageShouldDisappear = previewImageShouldDisappear,
                mediaControlsDismissed = mediaControlsDismissed,
                setDismissMediaControl = { mediaControlsDismissed = it },
                mediaSwipeState = mediaSwipeState,
                snackbarHostState = snackbarHostState,
                scrollState = scrollState,
                title = title,
                completeWorkout = completeWorkout,
                completeSet = completeSet,
                onClose = onClose,
                viewModel = viewModel,
                mediaVM = mediaVM
            )
        } else {
            SinglePaneWorkout(
                animatedVisibilityScope = animatedVisibilityScope,
                navigator = navigator,
                previewExercise = previewExercise,
                workoutState = workoutState,
                currentExerciseState = currentExerciseState,
                pagesContent = pagesContent,
                mediaState = mediaState,
                pagerState = pagerState,
                sharedStateCard = sharedStateCard,
                sharedStateImg = sharedStateImg,
                containerTransitionFinished = containerTransitionFinished,
                previewImageShouldDisappear = previewImageShouldDisappear,
                mediaControlsDismissed = mediaControlsDismissed,
                setDismissMediaControl = { mediaControlsDismissed = it },
                mediaSwipeState = mediaSwipeState,
                snackbarHostState = snackbarHostState,
                scrollState = scrollState,
                title = title,
                completeWorkout = completeWorkout,
                completeSet = completeSet,
                onClose = onClose,
                viewModel = viewModel,
                mediaVM = mediaVM
            )
        }
    } else if (workoutState.workoutId != 0L){
        // program is empty, prompt to add an exercise
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { navigator.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.go_back_icon)
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            floatingActionButton = {
                val items =
                    listOf(
                        FabItemData(
                            Icons.Default.Add,
                            R.string.empty_workout_add_text,
                        ) {
                            navigator.navigate(
                                ExercisesByMuscleDestination(
                                    programName = context.getString(R.string.current_workout),
                                    workoutId = workoutState.workoutId,
                                )
                            )
                        },
                        FabItemData(
                            Icons.Default.Edit,
                            R.string.empty_workout_edit_text,

                            ) {
                            navigator.navigate(
                                ExercisesByMuscleDestination(
                                    programName = context.getString(R.string.current_and_future_workouts),  // FIXME: all workouts?
                                    workoutId = workoutState.workoutId,
                                    programId = workoutState.programId
                                )
                            )
                        }
                    )

                var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

                BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

                FloatingActionButtonMenu(
                    expanded = fabMenuExpanded,
                    button = {
                        if (fabMenuExpanded) {
                            ToggleFloatingActionButton(
                                modifier =
                                    Modifier
                                        .semantics {
                                            traversalIndex = -1f
                                            stateDescription =
                                                if (fabMenuExpanded)
                                                    context.getString(R.string.expanded)
                                                else
                                                    context.getString(R.string.collapsed)
                                            contentDescription =
                                                context.getString(R.string.toggle_menu)
                                        }
                                        .animateFloatingActionButton(
                                            visible = fabMenuExpanded,
                                            alignment = Alignment.BottomEnd,
                                        ),
                                checked = fabMenuExpanded,
                                onCheckedChange = { fabMenuExpanded = !fabMenuExpanded },
                            ) {
                                val imageVector by remember {
                                    derivedStateOf {
                                        if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add
                                    }
                                }
                                Icon(
                                    painter = rememberVectorPainter(imageVector),
                                    contentDescription = null,
                                    tint = if (fabMenuExpanded)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.animateIcon({ checkedProgress }),
                                )
                            }
                        } else {
                            // TODO: currently official API has a bug and doesn't show a button
                            //  when not expanded, this is temporary fix
                            FloatingActionButton({
                                fabMenuExpanded = true
                            }) {
                                Icon(
                                    Icons.Default.Add,
                                    stringResource(R.string.add_an_exercise_to_current_workout),
                                )
                            }
                        }
                    },
                ) {
                    items.forEachIndexed { i, item ->
                        FloatingActionButtonMenuItem(
                            modifier =
                                Modifier.semantics {
                                    isTraversalGroup = true
                                    // Add a custom a11y action to allow closing the menu when focusing
                                    // the last menu item, since the close button comes before the first
                                    // menu item in the traversal order.
                                    if (i == items.size - 1) {
                                        customActions =
                                            listOf(
                                                CustomAccessibilityAction(
                                                    label = context.getString(R.string.close_menu),
                                                    action = {
                                                        fabMenuExpanded = false
                                                        true
                                                    },
                                                )
                                            )
                                    }
                                },
                            onClick = {
                                fabMenuExpanded = false
                                item.onClick()
                            },
                            icon = { Icon(item.icon, contentDescription = null) },
                            text = { Text(text = stringResource(item.textResId)) },
                        )
                    }
                }
//                Column(
//                    verticalArrangement = Arrangement.Bottom,
//                    horizontalAlignment = Alignment.End
//                ) {
//                    SmallFloatingActionButton(onClick = {
//                    },
//                    modifier = Modifier.padding(bottom = 16.dp),
//                    containerColor = MaterialTheme.colorScheme.secondary) {
//                        Icon(Icons.Default.Edit,
//                            stringResource(R.string.add_an_exercise_to_current_and_future_workouts_of_this_program)
//                        )
//                    }
//                    MediumFloatingActionButton(onClick = {
//
//                    }) {
//                        Icon(
//                            Icons.Default.Add,
//                            stringResource(R.string.add_an_exercise_to_current_workout),
//                            modifier = Modifier.size(FloatingActionButtonDefaults.MediumIconSize)
//                        )
//                    }
//                }
            })
        { innerPadding ->
            EmptyScreenInfo(
                Icons.Default.FitnessCenter,
                R.string.empty_exercises,
                R.string.empty_exercises,
                R.string.workout_empty_exercises,
                modifier = Modifier.padding(innerPadding)
            ) {
                Text(
                    stringResource(R.string.note_empty_workout),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(40.dp)  // big padding to avoid being under fab
                )
            }
        }
    } else {
        // couldn't init workout, have a scaffold to show error then go back
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) {}
    }
}

data class FabItemData(
    val icon: ImageVector,
    val textResId: Int,
    val onClick: () -> Unit
)