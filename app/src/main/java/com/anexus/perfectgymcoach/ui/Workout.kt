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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.data.exercise.Exercise.Companion.equipment2increment
import com.anexus.perfectgymcoach.data.exercise.ExerciseRecord
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import com.anexus.perfectgymcoach.data.exercise.WorkoutExerciseAndInfo
import com.anexus.perfectgymcoach.ui.components.CancelWorkoutDialog
import com.anexus.perfectgymcoach.ui.components.FullScreenImageCard
import com.anexus.perfectgymcoach.ui.components.PGCSmallTopBar
import com.anexus.perfectgymcoach.viewmodels.WorkoutEvent
import com.anexus.perfectgymcoach.viewmodels.WorkoutViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import kotlin.math.min


@OptIn(ExperimentalFoundationApi::class,
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
    LaunchedEffect(key1 = currentExercise) {
        if (currentExercise != null)
            viewModel.onEvent(
                WorkoutEvent.GetExerciseRecords(
                    currentExercise!!.extExerciseId,
                    pagerState.currentPage
                )
            )
    }
    val currentExerciseRecords: List<ExerciseRecord> = viewModel.state.value.currentExerciseRecords
    val timer = {
        " " + (viewModel.state.value.workoutTime?.let { DateUtils.formatElapsedTime(it) } ?: "")
    }

    val haptic = LocalHapticFeedback.current

    val scope = rememberCoroutineScope()

    val title = @Composable { Text(currentExercise?.name ?: "End", overflow = TextOverflow.Clip) }

    val setsDone = remember { derivedStateOf{
        viewModel.state.value.currentExerciseCurrentRecord?.reps?.size ?: 0
    } }

    LaunchedEffect(currentExerciseRecords, setsDone){
        val currentRecord = currentExerciseRecords.lastOrNull()

        if (currentRecord != null) {
            val index = min(setsDone.value, currentRecord.weights.size-1)
            viewModel.onEvent(WorkoutEvent.UpdateWeight(currentRecord.weights[index]))
        }
    }

    LaunchedEffect(currentExercise, setsDone){
        if (currentExercise != null) {
            viewModel.onEvent(
                WorkoutEvent.UpdateReps(
                    currentExercise!!.reps[setsDone.value]
                )
            )
        }
    }

    val onClose = {
        if (viewModel.state.value.workoutTime == null)
            navController.popBackStack()
        else
            viewModel.onEvent(WorkoutEvent.ToggleCancelWorkoutDialog)
        Unit
    }

    val completeWorkout: () -> Unit = {    // TODO: should go to recap screen
        viewModel.onEvent(WorkoutEvent.FinishWorkout)
        navController.popBackStack()
    }

    if (viewModel.state.value.workoutExercisesAndInfo.isNotEmpty()) {
        BackHandler(onBack = onClose)
        FullScreenImageCard(
            topAppBarNavigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Go back" // TODO: cancel workout?
                    )
                }
            },
            topAppBarActions = {
                Row(verticalAlignment = CenterVertically) {
                    Text(timer(), style = MaterialTheme.typography.titleLarge)
                    if (viewModel.state.value.workoutTime != null) {
                        TextButton(onClick = completeWorkout ) {
                            Text("Finish")
                        }
                    }
                }
            },
            title = title,
            image = { modifier ->
                Box(Modifier.wrapContentHeight(Top), contentAlignment = TopCenter) {
                    Image(
                        painterResource(id =
                            if (pagerState.currentPage == viewModel.state.value.workoutExercisesAndInfo.size)
                                R.drawable.finish_workout
                            else currentExercise!!.image),
                        null,
                        modifier
                    )
                    HorizontalPagerIndicator(
                        pagerState = pagerState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                    )
                }
            },
            content = {
                Column(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage-1) }},
                            enabled = pagerState.currentPage > 0
                        ) {
                            Icon(Icons.Outlined.ArrowBack, null)
                        }
                        Row(
                            verticalAlignment = CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            ProvideTextStyle(value = MaterialTheme.typography.headlineMedium) {
                                CompositionLocalProvider(
                                    content = title
                                )
                            }
//                                ExerciseSettingsMenu()
                        }
                        IconButton(
                            onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage+1) }},
                            enabled = pagerState.currentPage < pagerState.pageCount-1
                        ) {
                            Icon(Icons.Outlined.ArrowForward, null)
                        }
                    }
                    // TODO: add additional page for finishing workout
                    HorizontalPager(
                        count = viewModel.state.value.workoutExercisesAndInfo.size+1,
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Top
                    ) { page ->
                        if (page == viewModel.state.value.workoutExercisesAndInfo.size) {
                            // page for finishing the workout
                            Text("Workout completed")
                        } else {
                            Column {
                                // content
                                Row(verticalAlignment = CenterVertically) {
                                    Text(
                                        "Current", Modifier.padding(vertical = 8.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                    ExerciseSettingsMenu()
                                }
                                ElevatedCard(Modifier.fillMaxWidth()) {
                                    Column(
                                        Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        viewModel.state.value.workoutExercisesAndInfo[page].reps.forEachIndexed { setCount, repsCount ->
                                            val toBeDone = setsDone.value <= setCount
                                            Row(
                                                verticalAlignment = CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .combinedClickable(onLongClick = {
                                                        haptic.performHapticFeedback(
                                                            HapticFeedbackType.LongPress
                                                        )
                                                        // TODO: open dialogue to modify
                                                    }, onClick = {
                                                        if (!toBeDone) {
                                                            // TODO: allow to modify
                                                        } else {
                                                            // TODO: copy reps/weight values
                                                        }
                                                    })
                                            ) {
                                                FilledIconToggleButton(
                                                    enabled = toBeDone,
                                                    checked = setsDone.value == setCount, // FIXME: should check other value
                                                    onCheckedChange = {
//                                                        checkedNumberReps = setCount // FIXME
                                                    }) {
                                                    Text((setCount + 1).toString())
                                                }
                                                Spacer(Modifier.width(8.dp))
                                                if (toBeDone) {
                                                    val currentRecord = currentExerciseRecords.firstOrNull()
                                                    if (currentRecord != null) {
                                                        val index = min(setCount, currentRecord.weights.size-1)
                                                        Text("Reps: $repsCount Weight: ${ currentRecord.weights[index] } kg")
                                                    } else {
                                                        Text("Reps: $repsCount Weight: ... kg")
                                                    }
                                                } else {
                                                    Text(
                                                        "Reps: ${viewModel.state.value.currentExerciseCurrentRecord!!.reps[setCount]} " +
                                                                "Weight: ${viewModel.state.value.currentExerciseCurrentRecord!!.weights[setCount]} kg",
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            }
                                        }
                                        TextButton(onClick = { viewModel.onEvent(WorkoutEvent.AddSetToExercise(page)) }) {
                                            Text("Add set")
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                if (currentExerciseRecords.isNotEmpty()) {
                                    Text(
                                        "History",
                                        Modifier.padding(bottom = 8.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                currentExerciseRecords.sortedByDescending { it.date }
                                    .forEach { record ->
                                        Card(Modifier.fillMaxWidth()) {
                                            Column(Modifier.padding(8.dp)) {
                                                val dateFormat = SimpleDateFormat("d MMM (yy)")
                                                Text(
                                                    dateFormat.format(record.date.time),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontStyle = FontStyle.Italic
                                                ) // FIXME
                                                Text("Tare: ${record.tare}") // FIXME
                                                record.reps.forEachIndexed { index, rep ->
                                                    Row(
                                                        verticalAlignment = CenterVertically,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .combinedClickable(onLongClick = {
                                                                haptic.performHapticFeedback(
                                                                    HapticFeedbackType.LongPress
                                                                )
                                                                // TODO: maybe nothing
                                                            }, onClick = {
                                                                // TODO: copy values to bottom bar
                                                            })
                                                    ) {
                                                        FilledIconToggleButton(checked = false, // FIXME: can use different component?
                                                            onCheckedChange = { }) {
                                                            Text((index + 1).toString())
                                                        }
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(
                                                            "Reps: $rep Weight: ${record.weights[index]} kg"
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                    }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Column(
                    Modifier
                        .padding(it)
                        .padding(horizontal = 16.dp)
                ) {
                    if (viewModel.state.value.workoutTime == null){
                        // workout has not started
                        Button(
                            onClick = { viewModel.onEvent(WorkoutEvent.StartWorkout(programId)) },
                            Modifier.fillMaxWidth()
                        ) {
                            Text("Start workout")
                        }
                    } else if (currentExercise == null){
                        // workout has started and it is on the end page
                        Button(onClick = completeWorkout,
                            modifier = Modifier.fillMaxWidth()) {
                            Text("Complete workout")
                        }
                    } else if ((viewModel.state.value.currentExerciseCurrentRecord?.reps?.size
                            ?: 0) >= currentExercise!!.reps.size
                    ) {
                        // workout started and the user has done all the reps in the page
                        OutlinedButton(
                            onClick = {
                                viewModel.onEvent(WorkoutEvent.AddSetToExercise(pagerState.currentPage))
                                      },
                            Modifier.fillMaxWidth()
                        ) {
                            Text("Add set")
                        }
                        Button(
                            onClick = { scope.launch{pagerState.animateScrollToPage(pagerState.currentPage+1) }},
                            Modifier.fillMaxWidth()
                        ) {
                            Text("Next exercise")
                        }

                    } else {
                        // normal case
                        Row (Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly) {
                            TextFieldWithButtons(
                                "Reps",
                                text = { viewModel.state.value.repsBottomBar.toString() },
                                onNewText = { new -> viewModel.onEvent(WorkoutEvent.UpdateReps(new.toInt())) },
                                onIncrement = { viewModel.onEvent(WorkoutEvent.UpdateReps(
                                    viewModel.state.value.repsBottomBar + 1
                                )) },
                                onDecrement = { viewModel.onEvent(WorkoutEvent.UpdateReps(
                                    viewModel.state.value.repsBottomBar - 1
                                )) }
                            )
                            Spacer(Modifier.width(8.dp))
                            TextFieldWithButtons(
                                "Weight",
                                text = { viewModel.state.value.weightBottomBar.toString() },
                                onNewText = { new -> viewModel.onEvent(WorkoutEvent.UpdateWeight(new.toFloat())) },
                                onIncrement = { viewModel.onEvent(WorkoutEvent.UpdateWeight(
                                    viewModel.state.value.weightBottomBar +
                                            equipment2increment[currentExercise!!.equipment]!!
                                )) },
                                onDecrement = { viewModel.onEvent(WorkoutEvent.UpdateWeight(
                                    viewModel.state.value.weightBottomBar -
                                            equipment2increment[currentExercise!!.equipment]!!
                                )) }
                            )
                        }
                        Row(Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    viewModel.onEvent(WorkoutEvent.CompleteSet(
                                        currentExercise!!.extExerciseId,
                                        pagerState.currentPage
                                    ))
                                },
                                Modifier.fillMaxWidth()
                            ) {
                                Text("Complete")
                            }
                        }
                    }
                }
            }
        )
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