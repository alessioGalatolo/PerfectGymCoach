package com.anexus.perfectgymcoach.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anexus.perfectgymcoach.data.exercise.ExerciseRecord
import com.anexus.perfectgymcoach.data.exercise.WorkoutExerciseAndInfo
import com.anexus.perfectgymcoach.viewmodels.WorkoutEvent
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalPagerApi::class, ExperimentalFoundationApi::class)
@Composable
fun ExercisePage(
    pagerState: PagerState,
    workoutExercisesAndInfo: List<WorkoutExerciseAndInfo>,
    setsDone: State<Int>,
    title: @Composable () -> Unit,
    addSet: () -> Unit,
    currentExerciseRecords: List<ExerciseRecord>,
    ongoingRecord: ExerciseRecord?,
    restCounter: Long?
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    Column(
        Modifier.padding(top = 8.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage-1) }},
                enabled = pagerState.currentPage > 0,
                modifier = Modifier
                    .wrapContentSize()
                    .weight(1f, false)
            ) {
                Icon(Icons.Outlined.ArrowBack, null)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .wrapContentSize()
                    .weight(4f, true)
            ) {
                ProvideTextStyle(
                    value = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center)) {
                    CompositionLocalProvider(
                        content = title
                    )
                }
//                                ExerciseSettingsMenu()
            }
            IconButton(
                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage+1) }},
                enabled = pagerState.currentPage < pagerState.pageCount-1,
                modifier = Modifier
                    .wrapContentSize()
                    .weight(1f, false)
            ) {
                Icon(Icons.Outlined.ArrowForward, null)
            }
        }
        HorizontalPager(
            count = workoutExercisesAndInfo.size+1,
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) { page ->
            if (page == workoutExercisesAndInfo.size) {
                // page for finishing the workout
                WorkoutFinishPage()
            } else {
                Column (Modifier.padding(horizontal = 16.dp)){
                    // content
                    if (restCounter != null){
                        Text("Time before next set: ", Modifier.align(CenterHorizontally))
                        Text("$restCounter", Modifier.align(CenterHorizontally),
                            style = MaterialTheme.typography.headlineMedium)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Current", Modifier.padding(vertical = 8.dp),
                            fontWeight = FontWeight.Bold
                        )
                        ExerciseSettingsMenu()
                    }
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(8.dp),
                            horizontalAlignment = CenterHorizontally
                        ) {
                            Text("Rest: ${workoutExercisesAndInfo[page].rest}s", Modifier.align(Alignment.Start))
                            workoutExercisesAndInfo[page].reps.forEachIndexed { setCount, repsCount ->
                                val toBeDone = setsDone.value <= setCount
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
                                            "Reps: ${ongoingRecord!!.reps[setCount]} " +
                                                    "Weight: ${ongoingRecord.weights[setCount]} kg",
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                            TextButton(onClick = addSet) {
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
                    var recordsToShow by remember { mutableStateOf(2) }
                    currentExerciseRecords.subList(0, min(currentExerciseRecords.size, recordsToShow)).forEach { record ->  // should maybe become lazy
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(8.dp)) {
                                val dateFormat = SimpleDateFormat("d MMM (yy)")
                                Text(
                                    dateFormat.format(record.date.time),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontStyle = FontStyle.Italic // TODO: add how many days ago
                                ) // FIXME
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
                    if (recordsToShow < currentExerciseRecords.size) {
                        TextButton(
                            onClick = { recordsToShow += 2 },
                            modifier = Modifier.align(CenterHorizontally)
                        ) {
                            Text("Show older records")
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutFinishPage() {
    Text("Workout completed")  // TODO
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