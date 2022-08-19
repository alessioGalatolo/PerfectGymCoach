package com.anexus.perfectgymcoach.ui

import android.text.format.DateUtils
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.data.exercise.ExerciseRecord
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import com.anexus.perfectgymcoach.ui.components.CancelWorkoutDialog
import com.anexus.perfectgymcoach.ui.components.FullScreenImageCard
import com.anexus.perfectgymcoach.viewmodels.WorkoutEvent
import com.anexus.perfectgymcoach.viewmodels.WorkoutViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import kotlin.time.DurationUnit
import kotlin.time.toDuration


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalPagerApi::class
)
@Composable
fun Workout(navController: NavHostController, programId: Long,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    viewModel.onEvent(WorkoutEvent.GetWorkoutExercises(programId))

    CancelWorkoutDialog(
        dialogueIsOpen = viewModel.state.value.cancelWorkoutDialogOpen,
        toggleDialog = { viewModel.onEvent(WorkoutEvent.ToggleCancelWorkoutDialog) },
        cancelWorkout = { viewModel.onEvent(WorkoutEvent.CancelWorkout); navController.popBackStack() },
        deleteData = { viewModel.onEvent(WorkoutEvent.DeleteCurrentRecords) }
    )
    var currentExercise: WorkoutExercise? = null
    val currentExerciseRecords: List<ExerciseRecord> = viewModel.state.value.currentExerciseRecords
    var timer = ""
    if (viewModel.state.value.workoutTime != null){
        timer = " " + DateUtils.formatElapsedTime(viewModel.state.value.workoutTime!!)
    }

    val haptic = LocalHapticFeedback.current

    val scope = rememberCoroutineScope()

    val title = @Composable { Text(currentExercise?.name ?: "End") }

    val pagerState = rememberPagerState()

    val completeWorkout: () -> Unit = {    // TODO: should go to recap screen
        viewModel.onEvent(WorkoutEvent.FinishWorkout)
        navController.popBackStack()
    }

    if (viewModel.state.value.workoutExercises.isNotEmpty()) {
        if (pagerState.currentPage < viewModel.state.value.workoutExercises.size) {
            currentExercise = viewModel.state.value.workoutExercises[pagerState.currentPage]
            viewModel.onEvent(WorkoutEvent.GetExerciseRecords(currentExercise.extExerciseId, pagerState.currentPage))
        }

        FullScreenImageCard(
            topAppBarNavigationIcon = {
                IconButton(onClick = {
                    if (viewModel.state.value.workoutTime == null)
                        navController.popBackStack()
                    else
                        viewModel.onEvent(WorkoutEvent.ToggleCancelWorkoutDialog)
                }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Go back" // TODO: cancel workout?
                    )
                }
            },
            topAppBarActions = {
                Row(verticalAlignment = CenterVertically) {
                    Text(timer, style = MaterialTheme.typography.titleLarge)
                    if (timer.isNotEmpty()) {
                        TextButton(onClick = completeWorkout ) {
                            Text("Finish")
                        }
                    }
                }
            },
            title = title,
            image = { modifier ->
                Box (Modifier.wrapContentHeight(Top), contentAlignment = TopCenter) {
                    Image(
                        painterResource(id = R.drawable.sample_image),
                        null,
                        modifier
                    ) // TODO: image of exercise
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
                        count = viewModel.state.value.workoutExercises.size+1,
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Top
                    ) { page ->
                        if (page == viewModel.state.value.workoutExercises.size) {
                            // page for finishing the workout
                            Text("Workout completed")
                        } else {
                            val repsDone = mutableStateOf(0)
                            if (viewModel.state.value.currentExerciseCurrentRecord != null) {
                                repsDone.value =
                                    viewModel.state.value.currentExerciseCurrentRecord!!.reps.size
                            }
                            var checkedNumberReps by remember { mutableStateOf(0) }
                            checkedNumberReps = repsDone.value
                            Column {
                                // content
                                Row(verticalAlignment = CenterVertically) {
                                    Text(
                                        "Current", Modifier.padding(vertical = 8.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                    ExerciseSettingsMenu()
                                    //                                    TextButton(onClick = { /*TODO*/ }) {
                                    //                                        Text("Change exercise")
                                    //                                    }
                                }
                                ElevatedCard(Modifier.fillMaxWidth()) {
                                    Column(
                                        Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        viewModel.state.value.workoutExercises[page].reps.forEachIndexed { setCount, repsCount ->
                                            val toBeDone = repsDone.value <= setCount
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
                                                        checkedNumberReps = setCount
                                                    })
                                            ) {
                                                FilledIconToggleButton(
                                                    enabled = toBeDone,
                                                    checked = checkedNumberReps == setCount,
                                                    onCheckedChange = {
                                                        checkedNumberReps = setCount
                                                    }) {
                                                    Text((setCount + 1).toString())
                                                }
                                                Spacer(Modifier.width(8.dp))
                                                if (toBeDone) {
                                                    Text("Reps: $repsCount Weight: .. kg")
                                                } else {
                                                    Text(
                                                        "Reps: ${viewModel.state.value.currentExerciseCurrentRecord!!.reps[setCount]} " +
                                                                "Weight: ${viewModel.state.value.currentExerciseCurrentRecord!!.weights[setCount]}",
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
                                                    style = MaterialTheme.typography.bodyLarge,
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
                                                            "Reps: $rep Weight: ${record.weights[index]} kg",
                                                            color = MaterialTheme.colorScheme.outline
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
                            ?: 0) >= currentExercise.reps.size
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
                        val reps =
                            rememberSaveable {
                                mutableStateOf(
                                    currentExercise.reps[0].toString()
                                )
                            } // fixme, does not update on page (exercise) change
                        val weight = rememberSaveable { mutableStateOf(0f.toString()) }
                        Row (Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly) {
                            TextFieldWithButtons(
                                "Reps",
                                text = reps,
                                onIncrement = { txt -> (txt.toInt() + 1).toString() },
                                onDecrement = { txt -> (txt.toInt() - 1).toString() }
                            )
                            Spacer(Modifier.width(8.dp))
                            TextFieldWithButtons(
                                "Weight",
                                text = weight,
                                onIncrement = { txt -> (txt.toFloat() + 2).toString() },
                                onDecrement = { txt -> (txt.toFloat() - 2).toString() }
                            ) // FIXME: equipment2increment[currentExercise?]
                        }
                        Row(Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    viewModel.onEvent(WorkoutEvent.CompleteSet(
                                        reps.value.toInt(),
                                        weight.value.toFloat(),
                                        currentExercise.extExerciseId,
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
    text: MutableState<String>,
    onIncrement: (String) -> String,
    onDecrement: (String) -> String
) {
    Row(verticalAlignment = CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, true)
    ) {
        IconButton(onClick = { text.value = onDecrement(text.value) }, modifier = Modifier.weight(0.3f)) {
            Icon(Icons.Filled.Remove, null)
        }
        OutlinedTextField(
            value = text.value,
            onValueChange = { text.value = it },
            singleLine = true,
            label = { Text(prompt) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .widthIn(1.dp, Dp.Infinity)
                .heightIn(1.dp, Dp.Infinity)
                .weight(0.5f)
        )
        IconButton(onClick = { text.value = onIncrement(text.value) }, modifier = Modifier.weight(0.3f)) {
            Icon(Icons.Filled.Add, null)
        }
    }
}