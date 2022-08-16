package com.anexus.perfectgymcoach.ui

import android.view.RoundedCorner
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialogDefaults.titleContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterStart
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.data.exercise.ExerciseRecord
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import com.anexus.perfectgymcoach.ui.components.FullScreenImageCard
import com.anexus.perfectgymcoach.viewmodels.WorkoutEvent
import com.anexus.perfectgymcoach.viewmodels.WorkoutViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import java.text.SimpleDateFormat
import kotlin.time.DurationUnit
import kotlin.time.toDuration


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun Workout(navController: NavHostController, programId: Long,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    viewModel.onEvent(WorkoutEvent.GetWorkoutExercises(programId))

    var currentExercise: WorkoutExercise? = null
    val currentExerciseRecords: List<ExerciseRecord> = viewModel.state.value.currentExerciseRecords
    if (viewModel.state.value.currentExercise != null &&
            viewModel.state.value.workoutExercises.isNotEmpty()) {
        currentExercise =
            viewModel.state.value.workoutExercises[viewModel.state.value.currentExercise!!]
        viewModel.onEvent(WorkoutEvent.GetExerciseRecords(currentExercise.extExerciseId))
    }
    var timer = ""
    if (viewModel.state.value.workoutStarted != null){
        timer = " " + viewModel.state.value.workoutStarted!!.toDuration(DurationUnit.SECONDS).toString()
    }

    val haptic = LocalHapticFeedback.current

    val title = @Composable { Text(currentExercise?.name ?: "") }

    FullScreenImageCard(
        topAppBarNavigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Go back" // TODO: cancel workout?
                )
            }
        },
        topAppBarActions = {
            Row (verticalAlignment = CenterVertically) {
                Text(timer, style = MaterialTheme.typography.titleLarge)
                if (timer.isNotEmpty()){
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(Icons.Default.Done, null)
                    }
                }
            }
        },
        title = title,
        image = {
            Image(
                painterResource(id = R.drawable.sample_image),
                null,
                modifier = it
            ) // TODO: image of exercise
        },
        content = {
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ){
                    IconButton(onClick = { viewModel.onEvent(WorkoutEvent.PreviousExercise) },
                        enabled = (viewModel.state.value.currentExercise ?: 0) > 0
                    ) {
                        Icon(Icons.Outlined.ArrowBack, null)
                    }
                    Row (verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center){
                        ProvideTextStyle(value = MaterialTheme.typography.headlineMedium) {
                            CompositionLocalProvider(
                                content = title
                            )
                        }
//                                    ExerciseSettingsMenu()
                    }
                    IconButton(onClick = { viewModel.onEvent(WorkoutEvent.NextExercise) },
                        enabled =  (viewModel.state.value.currentExercise ?: Int.MAX_VALUE) < viewModel.state.value.workoutExercises.size-1) {
                        Icon(Icons.Outlined.ArrowForward, null)
                    }
                }

                // content
                if (currentExercise != null) {
                    Row (verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Current", Modifier.padding(vertical = 8.dp),
                            fontWeight = FontWeight.Bold
                        )
                        ExerciseSettingsMenu()
//                                    TextButton(onClick = { /*TODO*/ }) {
//                                        Text("Change exercise")
//                                    }
                    }
                    ElevatedCard (Modifier.fillMaxWidth()){
                        var checkedNumber by remember { mutableStateOf(0) }
                        Column (
                            Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally){
                            currentExercise.reps.forEachIndexed() {setCount, repsCount ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(onLongClick = {
                                            haptic.performHapticFeedback(
                                                HapticFeedbackType.LongPress
                                            )
                                            // TODO: open dialogue to modify
                                        }, onClick = {
                                            checkedNumber = setCount
                                        })
                                ) {
                                    FilledIconToggleButton(checked = checkedNumber == setCount,
                                        onCheckedChange = { checkedNumber = setCount }) {
                                        Text((setCount+1).toString())
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text("Reps: $repsCount Weight: .. kg")
                                }
                            }
                            TextButton(onClick = { /*TODO*/ }) {
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
                    currentExerciseRecords.forEach { record ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(8.dp)) {
                                Text("${record.date}", style = MaterialTheme.typography.bodyLarge) // FIXME
                                Text("Tare: ${record.tare}") // FIXME
                                record.reps.forEachIndexed { index, rep ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
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
                                            Text((index+1).toString())
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text("Reps: $rep Weight: ${record.weights[index]} kg",
                                            color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        },
        bottomBar = {
            Column (
                Modifier.padding(it).padding(horizontal = 16.dp)){
                if (viewModel.state.value.workoutStarted != null) {
                    Row {
                        val reps =
                            remember { mutableStateOf(currentExercise?.reps?.get(0) ?: 0) } // fixme
                        TextFieldWithButtons(
                            "Reps",
                            initialValue = reps,
                            increment = 1
                        )
                        Spacer(Modifier.width(8.dp))
                        TextFieldWithButtons(
                            "Weight",
                            initialValue = mutableStateOf(0),
                            2
                        ) // FIXME: equipment2increment[currentExercise?]
                    }
                }
                Row (Modifier.fillMaxWidth()){
                    if (viewModel.state.value.workoutStarted != null) {
                        Button(onClick = { /*TODO*/ }, Modifier.fillMaxWidth()) {
                            Text("Complete")
                        }
                    } else {
                        Button(onClick = {viewModel.onEvent(WorkoutEvent.StartWorkout)},
                            Modifier.fillMaxWidth()){
                            Text("Start workout")
                        }
                    }
                }
            }
        }
    )
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
    initialValue: MutableState<Int> = mutableStateOf(0),
    increment: Int
) {
    Row(verticalAlignment = CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, true)
    ) {
        var text by remember { mutableStateOf("${initialValue.value}") }
        IconButton(onClick = { text = "${text.toInt() - increment}" }, modifier = Modifier.weight(0.3f)) {
            Icon(Icons.Filled.Remove, null)
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            label = { Text(prompt) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .widthIn(1.dp, Dp.Infinity)
                .heightIn(1.dp, Dp.Infinity)
                .weight(0.5f)
        )
        IconButton(onClick = { text = "${text.toInt() + increment}" }, modifier = Modifier.weight(0.3f)) {
            Icon(Icons.Filled.Add, null)
        }
    }
}

//@Preview
//@Composable
//fun WorkoutScreenPreview() {
//
//}