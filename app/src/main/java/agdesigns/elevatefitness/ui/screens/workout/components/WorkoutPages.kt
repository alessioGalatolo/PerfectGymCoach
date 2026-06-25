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
import agdesigns.elevatefitness.data.db.entity.TrackingResult
import agdesigns.elevatefitness.shared.SetType
import agdesigns.elevatefitness.data.db.entity.WorkoutRecord
import agdesigns.elevatefitness.ui.navigation.DestinationsNavigator
import agdesigns.elevatefitness.ui.navigation.ExerciseStatsDestination
import agdesigns.elevatefitness.ui.common.AdaptiveCircularTimer
import agdesigns.elevatefitness.ui.common.ChangeRepsWeightDialog
import agdesigns.elevatefitness.ui.common.InfoDialog
import agdesigns.elevatefitness.ui.screens.workout.CurrentExerciseState
import agdesigns.elevatefitness.ui.screens.workout.WorkoutPagesContent
import agdesigns.elevatefitness.ui.screens.workout.WorkoutState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
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
import agdesigns.elevatefitness.ui.common.CompletionCheckmark
import agdesigns.elevatefitness.ui.common.GroupedCard
import agdesigns.elevatefitness.ui.common.IconAndLabel
import agdesigns.elevatefitness.ui.common.IconsWithOverflow
import agdesigns.elevatefitness.ui.common.UpdateWeightDialog
import agdesigns.elevatefitness.ui.common.columnProviderWithHighlight
import agdesigns.elevatefitness.ui.screens.workout.ModificationSuggestion
import agdesigns.elevatefitness.ui.screens.workout.SetDisplayRow
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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
    showTitle: Boolean,
    title: @Composable (Modifier) -> Unit,
    addSet: () -> Unit,
    updateTare: (BarbellType) -> Unit,
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
    updateSetType: (Int, Int, SetType) -> Unit,
    finishWorkout: () -> Unit,
    updateUserWeight: (Float) -> Unit
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

    var exercisesOverviewSheetOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    if (exercisesOverviewSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { exercisesOverviewSheetOpen = false },
            sheetState = sheetState
        ) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                itemsIndexed(pagesContent.exercises) { index, exercise ->
                    val setsDone = pagesContent.exerciseSetsDone.getOrElse(index) { 0 }
                    val selected = index == horizontalPagerState.currentPage
                    WorkoutOverviewListItem(
                        name = exercise.name,
                        imageModel = exercise.image,
                        setsDone = setsDone,
                        totalSets = exercise.reps.size,
                        selected = selected,
                        isDurationBased = exercise.overriddenDurationBased,
                        onClick = {
                            exercisesOverviewSheetOpen = false
                            scope.launch {
                                horizontalPagerState.animateScrollToPage(index)
                            }
                        }
                    )
                }
                if (workoutState.workoutStarted) {
                    item {
                        val selected = horizontalPagerState.currentPage == pagesContent.exercises.size
                        Card(
                            onClick = {
                                exercisesOverviewSheetOpen = false
                                scope.launch {
                                    horizontalPagerState.animateScrollToPage(pagesContent.exercises.size)
                                }
                            },
                            colors = if (selected)
                                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            else
                                CardDefaults.cardColors(),
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.DoneAll,
                                    contentDescription = null,
                                    tint = if (selected)
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .padding(16.dp)
                                )
                                Text(
                                    stringResource(R.string.end_of_workout),
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp)
                                        .weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selected)
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.navigationBarsPadding())
                }
            }
        }
    }

    Column(
        Modifier.padding(top = 8.dp)
    ) {
        if (showTitle) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            horizontalPagerState.animateScrollToPage(
                                horizontalPagerState.currentPage - 1
                            )
                        }
                    },
                    enabled = horizontalPagerState.currentPage > 0,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        stringResource(R.string.arrowback_icon_previous_ex)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { exercisesOverviewSheetOpen = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    ProvideTextStyle(
                        value = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center)
                    ) {
                        CompositionLocalProvider(
                            content = {
                                title(Modifier.weight(1f))
                            }
                        )
                    }
                    val animatedRotation by animateFloatAsState(if (exercisesOverviewSheetOpen) 180f else 0f)
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .animateContentSize()
                            .rotate(animatedRotation),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = {
                        scope.launch {
                            horizontalPagerState.animateScrollToPage(
                                horizontalPagerState.currentPage + 1
                            )
                        }
                    },
                    enabled = horizontalPagerState.currentPage < horizontalPagerState.pageCount - 1,
                    modifier = Modifier
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForward,
                        stringResource(R.string.arrowforward_icon_next_ex)
                    )
                }
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
        val haptic = LocalHapticFeedback.current
        val restTimeSecs = currentExerciseState.restTimeSecs
        val restCounterProgress = restCounterProgress
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
                        selectedBarbellType = null,
                        isDurationBased = previewExercise.overriddenDurationBased,
                        repsWeightRows = previewExercise.reps.map {
                            SetDisplayRow(
                                reps = it.toString(),
                                weight = "...",
                                toBeDone = true
                            )
                        },
                        setsDone = 0,
                        trackingResult = null,
                        records = emptyList(),
                        imperialSystem = workoutState.imperialSystem,
                        workoutStarted = false,
                        fabHeight = fabHeight,
                        bottomPadding = bottomPadding,
                        modificationSuggestion = null,
                        lastBodyweightUpdate = ZonedDateTime.now(),
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
                        updateSetType = { _, _ -> },
                        updateUserWeight = { _ -> }
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
                            fabHeight,
                            bottomPadding,
                            addExercise = {
                                addExercise(page - 1, pagesContent.exercises.size)
                            },
                            finishWorkout = finishWorkout
                        )
                    } else {
                        ExercisePage(
                            isLoading = currentExerciseState.isLoading,
                            trackingResult = pagesContent.ongoingRecords.getOrNull(page)?.trackingResults?.lastOrNull(),
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
                            selectedBarbellType = workoutState.selectedBarbells.getOrNull(page),
                            isDurationBased = pagesContent.exercises[page].overriddenDurationBased,
                            repsWeightRows = pagesContent.exerciseRepsWeightRows[page],
                            setsDone = pagesContent.exerciseSetsDone[page],
                            records = pagesContent.exerciseRecords[page],
                            imperialSystem = workoutState.imperialSystem,
                            workoutStarted = workoutState.workoutStarted,
                            fabHeight = fabHeight,
                            bottomPadding = bottomPadding,
                            lastBodyweightUpdate = workoutState.lastBodyweightUpdate,
                            settingsMenu = {
                                ExerciseSettingsMenu(
                                    changeExercise = {
                                        changeExercise(page, pagesContent.exercises.size)
                                    },
                                    removeExercise = {
                                        removeExercise(page)
                                    },
                                    addExercise = {
                                        addExercise(page, pagesContent.exercises.size)
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
                            },
                            updateUserWeight = updateUserWeight
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
    selectedBarbellType: BarbellType?,
    isDurationBased: Boolean,
    repsWeightRows: List<SetDisplayRow>,
    setsDone: Int,
    trackingResult: TrackingResult?,
    records: List<ExerciseRecordAndEquipment>,
    imperialSystem: Boolean,
    workoutStarted: Boolean,
    fabHeight: Dp,
    bottomPadding: Dp,
    lastBodyweightUpdate: ZonedDateTime,
    modificationSuggestion: ModificationSuggestion?,
    settingsMenu: @Composable () -> Unit,
    addSet: () -> Unit,
    updateRowValues: (Int, Float, Int) -> Unit,
    updateTare: (BarbellType) -> Unit,
    updateBottomBar: (Int?, Float?) -> Unit,
    toggleOtherEquipment: () -> Unit,
    toggleInfoDialog: () -> Unit,
    deleteSet: (Int) -> Unit,
    onAcceptSuggestion: () -> Unit,
    updateSetType: (Int, SetType) -> Unit,
    updateUserWeight: (Float) -> Unit
) {
    Column (Modifier.padding(horizontal = 16.dp)){
        if (exerciseNote.isNotBlank()) {
            Text(text = buildAnnotatedString {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(stringResource(R.string.note))
                }
                append(exerciseNote)
            }, modifier = Modifier.align(CenterHorizontally))
        }
        // TODO: show max 1 suggestion at a time (i.e., among modifications, weight update,
        //  enable ongoing notifications, etc...)
        SuggestModificationCard(
            isLoading = isLoading,
            hasDoneSomeSets = setsDone > 0,
            modificationSuggestion = modificationSuggestion,
            onAcceptSuggestion = onAcceptSuggestion,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        SuggestBodyweightUpdate(
            isLoading = isLoading,
            equipment = equipment,
            lastBodyweightUpdate = lastBodyweightUpdate,
            updateUserWeight = updateUserWeight
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
                    BarbellSelector(
                        selectedBarbellType = selectedBarbellType,
                        tare = tare,
                        toggleOtherEquipment = toggleOtherEquipment,
                        useImperialSystem = imperialSystem,
                        onBarbellSelected = { barbellType -> updateTare(barbellType) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(CenterHorizontally)
                    )
                }

                if (trackingResult != null && trackingResult.detectedReps > 0) {
                    SetTrackingChart(trackingResult)
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
    modifier: Modifier = Modifier,
    // true when used to show the suggestion card in the profile section
    disableActions: Boolean = false
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
                    {
                        if (!disableActions) {
                            notToday = true
                        }
                    },
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
                TextButton(onClick = {
                    if (!disableActions) {
                        notToday = true
                    }
                }) {
                    Text(text = stringResource(R.string.modification_suggestion_not_this_time))
                }
                Button(onClick = onAcceptSuggestion) {
                    Text(text = stringResource(R.string.modification_suggestion_do_it))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SuggestBodyweightUpdate(
    isLoading: Boolean,
    equipment: Equipment,
    lastBodyweightUpdate: ZonedDateTime,
    modifier: Modifier = Modifier,
    updateUserWeight: (Float) -> Unit,
) {
    var notToday by rememberSaveable { mutableStateOf(false) }
    var showUpdateWeightDialog by remember { mutableStateOf(false) }

    UpdateWeightDialog(
        prompt = stringResource(R.string.new_weight),
        dialogueIsOpen = showUpdateWeightDialog,
        toggleDialog = { showUpdateWeightDialog = !showUpdateWeightDialog },
        updateWeight = updateUserWeight
    )
    val MAX_DAYS_WITHOUT_UPDATE = 30L
    AnimatedVisibility(
        equipment == Equipment.BODY_WEIGHT &&
                !notToday &&
                !isLoading &&
                lastBodyweightUpdate.isBefore(ZonedDateTime.now().minusDays(MAX_DAYS_WITHOUT_UPDATE)),
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
                    text = stringResource(R.string.update_body_weight),
                    modifier = Modifier
                        .padding(16.dp)
                        .weight(1f),
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton({
                        notToday = true
                    },
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
                text = stringResource(
                    R.string.update_bodyweight_desc,
                    MAX_DAYS_WITHOUT_UPDATE
                ),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    notToday = true
                }) {
                    Text(text = stringResource(R.string.modification_suggestion_not_this_time))
                }
                Button(onClick = {
                    showUpdateWeightDialog = true
                }) {
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
            val daysAgo = remember { ChronoUnit.DAYS.between(record.date, ZonedDateTime.now()) }
            Text(
                record.date.format(formatter) + stringResource(R.string.days_ago, daysAgo),
                style = MaterialTheme.typography.titleMedium,
                fontStyle = FontStyle.Italic
            )
            if (record.equipment == Equipment.BARBELL) {
                val barbellTypeRes = remember {
                    record.barbellTypeResKey.takeIf { it.isNotEmpty() }?.let { resKey ->
                        BarbellType.entries.find { it.barbellResKey == resKey }?.barbellResource
                    } ?: barbellResFromWeight(record.tare)
                }
                Text(
                    stringResource(
                        R.string.barbell_used,
                        stringResource(barbellTypeRes),
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
    fabHeight: Dp,
    bottomPadding: Dp,
    addExercise: () -> Unit,
    finishWorkout: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(top = 8.dp)
    ){
        Text(
            stringResource(
                R.string.total_workout_time,
                workoutTimeFormatted
            ), style = MaterialTheme.typography.titleLarge)
        TextButton(
            onClick = finishWorkout,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            CompletionCheckmark(
                modifier = Modifier.size(140.dp)
            )
            Text(
                stringResource(R.string.complete_workout),
                style = MaterialTheme.typography.headlineLargeEmphasized,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(stringResource(R.string.workout_completion_tip), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = addExercise, modifier = Modifier.align(CenterHorizontally)) {
            Text(stringResource(R.string.add_exercise_to_workout))
        }
        Spacer(Modifier.height(160.dp))
        if (fabHeight > 0.dp) {
            Spacer(Modifier.height(fabHeight))
        }
        Spacer(Modifier.height(bottomPadding))
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExerciseSettingsMenu(
    changeExercise: () -> Unit,
    removeExercise: () -> Unit,
    addExercise: () -> Unit,
    viewStatistics: () -> Unit,
    mediaControlsDismissed: Boolean,
    showMediaControls: () -> Unit
) {
    IconsWithOverflow(
        maxVisibleItems = 0,
        contents = buildList {
            add(
                IconAndLabel(
                    icon = Icons.Outlined.Edit,
                    label = stringResource(R.string.replace_exercise),
                    onClick = changeExercise
                )
            )
            add(
                IconAndLabel(
                    icon = Icons.Outlined.Delete,
                    label = stringResource(R.string.skip_exercise_this_workout_only),
                    onClick = removeExercise
                )
            )
            add(
                IconAndLabel(
                    icon = Icons.Outlined.Add,
                    label = stringResource(R.string.add_another_exercise),
                    onClick = addExercise
                )
            )
            add(
                IconAndLabel(
                    icon = Icons.Outlined.Timeline,
                    label = stringResource(R.string.view_exercise_history_and_stats),
                    onClick = viewStatistics
                )
            )
            if (mediaControlsDismissed) {
                add(
                    IconAndLabel(
                        icon = Icons.Outlined.PlayArrow,
                        label = stringResource(R.string.show_media_controls),
                        onClick = showMediaControls
                    )
                )
            }
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BarbellSelector(
    selectedBarbellType: BarbellType?,
    tare: Float?,
    toggleOtherEquipment: () -> Unit,
    useImperialSystem: Boolean,
    onBarbellSelected: (BarbellType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    val selectedDisplayText = when {
        selectedBarbellType == null || selectedBarbellType == BarbellType.OTHER ->
            stringResource(BarbellType.OTHER.barbellResource) +
                    " " + weightAndUnit(tare ?: 0f, useImperialSystem, inParenthesis = true)
        else ->
            stringResource(selectedBarbellType.barbellResource) +
                    " (${selectedBarbellType.weight[useImperialSystem]} ${
                        if (useImperialSystem) stringResource(R.string.lb)
                        else stringResource(R.string.kg)
                    })"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedDisplayText,
            onValueChange = {},
            label = { Text(stringResource(R.string.barbell)) },
            trailingIcon = {
                Row(verticalAlignment = CenterVertically) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            singleLine = true,
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
        )

        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize()
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(0, 1),
                interactionSource = remember { MutableInteractionSource() }
            ) {
                BarbellType.entries.forEachIndexed { index, option ->
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
                    val isSelected = option == selectedBarbellType

                    DropdownMenuItem(
                        text = { Text(optionText) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (option == BarbellType.OTHER)
                                    Icons.Rounded.Edit
                                else
                                    Icons.Rounded.FitnessCenter,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = if (isSelected) {
                            { Icon(Icons.Default.CheckCircle, contentDescription = null) }
                        } else null,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            if (option == BarbellType.OTHER) {
                                toggleOtherEquipment()
                            } else {
                                onBarbellSelected(option)
                            }
                            expanded = false
                        },
                        selected = isSelected,
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        shapes = MenuDefaults.itemShapes()
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
        DropdownMenuPopup(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(0, 1),
                interactionSource = remember { MutableInteractionSource() },
            ) {
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
                        selected = type == setType,
                        trailingIcon = if (type == setType) {
                            {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                            }
                        } else null,
                        shapes = MenuDefaults.itemShapes()
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        val textColor =
            if (toBeDone) LocalContentColor.current else MaterialTheme.colorScheme.outline
        val unitString =
            if (imperialSystem) stringResource(R.string.lb) else stringResource(R.string.kg)

        val autoAwesomeId = "auto_awesome"
        val inlineContent = mapOf(
            autoAwesomeId to InlineTextContent(
                placeholder = Placeholder(16.sp, 16.sp, PlaceholderVerticalAlign.Center)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = textColor
                )
            }
        )

        val annotatedText = buildAnnotatedString {
            val style = SpanStyle(color = textColor)

            // Reps label
            withStyle(style) {
                append(
                    if (isDurationBased) stringResource(R.string.exercise_hold) + ": "
                    else stringResource(R.string.reps) + ": "
                )
            }

            // Reps value struck through when a projection differs
            val repsStruck = projectedReps != null && repsInRow != projectedReps
            withStyle(style.copy(textDecoration = if (repsStruck) TextDecoration.LineThrough else TextDecoration.None)) {
                append(repsInRow)
            }

            // Projected reps (icon + value)
            if (repsStruck) {
                appendInlineContent(autoAwesomeId)
                withStyle(style) { append(projectedReps) }
            }

            // Reps suffix
            withStyle(style) {
                append(if (isDurationBased) "s " else " ")
            }

            // Weight label
            withStyle(style) {
                append(stringResource(R.string.weight) + ": ")
            }

            // Weight value, omitted when loading ("...") and a projection exists
            if (projectedWeight == null || weightInRow != "...") {
                val weightStruck = projectedWeight != null && weightInRow != projectedWeight
                withStyle(style.copy(textDecoration = if (weightStruck) TextDecoration.LineThrough else TextDecoration.None)) {
                    append(weightInRow)
                }

                // Projected weight (icon + value)
                if (weightStruck) {
                    appendInlineContent(autoAwesomeId)
                    withStyle(style) { append(projectedWeight) }
                }
            } else {
                appendInlineContent(autoAwesomeId)
                withStyle(style) { append(projectedWeight) }
            }

            // Unit
            withStyle(style) { append(" $unitString") }
        }

        Text(
            text = annotatedText,
            inlineContent = inlineContent
        )
    }
}

@Composable
fun WorkoutOverviewListItem(
    name: String,
    imageModel: Any?,
    selected: Boolean,
    setsDone: Int,
    totalSets: Int,
    isDurationBased: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = if (selected)
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        else
            CardDefaults.cardColors(),
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.large)
                )
                if (selected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .weight(1f)
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                val progress = if (totalSets > 0) setsDone.toFloat() / totalSets else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "$setsDone / $totalSets ${if (isDurationBased) stringResource(R.string.exercise_hold) else stringResource(R.string.sets)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetTrackingChart(
    result: TrackingResult,
    baseShape: CornerBasedShape = MaterialTheme.shapes.small,
    baseColor: Color = MaterialTheme.colorScheme.secondary,
    thickness: Dp = 25.dp,
    columnCollectionSpacing: Dp = 4.dp,
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(result) {
        val romValues = result.repMetrics.map { it.rangeOfMotionM * 100.0 }
        val durationValues = result.repMetrics.map { (it.concentricMs + it.eccentricMs) }
        modelProducer.runTransaction {
            columnSeries { series(romValues) }
            lineSeries { series(durationValues) }
        }
    }
    val cmString = stringResource(R.string.centimeters_short)
    val msString = stringResource(R.string.milliseconds_short)
    val repFormatter = CartesianValueFormatter { _, x, _ -> "R${(x.toInt() + 1)}" }
    val romFormatter = CartesianValueFormatter { _, y, _ -> "${"%.0f".format(y)}$cmString" }
    val durFormatter = CartesianValueFormatter { _, y, _ -> "${"%.0f".format(y)}$msString" }
    Column {
        Text(
            stringResource(R.string.last_set_tracking_data),
            style = MaterialTheme.typography.titleMediumEmphasized,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .fillMaxWidth()
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier
                    .size(10.dp)
                    .background(
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.shapes.extraSmall
                    ))
                // TODO: convert cm to in when imperial?
                Text(stringResource(R.string.rom_cm), style = MaterialTheme.typography.labelSmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.extraSmall))
                Text(stringResource(R.string.duration_ms), style = MaterialTheme.typography.labelSmall)
            }
        }
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(
                    columnProvider = columnProviderWithHighlight(baseShape, baseColor, thickness),
                    columnCollectionSpacing = columnCollectionSpacing,
                    verticalAxisPosition = Axis.Position.Vertical.Start
                ),
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        listOf(
                            LineCartesianLayer.rememberLine(LineCartesianLayer.LineFill.single(Fill(
                                MaterialTheme.colorScheme.tertiary
                            )))
                        )
                    ),
                    verticalAxisPosition = Axis.Position.Vertical.End
                ),
                startAxis = VerticalAxis.rememberStart(
                    line = rememberLineComponent(Fill.Transparent),
                    tick = rememberLineComponent(Fill.Transparent),
                    valueFormatter = romFormatter,
                    itemPlacer = VerticalAxis.ItemPlacer.step(step = { 5.0 })
                ),
                endAxis = VerticalAxis.rememberEnd(
                    line = rememberLineComponent(Fill.Transparent),
                    tick = rememberLineComponent(Fill.Transparent),
                    valueFormatter = durFormatter,
                    guideline = null,
                    itemPlacer = VerticalAxis.ItemPlacer.step(step = { 5.0 })
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    tick = rememberLineComponent(Fill.Transparent),
                    guideline = rememberLineComponent(Fill.Transparent),
                    valueFormatter = repFormatter,
                ),
            ),
            modelProducer = modelProducer,
            scrollState = rememberVicoScrollState(false),
            animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
        )
    }
}