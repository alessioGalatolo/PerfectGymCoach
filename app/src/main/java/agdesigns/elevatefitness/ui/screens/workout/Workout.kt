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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.palette.graphics.Palette
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.WorkoutExercise
import com.agdesignes.shared.maybeLbToKg
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import agdesigns.elevatefitness.data.db.entity.Theme
import agdesigns.elevatefitness.data.db.entity.ExerciseRecordAndEquipment
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
import agdesigns.elevatefitness.ui.screens.workout.components.ExercisePage
import agdesigns.elevatefitness.ui.screens.workout.components.WorkoutBottomBar
import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.remote.interactions.RemoteActivityHelper
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
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import kotlin.math.max

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

    // TODO: move to a repository and call from VM
    LaunchedEffect(Unit) {
        // maybe open wear os app
        val openWearIntent = Intent(Intent.ACTION_VIEW).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            setData("elevatefitnesswear://startworkout".toUri())
        }
        val remoteActivityHelper = RemoteActivityHelper(context)
        remoteActivityHelper.startRemoteActivity(openWearIntent)
    }

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

    // communicate with viewModel so that it know current exercise
    LaunchedEffect(pagerState.currentPage) {
        viewModel.onEvent(WorkoutEvent.UpdateCurrentPage(pagerState.currentPage))
    }

    // title for top app bar, do not share bounds for animation
    val titleTopBar = @Composable { Text(
        currentExerciseState.exerciseTitle ?: stringResource(R.string.end_of_workout),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    ) }
    // title below image, share bounds for animation
    val titleModifier = if(currentExerciseState.currentExercise?.extProgramExerciseId == previewExercise?.programExerciseId)
        Modifier.sharedBounds(
            sharedStateTitle,
            animatedVisibilityScope,
            boundsTransform = { _, _ ->
                MotionScheme.expressive().slowSpatialSpec()
            }
        )
    else Modifier

    val title = @Composable { Text(
        currentExerciseState.exerciseTitle ?: stringResource(R.string.end_of_workout),
        overflow = TextOverflow.Ellipsis,
        maxLines = 3,
        modifier = titleModifier  // FIXME: misbehaves
    ) }

    EnterIntensityAndFinishDialog(
        dialogIsOpen = workoutState.enterIntensityDialogOpen,
        lastIntensity = workoutState.lastWorkoutIntensity,
        dismissDialog = { viewModel.onEvent(WorkoutEvent.ToggleEnterIntensityDialog) },
        completeWorkout = { viewModel.onEvent(WorkoutEvent.FinishWorkout(it)) }
    )
    val completeWorkout: () -> Unit = {
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
    // We are animating from the previous screen but do not have the data yet. Use placeholder screen
    // to finish the animation, then show the actual screen. We need to record whether the animation
    // has finished once as, depending on how do user is interacting, it may keep going
    var animationHasFinished by remember { mutableStateOf(false) }
    animationHasFinished = animationHasFinished || !animatedVisibilityScope.transition.isRunning
    if (pagesContent.exercises.isNotEmpty() && animationHasFinished) {
        val currentImageId = if (pagerState.currentPage == pagesContent.exercises.size)
            R.drawable.finish_workout
        else currentExerciseState.currentExercise?.image  ?: R.drawable.finish_workout
        FullScreenImageCard(
            animatedVisibilityScope = animatedVisibilityScope,
            sharedState = sharedStateCard,
            snackbarHostState = snackbarHostState,
            topAppBarNavigationIcon = { appBarShown ->
                val needsDarkColor = (brightImage.value && !appBarShown) || (appBarShown && !useDarkTheme)
                IconButton(
                    shapes = IconButtonDefaults.shapes(),
                    onClick = onClose
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close_icon),
                        tint = if (needsDarkColor)
                            Color.Black
                        else
                            Color.White
                    )
                }
            },
            topAppBarActions = { appBarShown ->
                Row(verticalAlignment = CenterVertically) {
                    val needsDarkColor = (brightImage.value && !appBarShown) ||
                            (appBarShown && !useDarkTheme)
                    Text(
                        currentExerciseState.workoutTimeFormatted,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (needsDarkColor)
                            Color.Black
                        else
                            Color.White
                    )
                    if (workoutState.startDate != null) {
                        Spacer(Modifier.width(4.dp))
                        FilledIconButton(onClick = {
                            completeWorkout()
                        }, shapes = IconButtonDefaults.shapes(
                            MaterialTheme.shapes.small,
                            MaterialTheme.shapes.extraLarge
                        )) {
                            Icon(
                                Icons.Default.DoneAll,
                                stringResource(R.string.finish),
//                                tint = if (needsDarkColor)
//                                    Color.Black
//                                else
//                                    Color.White
                            )
                        }
                    }
                }
            },
            title = titleTopBar,
            image = {
                val roundedCornersShape = CardDefaults.shape
                Box(Modifier
                    .wrapContentHeight(Top), contentAlignment = TopCenter) { // TODO: add swipe
                    AsyncImage(
                        ImageRequest.Builder(context)
                            .allowHardware(false) // pixel access is not supported on Config#HARDWARE bitmaps
                            .data(currentImageId)
                            .crossfade(true)
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
                            )
                            .graphicsLayer(
                                shape = roundedCornersShape,
                                clip = true
                            ),
                        contentScale = ContentScale.Crop
                    )

                    HorizontalPagerIndicator(
                        pagerState = pagerState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                    )
                }
            },
            imageHeight = imageHeight,
            brightImage = brightImage.value,
            darkTheme = useDarkTheme,
            content = { bottomPadding ->
                val progressAnim = remember(currentExerciseState.restTimestamp) { Animatable(1f) }

                LaunchedEffect(
                    currentExerciseState.restTimestamp,
                    currentExerciseState.currentExerciseRest
                ) {
                    if (
                        currentExerciseState.restTimestamp != null &&
                        currentExerciseState.currentExerciseRest != null
                    ) {
                        progressAnim.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(
                                (currentExerciseState.currentExerciseRest!! * 1000).toInt(),
                                easing = LinearEasing
                            )
                        )
                    }
                }


                ExercisePage(
                    navigator = navigator,
                    horizontalPagerState = pagerState,
                    currentExerciseState = currentExerciseState,
                    pagesContent = pagesContent,
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
                    removeExercise = { viewModel.onEvent(WorkoutEvent.RemoveExercise(it)) }
                )
            },
            floatingActionButton = {
                if (!mediaState.needsAccess || mediaState.canAskAccess) {
                    val visibleFabHeight = SwipeableMediaPlayingDefaults.totalHeight +
                            16.dp // fab bottom padding
                    var dismissed by remember { mutableStateOf(false) }
                    fabHeight = if (dismissed) 0.dp else visibleFabHeight
                    AnimatedVisibility(
                        visible = !pagerState.isScrollInProgress && !dismissed,
                        enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                        exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec())
                    ) {
                        SwipeableMediaPlaying(
                            onDismiss = { dismissed = true },
                            state = mediaState,
                            togglePlayPause = { mediaVM.togglePlayPause() },
                            playNext = { mediaVM.playNext() },
                            modifier = Modifier.padding(start = 32.dp), // weird padding as it pretends to be a fab
                            openPermissionDialog = {
                                viewModel.onEvent(WorkoutEvent.ToggleRequestNotificationAccessDialog)
                            }
                        )
                    }
                }
            }
        ) { padding ->
            // FIXME: when becoming invisible, causes bottomPadding to become 0, thus removing the bottom
            // spacer in the exercises and a slight movement in the list
            AnimatedVisibility(
                visible = !pagerState.isScrollInProgress,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
            ) {
                WorkoutBottomBar(
                    workoutState = workoutState,
                    currentExerciseState = currentExerciseState,
                    contentPadding = padding,
                    startWorkout = { viewModel.onEvent(WorkoutEvent.StartWorkout) },
                    completeWorkout = completeWorkout,
                    completeSet = {
                        // FIXME: should only call VM.onEvent, then VM should emit a side effect if superset
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
                    addSet = { viewModel.onEvent(WorkoutEvent.AddSetToCurrentExercise) },
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
        }
    } else if (previewExercise != null) {
        // placeholder mainly used for animation
        FullScreenImageCard(
            animatedVisibilityScope = animatedVisibilityScope,
            sharedState = sharedStateCard,
            snackbarHostState = snackbarHostState,
            topAppBarNavigationIcon = { appBarShown ->
                val needsDarkColor = (brightImage.value && !appBarShown) || (appBarShown && !useDarkTheme)
                IconButton(
                    shapes = IconButtonDefaults.shapes(),
                    onClick = { /* just a placeholder, won't be clicked anyway */}) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close_icon),
                        tint = if (needsDarkColor) Color.Gray else Color.White
                    )
                }
            },
            topAppBarActions = {},
            title = { },
            image = {
                val roundedCornersShape = CardDefaults.shape
                Box(Modifier
                    .wrapContentHeight(Top), contentAlignment = TopCenter) {
                    AsyncImage(
                        ImageRequest.Builder(context)
                            .allowHardware(false)
                            .data(previewExercise.image)
                            .crossfade(true)
                            .listener { _, result ->
                                val image = result.image.toBitmap()
                                Palette.from(image).maximumColorCount(3)
                                    .clearFilters()
                                    .setRegion(0, 0, image.width, 50)
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
                            )
                            .graphicsLayer(
                                shape = roundedCornersShape,
                                clip = true // <- this ensures clipping is applied during transition
                            ),
                        contentScale = ContentScale.Crop
                    )
                }
            },
            imageHeight = imageHeight,
            brightImage = brightImage.value,
            darkTheme = useDarkTheme,
            content = { bottomPadding ->
                ExercisePage(
                    bottomPadding = bottomPadding,
                    horizontalPagerState = rememberPagerState(pageCount = { 2 }),
                    currentExerciseState = currentExerciseState,  // FIXME: this will break stuff
                    pagesContent = pagesContent,
                    workoutState = workoutState,
                    navigator = navigator,
                    fabHeight = 0.dp,
                    title = {
                        Text(
                            previewExercise.name, modifier = Modifier.sharedBounds(
                                sharedStateTitle,
                                animatedVisibilityScope,
                                boundsTransform = { _, _ ->
                                    MotionScheme.expressive().slowSpatialSpec()
                                }
                            )
                        )
                    },
                    addSet = { },
                    updateBottomBar = { _, _ -> },
                    updateValues = { _, _, _, _ -> },
                    updateTare = { },
                    toggleOtherEquipment = { },
                    changeExercise = { _, _ -> },
                    removeExercise = { },
                    restCounterProgress = null,
                    updateExerciseProbability = { _ -> }
                )
            },
            floatingActionButton = {},
            bottomBar = { _ -> }
        )
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
                                    programId = programId
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
    }
}

data class FabItemData(
    val icon: ImageVector,
    val textResId: Int,
    val onClick: () -> Unit
)