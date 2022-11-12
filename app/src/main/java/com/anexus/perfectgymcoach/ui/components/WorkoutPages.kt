package com.anexus.perfectgymcoach.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anexus.perfectgymcoach.data.exercise.ExerciseRecord
import com.anexus.perfectgymcoach.data.exercise.WorkoutExerciseAndInfo
import com.anexus.perfectgymcoach.data.workout_record.WorkoutRecord
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import kotlin.math.min

@OptIn(ExperimentalPagerApi::class, ExperimentalFoundationApi::class)
@Composable
fun ExercisePage(
    pagerState: PagerState,
    workoutTime: Long?,
    workoutExercisesAndInfo: List<WorkoutExerciseAndInfo>,
    setsDone: State<Int>,
    title: @Composable () -> Unit,
    addSet: () -> Unit,
    updateBottomBar: (Int, Float) -> Unit,
    currentExerciseRecords: List<ExerciseRecord>,
    ongoingRecord: ExerciseRecord?,
    restCounter: Long?,
    workoutIntensity: MutableState<WorkoutRecord.WorkoutIntensity>
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
            count = if (workoutTime != null) workoutExercisesAndInfo.size+1 else workoutExercisesAndInfo.size,
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) { page ->
            if (page == workoutExercisesAndInfo.size) {
                // page for finishing the workout
                WorkoutFinishPage(workoutTime!!, workoutIntensity)
            } else {
                Column (Modifier.padding(horizontal = 16.dp)){
                    if (workoutExercisesAndInfo[page].note.isNotBlank()) {
                        Text(text = buildAnnotatedString {
                                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                    append("Note: ")
                                }
                                append(workoutExercisesAndInfo[page].note)
                            }, modifier = Modifier.align(CenterHorizontally))
                    }
                    // content
                    if (restCounter != null){
                        Text("Time before next set: ", Modifier.align(CenterHorizontally))
                        Text("$restCounter", Modifier.align(CenterHorizontally),
                            style = MaterialTheme.typography.headlineMedium)
                        if (restCounter == 1L || restCounter == 2L || restCounter == 3L) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Current" +
                            if (workoutExercisesAndInfo[page].supersetExercise != null) " - Part of superset" else "",
                            Modifier.padding(vertical = 8.dp),
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
                                val repsInRow: String
                                val weightInRow: String
                                if (toBeDone || setCount >= ongoingRecord!!.reps.size) {
                                    repsInRow = repsCount.toString()
                                    val currentRecord = currentExerciseRecords.firstOrNull()
                                    weightInRow = if (currentRecord != null) {
                                        val index = min(setCount, currentRecord.weights.size-1)
                                        currentRecord.weights[index].toString()
                                    } else {
                                        "..."
                                    }
                                } else {
                                    repsInRow = ongoingRecord.reps[setCount].toString()
                                    weightInRow = ongoingRecord.weights[setCount].toString()
                                }
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
                                            haptic.performHapticFeedback(
                                                HapticFeedbackType.TextHandleMove // FIXME: not right haptic
                                            )
                                            updateBottomBar(
                                                repsInRow.toInt(),
                                                weightInRow.toFloatOrNull() ?: 0f
                                            )
//                                            if (!toBeDone) {
//                                                // TODO: allow to modify
//                                            } else {
//                                                // TODO: change bottom bar
//                                            }
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
                                    Text("Reps: $repsInRow Weight: $weightInRow kg",
                                        color = if (toBeDone) LocalContentColor.current else MaterialTheme.colorScheme.outline
                                    )
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
                                                updateBottomBar(rep, record.weights[index])
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WorkoutFinishPage(
    workoutTime: Long,
    workoutIntensity: MutableState<WorkoutRecord.WorkoutIntensity>
) {
    Column(
        Modifier
            .padding(horizontal = 8.dp)
            .padding(top = 8.dp)){
        Text("Total workout time: ${DateUtils.formatElapsedTime(workoutTime)}", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Row (Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically){
            Text("How intense was this workout?"/*, Modifier.weight(1f)*/)
            var expanded by remember { mutableStateOf(false) }
            Spacer(Modifier.width(16.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier/*.weight(1f)*/
                    .widthIn(1.dp, Dp.Infinity)
                    .heightIn(1.dp, Dp.Infinity)
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = ""/*selectedOptionText.description.substringBefore("(")*/,
                    onValueChange = {},
                    leadingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(8.dp)){
                            repeat(WorkoutRecord.WorkoutIntensity.values().size){
                                Icon(Icons.Default.FitnessCenter, null,
                                    tint = if (it < workoutIntensity.value.ordinal+1) LocalContentColor.current else Color.Transparent)
                            }
                        }
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(containerColor = Color.Transparent),
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    WorkoutRecord.WorkoutIntensity.values().forEachIndexed { index, selectionOption ->
                        DropdownMenuItem(
                            text = {},
                            leadingIcon = {
                                Row(){
                                    repeat(index+1){
                                        Icon(Icons.Default.FitnessCenter, null)
                                    }
                                }
                            },
                            onClick = {
                                workoutIntensity.value = selectionOption
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        TextButton(onClick = { /*TODO*/ }, modifier = Modifier.align(CenterHorizontally)) {
            Text("Add exercise to workout")
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