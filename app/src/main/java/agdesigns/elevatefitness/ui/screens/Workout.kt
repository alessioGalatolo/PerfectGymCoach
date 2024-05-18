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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
import agdesigns.elevatefitness.service.NotificationListener
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.core.content.ContextCompat.getSystemService
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.ExercisesByMuscleDestination
import com.ramcosta.composedestinations.generated.destinations.WorkoutRecapDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Destination<WorkoutOnlyGraph>(start = true)
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class
)
@Composable
fun Workout(
    navigator: DestinationsNavigator,
    programId: Long = 0,
    quickStart: Boolean = false,
    resumeWorkout: Boolean = false,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    if (resumeWorkout)
        viewModel.onEvent(WorkoutEvent.ResumeWorkout)
    else
        viewModel.onEvent(WorkoutEvent.InitWorkout(programId))

    val scope = rememberCoroutineScope()

    val startWorkout = rememberSaveable { mutableStateOf(quickStart) }
    val context = LocalContext.current
    var alreadyRequestedPermission by rememberSaveable { mutableStateOf(false) }

    val shouldOpenRequest by remember { derivedStateOf {
        !viewModel.state.value.cantRequestNotificationAccess
        && !hasNotificationAccess(context)
        && !viewModel.state.value.requestNotificationAccessDialogOpen
        && !alreadyRequestedPermission
    } }
    // FIXME: for some reason this only becomes true when a workout has started??
    // I don't know why but it is an acceptable behaviour for now
    LaunchedEffect(shouldOpenRequest) {
        if (
            shouldOpenRequest
        ) {
            viewModel.onEvent(WorkoutEvent.ToggleRequestNotificationAccessDialog)
            alreadyRequestedPermission = true
        }
    }
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

    LaunchedEffect (viewModel.state.value.shutDown){
        if (viewModel.state.value.shutDown) {
            navigator.navigateUp()
            navigator.navigate(
                WorkoutRecapDestination(workoutId = viewModel.state.value.workoutId)
            )
        }
    }

    RequestNotificationAccessDialog(
        dialogIsOpen = viewModel.state.value.requestNotificationAccessDialogOpen,
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
        dialogueIsOpen = viewModel.state.value.cancelWorkoutDialogOpen,
        toggleDialog = { viewModel.onEvent(WorkoutEvent.ToggleCancelWorkoutDialog) },
        cancelWorkout = { viewModel.onEvent(WorkoutEvent.CancelWorkout); navigator.navigateUp() },
        deleteData = { viewModel.onEvent(WorkoutEvent.DeleteCurrentRecords) }
    )
    InputOtherEquipmentDialog(
        dialogIsOpen = viewModel.state.value.otherEquipmentDialogOpen,
        toggleDialog = { viewModel.onEvent(WorkoutEvent.ToggleOtherEquipmentDialog) },
        weightUnit = if (viewModel.state.value.imperialSystem) "lb" else "kg",
        updateTare = { tare -> viewModel.onEvent(WorkoutEvent.UpdateTare(maybeLbToKg(tare, viewModel.state.value.imperialSystem))) }
    )

    val pagerState = rememberPagerState(pageCount = {
        if (viewModel.state.value.workoutTime != null)
            viewModel.state.value.workoutExercises.size+1
        else
            viewModel.state.value.workoutExercises.size
    })
    val currentExercise: WorkoutExercise? by remember {
        derivedStateOf {
            if (pagerState.currentPage < viewModel.state.value.workoutExercises.size) {
                viewModel.state.value.workoutExercises[pagerState.currentPage]
            } else {
                null
            }
        }
    }

    val timer = {
        " " + (viewModel.state.value.workoutTime?.let { DateUtils.formatElapsedTime(it) } ?: "")
    }


    val title = @Composable { Text(
        currentExercise?.name?.plus(currentExercise?.variation) ?: "End of workout",
        overflow = TextOverflow.Ellipsis,
//        maxLines = 1
    ) }

    val currentExerciseRecord by remember { derivedStateOf {
        if (pagerState.currentPage < viewModel.state.value.workoutExercises.size)
            viewModel.state.value.allRecords[
                    viewModel.state.value.workoutExercises[pagerState.currentPage].extExerciseId
            ] ?: emptyList()
        else
            emptyList()
    }}

    // record being set right now for current exercise
    val ongoingRecord by remember { derivedStateOf {
        currentExerciseRecord.find {
            it.extWorkoutId == viewModel.state.value.workoutId && it.exerciseInWorkout == pagerState.currentPage
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

    LaunchedEffect(currentExercise, setsDone){
        if (currentExercise != null && setsDone.value < currentExercise!!.reps.size) {
            viewModel.onEvent(
                WorkoutEvent.UpdateReps(
                    currentExercise!!.reps[setsDone.value].toString()
                )
            )
        }
    }

    LaunchedEffect(viewModel.state.value.allRecords, pagerState.currentPage, setsDone){
        val currentRecord = recordsToDisplay.firstOrNull()

        if (currentRecord != null) {
            if (setsDone.value > 0) {
                viewModel.onEvent(
                    WorkoutEvent.UpdateWeight(
                        maybeKgToLb(
                            ongoingRecord!!.weights[setsDone.value - 1],
                            viewModel.state.value.imperialSystem
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
                            viewModel.state.value.imperialSystem
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
        if (viewModel.state.value.workoutTime == null)
            navigator.navigateUp()
        else
            viewModel.onEvent(WorkoutEvent.ToggleCancelWorkoutDialog)
        Unit
    }

    val workoutIntensity = rememberSaveable { mutableStateOf(WorkoutRecord.WorkoutIntensity.NORMAL_INTENSITY) }

    val completeWorkout: () -> Unit = {
        viewModel.onEvent(WorkoutEvent.FinishWorkout(workoutIntensity.value))
    }

    val pagerPageCount by remember { derivedStateOf {
        if (viewModel.state.value.workoutTime != null)
            viewModel.state.value.workoutExercises.size+1
        else
            viewModel.state.value.workoutExercises.size
    }}

    var fabHeight by remember { mutableStateOf(0.dp) }

    if (viewModel.state.value.workoutExercises.isNotEmpty()) {
        BackHandler(onBack = onClose)
        val currentImageId by remember { derivedStateOf {
            if (pagerState.currentPage == viewModel.state.value.workoutExercises.size)
                R.drawable.finish_workout
            else currentExercise!!.image
        }}
        val brightImage = remember { mutableStateOf(false) }
        val imageWidth = LocalConfiguration.current.screenWidthDp.dp
        val imageHeight = imageWidth/3*2
        val systemTheme = isSystemInDarkTheme()
        val useDarkTheme by remember { derivedStateOf {
            when (viewModel.state.value.userTheme) {
                Theme.SYSTEM -> systemTheme
                Theme.LIGHT -> false
                Theme.DARK -> true
            }
        }}
        FullScreenImageCard(
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
                    if (viewModel.state.value.workoutTime != null) {
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
                Box(Modifier.wrapContentHeight(Top), contentAlignment = TopCenter) { // TODO: add swipe
                    AsyncImage(
                        ImageRequest.Builder(context)
//                            .size(imageWidth, imageHeight)
                            .allowHardware(false)
                            .data(currentImageId)
                            .crossfade(true)
                            .listener { _, result ->
                                val image = result.drawable.toBitmap()
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
                        contentScale = ContentScale.Crop/*
                        onState = { imageHeight.value =  it.painter?.intrinsicSize?.height ?: 0f },
                        Modifier.fillMaxWidth().onSizeChanged { imageHeight.value = it.height.toFloat() }*/
//                        loading = { CircularProgressIndicator() }
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
                val restCounter: Long? by remember { derivedStateOf {
                    if (viewModel.state.value.restTimestamp != null && currentExercise != null)
                        kotlin.math.max(0L,
                            viewModel.state.value.restTimestamp!! - viewModel.state.value.workoutTime!!)
                    else null
                }}
                ExercisePage(
                    pagerState = pagerState,
                    workoutTime = viewModel.state.value.workoutTime,
                    workoutExercises = viewModel.state.value.workoutExercises,
                    workoutId = viewModel.state.value.workoutId,
                    navigator = navigator,
                    setsDone = setsDone,
                    ongoingRecord = ongoingRecord,
                    currentExerciseRecords = recordsToDisplay,
                    exerciseDescription = currentExercise?.description ?: "Description not available",
                    fabHeight = fabHeight,
                    title = title,
                    addSet = { viewModel.onEvent(WorkoutEvent.AddSetToExercise(pagerState.currentPage)) },
                    restCounter = restCounter,
                    workoutIntensity = workoutIntensity,
                    updateBottomBar = { rep, weight ->
                        viewModel.onEvent(WorkoutEvent.UpdateReps(rep.toString()))
                        viewModel.onEvent(WorkoutEvent.UpdateWeight(weight.toString()))
                    },
                    updateValues = { a, b, c, d -> viewModel.onEvent(WorkoutEvent.EditSetRecord(a, b, c, d)) },
                    updateTare = { tare -> viewModel.onEvent(WorkoutEvent.UpdateTare(tare))},
                    useImperialSystem = viewModel.state.value.imperialSystem,
                    tare = viewModel.state.value.tare,
                    toggleOtherEquipment = { viewModel.onEvent(WorkoutEvent.ToggleOtherEquipmentDialog) },
                    changeExercise = { exerciseInWorkout, originalSize ->
                        scope.launch {
                            awaitFrame()  // wait for exercise to be added
                            awaitFrame()
                            viewModel.onEvent(
                                WorkoutEvent.DeleteChangeExercise(
                                    exerciseInWorkout,
                                    originalSize
                                )
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                if (mediaTitle != null) {
                    fabHeight = 8.dp + 8.dp + 48.dp + 8.dp + 16.dp
                    val darkColors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(LocalContext.current) else darkColorScheme()
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
                                colors = CardDefaults.elevatedCardColors(containerColor = darkColors.surface),
                                modifier = Modifier.padding(start = 32.dp).clickable {  // weird padding as it pretends to be a fab
                                    if (session?.packageName != null) {
                                        val intent = context.packageManager.getLaunchIntentForPackage(session!!.packageName!!)
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
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White
                                        )
                                        Text(
                                            mediaAuthor,
                                            maxLines = 1,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = darkColors.secondary
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(
                                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
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
                                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                                        onClick = {
                                            if (session != null) {
                                                session!!.transportControls.skipToNext()
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.SkipNext, "Next track")
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        ) { padding, bottomBarSurface ->
            val snackbarState = remember { SnackbarHostState() }
            Column {
                SnackbarHost(hostState = snackbarState)
                AnimatedVisibility(
                    visible = !pagerState.isScrollInProgress,
                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
                ) {
                    bottomBarSurface {
                        WorkoutBottomBar(
                            contentPadding = padding,
                            workoutStarted = viewModel.state.value.workoutTime == null,
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
                                        snackbarState.showSnackbar("Please enter valid numbers")
                                    }
                                } else if ((currentExercise?.supersetExercise ?: 0L) != 0L) {
                                    val superExercise =
                                        viewModel.state.value.workoutExercises.find {
                                            it.extProgramExerciseId == currentExercise!!.supersetExercise
                                        }
                                    if (superExercise != null) {
                                        if (viewModel.state.value.workoutExercises.indexOf(
                                                superExercise
                                            ) >
                                            viewModel.state.value.workoutExercises.indexOf(
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
                            repsToDisplay = viewModel.state.value.repsBottomBar,
                            updateReps = { value -> viewModel.onEvent(WorkoutEvent.UpdateReps(value)) },
                            weightToDisplay = viewModel.state.value.weightBottomBar,
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
            }
        }
    } else if (viewModel.state.value.workoutId != 0L){
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
                                workoutId = viewModel.state.value.workoutId,
                                programId = programId
                            ),
                            onlyIfResumed = true
                        )
                    }, Modifier.padding(bottom = 24.dp),
                    containerColor = MaterialTheme.colorScheme.secondary) {
                        Icon(Icons.Default.Edit, "Add an exercise to current and future workouts of this program")
                    }
                    LargeFloatingActionButton(onClick = {
                        navigator.navigate(
                            ExercisesByMuscleDestination(
                                programName = "Current workout",
                                workoutId = viewModel.state.value.workoutId,
                            ),
                            onlyIfResumed = true
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
