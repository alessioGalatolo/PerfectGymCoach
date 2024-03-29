package com.anexus.perfectgymcoach.ui.screens

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
import androidx.navigation.NavHostController
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.data.exercise.ProgramExerciseAndInfo
import com.anexus.perfectgymcoach.data.workout_exercise.WorkoutExercise
import com.anexus.perfectgymcoach.data.workout_record.WorkoutRecord
import com.anexus.perfectgymcoach.ui.WorkoutNavGraph
import com.anexus.perfectgymcoach.ui.components.*
import com.anexus.perfectgymcoach.ui.destinations.ExercisesByMuscleDestination
import com.anexus.perfectgymcoach.ui.destinations.WorkoutRecapDestination
import com.anexus.perfectgymcoach.ui.maybeKgToLb
import com.anexus.perfectgymcoach.ui.maybeLbToKg
import com.anexus.perfectgymcoach.viewmodels.WorkoutEvent
import com.anexus.perfectgymcoach.viewmodels.WorkoutViewModel
import com.google.accompanist.pager.HorizontalPagerIndicator
import androidx.compose.foundation.pager.rememberPagerState
import com.anexus.perfectgymcoach.data.Theme
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch

@WorkoutNavGraph(start = true)
@Destination
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


    if (startWorkout.value){
        scope.launch {
            viewModel.onEvent(WorkoutEvent.StartWorkout)
            startWorkout.value = false
        }
    }

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

    val pagerState = rememberPagerState()
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
        navigator.navigateUp()
        navigator.navigate(
            WorkoutRecapDestination(workoutId = viewModel.state.value.workoutId)
        )
    }

    val pagerPageCount by remember { derivedStateOf {
        if (viewModel.state.value.workoutTime != null)
            viewModel.state.value.workoutExercises.size+1
        else
            viewModel.state.value.workoutExercises.size
    }}

    if (viewModel.state.value.workoutExercises.isNotEmpty()) {
        BackHandler(onBack = onClose)
        val currentImageId by remember { derivedStateOf {
            if (pagerState.currentPage == viewModel.state.value.workoutExercises.size)
                R.drawable.finish_workout
            else currentExercise!!.image
        }}
        val context = LocalContext.current
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
                        null,
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
                                imageVector = Icons.Filled.ArrowBack,
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
                        Icon(Icons.Default.Edit, null)
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
                            Icons.Default.Add, null,
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
