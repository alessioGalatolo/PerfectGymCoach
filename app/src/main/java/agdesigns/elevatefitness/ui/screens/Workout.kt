package agdesigns.elevatefitness.ui.screens

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
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
import agdesigns.elevatefitness.data.workout_exercise.WorkoutExercise
import agdesigns.elevatefitness.data.workout_record.WorkoutRecord
import agdesigns.elevatefitness.ui.components.*
import agdesigns.elevatefitness.ui.maybeKgToLb
import agdesigns.elevatefitness.ui.maybeLbToKg
import agdesigns.elevatefitness.viewmodels.WorkoutEvent
import agdesigns.elevatefitness.viewmodels.WorkoutViewModel
import com.google.accompanist.pager.HorizontalPagerIndicator
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import agdesigns.elevatefitness.data.Theme
import agdesigns.elevatefitness.data.exercise.ExerciseRecordAndEquipment
import agdesigns.elevatefitness.data.exercise.ProgramExerciseAndInfo
import agdesigns.elevatefitness.service.NotificationListener
import agdesigns.elevatefitness.ui.FadeTransition
import agdesigns.elevatefitness.ui.WorkoutOnlyGraph
import agdesigns.elevatefitness.ui.hasNotificationAccess
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.media.session.PlaybackState.STATE_PLAYING
import android.os.Build
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.core.content.ContextCompat.getSystemService
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.ExercisesByMuscleDestination
import com.ramcosta.composedestinations.generated.destinations.WorkoutRecapDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import kotlin.math.max

@Destination<WorkoutOnlyGraph>(start = true, style = FadeTransition::class)
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun SharedTransitionScope.Workout(
    animatedVisibilityScope: AnimatedVisibilityScope,
    navigator: DestinationsNavigator,
    programId: Long = 0,
    previewExercise: ProgramExerciseAndInfo? = null, // preview of the first exercise, used for transition
    quickStart: Boolean = false,
    resumeWorkout: Boolean = false,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val workoutState by viewModel.state.collectAsState()
    // when exiting the screen, stop wear workout
    DisposableEffect(Unit) {
        onDispose {
            viewModel.onEvent(WorkoutEvent.InterruptWearWorkout)
        }
    }

    // for container transform animation
    val sharedStateCard = rememberSharedContentState("card_$programId")
    val sharedStateImg = rememberSharedContentState("img_$programId")
    val sharedStateTitle = rememberSharedContentState("exName_$programId")

    val snackbarHostState = remember { SnackbarHostState() }
    if (resumeWorkout)
        viewModel.onEvent(WorkoutEvent.ResumeWorkout)
    else
        viewModel.onEvent(WorkoutEvent.InitWorkout(programId))

    val scope = rememberCoroutineScope()

    val startWorkout = rememberSaveable { mutableStateOf(quickStart) }
    val context = LocalContext.current

    // request to have notification access to show music playing
    var alreadyRequestedPermission by rememberSaveable { mutableStateOf(false) }
    // Show media card and ask user if they want it with actual content
    val shouldTeaseMediaAccess by remember { derivedStateOf {
        !workoutState.cantRequestNotificationAccess
        && !hasNotificationAccess(context)
        && !alreadyRequestedPermission
    } }

    var retrieveMediaJob: Job? by remember {
        mutableStateOf(null)
    }
    var session: MediaController? by remember { mutableStateOf(null) }
    var mediaTitle: String? by remember { mutableStateOf(null) }
    var mediaAuthor: String by remember { mutableStateOf("Author not available") }
    var isPlaying: Boolean by remember { mutableStateOf(false) }
    var artworkBitmap: Bitmap? by remember { mutableStateOf(null) }
    DisposableEffect(context) {
        // FIXME: this looks like it belongs in a viewModel but the problem is the context
        // TODO: perhaps move mediaTitle/Author to viewModel and job to repository?
        retrieveMediaJob = scope.launch {
            while (true) {
                if (hasNotificationAccess(context) && session == null) {
                    val m = getSystemService(context, MediaSessionManager::class.java)!!
                    val component = ComponentName(context, NotificationListener::class.java)
                    session = m.getActiveSessions(component).filter {
                        it.metadata?.description?.title != null
                    }.getOrNull(0)
                    if (session != null) {
                        val callback = object : MediaController.Callback() {
                            override fun onPlaybackStateChanged(state: PlaybackState?) {
                                isPlaying = state?.state == STATE_PLAYING
                            }

                            override fun onMetadataChanged(metadata: MediaMetadata?) {
                                mediaTitle = metadata?.description?.title?.toString()
                                mediaAuthor = metadata?.description?.subtitle?.toString() ?: "Author not available"
                                val newBitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                                    ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                                if (newBitmap != null && !newBitmap.sameAs(artworkBitmap)) {
                                    artworkBitmap = newBitmap
                                }
                            }
                        }
                        session!!.registerCallback(callback)
                        mediaTitle = session!!.metadata!!.description.title.toString()
                        mediaAuthor = session!!.metadata!!.description.subtitle.toString()
                        artworkBitmap = session!!.metadata!!.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                            ?: session!!.metadata!!.getBitmap(MediaMetadata.METADATA_KEY_ART)
                        isPlaying = session!!.playbackState?.state == STATE_PLAYING
                    }
                }
                delay(100)
            }
        }
        onDispose {
            retrieveMediaJob?.cancel()
        }
    }
    LaunchedEffect(startWorkout.value) {
        if (startWorkout.value) {
            viewModel.onEvent(WorkoutEvent.StartWorkout)
            startWorkout.value = false
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
        dialogueIsOpen = workoutState.cancelWorkoutDialogOpen,
        toggleDialog = { viewModel.onEvent(WorkoutEvent.ToggleCancelWorkoutDialog) },
        cancelWorkout = { viewModel.onEvent(WorkoutEvent.CancelWorkout); navigator.navigateUp() },
        deleteData = { viewModel.onEvent(WorkoutEvent.DeleteCurrentRecords) },
        hasRecords = workoutState.hasRecordedExercise
    )
    InputOtherEquipmentDialog(
        dialogIsOpen = workoutState.otherEquipmentDialogOpen,
        toggleDialog = { viewModel.onEvent(WorkoutEvent.ToggleOtherEquipmentDialog) },
        weightUnit = if (workoutState.imperialSystem) "lb" else "kg",
        updateTare = { tare -> viewModel.onEvent(WorkoutEvent.UpdateTare(maybeLbToKg(tare, workoutState.imperialSystem))) }
    )

    val pagerState = rememberPagerState(
        initialPage = previewExercise?.orderInProgram ?: 0,
        pageCount = {
        if (workoutState.startDate != null)
            workoutState.workoutExercises.size+1
        else
            workoutState.workoutExercises.size
    })
    // communicate with viewModel so that it know current exercise
    // FIXME: wouldn't it be easier to use currentExercise?
    LaunchedEffect(pagerState.currentPage, workoutState.workoutExercises) {
        viewModel.onEvent(WorkoutEvent.UpdateCurrentPage(pagerState.currentPage))
    }
    val currentExercise: WorkoutExercise? by remember {
        derivedStateOf {
            if (pagerState.currentPage < workoutState.workoutExercises.size) {
                workoutState.workoutExercises[pagerState.currentPage]
            } else {
                null
            }
        }
    }

    val workoutTimeMillis by remember {
        derivedStateOf {
            workoutState.startDate?.toInstant()?.toEpochMilli()?.let {
                workoutState.currentTime.toInstant().toEpochMilli() - it
            } ?: 0L
        }
    }
    val timer = {" " + if (workoutTimeMillis > 0L) DateUtils.formatElapsedTime(workoutTimeMillis / 1000) else "" }


    val title = @Composable { Text(
        currentExercise?.name?.plus(currentExercise?.variation) ?: "End of workout",
        overflow = TextOverflow.Ellipsis,
//        maxLines = 1
    ) }

    val currentExerciseRecord by remember { derivedStateOf {
        if (pagerState.currentPage < workoutState.workoutExercises.size)
            workoutState.allRecords[
                    workoutState.workoutExercises[pagerState.currentPage].extExerciseId
            ] ?: emptyList()
        else
            emptyList()
    }}

    // record being set right now for current exercise
    val ongoingRecord by remember { derivedStateOf {
        currentExerciseRecord.find {
            it.extWorkoutId == workoutState.workoutId && it.exerciseInWorkout == pagerState.currentPage
        }
    }}

    // records for current exercise minus ongoingRecord
    val recordsToDisplay by remember { derivedStateOf {
        if (ongoingRecord != null)
            currentExerciseRecord.minus(ongoingRecord!!).sortedByDescending { it.date }
        else
            currentExerciseRecord.sortedByDescending { it.date }
    }}

    val setsDone = remember { derivedStateOf{
        ongoingRecord?.reps?.size ?: 0
    }}

    LaunchedEffect(setsDone.value){
        // update viewModel so that it can be transmitted to wear os
        viewModel.onEvent(WorkoutEvent.UpdateSetsDone(setsDone.value))
    }
    LaunchedEffect(currentExercise, setsDone){
        if (currentExercise != null && setsDone.value < currentExercise!!.reps.size) {
            viewModel.onEvent(
                WorkoutEvent.UpdateReps(
                    currentExercise!!.reps[setsDone.value].toString()
                )
            )
        }
    }

    LaunchedEffect(workoutState.allRecords, pagerState.currentPage, setsDone){
        val currentRecord = recordsToDisplay.firstOrNull()

        if (currentRecord != null) {
            if (setsDone.value > 0) {
                viewModel.onEvent(
                    WorkoutEvent.UpdateWeight(
                        maybeKgToLb(
                            ongoingRecord!!.weights[setsDone.value - 1],
                            workoutState.imperialSystem
                        ).toString()
                    )
                )
                viewModel.onEvent(
                    WorkoutEvent.UpdateTare(
                        ongoingRecord!!.tare
                    )
                )
            } else {
//            val index = min(setsDone.value, currentRecord.weights.size - 1)
                viewModel.onEvent(
                    WorkoutEvent.UpdateWeight(
                        maybeKgToLb(
                            currentRecord.weights[0],
                            workoutState.imperialSystem
                        ).toString()
                    )
                )
                viewModel.onEvent(WorkoutEvent.UpdateTare(
                    currentRecord.tare
                ))
            }
        } else {
            viewModel.onEvent(WorkoutEvent.UpdateTare(
                0f
            ))
        }
    }

    val onClose = {
        if (workoutState.startDate == null)
            navigator.navigateUp()
        else
            viewModel.onEvent(WorkoutEvent.ToggleCancelWorkoutDialog)
        Unit
    }

    // TODO: instead of having the use select the intensity in the last page, have a slider in a dialog that pops up
    val workoutIntensity = rememberSaveable { mutableStateOf(WorkoutRecord.WorkoutIntensity.NORMAL_INTENSITY) }

    val completeWorkout: () -> Unit = {
        viewModel.onEvent(WorkoutEvent.FinishWorkout(workoutIntensity.value))
    }

    val pagerPageCount by remember { derivedStateOf {
        if (workoutState.startDate != null)
            workoutState.workoutExercises.size+1
        else
            workoutState.workoutExercises.size
    }}

    var fabHeight by remember { mutableStateOf(0.dp) }

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
    if (workoutState.workoutExercises.isNotEmpty() && !animatedVisibilityScope.transition.isRunning) {
        BackHandler(onBack = onClose)
        val currentImageId by remember { derivedStateOf {
            if (pagerState.currentPage == workoutState.workoutExercises.size)
                R.drawable.finish_workout
            else currentExercise!!.image
        }}
        FullScreenImageCard(
            animatedVisibilityScope = animatedVisibilityScope,
            sharedState = sharedStateCard,
            snackbarHostState = snackbarHostState,
            topAppBarNavigationIcon = { appBarShown ->
                val needsDarkColor = (brightImage.value && !appBarShown) || (appBarShown && !useDarkTheme)
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = if (needsDarkColor) Color.Gray else Color.White
                    )
                }
            },
            topAppBarActions = { appBarShown ->
                Row(verticalAlignment = CenterVertically) {
                    val needsDarkColor = (brightImage.value && !appBarShown) ||
                            (appBarShown && !useDarkTheme)
                    Text(timer(), style = MaterialTheme.typography.titleLarge,
                        color = if (needsDarkColor) Color.Black else Color.White)  // FIXME should use default colors
                    if (workoutState.startDate != null) {
                        TextButton(onClick = {
                        if (pagerState.currentPage == pagerPageCount-1)
                            completeWorkout()
                        else
                            scope.launch{ pagerState.animateScrollToPage(pagerPageCount-1) }
                        }) {
                            Text("Finish", color = if (needsDarkColor) Color.Gray else Color.White)
                        }
                    }
                }
            },
            title = title,
            image = {
                val roundedCornersShape = CardDefaults.shape
                Box(Modifier
                    .wrapContentHeight(Top)
                    .graphicsLayer {
                        shape = roundedCornersShape
                        clip = true // <- this ensures clipping is applied during transition
                    }
                    .sharedElement(
                        sharedStateImg,
                        animatedVisibilityScope,
                        clipInOverlayDuringTransition = OverlayClip(roundedCornersShape)
                    ), contentAlignment = TopCenter) { // TODO: add swipe
                    AsyncImage(
                        ImageRequest.Builder(context)
                            .allowHardware(false)
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
                        "Exercise image",
                        Modifier
                            .fillMaxWidth()
                            .height(imageHeight),
                        contentScale = ContentScale.Crop
                    )

                    HorizontalPagerIndicator(
                        pagerState = pagerState,
                        pageCount = pagerPageCount,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                    )
                }
            },
            imageHeight = imageHeight,
            brightImage = brightImage.value,
            darkTheme = useDarkTheme,
            content = {
                val restCounterMillis: Long? by remember { derivedStateOf {
                    if (workoutState.restTimestamp != null && currentExercise != null)
                        max(0L,
                            workoutState.restTimestamp?.toInstant()?.toEpochMilli()?.minus(
                                workoutState.currentTime.toInstant().toEpochMilli()
                            ) ?: 0L
                        )
                    else null
                }}
                ExercisePage(
                    pagerState = pagerState,
                    workoutTimeMillis = workoutTimeMillis,
                    workoutExercises = workoutState.workoutExercises,
                    workoutId = workoutState.workoutId,
                    navigator = navigator,
                    setsDone = setsDone,
                    ongoingRecord = ongoingRecord,
                    currentExerciseRecords = recordsToDisplay,
                    exerciseDescription = currentExercise?.description ?: "Description not available",
                    fabHeight = fabHeight,
                    title = title,
                    addSet = { viewModel.onEvent(WorkoutEvent.AddSetToExercise(pagerState.currentPage)) },
                    restCounterMillis = restCounterMillis,
                    workoutIntensity = workoutIntensity,
                    updateExerciseProbability = { probability ->
                        scope.launch {
                            // if already snackbarring, dismiss it before a new one.
                            snackbarHostState.currentSnackbarData?.dismiss()
                            if (probability > 0)
                                snackbarHostState.showSnackbar("Increasing exercise probability when generating new plans...")
                            else
                                snackbarHostState.showSnackbar("Decreasing exercise probability when generating new plans...")
                        }
                        viewModel.onEvent(WorkoutEvent.UpdateExerciseProbability(pagerState.currentPage, probability))
                    },
                    updateBottomBar = { rep, weight ->
                        if (rep != null)
                            viewModel.onEvent(WorkoutEvent.UpdateReps(rep.toString()))
                        else
                            // this should never happen. Log it
                            Log.w("Workout", "updateBottomBar called with null rep")
                        if (weight != null)
                            viewModel.onEvent(WorkoutEvent.UpdateWeight(weight.toString()))
                    },
                    updateValues = { a, b, c, d -> viewModel.onEvent(WorkoutEvent.EditSetRecord(a, b, c, d)) },
                    updateTare = { tare -> viewModel.onEvent(WorkoutEvent.UpdateTare(tare))},
                    useImperialSystem = workoutState.imperialSystem,
                    tare = workoutState.tare,
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
                if (mediaTitle == null && shouldTeaseMediaAccess) {
                    mediaTitle = "Do you want your playing songs here?"
                    mediaAuthor = "Tap to learn more or swipe to dismiss"
                }
                if (mediaTitle != null) {
                    fabHeight = 8.dp + 8.dp + 48.dp + 8.dp + 16.dp
                    // swap color scheme i.e., if in dark use light colors, otherwise dark colors
                    var colors: ColorScheme = if (useDarkTheme) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicLightColorScheme(
                            LocalContext.current
                        ) else lightColorScheme()
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(
                            LocalContext.current
                        ) else darkColorScheme()
                    }
                    SwipeToDismissBox(
                        state = rememberSwipeToDismissBoxState(),
                        backgroundContent = {}
                    ) {
                        AnimatedVisibility(
                            visible = !pagerState.isScrollInProgress,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            ElevatedCard(
                                colors = CardDefaults.elevatedCardColors(containerColor = colors.surface),
                                modifier = Modifier
                                    .padding(start = 32.dp)
                                    .clickable {  // weird padding as it pretends to be a fab
                                        if (shouldTeaseMediaAccess) {
                                            viewModel.onEvent(WorkoutEvent.ToggleRequestNotificationAccessDialog)
                                        } else if (session?.packageName != null) {
                                            val intent =
                                                context.packageManager.getLaunchIntentForPackage(
                                                    session!!.packageName!!
                                                )
                                            context.startActivity(intent)
                                        }
                                    }
                            ) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp)
                                ) {
                                    if (artworkBitmap != null) {
                                        AsyncImage(
                                            artworkBitmap, "Song artwork",
                                            Modifier
                                                .size(48.dp)
                                                .clip(
                                                    RoundedCornerShape(8.dp)
                                                )
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.MusicNote,
                                            "No song artwork",
                                            Modifier.size(48.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            mediaTitle!!,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = colors.onSurface
                                        )
                                        Text(
                                            mediaAuthor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.secondary
                                        )
                                    }
                                    // if we are just teasing, gain space by removing buttons
                                    if (!shouldTeaseMediaAccess) {
                                        Spacer(Modifier.width(8.dp))
                                        IconButton(
                                            colors = IconButtonDefaults.iconButtonColors(contentColor = colors.onSurface),
                                            onClick = {
                                                if (session != null) {
                                                    if (session!!.playbackState?.state == STATE_PLAYING)
                                                        session!!.transportControls.pause()
                                                    else
                                                        session!!.transportControls.play()
                                                }
                                            }
                                        ) {
                                            if (isPlaying) {
                                                Icon(Icons.Default.Pause, "Pause")
                                            } else {
                                                Icon(Icons.Default.PlayArrow, "Play")
                                            }
                                        }
                                        IconButton(
                                            colors = IconButtonDefaults.iconButtonColors(contentColor = colors.onSurface),
                                            onClick = {
                                                if (session != null) {
                                                    session!!.transportControls.skipToNext()
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.SkipNext, "Next track")
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        ) { padding, bottomBarSurface ->
            // FIXME: column doesn't seem necessary here
//            Column (modifier = Modifier.background(Color.Black)) {
                AnimatedVisibility(
                    visible = !pagerState.isScrollInProgress,
                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
                ) {
                    bottomBarSurface {
                        WorkoutBottomBar(
                            contentPadding = padding,
                            workoutStarted = workoutState.startDate != null,
                            startWorkout = { viewModel.onEvent(WorkoutEvent.StartWorkout) },
                            currentExercise = currentExercise,
                            completeWorkout = completeWorkout,
                            completeSet = {
                                if (!viewModel.onEvent(
                                        WorkoutEvent.TryCompleteSet(
                                            pagerState.currentPage,
                                            currentExercise!!.rest[setsDone.value].toLong()
                                        )
                                    )
                                ) {
                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar("Please enter valid numbers")
                                    }
                                } else if ((currentExercise?.supersetExercise ?: 0L) != 0L) {
                                    val superExercise =
                                        workoutState.workoutExercises.find {
                                            it.extProgramExerciseId == currentExercise!!.supersetExercise
                                        }
                                    if (superExercise != null) {
                                        if (workoutState.workoutExercises.indexOf(
                                                superExercise
                                            ) >
                                            workoutState.workoutExercises.indexOf(
                                                currentExercise
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
                            }, setsFinished = setsDone.value >= (currentExercise?.reps?.size ?: 0),
                            addSet = { viewModel.onEvent(WorkoutEvent.AddSetToExercise(pagerState.currentPage)) },
                            goToNextExercise = {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        pagerState.currentPage + 1
                                    )
                                }
                            },
                            repsToDisplay = workoutState.repsBottomBar,
                            updateReps = { value -> viewModel.onEvent(WorkoutEvent.UpdateReps(value)) },
                            weightToDisplay = workoutState.weightBottomBar,
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
//                }
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
                IconButton(onClick = { /* just a placeholder, won't be clicked anyway */}) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = if (needsDarkColor) Color.Gray else Color.White
                    )
                }
            },
            topAppBarActions = {},
            title = { },
            image = {
                val roundedCornersShape = CardDefaults.shape
                Box(Modifier
                    .wrapContentHeight(Top)
                    .graphicsLayer(
                        shape = roundedCornersShape,
                        clip = true // <- this ensures clipping is applied during transition
                    )
                    .sharedElement(
                        sharedStateImg,
                        animatedVisibilityScope,
                        clipInOverlayDuringTransition = OverlayClip(roundedCornersShape)
                    ), contentAlignment = TopCenter) { // TODO: add swipe
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
                        "Exercise image",
                        Modifier
                            .fillMaxWidth()
                            .height(imageHeight),
                        contentScale = ContentScale.Crop
                    )
                }
            },
            imageHeight = imageHeight,
            brightImage = brightImage.value,
            darkTheme = useDarkTheme,
            content = {
                val exampleRecord = ExerciseRecordAndEquipment(
                    recordId = 0L,
                    extExerciseId = 0L,
                    extWorkoutId = 0L,
                    exerciseInWorkout = previewExercise.orderInProgram,
                    date = ZonedDateTime.now(),
                    reps = previewExercise.reps,
                    weights = previewExercise.reps.map { 0f },
                    tare = 0f,
                    variation = previewExercise.variation,
                    rest = previewExercise.rest,
                    equipment = previewExercise.equipment
                )
                val workoutExercisesExample = listOf(
                    WorkoutExercise(
                        workoutExerciseId = 0L,
                        extWorkoutId = 0L,
                        extProgramExerciseId = 0L,
                        extExerciseId = 0L,
                        name = previewExercise.name,
                        image = previewExercise.image,
                        description = previewExercise.description,
                        equipment = previewExercise.equipment,
                        orderInProgram = previewExercise.orderInProgram,
                        reps = previewExercise.reps,
                        rest = previewExercise.rest,
                        note = previewExercise.note,
                        variation = previewExercise.variation,
                        supersetExercise = previewExercise.supersetExercise
                    ),
                    WorkoutExercise(
                        workoutExerciseId = 0L,
                        extWorkoutId = 0L,
                        extProgramExerciseId = 0L,
                        extExerciseId = 0L,
                        name = previewExercise.name,
                        image = previewExercise.image,
                        description = previewExercise.description,
                        equipment = previewExercise.equipment,
                        orderInProgram = previewExercise.orderInProgram,
                        reps = previewExercise.reps,
                        rest = previewExercise.rest,
                        note = previewExercise.note,
                        variation = previewExercise.variation,
                        supersetExercise = previewExercise.supersetExercise
                    )

                )
                ExercisePage(
                    pagerState = rememberPagerState(pageCount = { 2 }),
                    workoutTimeMillis = 0L,
                    workoutExercises = workoutExercisesExample,
                    workoutId = 0L,
                    navigator = navigator,
                    setsDone = setsDone,
                    ongoingRecord = exampleRecord,
                    currentExerciseRecords = emptyList(),
                    exerciseDescription = "",
                    fabHeight = 0.dp,
                    title = {
                        Text(
                            previewExercise.name, modifier = Modifier.sharedBounds(
                                sharedStateTitle,
                                animatedVisibilityScope,
                            )
                        )
                    },
                    addSet = { },
                    updateBottomBar = { _, _ -> },
                    updateValues = { _, _, _, _ -> },
                    updateTare = { },
                    useImperialSystem = false,
                    tare = 0f,
                    toggleOtherEquipment = { },
                    changeExercise = { _, _ -> },
                    removeExercise = { },
                    restCounterMillis = null,
                    workoutIntensity = workoutIntensity,
                    updateExerciseProbability = { _ -> }
                )
            },
            floatingActionButton = {},
            bottomBar = { _, _ -> }
        )
    } else if (workoutState.workoutId != 0L){
        // program is empty, prompt to add an exercise
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { navigator.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Go back"
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            floatingActionButton = {
                Column(
                    Modifier.navigationBarsPadding(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ) {  // FIXME: not really happy about the double FABs
                    SmallFloatingActionButton(onClick = {
                        navigator.navigate(
                            ExercisesByMuscleDestination(
                                programName = "Current and future workouts",  // FIXME: all workouts?
                                workoutId = workoutState.workoutId,
                                programId = programId
                            )
                        )
                    }, Modifier.padding(bottom = 24.dp),
                        containerColor = MaterialTheme.colorScheme.secondary) {
                        Icon(Icons.Default.Edit, "Add an exercise to current and future workouts of this program")
                    }
                    LargeFloatingActionButton(onClick = {
                        navigator.navigate(
                            ExercisesByMuscleDestination(
                                programName = "Current workout",
                                workoutId = workoutState.workoutId,
                            )
                        )
                    }) {
                        Icon(
                            Icons.Default.Add, "Add an exercise to current workout",
                            Modifier.size(FloatingActionButtonDefaults.LargeIconSize)
                        )
                    }
                }
            }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.FitnessCenter,
                    contentDescription = "",
                    modifier = Modifier.size(160.dp)
                )
                Text(
                    stringResource(id = R.string.workout_empty_exercises),
                    modifier = Modifier.padding(16.dp)
                )
                Text(
                    stringResource(R.string.note_empty_workout),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
