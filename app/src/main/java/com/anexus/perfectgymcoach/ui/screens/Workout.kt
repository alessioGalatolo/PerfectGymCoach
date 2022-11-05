package com.anexus.perfectgymcoach.ui.screens

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
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
import com.anexus.perfectgymcoach.data.exercise.WorkoutExerciseAndInfo
import com.anexus.perfectgymcoach.data.workout_record.WorkoutRecord
import com.anexus.perfectgymcoach.ui.MainScreen
import com.anexus.perfectgymcoach.ui.components.*
import com.anexus.perfectgymcoach.viewmodels.WorkoutEvent
import com.anexus.perfectgymcoach.viewmodels.WorkoutViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch


@OptIn(
    ExperimentalPagerApi::class, ExperimentalMaterial3Api::class
)
@Composable
fun Workout(navController: NavHostController, programId: Long,
            quickStart: Boolean,
            resumeWorkout: Boolean = false,
            viewModel: WorkoutViewModel = hiltViewModel()
) {
    if (resumeWorkout)
        viewModel.onEvent(WorkoutEvent.ResumeWorkout)
    else
        viewModel.onEvent(WorkoutEvent.GetWorkoutExercises(programId))

    val scope = rememberCoroutineScope()

    val startWorkout = rememberSaveable { mutableStateOf(quickStart) }

    if (startWorkout.value){
        scope.launch {
            awaitFrame()  // FIXME: not the proper way of doing this (needs to wait for first collection)
            awaitFrame()
            viewModel.onEvent(WorkoutEvent.StartWorkout(programId))
            startWorkout.value = false
        }
    }

    CancelWorkoutDialog(
        dialogueIsOpen = viewModel.state.value.cancelWorkoutDialogOpen,
        toggleDialog = { viewModel.onEvent(WorkoutEvent.ToggleCancelWorkoutDialog) },
        cancelWorkout = { viewModel.onEvent(WorkoutEvent.CancelWorkout); navController.popBackStack() },
        deleteData = { viewModel.onEvent(WorkoutEvent.DeleteCurrentRecords) }
    )
    val pagerState = rememberPagerState()
    val currentExercise: WorkoutExerciseAndInfo? by remember {
        derivedStateOf {
            if (pagerState.currentPage < viewModel.state.value.workoutExercisesAndInfo.size) {
                viewModel.state.value.workoutExercisesAndInfo[pagerState.currentPage]
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
        if (pagerState.currentPage < viewModel.state.value.workoutExercisesAndInfo.size)
            viewModel.state.value.allRecords[
                    viewModel.state.value.workoutExercisesAndInfo[pagerState.currentPage].extExerciseId
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
        val currentRecord = recordsToDisplay.firstOrNull()  // FixME: sometimes not latest (was it fixed?)

        if (currentRecord != null) {
            if (setsDone.value > 0)
                viewModel.onEvent(WorkoutEvent.UpdateWeight(ongoingRecord!!.weights[setsDone.value-1].toString()))
            else
//            val index = min(setsDone.value, currentRecord.weights.size - 1)
                viewModel.onEvent(WorkoutEvent.UpdateWeight(currentRecord.weights[0].toString()))
        }
    }

    val onClose = {
        if (viewModel.state.value.workoutTime == null)
            navController.popBackStack()
        else
            viewModel.onEvent(WorkoutEvent.ToggleCancelWorkoutDialog)
        Unit
    }

    val workoutIntensity = rememberSaveable { mutableStateOf(WorkoutRecord.WorkoutIntensity.NORMAL_INTENSITY) }

    val completeWorkout: () -> Unit = {
        viewModel.onEvent(WorkoutEvent.FinishWorkout(programId, workoutIntensity.value))
        navController.popBackStack()
        navController.navigate("${MainScreen.WorkoutRecap.route}/${viewModel.state.value.workoutId}")
    }

    if (viewModel.state.value.workoutExercisesAndInfo.isNotEmpty()) {
        BackHandler(onBack = onClose)
        val currentImageId by remember { derivedStateOf {
            if (pagerState.currentPage == viewModel.state.value.workoutExercisesAndInfo.size)
                R.drawable.finish_workout
            else currentExercise!!.image
        }}
        val context = LocalContext.current
        val brightImage = remember { mutableStateOf(false) }
        val imageWidth = LocalConfiguration.current.screenWidthDp.dp
        val imageHeight = imageWidth/3*2
        FullScreenImageCard(
            topAppBarNavigationIcon = { appBarShown ->
                val needsDarkColor = (brightImage.value && !appBarShown) || (appBarShown && !isSystemInDarkTheme())
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
                            (appBarShown && !isSystemInDarkTheme())
                    Text(timer(), style = MaterialTheme.typography.titleLarge,
                        color = if (needsDarkColor) Color.Black else Color.White)  // FIXME should use default colors
                    if (viewModel.state.value.workoutTime != null) {
                        TextButton(onClick = {
                        if (pagerState.currentPage == pagerState.pageCount-1)
                            completeWorkout()
                        else
                            scope.launch{ pagerState.animateScrollToPage(pagerState.pageCount-1) }
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
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                    )
                }
            },
            imageHeight = imageHeight,
            brightImage = brightImage.value,
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
                    setsDone = setsDone,
                    workoutExercisesAndInfo = viewModel.state.value.workoutExercisesAndInfo,
                    ongoingRecord = ongoingRecord,
                    currentExerciseRecords = recordsToDisplay,
                    title = title,
                    addSet = { viewModel.onEvent(WorkoutEvent.AddSetToExercise(pagerState.currentPage)) },
                    restCounter = restCounter,
                    workoutIntensity = workoutIntensity,
                    updateBottomBar = { rep, weight ->
                        viewModel.onEvent(WorkoutEvent.UpdateReps(rep.toString()))
                        viewModel.onEvent(WorkoutEvent.UpdateWeight(weight.toString()))
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
                            startWorkout = { viewModel.onEvent(WorkoutEvent.StartWorkout(programId)) },
                            currentExercise = currentExercise,
                            completeWorkout = completeWorkout,
                            completeSet = {
                                if (!viewModel.onEvent(
                                        WorkoutEvent.TryCompleteSet(
                                            pagerState.currentPage,
                                            currentExercise!!.rest.toLong()
                                        )
                                    )
                                ) {
                                    scope.launch {
                                        snackbarState.showSnackbar("Please enter valid numbers")
                                    }
                                } else if ((currentExercise?.supersetExercise ?: 0L) != 0L) {
                                    val superExercise =
                                        viewModel.state.value.workoutExercisesAndInfo.find {
                                            it.workoutExerciseId == currentExercise!!.supersetExercise
                                        }
                                    if (superExercise != null) {
                                        if (viewModel.state.value.workoutExercisesAndInfo.indexOf(
                                                superExercise
                                            ) >
                                            viewModel.state.value.workoutExercisesAndInfo.indexOf(
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
    } else {
        // program is empty, prompt to add an exercise
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                SmallTopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
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
            LargeFloatingActionButton(onClick = { navController.navigate(
                "${MainScreen.ExercisesByMuscle.route}/" +
                        "Current workout/" +
                        "$programId" +
                        "${false}"
            ) }, Modifier.navigationBarsPadding()) {
                Icon(Icons.Default.Add, null,
                    Modifier.size(FloatingActionButtonDefaults.LargeIconSize))
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
            }
        }
    }
}
