package agdesigns.elevatefitness.ui.components

import android.text.format.DateUtils
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.exercise.*
import agdesigns.elevatefitness.data.workout_exercise.WorkoutExercise
import agdesigns.elevatefitness.data.workout_record.WorkoutRecord
import agdesigns.elevatefitness.ui.barbellFromWeight
import agdesigns.elevatefitness.ui.maybeKgToLb
import agdesigns.elevatefitness.ui.maybeLbToKg
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.ui.platform.LocalDensity
import com.ramcosta.composedestinations.generated.destinations.ExercisesByMuscleDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import kotlin.math.min

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun ExercisePage(
    pagerState: PagerState,
    workoutTimeMillis: Long,  // is 0L when workout has not started
    workoutExercises: List<WorkoutExercise>,
    workoutId: Long,
    navigator: DestinationsNavigator,
    setsDone: State<Int>,
    fabHeight: Dp,
    title: @Composable () -> Unit,
    exerciseDescription: String,
    addSet: () -> Unit,
    updateBottomBar: (Int?, Float?) -> Unit,
    currentExerciseRecords: List<ExerciseRecordAndEquipment>,
    ongoingRecord: ExerciseRecordAndEquipment?,
    restCounterMillis: Long?,
    workoutIntensity: MutableState<WorkoutRecord.WorkoutIntensity>,
    useImperialSystem: Boolean,
    tare: Float,
    updateExerciseProbability: (Int) -> Unit,
    updateTare: (Float) -> Unit,
    updateValues: (Int, Float, Int, Int) -> Unit,
    toggleOtherEquipment: () -> Unit,
    changeExercise: (Int, Int) -> Unit,
    removeExercise: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var infoDialogOpen by remember { mutableStateOf(false) }
    InfoDialog(dialogueIsOpen = infoDialogOpen, toggleDialogue = { infoDialogOpen = !infoDialogOpen }) {
        Text(exerciseDescription)
    }
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
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Previous exercise")
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
            }
            IconButton(
                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage+1) }},
                enabled = pagerState.currentPage < if (workoutTimeMillis > 0L) workoutExercises.size else workoutExercises.size-1,
                modifier = Modifier
                    .wrapContentSize()
                    .weight(1f, false)
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Next exercise")
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) { page ->
            if (page == workoutExercises.size) {
                // page for finishing the workout
                WorkoutFinishPage(workoutTimeMillis, workoutIntensity, workoutId, fabHeight, navigator)
            } else {
                Column (Modifier.padding(horizontal = 16.dp)){
                    if (workoutExercises[page].note.isNotBlank()) {
                        Text(text = buildAnnotatedString {
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                append("Note: ")
                            }
                            append(workoutExercises[page].note)
                        }, modifier = Modifier.align(CenterHorizontally))
                    }
                    Row (Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                        var likesExercise by rememberSaveable { mutableStateOf(false) }
                        var dislikesExercise by rememberSaveable { mutableStateOf(false) }

                        IconButton(onClick = {
                            // if already disliked, remove dislike
                            if (dislikesExercise) {
                                dislikesExercise = false
                                // increase prob to remove previous dislike
                                updateExerciseProbability(1)
                            } else {
                                val increment = if (likesExercise) -1 else -2
                                updateExerciseProbability(increment)
                                likesExercise = false
                                dislikesExercise = true
                            }
                        }) {
                            Icon(
                                if (dislikesExercise) Icons.Default.ThumbDown else Icons.Outlined.ThumbDown,
                                "Dislike this exercise"
                            )
                        }
                        // FIXME: it is not clear that this is info about the exercise, not about the like/dislike
                        IconButton(
                            onClick = { infoDialogOpen = true }
                        ) {
                            Icon(Icons.Outlined.Info, "Exercise description")
                        }
                        IconButton(onClick = {
                            if (likesExercise) {
                                likesExercise = false
                                updateExerciseProbability(-1)
                            } else {
                                val increment = if (dislikesExercise) 1 else 2
                                updateExerciseProbability(increment)
                                likesExercise = true
                                dislikesExercise = false
                            }

                        }) {
                            Icon(
                                if (likesExercise) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                                "Like this exercise"
                            )
                        }
                    }
                    // content
                    if (restCounterMillis != null){
                        Text("Time before next set: ", Modifier.align(CenterHorizontally))
                        Text("${restCounterMillis / 1000}", Modifier.align(CenterHorizontally),
                            style = MaterialTheme.typography.headlineMedium)
                        LaunchedEffect(restCounterMillis / 1000) {
                            if (restCounterMillis / 1000 == 1L || restCounterMillis / 1000 == 2L || restCounterMillis / 1000 == 3L) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Current" +
                                    if (workoutExercises[page].supersetExercise != null) " - Part of superset" else "",
                            Modifier.padding(vertical = 8.dp),
                            fontWeight = FontWeight.Bold
                        )

                        AnimatedVisibility(
                            visible = workoutTimeMillis > 0L,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            ExerciseSettingsMenu(changeExercise = {
                                changeExercise(page, workoutExercises.size)
                                navigator.navigate(
                                    ExercisesByMuscleDestination(
                                        programName = "Current workout",
                                        workoutId = workoutId,
                                        returnAfterAdding = true
                                    )
                                )
                            }, removeExercise = {
                                removeExercise(page)
                            }, addExercise = {
                                navigator.navigate(
                                    ExercisesByMuscleDestination(
                                        programName = "Current workout",
                                        workoutId = workoutId,
                                        returnAfterAdding = true
                                    )
                                )
                                scope.launch { pagerState.animateScrollToPage(pagerState.pageCount-1) }
                            })
                        }
                    }
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(dimensionResource(R.dimen.card_inner_padding)),
                            horizontalAlignment = CenterHorizontally
                        ) {
                            Text("Rest: " +
                                    "${workoutExercises[page].rest[
                                            min(setsDone.value, workoutExercises[page].rest.size-1)
                                    ]}s", Modifier.align(Alignment.Start))

                            // if barbell, allow to add barbell weight (used for volume)
                            AnimatedVisibility(
                                visible = workoutTimeMillis > 0L &&
                                        workoutExercises[page].equipment == Exercise.Equipment.BARBELL,
                                enter = slideInVertically() + fadeIn(),
                                exit = slideOutVertically() + fadeOut()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Barbell: ")
                                    val barbellName: String =
                                            barbellFromWeight(tare, useImperialSystem, false)

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        var expanded by remember { mutableStateOf(false) }
                                        ExposedDropdownMenuBox(
                                            expanded = expanded,
                                            onExpandedChange = { expanded = !expanded },
                                            modifier = Modifier
                                                .widthIn(1.dp, Dp.Infinity)
                                                .weight(1f)
                                        ) {
                                            OutlinedTextField(
                                                readOnly = true,
                                                value = barbellName,
                                                onValueChange = {},
                                                trailingIcon = {
                                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                                        expanded = expanded
                                                    )
                                                },
                                                colors = ExposedDropdownMenuDefaults.textFieldColors(
                                                    focusedContainerColor = Color.Transparent,
                                                    unfocusedContainerColor = Color.Transparent,
                                                    errorContainerColor = Color.Transparent
                                                ),
                                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                            )
                                            ExposedDropdownMenu(
                                                expanded = expanded,
                                                onDismissRequest = { expanded = false },
                                            ) {
                                                ExerciseRecord.BarbellType.entries.forEach { selectionOption ->
                                                    val fullName = if (selectionOption == ExerciseRecord.BarbellType.OTHER)
                                                        selectionOption.barbellName + " (Custom value)"
                                                    else
                                                        selectionOption.barbellName +
                                                            " (${selectionOption.weight[useImperialSystem]} ${if (useImperialSystem) "lb" else "kg"})"
                                                    DropdownMenuItem(
                                                        text = { Text(fullName) },
                                                        onClick = {
                                                            expanded = false
                                                            updateTare(maybeLbToKg(selectionOption.weight[useImperialSystem]!!, useImperialSystem))
                                                            if (selectionOption == ExerciseRecord.BarbellType.OTHER)
                                                                toggleOtherEquipment()
                                                        },
                                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }
                            }
                            workoutExercises[page].reps.forEachIndexed { setCount, repsCount ->
                                val toBeDone = setsDone.value <= setCount
                                val repsInRow: String
                                val weightInRow: String
                                if (toBeDone || setCount >= (ongoingRecord?.reps?.size ?: 0)) {
                                    repsInRow = repsCount.toString()
                                    val currentRecord = currentExerciseRecords.firstOrNull()
                                    weightInRow = if (currentRecord != null) {
                                        val index = min(setCount, currentRecord.weights.size-1)
                                        maybeKgToLb(currentRecord.weights[index], useImperialSystem).toString()
                                    } else {
                                        "..."
                                    }
                                } else {
                                    // if ongoingRecord is null, it should go in the other branch anyway
                                    repsInRow = ongoingRecord!!.reps[setCount].toString()
                                    weightInRow = maybeKgToLb(ongoingRecord.weights[setCount], useImperialSystem).toString()
                                }
                                var dialogIsOpen by rememberSaveable { mutableStateOf(false) }
                                ChangeRepsWeightDialog(
                                    dialogIsOpen,
                                    { dialogIsOpen = !dialogIsOpen },
                                    repsInRow,
                                    weightInRow,
                                    { reps, weight -> updateValues(reps, maybeLbToKg(weight, useImperialSystem), page, setCount) }
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(onLongClick = {
                                            if (!toBeDone) {
                                                haptic.performHapticFeedback(
                                                    HapticFeedbackType.LongPress
                                                )
                                                dialogIsOpen = true
                                            }
                                        }, onClick = {
                                            haptic.performHapticFeedback(
                                                HapticFeedbackType.TextHandleMove // FIXME: not right haptic
                                            )
                                            updateBottomBar(
                                                repsInRow.toInt(),
                                                weightInRow.toFloatOrNull()
                                            )
                                        })
                                ) {
                                    FilledIconToggleButton(
                                        enabled = toBeDone,
                                        checked = setsDone.value == setCount,
                                        onCheckedChange = {}
                                    ) {
                                        Text((setCount + 1).toString())
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text("Reps: $repsInRow Weight: $weightInRow " + if(useImperialSystem) "lb" else "kg",
                                        color = if (toBeDone) LocalContentColor.current else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            AnimatedVisibility(
                                visible = workoutTimeMillis > 0L,
                                enter = slideInVertically() + fadeIn(),
                                exit = slideOutVertically() + fadeOut()
                            ) {
                                TextButton(onClick = addSet) {
                                    Text("Add set")
                                }
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
                    var recordsToShow by remember { mutableIntStateOf(2) }
                    currentExerciseRecords.subList(0, min(currentExerciseRecords.size, recordsToShow)).forEach { record ->  // should maybe become lazy
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(dimensionResource(R.dimen.card_inner_padding))) {
                                val formatter = DateTimeFormatter.ofPattern("d MMM (yy)")
                                Text(
                                    record.date.format(formatter),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontStyle = FontStyle.Italic // TODO: add how many days ago
                                )
                                if (record.equipment == Exercise.Equipment.BARBELL) {
                                    Text("Barbell used: " + barbellFromWeight(record.tare, useImperialSystem, true)
                                    )
                                } else if (record.equipment == Exercise.Equipment.BODY_WEIGHT) {
                                    // FIXME: bug where bodyweight = 0? <- this may have been fixed with the new state update
                                    Text("Bodyweight at the time: ${maybeKgToLb(record.tare, useImperialSystem)} " + if(useImperialSystem) "lb" else "kg")
                                }
                                record.reps.forEachIndexed { index, rep ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(onLongClick = {

                                            }, onClick = {
                                                updateBottomBar(
                                                    rep,
                                                    maybeKgToLb(
                                                        record.weights[index],
                                                        useImperialSystem
                                                    )
                                                )
                                            })
                                    ) {
                                        FilledIconToggleButton(checked = false, // FIXME: can use different component?
                                            onCheckedChange = { }) {
                                            Text((index + 1).toString())
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Reps: $rep Weight: ${maybeKgToLb(record.weights[index], useImperialSystem)} " + if(useImperialSystem) "lb" else "kg"
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(dimensionResource(R.dimen.card_space_between)))
                    }
                    if (recordsToShow < currentExerciseRecords.size) {
                        TextButton(
                            onClick = { recordsToShow += 2 },
                            modifier = Modifier.align(CenterHorizontally)
                        ) {
                            Text("Show older records")
                        }
                        Spacer(Modifier.height(dimensionResource(R.dimen.card_space_between)))
                    }
                    if (fabHeight > 0.dp) {
                        Spacer(Modifier.height(fabHeight))
                    }
                }
            }
            // Spacer for nav bar
            val localDensity = LocalDensity.current
            Spacer(Modifier.height(with(localDensity) { WindowInsets.navigationBars.getBottom(localDensity).toDp() }))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutFinishPage(
    workoutTimeMillis: Long,
    workoutIntensity: MutableState<WorkoutRecord.WorkoutIntensity>,
    workoutId: Long,
    fabHeight: Dp,
    navigator: DestinationsNavigator
) {
    Column(
        Modifier
            .padding(horizontal = 8.dp)
            .padding(top = 8.dp)){
        Text("Total workout time: ${DateUtils.formatElapsedTime(workoutTimeMillis / 1000)}", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(32.dp))
        Text("Don't forget to stretch after you finish!", style = MaterialTheme.typography.titleMedium)
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
                            repeat(WorkoutRecord.WorkoutIntensity.entries.size){
                                Icon(Icons.Default.FitnessCenter, "Intensity",
                                    tint = if (it < workoutIntensity.value.ordinal+1) LocalContentColor.current else Color.Transparent)
                            }
                        }
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    WorkoutRecord.WorkoutIntensity.entries.forEachIndexed { index, selectionOption ->
                        DropdownMenuItem(
                            text = {},
                            leadingIcon = {
                                Row {
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
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { navigator.navigate(
            ExercisesByMuscleDestination(
                programName = "Current workout",
                workoutId = workoutId,
            )
        ) }, modifier = Modifier.align(CenterHorizontally)) {
            Text("Add exercise to workout")
        }
        Spacer(Modifier.height(160.dp))
        if (fabHeight > 0.dp) {
            Spacer(Modifier.height(fabHeight))
        }
    }
}


@Composable
fun ExerciseSettingsMenu(
    changeExercise: () -> Unit,
    removeExercise: () -> Unit,
    addExercise: () -> Unit
) {
    Box(
        modifier = Modifier.wrapContentSize()
    ) {
        var expanded by remember { mutableStateOf(false) }
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "More options"
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Replace exercise") },
                onClick = changeExercise,
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Replace exercise"
                    )
                })
            DropdownMenuItem(
                text = { Text("Skip this time") },
                onClick = removeExercise,
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Skip this time"
                    )
                })
            DropdownMenuItem(
                text = { Text("Add another exercise") },
                onClick = addExercise,
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "Add another exercise"
                    )
                })
        }
    }
}