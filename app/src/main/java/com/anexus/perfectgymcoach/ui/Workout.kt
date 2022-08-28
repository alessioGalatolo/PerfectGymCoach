package com.anexus.perfectgymcoach.ui

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.input.KeyboardType
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
import com.anexus.perfectgymcoach.data.exercise.Exercise.Companion.equipment2increment
import com.anexus.perfectgymcoach.data.exercise.WorkoutExerciseAndInfo
import com.anexus.perfectgymcoach.ui.components.CancelWorkoutDialog
import com.anexus.perfectgymcoach.ui.components.FullScreenImageCard
import com.anexus.perfectgymcoach.ui.components.PGCSmallTopBar
import com.anexus.perfectgymcoach.viewmodels.WorkoutEvent
import com.anexus.perfectgymcoach.viewmodels.WorkoutViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import kotlin.math.min


@OptIn(
    ExperimentalPagerApi::class, ExperimentalMaterial3Api::class
)
@Composable
fun Workout(navController: NavHostController, programId: Long,
            quickStart: Boolean,
            viewModel: WorkoutViewModel = hiltViewModel()
) {
    viewModel.onEvent(WorkoutEvent.GetWorkoutExercises(programId))

    val startWorkout = rememberSaveable { mutableStateOf(quickStart) }
    if (startWorkout.value){
        viewModel.onEvent(WorkoutEvent.StartWorkout(programId))
        startWorkout.value = false
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

    val scope = rememberCoroutineScope()

    val title = @Composable { Text(
        currentExercise?.name ?: "End",
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
                    currentExercise!!.reps[setsDone.value]
                )
            )
        }
    }

    LaunchedEffect(viewModel.state.value.allRecords, pagerState.currentPage, setsDone){
        val currentRecord = recordsToDisplay.firstOrNull()  // FixME: sometimes not latest

        if (currentRecord != null) {
            if (setsDone.value > 0)
                viewModel.onEvent(WorkoutEvent.UpdateWeight(ongoingRecord!!.weights[setsDone.value-1]))
            else
//            val index = min(setsDone.value, currentRecord.weights.size - 1)
                viewModel.onEvent(WorkoutEvent.UpdateWeight(currentRecord.weights[0]))
        }
    }

    val onClose = {
        if (viewModel.state.value.workoutTime == null)
            navController.popBackStack()
        else
            viewModel.onEvent(WorkoutEvent.ToggleCancelWorkoutDialog)
        Unit
    }

    val completeWorkout: () -> Unit = {    // TODO: should go to recap screen, change upcoming program
        viewModel.onEvent(WorkoutEvent.FinishWorkout)
        navController.popBackStack()
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
            topAppBarNavigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Go back" // TODO: cancel workout?
                    )
                }
            },
            topAppBarActions = { appBarShown ->  // FIXME: value not changing when needed
                Row(verticalAlignment = CenterVertically) {
                    Text(timer(), style = MaterialTheme.typography.titleLarge,
                        color = if (brightImage.value || appBarShown) MaterialTheme.typography.titleLarge.color else Color.White)
                    if (viewModel.state.value.workoutTime != null) {
                        TextButton(onClick = completeWorkout ) {
                            Text("Finish",
                                color = if (brightImage.value || appBarShown) ButtonDefaults.textButtonColors().contentColor(
                                    enabled = true
                                ).value else Color.LightGray)
                        }
                    }
                }
            },
            title = title,
            image = {
                Box(Modifier.wrapContentHeight(Top), contentAlignment = TopCenter) {
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
//                                    .setRegion(0, 0, image.width,50)
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
                ExercisePage(
                    pagerState = pagerState,
                    setsDone = setsDone,
                    workoutExercisesAndInfo = viewModel.state.value.workoutExercisesAndInfo,
                    ongoingRecord = ongoingRecord,
                    currentExerciseRecords = recordsToDisplay,
                    title = title,
                    addSet = { viewModel.onEvent(WorkoutEvent.AddSetToExercise(pagerState.currentPage)) }
                )
            }
        ) {
            Column(
                Modifier
                    .padding(it)
                    .padding(horizontal = 16.dp)
            ) {
                if (viewModel.state.value.workoutTime == null) {
                    // workout has not started
                    Button(
                        onClick = { viewModel.onEvent(WorkoutEvent.StartWorkout(programId)) },
                        Modifier.fillMaxWidth()
                    ) {
                        Text("Start workout")
                    }
                } else if (currentExercise == null) {
                    // workout has started and it is on the end page
                    Button(
                        onClick = completeWorkout,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Complete workout")
                    }
                } else if (setsDone.value >= currentExercise!!.reps.size
                ) {
                    // workout started and the user has done all the sets in the page
                    OutlinedButton(
                        onClick = {
                            viewModel.onEvent(WorkoutEvent.AddSetToExercise(pagerState.currentPage))
                        },
                        Modifier.fillMaxWidth()
                    ) {
                        Text("Add set")
                    }
                    Button(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                        Modifier.fillMaxWidth()
                    ) {
                        Text("Next exercise")
                    }

                } else {
                    // normal case
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextFieldWithButtons(
                            "Reps",
                            text = { viewModel.state.value.repsBottomBar.toString() },
                            onNewText = { new -> viewModel.onEvent(WorkoutEvent.UpdateReps(new.toInt())) },
                            onIncrement = {
                                viewModel.onEvent(
                                    WorkoutEvent.UpdateReps(
                                        viewModel.state.value.repsBottomBar + 1
                                    )
                                )
                            },
                            onDecrement = {
                                viewModel.onEvent(
                                    WorkoutEvent.UpdateReps(
                                        viewModel.state.value.repsBottomBar - 1
                                    )
                                )
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        TextFieldWithButtons(
                            "Weight",
                            text = { viewModel.state.value.weightBottomBar.toString() },
                            onNewText = { new -> viewModel.onEvent(WorkoutEvent.UpdateWeight(new.toFloat())) },
                            onIncrement = {
                                viewModel.onEvent(
                                    WorkoutEvent.UpdateWeight(
                                        viewModel.state.value.weightBottomBar +
                                                equipment2increment[currentExercise!!.equipment]!!
                                    )
                                )
                            },
                            onDecrement = {
                                viewModel.onEvent(
                                    WorkoutEvent.UpdateWeight(
                                        viewModel.state.value.weightBottomBar -
                                                equipment2increment[currentExercise!!.equipment]!!
                                    )
                                )
                            }
                        )
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                viewModel.onEvent(
                                    WorkoutEvent.CompleteSet(
                                        pagerState.currentPage
                                    )
                                )
                            },
                            Modifier.fillMaxWidth()
                        ) {
                            Text("Complete")
                        }
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
                PGCSmallTopBar(scrollBehavior = scrollBehavior, navController = navController) {
//                    Text(programName)
                }
            },
            floatingActionButton = {
            LargeFloatingActionButton(onClick = { navController.navigate(
                "${MainScreen.ExercisesByMuscle.route}/" +
                        " /" +
                        "$programId"
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

@Composable
fun ExerciseSettingsMenu() {
    Box(
        modifier = Modifier.wrapContentSize()
    ) {
        var expanded by remember { mutableStateOf(false) }
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Localized description"
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Change exercise") },
                onClick = { /* Handle edit! */ },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = null
                    )
                })
            DropdownMenuItem(
                text = { Text("Send Feedback") },
                onClick = { /* Handle send feedback! */ },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Email,
                        contentDescription = null
                    )
                })
            DropdownMenuItem(
                text = { Text("Cancel workout") },
                onClick = { /* Handle cancel! */ },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = null
                    )
                })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.TextFieldWithButtons(
    prompt: String,
    text: () -> String,
    onNewText: (String) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(verticalAlignment = CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, true)
    ) {
        IconButton(onClick = onDecrement, modifier = Modifier.weight(0.3f)) {
            Icon(Icons.Filled.Remove, null)
        }
        OutlinedTextField(
            value = text(),
            onValueChange = onNewText,
            singleLine = true,
            label = { Text(prompt) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .widthIn(1.dp, Dp.Infinity)
                .heightIn(1.dp, Dp.Infinity)
                .weight(0.5f)
        )
        IconButton(onClick = onIncrement, modifier = Modifier.weight(0.3f)) {
            Icon(Icons.Filled.Add, null)
        }
    }
}