package agdesigns.elevatefitness.ui.screens.workout.components

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.ExerciseRecordAndEquipment
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseAndInfo
import agdesigns.elevatefitness.shared.SetType
import agdesigns.elevatefitness.data.db.entity.WorkoutRecord
import agdesigns.elevatefitness.ui.common.AdaptiveCircularTimer
import agdesigns.elevatefitness.ui.common.ChangeRepsWeightDialog
import agdesigns.elevatefitness.ui.common.InfoDialog
import agdesigns.elevatefitness.ui.screens.workout.CurrentExerciseState
import agdesigns.elevatefitness.ui.screens.workout.WorkoutPagesContent
import agdesigns.elevatefitness.ui.screens.workout.WorkoutState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import agdesigns.elevatefitness.shared.BarbellType
import agdesigns.elevatefitness.shared.Equipment
import agdesigns.elevatefitness.shared.barbellResFromWeight
import agdesigns.elevatefitness.shared.maybeKgToLb
import agdesigns.elevatefitness.shared.maybeLbToKg
import agdesigns.elevatefitness.shared.weightAndUnit
import agdesigns.elevatefitness.ui.common.GroupedCard
import agdesigns.elevatefitness.ui.screens.workout.ModificationSuggestion
import agdesigns.elevatefitness.ui.screens.workout.SetDisplayRow
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.style.TextDecoration
import com.ramcosta.composedestinations.generated.destinations.ExerciseStatsDestination
import com.ramcosta.composedestinations.generated.destinations.ExercisesByMuscleDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import kotlin.collections.forEachIndexed
import kotlin.collections.isNotEmpty
import kotlin.math.min

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun SharedTransitionScope.ExercisePages(
    navigator: DestinationsNavigator,
    horizontalPagerState: PagerState,
    currentExerciseState: CurrentExerciseState,  // DO NOT USE FROM INSIDE PAGER
    pagesContent: WorkoutPagesContent,
    workoutState: WorkoutState,
    previewExercise: ProgramExerciseAndInfo?,
    bottomPadding: Dp,
    fabHeight: Dp,
    restCounterProgress: Float?,
    title: @Composable () -> Unit,
    addSet: () -> Unit,
    updateTare: (Float) -> Unit,
    updateBottomBar: (Int?, Float?) -> Unit,
    updateValues: (Int, Float, Int, Int) -> Unit,
    deleteSet: (Int, Int) -> Unit,
    toggleOtherEquipment: () -> Unit,
    addExercise: (Int, Int) -> Unit,
    changeExercise: (Int, Int) -> Unit,
    removeExercise: (Int) -> Unit,
    mediaControlsDismissed: Boolean,
    resetMediaControlVisibility: () -> Unit,
    dontRequestOngoingWorkoutNotification: () -> Unit,
    refreshPromotedNotificationAccess: () -> Unit,
    onAcceptSuggestion: (Int) -> Unit,
    updateSetType: (Int, Int, SetType) -> Unit
) {
    val scope = rememberCoroutineScope()

    var infoDialogOpen by remember { mutableStateOf(false) }

    // Should only show preview when transitioning *into* the workout
    var containerTransitionFinished by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(this.isTransitionActive) {
        if (!this@ExercisePages.isTransitionActive) {
            containerTransitionFinished = true
        }
    }
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
                // FIXME:
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
        if (workoutState.workoutStarted) {
            // maybe request notification access
            NotificationPermission(
                canAsk = !workoutState.cantRequestOngoingWorkoutNotification,
                onDontAskAgain = dontRequestOngoingWorkoutNotification,
                hasPromotedNotificationAccess = workoutState.canPostPromotedNotifications,
                refreshPromotedNotificationAccess = refreshPromotedNotificationAccess,
                modifier = Modifier.padding(16.dp),
            )
        }
        Box {
            this@Column.AnimatedVisibility(
                visible = !containerTransitionFinished || currentExerciseState.isLoading,
                enter = EnterTransition.None,
                exit = fadeOut(
                    MotionScheme.expressive().slowEffectsSpec()
                )
            ) {
                if (previewExercise != null) {
                    // placeholder for preview
                    ExercisePage(
                        isLoading = true,
                        exerciseNote = "",
                        supersetWith = null,
                        exerciseRest = previewExercise.rest.getOrNull(0) ?: 0,
                        equipment = Equipment.EVERYTHING,
                        tare = null,
                        isDurationBased = previewExercise.overriddenDurationBased,
                        repsWeightRows = previewExercise.reps.map {
                            SetDisplayRow(
                                reps = it.toString(),
                                weight = "...",
                                toBeDone = true
                            )
                        },
                        setsDone = 0,
                        records = emptyList(),
                        imperialSystem = workoutState.imperialSystem,
                        workoutStarted = false,
                        restTimeSecs = null,
                        restCounterProgress = null,
                        fabHeight = fabHeight,
                        bottomPadding = bottomPadding,
                        modificationSuggestion = null,
                        settingsMenu = {
                            ExerciseSettingsMenu(
                                {},
                                {}, {},
                                {},
                                false,
                                {}
                            )
                        },
                        addSet = {},
                        updateRowValues = { _, _, _ -> },
                        updateTare = {},
                        updateBottomBar = { _, _ -> },
                        toggleOtherEquipment = {},
                        toggleInfoDialog = {},
                        deleteSet = {},
                        onAcceptSuggestion = {},
                        updateSetType = { _, _ -> }
                    )
                }
            }
            this@Column.AnimatedVisibility(
                visible = containerTransitionFinished && !currentExerciseState.isLoading,
                enter = fadeIn(
                    MotionScheme.expressive().fastEffectsSpec()
                ),
                exit = ExitTransition.None
            ) {
                HorizontalPager(
                    state = horizontalPagerState,
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Top,
                    userScrollEnabled = !workoutState.lockHorizontalScroll
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
                        ExercisePage(
                            isLoading = currentExerciseState.isLoading,
                            modificationSuggestion = pagesContent.modificationsSuggestions[page],
                            exerciseNote = pagesContent.exercises[page].note,
                            supersetWith = when (pagesContent.exercises[page].supersetExercise) {
                                null -> null
                                pagesContent.exercises.getOrNull(page + 1)?.extProgramExerciseId -> pagesContent.exercises[page + 1].name
                                pagesContent.exercises.getOrNull(page - 1)?.extProgramExerciseId -> pagesContent.exercises[page - 1].name
                                else -> null
                            },
                            exerciseRest = pagesContent.exercises[page].rest[
                                min(
                                    pagesContent.exerciseSetsDone[page],
                                    pagesContent.exercises[page].rest.size - 1
                                )
                            ],
                            equipment = pagesContent.exercises[page].equipment,
                            tare = workoutState.tares.getOrNull(page),
                            isDurationBased = pagesContent.exercises[page].overriddenDurationBased,
                            repsWeightRows = pagesContent.exerciseRepsWeightRows[page],
                            setsDone = pagesContent.exerciseSetsDone[page],
                            records = pagesContent.exerciseRecords[page],
                            imperialSystem = workoutState.imperialSystem,
                            workoutStarted = workoutState.workoutStarted,
                            restTimeSecs = currentExerciseState.restTimeSecs,
                            restCounterProgress = restCounterProgress,
                            fabHeight = fabHeight,
                            bottomPadding = bottomPadding,
                            settingsMenu = {
                                val currentWorkoutString = stringResource(R.string.current_workout)
                                ExerciseSettingsMenu(
                                    changeExercise = {
                                        changeExercise(page, pagesContent.exercises.size)
                                        navigator.navigate(
                                            ExercisesByMuscleDestination(
                                                programName = currentWorkoutString,
                                                workoutId = workoutState.workoutId,
                                                returnAfterAdding = true
                                            )
                                        )
                                    },
                                    removeExercise = {
                                        removeExercise(page)
                                    },
                                    addExercise = {
                                        addExercise(page, pagesContent.exercises.size)
                                        navigator.navigate(
                                            ExercisesByMuscleDestination(
                                                programName = currentWorkoutString,
                                                workoutId = workoutState.workoutId,
                                                returnAfterAdding = true,
                                                insertAtPosition = page+1
                                            )
                                        )
                                    },
                                    viewStatistics = {
                                        navigator.navigate(
                                            ExerciseStatsDestination(
                                                pagesContent.exercises[page].extExerciseId
                                            )
                                        )
                                    },
                                    mediaControlsDismissed = mediaControlsDismissed,
                                    showMediaControls = resetMediaControlVisibility
                                )
                            },
                            addSet = addSet,
                            updateRowValues = { reps, weight, setCount ->
                                updateValues(
                                    reps,
                                    maybeLbToKg(weight, workoutState.imperialSystem),
                                    page,
                                    setCount
                                )
                            },
                            deleteSet = { setCount ->
                                deleteSet(page, setCount)
                            },
                            updateTare = updateTare,
                            updateBottomBar = updateBottomBar,
                            toggleOtherEquipment = toggleOtherEquipment,
                            toggleInfoDialog = { infoDialogOpen = true },
                            onAcceptSuggestion = { onAcceptSuggestion(page) },
                            updateSetType = { setCount, setType ->
                                updateSetType(
                                    page,
                                    setCount,
                                    setType
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExercisePage(
    isLoading: Boolean,
    exerciseNote: String,
    supersetWith: String?,
    exerciseRest: Int,
    equipment: Equipment,
    tare: Float?,
    isDurationBased: Boolean,
    repsWeightRows: List<SetDisplayRow>,
    setsDone: Int,
    records: List<ExerciseRecordAndEquipment>,
    imperialSystem: Boolean,
    workoutStarted: Boolean,
    restTimeSecs: Long?,
    restCounterProgress: Float?,
    fabHeight: Dp,
    bottomPadding: Dp,
    modificationSuggestion: ModificationSuggestion?,
    settingsMenu: @Composable (() -> Unit),
    addSet: () -> Unit,
    updateRowValues: (Int, Float, Int) -> Unit,
    updateTare: (Float) -> Unit,
    updateBottomBar: (Int?, Float?) -> Unit,
    toggleOtherEquipment: () -> Unit,
    toggleInfoDialog: () -> Unit,
    deleteSet: (Int) -> Unit,
    onAcceptSuggestion: () -> Unit,
    updateSetType: (Int, SetType) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Column (Modifier.padding(horizontal = 16.dp)){
        if (exerciseNote.isNotBlank()) {
            Text(text = buildAnnotatedString {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(stringResource(R.string.note))
                }
                append(exerciseNote)
            }, modifier = Modifier.align(CenterHorizontally))
        }
        // content
        if (restTimeSecs != null && restCounterProgress != null){
            AdaptiveCircularTimer(
                restTimeSecs,
                restCounterProgress,
                Modifier.align(CenterHorizontally)
            )
            LaunchedEffect(restTimeSecs) {
                // do not vibrate on 0L as this will be called multiple times with 0L
                if (restTimeSecs == 2L || restTimeSecs == 3L) {
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                } else if (restTimeSecs == 1L) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    delay(1000)
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }
            }
        }
        SuggestModificationCard(
            isLoading = isLoading,
            hasDoneSomeSets = setsDone > 0,
            modificationSuggestion = modificationSuggestion,
            onAcceptSuggestion = onAcceptSuggestion,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.current_exercise),
                Modifier.padding(vertical = 8.dp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(
                onClick = toggleInfoDialog
            ) {
                Icon(Icons.Outlined.Info, stringResource(R.string.info_icon_ex_desc))
            }
            if (supersetWith != null) {
                Text(
                    stringResource(R.string.part_of_superset) + supersetWith,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
            settingsMenu()
        }
        GroupedCard(Modifier.fillMaxWidth()) {
            subCard {
                Text(stringResource(R.string.rest) +
                        ": ${exerciseRest}s", Modifier.align(Alignment.Start))

                // if barbell, allow to add barbell weight (used for volume)
                AnimatedVisibility(
                    visible = workoutStarted && equipment == Equipment.BARBELL,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    // FIXME: barbellResFromWeight should be computed in ViewModel
                    val barbellName: String =
                        stringResource(barbellResFromWeight(tare ?: 0f)) +
                                " " +
                                weightAndUnit(tare ?: 0f,
                                    imperialSystem,
                                    inParenthesis = true
                                )

                    BarbellSelector(
                        selectedBarbell = barbellName,
                        toggleOtherEquipment = toggleOtherEquipment,
                        useImperialSystem = imperialSystem,
                        onBarbellSelected = { weight -> updateTare(weight) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(CenterHorizontally)
                    )
                }

            }
            val totalWarmupSets = repsWeightRows.count { it.setType == SetType.WARMUP }
            // Awesome sets are sets it is suggesting to add
            val totalAwesomeSets = repsWeightRows.count { it.setType == SetType.AWESOME }
            if (totalWarmupSets > 0) {
                subCard {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            stringResource(SetType.WARMUP.displayRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    repsWeightRows.filter { it.setType == SetType.WARMUP }.forEachIndexed { setCount, setDisplayRow ->
                        Spacer(Modifier.width(4.dp))
                        WorkoutRepsWeightRow(
                            repsInRow = setDisplayRow.reps,
                            weightInRow = setDisplayRow.weight,
                            setsDone = setsDone,
                            setCount = setCount,
                            totalWarmupSets = totalWarmupSets,
                            isDurationBased = isDurationBased,
                            setType = setDisplayRow.setType,
                            imperialSystem = imperialSystem,
                            toBeDone = setDisplayRow.toBeDone,
                            projectedReps = setDisplayRow.projectedReps,
                            projectedWeight = setDisplayRow.projectedWeight,
                            updateRowValues = { reps, weight ->
                                updateRowValues(reps, weight, setCount)
                            },
                            deleteSet = {
                                deleteSet(setCount)
                            },
                            updateBottomBar = updateBottomBar,
                            updateSetType = { setType ->
                                updateSetType(
                                    setCount,
                                    setType
                                )
                            }
                        )
                    }
                }
            }
            subCard {
                repsWeightRows.forEachIndexed { setCount, (repsInRow, weightInRow, toBeDone, setType, projectedRep, projectedWeight) ->
                    if (setType == SetType.WARMUP) return@forEachIndexed
                    if (setType == SetType.AWESOME) return@forEachIndexed

                    WorkoutRepsWeightRow(
                        repsInRow = repsInRow,
                        weightInRow = weightInRow,
                        setsDone = setsDone,
                        setCount = setCount,
                        totalWarmupSets = totalWarmupSets,
                        isDurationBased = isDurationBased,
                        setType = setType,
                        imperialSystem = imperialSystem,
                        toBeDone = toBeDone,
                        projectedReps = projectedRep,
                        projectedWeight = projectedWeight,
                        updateRowValues = { reps, weight ->
                            updateRowValues(reps, weight, setCount)
                        },
                        deleteSet = {
                            deleteSet(setCount)
                        },
                        updateBottomBar = updateBottomBar,
                        updateSetType = { setType ->
                            updateSetType(
                                setCount,
                                setType
                            )
                        }
                    )
                }
                if (totalAwesomeSets == 0) {
                    AnimatedVisibility(
                        visible = workoutStarted,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        TextButton(onClick = addSet) {
                            Text(
                                stringResource(R.string.add_set),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            if (totalAwesomeSets > 0) {
                subCard {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            stringResource(SetType.AWESOME.displayRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    repsWeightRows.filter { it.setType == SetType.AWESOME }.forEachIndexed { index, setDisplayRow ->
                        val setCount = repsWeightRows.size - totalAwesomeSets + index
                        Spacer(Modifier.width(4.dp))
                        WorkoutRepsWeightRow(
                            repsInRow = setDisplayRow.reps,
                            weightInRow = setDisplayRow.weight,
                            setsDone = setsDone,
                            setCount = setCount,
                            totalWarmupSets = totalWarmupSets,
                            isDurationBased = isDurationBased,
                            setType = setDisplayRow.setType,
                            imperialSystem = imperialSystem,
                            toBeDone = setDisplayRow.toBeDone,
                            projectedReps = setDisplayRow.projectedReps,
                            projectedWeight = setDisplayRow.projectedWeight,
                            updateRowValues = { reps, weight ->
                                updateRowValues(reps, weight, setCount)
                            },
                            deleteSet = {
                                deleteSet(setCount)
                            },
                            updateBottomBar = updateBottomBar,
                            updateSetType = { setType ->
                                updateSetType(
                                    setCount,
                                    setType
                                )
                            }
                        )
                    }
                    AnimatedVisibility(
                        visible = workoutStarted,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        TextButton(onClick = addSet) {
                            Text(
                                stringResource(R.string.add_set),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (records.isNotEmpty()) {
            Text(
                stringResource(R.string.history),
                Modifier.padding(vertical = 8.dp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            var recordsToShow by remember { mutableIntStateOf(2) }

            // TODO: should maybe become lazy
            records.subList(0, min(records.size, recordsToShow)).forEach { record ->
                HistoricRecord(
                    record,
                    imperialSystem,
                    modifier = Modifier.padding(bottom = dimensionResource(R.dimen.card_space_between)),
                    onRecordRowClick = { reps, weight ->
                        updateBottomBar(
                            reps,
                            maybeKgToLb(
                                weight,
                                imperialSystem
                            )
                        )
                    }
                )
            }
            // FIXME: works initially, but at some point tapping it doesn't work
            //  and no more records are shown. This happens if records are odd
            if (recordsToShow < records.size) {
                TextButton(
                    onClick = { recordsToShow += 2 },
                    modifier = Modifier.align(CenterHorizontally)
                ) {
                    Text(stringResource(R.string.show_older_records))
                }
                Spacer(Modifier.height(dimensionResource(R.dimen.card_space_between)))
            }
        }
        if (fabHeight > 0.dp) {
            // add some padding to the fabHeight
            Spacer(Modifier.height(fabHeight + 8.dp))
        }
        // This is the padding for an eventual bottom bar
        Spacer(Modifier.height(bottomPadding))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SuggestModificationCard(
    isLoading: Boolean,
    hasDoneSomeSets: Boolean,
    modificationSuggestion: ModificationSuggestion?,
    onAcceptSuggestion: () -> Unit,
    modifier: Modifier = Modifier
) {
    var notToday by rememberSaveable { mutableStateOf(false) }
    AnimatedVisibility(
        modificationSuggestion != null &&
                !notToday &&
                !isLoading &&
                (!hasDoneSomeSets || modificationSuggestion.type == WorkoutRecord.ModificationType.EXERCISE_ADDED) ,
        enter = slideInVertically(
            animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()
        ) + fadeIn(
            animationSpec = MaterialTheme.motionScheme.slowEffectsSpec()
        ),
        exit = slideOutVertically(
            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
        ) + fadeOut(
            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()
        )
    ) {
        Card(
            modifier = modifier,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (modificationSuggestion?.type) {
                        WorkoutRecord.ModificationType.EXERCISE_ADDED -> stringResource(R.string.modification_suggestion_add_exercise)
                        WorkoutRecord.ModificationType.EXERCISE_SKIPPED -> stringResource(R.string.modification_suggestion_skip_exercise)
                        WorkoutRecord.ModificationType.EXERCISE_REPLACED -> stringResource(R.string.modification_suggestion_replace_exercise)
                        null -> ""
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .weight(1f),
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(
                    { notToday = true },
                    modifier = Modifier.padding(8.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.close_icon)
                    )
                }
            }
            Text(
                text = when (modificationSuggestion?.type) {
                    WorkoutRecord.ModificationType.EXERCISE_ADDED -> stringResource(
                        R.string.modification_suggestion_desc_add,
                        modificationSuggestion.newWorkoutExercise?.name ?: ""
                    )

                    WorkoutRecord.ModificationType.EXERCISE_SKIPPED -> stringResource(R.string.modification_suggestion_desc_skip)
                    WorkoutRecord.ModificationType.EXERCISE_REPLACED -> stringResource(
                        R.string.modification_suggestion_desc_replace,
                        modificationSuggestion.newWorkoutExercise?.name ?: ""
                    )
                    null -> ""
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { notToday = true }) {
                    Text(text = stringResource(R.string.modification_suggestion_not_this_time))
                }
                Button(onClick = onAcceptSuggestion) {
                    Text(text = stringResource(R.string.modification_suggestion_do_it))
                }
            }
        }
    }
}

@Composable
fun HistoricRecord(
    record: ExerciseRecordAndEquipment,
    imperialSystem: Boolean,
    modifier: Modifier = Modifier,
    onRecordRowClick: (Int, Float) -> Unit = { _, _ -> }
) {
    Card(modifier.fillMaxWidth()) {
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
                        weightAndUnit(record.tare, imperialSystem, true)
                    )
                )
            } else if (record.equipment == Equipment.BODY_WEIGHT) {
                Text(
                    stringResource(
                        R.string.bodyweight_at_the_time,
                        weightAndUnit(record.tare, imperialSystem)
                    )
                )
            }
            val warmupSets = record.setTypes?.count { it == SetType.WARMUP } ?: 0
            record.reps.forEachIndexed { index, rep ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CardDefaults.shape) // rounded bounds when clicking
                        .clickable {
                            onRecordRowClick(
                                rep,
                                record.weights[index]
                            )
                        }
                ) {
                    FilledIconToggleButton(
                        enabled = false,
                        checked = false,
                        onCheckedChange = { }) {
                        if (record.setTypes?.getOrElse(index) { SetType.NORMAL } == SetType.NORMAL) {
                            Text((index + 1 - warmupSets).toString())
                        } else if (record.setTypes?.getOrNull(index) == SetType.AWESOME) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                stringResource(SetType.AWESOME.displayRes)
                            )
                        } else {
                            Text(
                                stringResource(
                                    (record.setTypes?.getOrNull(index) ?: SetType.NORMAL).displayRes
                                ).first().uppercase()
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(
                            if (record.overriddenDurationBased)
                                R.string.duration_weight
                            else
                                R.string.reps_weight,
                            rep,
                            maybeKgToLb(
                                record.weights[index],
                                imperialSystem
                            ),
                            if (imperialSystem)
                                stringResource(R.string.lb)
                            else
                                stringResource(R.string.kg)
                        )
                    )
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
    addExercise: () -> Unit,
    viewStatistics: () -> Unit,
    mediaControlsDismissed: Boolean,
    showMediaControls: () -> Unit
) {
    Box(
        modifier = Modifier.wrapContentSize()
    ) {
        var expanded by remember { mutableStateOf(false) }

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
                onClick = {
                    expanded = false
                    changeExercise()
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.replace_exercise)
                    )
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.skip_exercise_this_workout_only)) },
                onClick = {
                    expanded = false
                    removeExercise()
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.skip_exercise_this_workout_only)
                    )
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.add_another_exercise)) },
                onClick = {
                    expanded = false
                    addExercise()
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.add_another_exercise)
                    )
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.view_exercise_history_and_stats)) },
                onClick = {
                    expanded = false
                    viewStatistics()
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Timeline,
                        contentDescription = stringResource(R.string.view_exercise_history_and_stats)
                    )
                }
            )
            if (mediaControlsDismissed) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.show_media_controls)) },
                    onClick = {
                        expanded = false
                        showMediaControls()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.PlayArrow,
                            contentDescription = stringResource(R.string.show_media_controls)
                        )
                    }
                )
            }
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
    val haptics = LocalHapticFeedback.current

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
                    val isSelected = selectedBarbell == optionText ||
                        (
                            option == BarbellType.OTHER &&
                            selectedBarbell.contains(stringResource(option.barbellResource))
                        )
                    val currentItemShape = if (isSelected)
                        MaterialTheme.shapes.extraExtraLarge
                    else
                        when (index) {
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

                    Card(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
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


@Composable
fun WorkoutRepsWeightRow(
    repsInRow: String,
    weightInRow: String,
    setsDone: Int,
    setCount: Int,
    totalWarmupSets: Int,
    projectedWeight: String?,
    projectedReps: String?,
    toBeDone: Boolean,
    isDurationBased: Boolean,
    imperialSystem: Boolean,
    setType: SetType,
    updateRowValues: (Int, Float) -> Unit,
    deleteSet: () -> Unit,
    updateBottomBar: (Int?, Float?) -> Unit,
    updateSetType: (SetType) -> Unit
) {
    var dialogIsOpen by rememberSaveable { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    ChangeRepsWeightDialog(
        dialogIsOpen = dialogIsOpen,
        toggleDialog = { dialogIsOpen = !dialogIsOpen },
        initialReps = repsInRow,
        initialWeight = weightInRow,
        updateValues = { reps, weight ->
            updateRowValues(
                reps,
                weight
            )
        },
        deleteSet = deleteSet
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
                    projectedReps?.toIntOrNull() ?: repsInRow.toIntOrNull(),
                    projectedWeight?.toFloatOrNull()
                        ?: weightInRow.toFloatOrNull()
                )
            })
    ) {
        val tooltipState = rememberTooltipState()
        var expanded by remember { mutableStateOf(false) }
        Box {
            TooltipBox(
                positionProvider =
                    TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(stringResource(setType.displayRes)) } },
                state = tooltipState,
            ) {
                FilledIconToggleButton(
                    enabled = toBeDone,
                    checked = setsDone == setCount,
                    onCheckedChange = {
                        expanded = !expanded
                    }
                ) {
                    if (setType == SetType.NORMAL) {
                        Text((setCount + 1 - totalWarmupSets).toString())
                    } else if (setType == SetType.AWESOME) {
                        Icon(setType.icon, null)
                    } else {
                        Text(stringResource(setType.displayRes).first().uppercase())
                    }
                }
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SetType.visibleEntries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(stringResource(type.displayRes)) },
                    leadingIcon = {
                        Icon(
                            imageVector = type.icon,
                            contentDescription = null
                        )
                    },
                    enabled = type != SetType.WARMUP || totalWarmupSets >= setCount,
                    onClick = {
                        updateSetType(type)
                        expanded = false
                    },
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        val textColor =
            if (toBeDone) LocalContentColor.current else MaterialTheme.colorScheme.outline
        val unitString =
            if (imperialSystem) stringResource(R.string.lb) else stringResource(
                R.string.kg
            )

        // Reps section
        Text(
            // FIXME: overflow in other languages
            text = if (isDurationBased) {
                stringResource(R.string.exercise_hold) + ": "
            } else {
                stringResource(R.string.reps) + ": "
            },
            color = textColor
        )
        Text(
            text = repsInRow,
            color = textColor,
            textDecoration = if (projectedReps != null && repsInRow != projectedReps) TextDecoration.LineThrough else TextDecoration.None
        )
        if (projectedReps != null && repsInRow != projectedReps) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = textColor
            )
            Text(
                text = projectedReps,
                color = textColor
            )
        }
        if (isDurationBased) {
            Text(
                "s ",
                color = textColor
            )
        } else {
            Text(
                " ",
                color = textColor
            )
        }

        // Weight section
        Text(
            text = stringResource(R.string.weight) + ": ", // " Weight: "
            color = textColor
        )
        if (projectedWeight == null || weightInRow != "...") {
            Text(
                text = weightInRow,
                color = textColor,
                textDecoration = if (projectedWeight != null && weightInRow != projectedWeight) TextDecoration.LineThrough else TextDecoration.None
            )
        }
        if (projectedWeight != null && weightInRow != projectedWeight) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = textColor
            )
            Text(
                text = projectedWeight,
                color = textColor
            )
        }

        // Unit
        Text(
            text = " $unitString",
            color = textColor
        )
    }
}