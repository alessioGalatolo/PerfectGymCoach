package agdesigns.elevatefitness.ui.screens.workout

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.Top
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.core.graphics.ColorUtils
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.palette.graphics.Palette
import agdesigns.elevatefitness.R
import agdesignes.elevatefitness.shared.maybeLbToKg
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import agdesigns.elevatefitness.data.db.entity.Theme
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseAndInfo
import agdesigns.elevatefitness.navigation.FadeTransition
import agdesigns.elevatefitness.navigation.WorkoutOnlyGraph
import agdesigns.elevatefitness.ui.common.CancelWorkoutDialog
import agdesigns.elevatefitness.ui.common.EmptyScreenInfo
import agdesigns.elevatefitness.ui.common.FullScreenImageCard
import agdesigns.elevatefitness.ui.common.HorizontalPagerIndicator
import agdesigns.elevatefitness.ui.common.InputOtherEquipmentDialog
import agdesigns.elevatefitness.ui.common.MediaViewModel
import agdesigns.elevatefitness.ui.common.RequestNotificationAccessDialog
import agdesigns.elevatefitness.ui.common.SharedElementKey
import agdesigns.elevatefitness.ui.common.SharedElementType
import agdesigns.elevatefitness.ui.common.SwipeableMediaPlaying
import agdesigns.elevatefitness.ui.common.SwipeableMediaPlayingDefaults
import agdesigns.elevatefitness.ui.screens.workout.components.EnterIntensityAndFinishDialog
import agdesigns.elevatefitness.ui.screens.workout.components.ExercisePages
import agdesigns.elevatefitness.ui.screens.workout.components.WorkoutBottomBar
import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.parameters.DeepLink
import com.ramcosta.composedestinations.generated.destinations.ExercisesByMuscleDestination
import com.ramcosta.composedestinations.generated.destinations.WorkoutRecapDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

@Destination<WorkoutOnlyGraph>(
    start = true,
    style = FadeTransition::class,
    deepLinks = [
        DeepLink(uriPattern="elevatefitness://autoopenworkout"),
        DeepLink(uriPattern="elevatefitness://workout/{programId}"),
    ]
)
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
        cancelWorkout = { viewModel.onEvent(WorkoutEvent.CancelWorkout); navigator.navigateUp() },
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

    // title for top app bar, do not share bounds for animation
    val titleTopBar = @Composable {
        Text(
            currentExerciseState.exerciseTitle ?: stringResource(R.string.end_of_workout),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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

    var fabHeight by remember { mutableStateOf(0.dp) }

    // if bright image (i.e., white), change status bar icons to dark
    val brightImage = remember { mutableStateOf(false) }
    val imageWidth = with (LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    val imageHeight = imageWidth/3*2
    val systemTheme = isSystemInDarkTheme()
    val useDarkTheme by remember { derivedStateOf {
        when (workoutState.userTheme) {
            Theme.SYSTEM -> systemTheme
            Theme.LIGHT -> false
            Theme.DARK -> true
        }
    }}
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
        val currentImageId = if (pagerState.currentPage == pagesContent.exercises.size)
            R.drawable.finish_workout
        else currentExerciseState.currentExercise?.image  ?: R.drawable.finish_workout
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
                        val needsDarkColor = (brightImage.value && !appBarShown) ||
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
                                stringResource(R.string.finish),
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
                        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
                        startWorkout = {
                            scope.launch {
                                haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                            }
                            viewModel.onEvent(WorkoutEvent.StartWorkout)
                        },
                        completeWorkout = completeWorkout,
                        completeSet = {
                            // FIXME: should only call VM.onEvent, then VM should emit a side effect if superset
                            scope.launch {
                                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            }
                            viewModel.onEvent(WorkoutEvent.CompleteSet)
                            if ((currentExerciseState.currentExercise?.supersetExercise ?: 0L) != 0L) {
                                val superExercise =
                                    pagesContent.exercises.find {
                                        it.extProgramExerciseId == currentExerciseState.currentExercise?.supersetExercise
                                    }
                                if (superExercise != null) {
                                    if (pagesContent.exercises.indexOf(
                                            superExercise
                                        ) >
                                        pagesContent.exercises.indexOf(
                                            currentExerciseState.currentExercise
                                        )
                                    ) {
                                        scope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    } else {
                                        scope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    }
                                }
                            }
                        },
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
                        updateReps = { value -> viewModel.onEvent(WorkoutEvent.UpdateReps(value)) },
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
                val roundedCornersShape = MaterialTheme.shapes.extraLarge.copy(
                    topStart = ZeroCornerSize,
                    topEnd = ZeroCornerSize
                )

                // preview image is placed below (z-wise) the actual image
                // actual image will fade in and cover the preview image
                Box(Modifier
                    .wrapContentHeight(Top), contentAlignment = TopCenter) {
                    // we need to fade preview image, otherwise it will be visible everytime a new image buffers
                    AnimatedVisibility(
                        !previewImageShouldDisappear,
                        enter = EnterTransition.None,
                        exit = fadeOut(MaterialTheme.motionScheme.slowEffectsSpec())
                    ) {
                        AsyncImage(
                            ImageRequest.Builder(context)
                                .data(previewExercise?.image ?: R.drawable.finish_workout)
                                .crossfade(true)
                                .build(),
                            stringResource(R.string.exercise_image),
                            Modifier
                                .fillMaxWidth()
                                .height(imageHeight)
                                .graphicsLayer(
                                    shape = roundedCornersShape,
                                    clip = true
                                )
                                .sharedBounds(
                                    sharedStateImg,
                                    animatedVisibilityScope,
                                    clipInOverlayDuringTransition = OverlayClip(roundedCornersShape),
                                    boundsTransform = { _, _ ->
                                        MotionScheme.expressive().slowSpatialSpec()
                                    }
                                ).graphicsLayer(
                                    shape = roundedCornersShape,
                                    clip = true
                                ),
                            contentScale = ContentScale.Crop
                        )
                    }
                    AnimatedVisibility(
                        visible = containerTransitionFinished && !currentExerciseState.isLoading,
                        enter = EnterTransition.None,
                        exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec())
                    ) {
                        AsyncImage(
                            ImageRequest.Builder(context)
                                .allowHardware(false) // pixel access is not supported on Config#HARDWARE bitmaps
                                .data(currentImageId)
    //                                    .crossfade(true)
                                .listener { _, result ->
                                    val image = result.image.toBitmap()
                                    Palette.from(image).maximumColorCount(3)
                                        .clearFilters()
                                        .setRegion(0, 0, image.width,50)
                                        .generate {
                                            brightImage.value = (ColorUtils.calculateLuminance(it?.getDominantColor(Color.Black.toArgb()) ?: 0)) > 0.5
                                        }
                                }
                                .build(),
                            stringResource(R.string.exercise_image),
                            Modifier
                                .fillMaxWidth()
                                .height(imageHeight)
                                .sharedBounds(
                                    sharedStateImg,
                                    animatedVisibilityScope,
                                    clipInOverlayDuringTransition = OverlayClip(roundedCornersShape),
                                    boundsTransform = { _, _ ->
                                        MotionScheme.expressive().slowSpatialSpec()
                                    }
                                ).graphicsLayer(
                                    shape = roundedCornersShape,
                                    clip = true
                                ),
                            contentScale = ContentScale.Crop
                        )
                    }
                    AnimatedVisibility(
                        visible = containerTransitionFinished && !currentExerciseState.isLoading,
                        enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
                        exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        HorizontalPagerIndicator(
                            pagerState = pagerState,
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            cardShape = MaterialTheme.shapes.extraLarge as RoundedCornerShape,
            floatingActionButton = {
                if (!mediaState.needsAccess || mediaState.canAskAccess) {
                    val visibleFabHeight = SwipeableMediaPlayingDefaults.totalHeight +
                            16.dp // fab bottom padding
                    fabHeight = if (mediaControlsDismissed) 0.dp else visibleFabHeight
                    AnimatedVisibility(
                        visible = containerTransitionFinished && !pagerState.isScrollInProgress && !mediaControlsDismissed,
                        enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                        exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec())
                    ) {
                        SwipeableMediaPlaying(
                            onDismiss = { mediaControlsDismissed = true },
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
            brightImage = brightImage.value,
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
                title = title,
                addSet = { viewModel.onEvent(WorkoutEvent.AddSetToCurrentExercise) },
                updateExerciseProbability = { probability ->
                    scope.launch {
                        // if already snackbarring, dismiss it before a new one.
                        snackbarHostState.currentSnackbarData?.dismiss()
                        if (probability > 0)
                            snackbarHostState.showSnackbar(context.getString(R.string.increasing_exercise_probability))
                        else
                            snackbarHostState.showSnackbar(context.getString(R.string.decreasing_exercise_probability))
                    }
                    viewModel.onEvent(
                        WorkoutEvent.UpdateExerciseProbability(
                            pagerState.currentPage,
                            probability
                        )
                    )
                },
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
                updateTare = { tare -> viewModel.onEvent(WorkoutEvent.UpdateTare(tare)) },
                toggleOtherEquipment = { viewModel.onEvent(WorkoutEvent.ToggleOtherEquipmentDialog) },
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
                mediaControlsDismissed = mediaControlsDismissed,
                resetMediaControlVisibility = {
                    scope.launch {
                        mediaSwipeState.reset()
                        mediaControlsDismissed = false
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
                }
            )
        }
    } else if (workoutState.workoutId != 0L){
        Log.d("Workout", "pagesContent.exercises: ${pagesContent.exercises}, isLoading: ${currentExerciseState.isLoading}, previewExercise: $previewExercise")
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