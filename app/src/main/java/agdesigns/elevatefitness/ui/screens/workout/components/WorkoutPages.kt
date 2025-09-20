package agdesigns.elevatefitness.ui.screens.workout.components

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
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
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
import agdesigns.elevatefitness.ui.common.AdaptiveCircularTimer
import agdesigns.elevatefitness.ui.common.ChangeRepsWeightDialog
import agdesigns.elevatefitness.ui.common.InfoDialog
import agdesigns.elevatefitness.ui.screens.workout.CurrentExerciseState
import agdesigns.elevatefitness.ui.screens.workout.WorkoutPagesContent
import agdesigns.elevatefitness.ui.screens.workout.WorkoutState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.agdesignes.shared.BarbellType
import com.agdesignes.shared.Equipment
import com.agdesignes.shared.barbellResFromWeight
import com.agdesignes.shared.maybeKgToLb
import com.agdesignes.shared.maybeLbToKg
import com.agdesignes.shared.weightAndUnit
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
    navigator: DestinationsNavigator,
    horizontalPagerState: PagerState,
    currentExerciseState: CurrentExerciseState,  // DO NOT USE FROM INSIDE PAGER
    pagesContent: WorkoutPagesContent,
    workoutState: WorkoutState,
    bottomPadding: Dp,
    fabHeight: Dp,
    restCounterProgress: Float?,
    title: @Composable () -> Unit,
    addSet: () -> Unit,
    updateExerciseProbability: (Int) -> Unit,
    updateTare: (Float) -> Unit,
    updateBottomBar: (Int?, Float?) -> Unit,
    updateValues: (Int, Float, Int, Int) -> Unit,
    toggleOtherEquipment: () -> Unit,
    changeExercise: (Int, Int) -> Unit,
    removeExercise: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var infoDialogOpen by remember { mutableStateOf(false) }
    InfoDialog(
        dialogueIsOpen = infoDialogOpen,
        toggleDialogue = { infoDialogOpen = !infoDialogOpen }) {
        Text(
            pagesContent.exercises.getOrNull(horizontalPagerState.currentPage)?.description ?:
            stringResource(R.string.exercise_description_not_available)
        )
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
                onClick = { scope.launch { horizontalPagerState.animateScrollToPage(horizontalPagerState.currentPage-1) }},
                enabled = horizontalPagerState.currentPage > 0,
                modifier = Modifier
                    .wrapContentSize()
                    .weight(1f, false)
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack,
                    stringResource(R.string.arrowback_icon_previous_ex)
                )
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
                onClick = { scope.launch { horizontalPagerState.animateScrollToPage(horizontalPagerState.currentPage+1) }},
                enabled = horizontalPagerState.currentPage < horizontalPagerState.pageCount-1,
                modifier = Modifier
                    .wrapContentSize()
                    .weight(1f, false)
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward,
                    stringResource(R.string.arrowforward_icon_next_ex)
                )
            }
        }
        HorizontalPager(
            state = horizontalPagerState,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) { page ->
            if (page == pagesContent.exercises.size) {
                // page for finishing the workout
                WorkoutFinishPage(
                    currentExerciseState.workoutTimeFormatted,
                    workoutState.workoutId,
                    fabHeight,
                    bottomPadding,
                    navigator
                )
            } else {
                Column (Modifier.padding(horizontal = 16.dp)){
                    if (pagesContent.exercises[page].note.isNotBlank()) {
                        Text(text = buildAnnotatedString {
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(stringResource(R.string.note))
                            }
                            append(pagesContent.exercises[page].note)
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
                                stringResource(R.string.thumbdown_icon_dislike_ex)
                            )
                        }
                        // FIXME: it is not clear that this is info about the exercise, not about the like/dislike
                        IconButton(
                            onClick = { infoDialogOpen = true }
                        ) {
                            Icon(Icons.Outlined.Info, stringResource(R.string.info_icon_ex_desc))
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
                                stringResource(R.string.thumbup_icon_like_ex)
                            )
                        }
                    }
                    // content
                    if (currentExerciseState.restTimeSecs != null && restCounterProgress != null){
                        AdaptiveCircularTimer(
                            currentExerciseState.restTimeSecs,
                            restCounterProgress,
                            Modifier.align(CenterHorizontally)
                        )
                        LaunchedEffect(currentExerciseState.restTimeSecs) {
                            // do not vibrate on 0L as this will be called multiple times with 0L
                            if (currentExerciseState.restTimeSecs == 2L || currentExerciseState.restTimeSecs == 3L) {
                                haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                            } else if (currentExerciseState.restTimeSecs == 1L) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.current_exercise) +
                                    if (pagesContent.exercises[page].supersetExercise != null) stringResource(
                                        R.string.part_of_superset
                                    ) else "",
                            Modifier.padding(vertical = 8.dp),
                            fontWeight = FontWeight.Bold
                        )

                        AnimatedVisibility(
                            visible = workoutState.workoutStarted,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            val currentWorkoutString = stringResource(R.string.current_workout)
                            ExerciseSettingsMenu(changeExercise = {
                                changeExercise(page, pagesContent.exercises.size)
                                navigator.navigate(
                                    ExercisesByMuscleDestination(
                                        programName = currentWorkoutString,
                                        workoutId = workoutState.workoutId,
                                        returnAfterAdding = true
                                    )
                                )
                            }, removeExercise = {
                                removeExercise(page)
                            }, addExercise = {
                                navigator.navigate(
                                    ExercisesByMuscleDestination(
                                        programName = currentWorkoutString,
                                        workoutId = workoutState.workoutId,
                                        returnAfterAdding = true
                                    )
                                )
                                // FIXME: if unsuccessful add (e.g., user goes back) do not scroll
                                scope.launch { horizontalPagerState.animateScrollToPage(horizontalPagerState.pageCount-1) }
                            })
                        }
                    }
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(dimensionResource(R.dimen.card_inner_padding)),
                            horizontalAlignment = CenterHorizontally
                        ) {
                            Text(stringResource(R.string.rest) +
                                    ": ${pagesContent.exercises[page].rest[
                                        min(pagesContent.exerciseSetsDone[page], pagesContent.exercises[page].rest.size-1)
                                    ]}s", Modifier.align(Alignment.Start))

                            // if barbell, allow to add barbell weight (used for volume)
                            AnimatedVisibility(
                                visible = workoutState.workoutStarted &&
                                        pagesContent.exercises[page].equipment == Equipment.BARBELL,
                                enter = slideInVertically() + fadeIn(),
                                exit = slideOutVertically() + fadeOut()
                            ) {
                                // FIXME: barbellResFromWeight should be computed in ViewModel
                                val barbellName: String =
                                    stringResource(barbellResFromWeight(workoutState.tares.getOrNull(page) ?: 0f)) +
                                            " " +
                                            weightAndUnit(
                                                workoutState
                                                    .tares
                                                    .getOrNull(
                                                        page
                                                    ) ?: 0f,
                                                workoutState.imperialSystem,
                                                inParenthesis = true
                                            )

                                BarbellSelector(
                                    selectedBarbell = barbellName,
                                    toggleOtherEquipment = toggleOtherEquipment,
                                    useImperialSystem = workoutState.imperialSystem,
                                    onBarbellSelected = { weight -> updateTare(weight) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(CenterHorizontally)
                                )
                            }
                            pagesContent.exerciseRepsWeightRows[page].forEachIndexed { setCount, (repsInRow, weightInRow) ->
                                val toBeDone = setCount >= pagesContent.exerciseSetsDone[page]
                                var dialogIsOpen by rememberSaveable { mutableStateOf(false) }
                                ChangeRepsWeightDialog(
                                    dialogIsOpen,
                                    { dialogIsOpen = !dialogIsOpen },
                                    repsInRow,
                                    weightInRow,
                                    { reps, weight ->
                                        updateValues(
                                            reps,
                                            maybeLbToKg(weight, workoutState.imperialSystem),
                                            page,
                                            setCount
                                        )
                                    }
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(CardDefaults.shape) // rounded bounds when clicking
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
                                        checked = pagesContent.exerciseSetsDone[page] == setCount,
                                        onCheckedChange = {}
                                    ) {
                                        Text((setCount + 1).toString())
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        stringResource(
                                            R.string.reps_weight,
                                            repsInRow,
                                            weightInRow,
                                            if(workoutState.imperialSystem)
                                                stringResource(R.string.lb)
                                            else
                                                stringResource(R.string.kg)
                                        ),
                                        color = if (toBeDone) LocalContentColor.current else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            AnimatedVisibility(
                                visible = workoutState.workoutStarted,
                                enter = slideInVertically() + fadeIn(),
                                exit = slideOutVertically() + fadeOut()
                            ) {
                                TextButton(onClick = addSet) {
                                    Text(stringResource(R.string.add_set))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (pagesContent.exerciseRecords[page].isNotEmpty()) {
                        Text(
                            stringResource(R.string.history),
                            Modifier.padding(bottom = 8.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    var recordsToShow by remember { mutableIntStateOf(2) }
                    pagesContent.exerciseRecords[page].subList(0, min(pagesContent.exerciseRecords[page].size, recordsToShow)).forEach { record ->  // should maybe become lazy
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(dimensionResource(R.dimen.card_inner_padding))) {
                                val formatter = DateTimeFormatter.ofPattern("d MMM (yy)")
                                Text(
                                    record.date.format(formatter),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontStyle = FontStyle.Italic // TODO: add how many days ago
                                )
                                if (record.equipment == Equipment.BARBELL) {
                                    Text(
                                        stringResource(
                                            R.string.barbell_used,
                                            stringResource(barbellResFromWeight(record.tare)),
                                            weightAndUnit(record.tare, workoutState.imperialSystem, true)
                                        )
                                    )
                                } else if (record.equipment == Equipment.BODY_WEIGHT) {
                                    // FIXME: bug where bodyweight = 0? <- this may have been fixed with the new state update
                                    Text(
                                        stringResource(
                                            R.string.bodyweight_at_the_time,
                                            weightAndUnit(record.tare, workoutState.imperialSystem)
                                        )
                                    )
                                }
                                record.reps.forEachIndexed { index, rep ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(CardDefaults.shape) // rounded bounds when clicking
                                            .combinedClickable(onLongClick = {

                                            }, onClick = {
                                                updateBottomBar(
                                                    rep,
                                                    maybeKgToLb(
                                                        record.weights[index],
                                                        workoutState.imperialSystem
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
                                            stringResource(
                                                R.string.reps_weight,
                                                rep,
                                                maybeKgToLb(
                                                    record.weights[index],
                                                    workoutState.imperialSystem
                                                ),
                                                if (workoutState.imperialSystem)
                                                    stringResource(R.string.lb)
                                                else
                                                    stringResource(R.string.kg)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(dimensionResource(R.dimen.card_space_between)))
                    }
                    // FIXME: works initially, but at some point tapping it doesn't work
                    //  and no more records are shown. This happens if records are odd
                    if (recordsToShow < pagesContent.exerciseRecords[page].size) {
                        TextButton(
                            onClick = { recordsToShow += 2 },
                            modifier = Modifier.align(CenterHorizontally)
                        ) {
                            Text(stringResource(R.string.show_older_records))
                        }
                        Spacer(Modifier.height(dimensionResource(R.dimen.card_space_between)))
                    }
                    if (fabHeight > 0.dp) {
                        // add some padding to the fabHeight
                        Spacer(Modifier.height(fabHeight + 8.dp))
                    }
                    // This is the padding for an eventual bottom bar
                    Spacer(Modifier.height(bottomPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutFinishPage(
    workoutTimeFormatted: String,
    workoutId: Long,
    fabHeight: Dp,
    bottomPadding: Dp,
    navigator: DestinationsNavigator
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(top = 8.dp)){
        Text(
            stringResource(
                R.string.total_workout_time,
                workoutTimeFormatted
            ), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(32.dp))
        Text(stringResource(R.string.workout_completion_tip), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        val currentWorkoutString = stringResource(R.string.current_workout)
        TextButton(onClick = { navigator.navigate(
            ExercisesByMuscleDestination(
                programName = currentWorkoutString,
                workoutId = workoutId,
            )
        ) }, modifier = Modifier.align(CenterHorizontally)) {
            Text(stringResource(R.string.add_exercise_to_workout))
        }
        Spacer(Modifier.height(160.dp))
        if (fabHeight > 0.dp) {
            Spacer(Modifier.height(fabHeight))
        }
        Spacer(Modifier.height(bottomPadding))
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

        // TODO: add "show media controls" if swiped away
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.morevert_icon_options)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.replace_exercise)) },
                onClick = changeExercise,
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.replace_exercise)
                    )
                })
            DropdownMenuItem(
                text = { Text(stringResource(R.string.skip_exercise_this_workout_only)) },
                onClick = {
                    // does not close automatically TODO: find actual issue
                    expanded = false
                    removeExercise()
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.skip_exercise_this_workout_only)
                    )
                })
            DropdownMenuItem(
                text = { Text(stringResource(R.string.add_another_exercise)) },
                onClick = addExercise,
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.add_another_exercise)
                    )
                })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BarbellSelector(
    selectedBarbell: String,
    toggleOtherEquipment: () -> Unit,
    useImperialSystem: Boolean,
    onBarbellSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // NOTE: cannot use ExposedDropdownMenu as it currently opens unreliably

    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier
        .padding(vertical = 8.dp)
        .clip(MaterialTheme.shapes.small)
        .background(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.shapes.small
        )
    ) {
        Card(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(
                width = 2.dp,
                color = if (isExpanded)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.barbell),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedBarbell,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }

                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = isExpanded
                )
            }
        }

        // Animated options list
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()
            ) + fadeIn(),
            exit = shrinkVertically(
                animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()
            ) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        RoundedCornerShape(16.dp)
                    ),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                BarbellType.entries.forEachIndexed { index, option ->
                    val defaultShape: RoundedCornerShape = when (CardDefaults.shape) {
                        is RoundedCornerShape -> CardDefaults.shape as RoundedCornerShape
                        else -> RoundedCornerShape(16.dp) // fallback
                    }
                    // the corner radius between items
                    val defaultOtherCorner: Dp = 4.dp
                    val currentItemShape = when (index) {
                        0 -> defaultShape.copy(
                            bottomStart = CornerSize(defaultOtherCorner),
                            bottomEnd = CornerSize(defaultOtherCorner)
                        )

                        BarbellType.entries.lastIndex -> defaultShape.copy(
                            topStart = CornerSize(defaultOtherCorner),
                            topEnd = CornerSize(defaultOtherCorner),
                        )

                        else -> RoundedCornerShape(
                            topStart = defaultOtherCorner,
                            topEnd = defaultOtherCorner,
                            bottomStart = defaultOtherCorner,
                            bottomEnd = defaultOtherCorner
                        )
                    }
                    val optionText = if (option == BarbellType.OTHER) {
                        stringResource(
                            R.string.barbell_custom_value,
                            stringResource(option.barbellResource)
                        )
                    } else {
                        stringResource(option.barbellResource) +
                                " (${option.weight[useImperialSystem]} ${
                                    if (useImperialSystem) stringResource(R.string.lb)
                                    else stringResource(R.string.kg)
                                })"
                    }

                    val isSelected = selectedBarbell == optionText || (option == BarbellType.OTHER && selectedBarbell.contains(stringResource(option.barbellResource)))

                    Card(
                        onClick = {
                            if (option == BarbellType.OTHER) {
                                toggleOtherEquipment()
                            } else {
                                onBarbellSelected(maybeLbToKg(option.weight[useImperialSystem]!!, useImperialSystem))
                            }
                            isExpanded = false
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.inverseSurface
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = currentItemShape
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Icon for barbell types
                                Icon(
                                    imageVector = if (option == BarbellType.OTHER)
                                        Icons.Rounded.Edit
                                    else
                                        Icons.Rounded.FitnessCenter,
                                    contentDescription = null,
                                    tint = if (isSelected)
                                        MaterialTheme.colorScheme.inverseOnSurface
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = optionText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.inverseOnSurface
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }

                            // Selection indicator
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.inversePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}