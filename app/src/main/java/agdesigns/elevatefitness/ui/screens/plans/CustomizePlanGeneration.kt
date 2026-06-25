package agdesigns.elevatefitness.ui.screens.plans

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.Exercise
import agdesigns.elevatefitness.shared.Equipment
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import agdesigns.elevatefitness.ui.common.SelectableCard
import agdesigns.elevatefitness.ui.screens.view_exercises.ExercisesEvent
import agdesigns.elevatefitness.ui.screens.view_exercises.ExercisesViewModel
import agdesigns.elevatefitness.utils.plus
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanDifficulty
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanGoal
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanSplit
import agdesigns.elevatefitness.ui.navigation.DestinationsNavigator
import agdesigns.elevatefitness.ui.navigation.CustomSplitEditorDestination
import agdesigns.elevatefitness.ui.navigation.ExcludeExercisesDestination
import agdesigns.elevatefitness.ui.navigation.ViewGeneratedPlanDestination
import agdesigns.elevatefitness.ui.common.ReversedCorner
import agdesigns.elevatefitness.ui.common.ReversedCornersShape
import agdesigns.elevatefitness.ui.common.SelectableCardDefaults
import agdesigns.elevatefitness.ui.common.lazyGroupedCard
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import sh.calvin.reorderable.rememberReorderableLazyListState

private val WorkoutPlanGoal.imageRes: Int
    get() = when (this) {
        WorkoutPlanGoal.HYPERTROPHY -> R.drawable.cable_curl
        WorkoutPlanGoal.STRENGTH -> R.drawable.headstand_push_up
        WorkoutPlanGoal.ENDURANCE -> R.drawable.plank
        WorkoutPlanGoal.CARDIO -> R.drawable.sit_ups
    }

private val WorkoutPlanDifficulty.shortLabelResource: Int
    get() = when (this) {
        WorkoutPlanDifficulty.AUTO -> R.string.plan_diff_auto_short
        WorkoutPlanDifficulty.BEGINNER -> R.string.plan_diff_beginner_short
        WorkoutPlanDifficulty.INTERMEDIATE -> R.string.plan_diff_intermediate_short
        WorkoutPlanDifficulty.ADVANCED -> R.string.plan_diff_advanced_short
    }

private val WorkoutPlanSplit.imageRes: Int?
    get() = when (this) {
        WorkoutPlanSplit.PPL -> R.drawable.bench_press
        WorkoutPlanSplit.BRO -> R.drawable.generic_barbell
        WorkoutPlanSplit.FULL_BODY -> R.drawable.generic_machine
        WorkoutPlanSplit.UPPER_LOWER -> R.drawable.chest_dip
        WorkoutPlanSplit.AUTO -> null
        WorkoutPlanSplit.CUSTOM -> null
    }

private val WorkoutPlanSplit.shortLabelResource: Int
    get() = when (this) {
        WorkoutPlanSplit.FULL_BODY -> R.string.splits_fullbody_short
        WorkoutPlanSplit.PPL -> R.string.splits_bro_short
        WorkoutPlanSplit.UPPER_LOWER -> R.string.splits_upper_lower_short
        WorkoutPlanSplit.BRO -> R.string.splits_gainz_short
        WorkoutPlanSplit.AUTO -> R.string.splits_auto
        WorkoutPlanSplit.CUSTOM -> R.string.splits_custom_short
    }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CustomizePlanGeneration(
    navigator: DestinationsNavigator,
    viewModel: CustomizePlanViewModel = hiltViewModel()
) {
    val hasPreviousWorkouts by viewModel.hasPreviousWorkouts.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()) { false }
    val goalChoice = rememberSaveable { mutableStateOf(WorkoutPlanGoal.HYPERTROPHY) }
    val expertiseLevel = rememberSaveable(hasPreviousWorkouts) {
        mutableStateOf(
            if (hasPreviousWorkouts)
                WorkoutPlanDifficulty.AUTO
            else
                WorkoutPlanDifficulty.INTERMEDIATE
        )
    }
    val workoutSplit = rememberSaveable { mutableStateOf(WorkoutPlanSplit.AUTO) }

    val excludedExerciseIds by viewModel.excludedExerciseIds.collectAsState()

    val customMuscleDays by viewModel.customDays.collectAsState()

    val customSplitIsValid = remember(customMuscleDays) {
        customMuscleDays.isNotEmpty() && customMuscleDays.any { day ->
            day.muscleOrdinals.isNotEmpty()
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = { Text(stringResource(R.string.generate_a_new_plan)) },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(
                        onClick = { navigator.navigateUp() },
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.close_icon)
                        )
                    }
                },
                actions = {
                    FilledIconButton(
                        onClick = {
                            navigator.navigateUp()
                            navigator.navigate(
                                ViewGeneratedPlanDestination(
                                    goalChoice.value,
                                    expertiseLevel.value,
                                    workoutSplit.value,
                                    excludedExerciseIds.toList(),
                                    customMuscleDays.map {
                                        it.muscleOrdinals.toList()
                                    }
                                )
                            )
                        },
                        shapes = IconButtonDefaults.shapes(
                            MaterialTheme.shapes.small,
                            MaterialTheme.shapes.extraLarge
                        ),
                        enabled = customSplitIsValid || workoutSplit.value != WorkoutPlanSplit.CUSTOM
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = stringResource(R.string.done_icon)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding + PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large.copy(
                        bottomStart = CornerSize(4.dp),
                        bottomEnd = CornerSize(4.dp)
                    ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    GoalSection(
                        selectedGoal = goalChoice.value,
                        onGoalSelected = { goalChoice.value = it }
                    )
                }
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large.copy(
                        topStart = CornerSize(4.dp),
                        topEnd = CornerSize(4.dp),
                        bottomStart = CornerSize(4.dp),
                        bottomEnd = CornerSize(4.dp)
                    ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    DifficultySection(
                        selectedDifficulty = expertiseLevel.value,
                        autoAvailable = hasPreviousWorkouts,
                        onDifficultySelected = { expertiseLevel.value = it },
                    )
                }
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large.copy(
                        topStart = CornerSize(4.dp),
                        topEnd = CornerSize(4.dp),
                        bottomStart = CornerSize(4.dp),
                        bottomEnd = CornerSize(4.dp)
                    ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    SplitSection(
                        selectedSplit = workoutSplit.value,
                        customMuscleDays = customMuscleDays,
                        customSplitIsValid = customSplitIsValid,
                        onSplitSelected = { workoutSplit.value = it },
                        onOpenCustomEditor = { navigator.navigate(CustomSplitEditorDestination(viewModel)) }
                    )
                }
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large.copy(
                        topStart = CornerSize(4.dp),
                        topEnd = CornerSize(4.dp)
                    ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    ExcludeExercisesSection(
                        excludedCount = excludedExerciseIds.size,
                        onOpenExcludeScreen = {
                            navigator.navigate(ExcludeExercisesDestination(excludedExerciseIds.toList()))
                        },
                        onClearAll = {
                            viewModel.saveExcludedExerciseIds(emptySet())
                        }
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        navigator.navigateUp()
                        navigator.navigate(
                            ViewGeneratedPlanDestination(
                                goalChoice.value,
                                expertiseLevel.value,
                                workoutSplit.value,
                                excludedExerciseIds.toList()
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 14.dp),
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(stringResource(R.string.generate_plan))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GoalSection(
    selectedGoal: WorkoutPlanGoal,
    onGoalSelected: (WorkoutPlanGoal) -> Unit
) {
    Column {
        SectionHeader(title = stringResource(R.string.what_is_your_goal_when_training))
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(WorkoutPlanGoal.entries) { goal ->
                PlanChoiceCard(
                    imageRes = goal.imageRes,
                    label = stringResource(goal.goalResource),
                    isSelected = goal == selectedGoal,
                    onClick = { onGoalSelected(goal) }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DifficultySection(
    selectedDifficulty: WorkoutPlanDifficulty,
    autoAvailable: Boolean,
    onDifficultySelected: (WorkoutPlanDifficulty) -> Unit
) {
    Column {
        SectionHeader(
            title = stringResource(R.string.what_is_your_expertise_level),
            subtitle = stringResource(selectedDifficulty.expertiseResource)
        )
        Spacer(Modifier.height(8.dp))
        if (autoAvailable) {
            ToggleButton(
                checked = WorkoutPlanDifficulty.AUTO == selectedDifficulty,
                onCheckedChange = { onDifficultySelected(WorkoutPlanDifficulty.AUTO) },
                shapes = ToggleButtonShapes(
                    shape = ButtonGroupDefaults.connectedButtonCheckedShape,
                    pressedShape = ButtonGroupDefaults.connectedLeadingButtonPressShape,
                    checkedShape = ButtonGroupDefaults.connectedButtonCheckedShape,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
//                        modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(
                    stringResource(WorkoutPlanDifficulty.AUTO.shortLabelResource),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            WorkoutPlanDifficulty.entries.minus(WorkoutPlanDifficulty.AUTO).forEachIndexed { index, difficulty ->
                val modifier = if (difficulty == selectedDifficulty)
                    Modifier.weight(1f + ButtonGroupDefaults.ExpandedRatio)
                else Modifier.weight(1f)

                ToggleButton(
                    checked = difficulty == selectedDifficulty,
                    onCheckedChange = { onDifficultySelected(difficulty) },
                    modifier = modifier,
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        WorkoutPlanDifficulty.entries.lastIndex-1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                ) {
                    Text(
                        stringResource(difficulty.shortLabelResource),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SplitSection(
    selectedSplit: WorkoutPlanSplit,
    customMuscleDays: List<ProgramDays>,
    customSplitIsValid: Boolean,
    onSplitSelected: (WorkoutPlanSplit) -> Unit,
    onOpenCustomEditor: () -> Unit
) {
    val subtitle = if (selectedSplit == WorkoutPlanSplit.CUSTOM && customMuscleDays.isNotEmpty()) {
        stringResource(R.string.custom_split_n_days, customMuscleDays.size)
    } else {
        stringResource(selectedSplit.splitResource)
    }
    Column {
        SectionHeader(
            title = stringResource(R.string.how_many_times_per_week_do_you_want_to_exercise),
            subtitle = subtitle
        )
        // if custom split is not valid, show error
        if (selectedSplit == WorkoutPlanSplit.CUSTOM && !customSplitIsValid) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 16.dp)
                )
                Text(
                    text = stringResource(R.string.custom_split_invalid),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(WorkoutPlanSplit.entries) { split ->
                PlanChoiceCard(
                    imageRes = split.imageRes,
                    label = stringResource(split.shortLabelResource),
                    isSelected = split == selectedSplit,
                    onClick = {
                        onSplitSelected(split)
                        if (split == WorkoutPlanSplit.CUSTOM) {
                            onOpenCustomEditor()
                        }
                    },
                    customIcon = when (split) {
                        WorkoutPlanSplit.CUSTOM -> Icons.Default.Create
                        WorkoutPlanSplit.AUTO -> Icons.Default.AutoAwesome
                        else -> null
                    }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlanChoiceCard(
    imageRes: Int?,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    customIcon: ImageVector? = null
) {
    SelectableCard(
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier
            .width(120.dp)
            .height(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceDim
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        if (imageRes != null) {
            AsyncImage(
                model = imageRes,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .clip(SelectableCardDefaults.shape(isSelected))
            )
        } else if (customIcon != null) {
            Surface(
                shape = SelectableCardDefaults.shape(isSelected),
                color = MaterialTheme.colorScheme.surfaceBright
            ) {
                Icon(
                    imageVector = customIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .height(64.dp)
                        .clip(SelectableCardDefaults.shape(isSelected))
                )
            }
        }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExcludeExercisesSection(
    excludedCount: Int,
    onOpenExcludeScreen: () -> Unit,
    onClearAll: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.exclude_exercises_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.exclude_exercises_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (excludedCount > 0) {
                TextButton(onClick = onClearAll) {
                    Text(stringResource(R.string.clear_all_excluded))
                }
            }
        }
        SelectableCard(
            selected = false,
            onClick = onOpenExcludeScreen,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (excludedCount == 0)
                        stringResource(R.string.no_exercises_excluded)
                    else
                        stringResource(R.string.n_exercises_excluded, excludedCount),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExcludeExercisesScreen(
    navigator: DestinationsNavigator,
    excludedIds: List<Long>,
    exercisesViewModel: ExercisesViewModel = hiltViewModel(),
    viewModel: CustomizePlanViewModel = hiltViewModel()
) {
    val exercisesState by exercisesViewModel.state.collectAsState()
    var localExcludedIds by remember { mutableStateOf(excludedIds.toSet()) }
    LaunchedEffect(localExcludedIds) {
        viewModel.saveExcludedExerciseIds(localExcludedIds)
    }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedMuscle by rememberSaveable { mutableStateOf(Exercise.Muscle.EVERYTHING) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()) { false }

    LaunchedEffect(selectedMuscle) {
        exercisesViewModel.onEvent(ExercisesEvent.GetExercises(selectedMuscle))
    }
    LaunchedEffect(searchQuery) {
        exercisesViewModel.onEvent(ExercisesEvent.FilterExercise(searchQuery))
    }

    val exercises = if (searchQuery.isBlank())
        exercisesState.exercisesFilterEquip ?: emptyList()
    else
        exercisesState.searchResults?.map { it.exercise } ?: emptyList()

    val newExercises = exercises.filter { it.exerciseId !in localExcludedIds }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                modifier = Modifier.clickable {
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = { Text(stringResource(R.string.n_exercises_excluded, localExcludedIds.size)) },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(
                        onClick = { navigator.navigateUp() },
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    if (localExcludedIds.isNotEmpty()) {
                        TextButton(onClick = { localExcludedIds = emptySet() }) {
                            Text(stringResource(R.string.clear_all_excluded))
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val cardColors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.search_exercises_to_exclude)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = null)
                        }
                    }
                } else null,
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        null,
                        modifier = Modifier
                            .size(30.dp)
                            .padding(4.dp)
                    )
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(
                        items = Equipment.entries.drop(1),
                        key = { _, e -> e.ordinal }
                    ) { _, equipment ->
                        FilterChip(
                            selected = equipment == exercisesState.equipToFiler,
                            onClick = {
                                exercisesViewModel.onEvent(
                                    ExercisesEvent.FilterExerciseEquipment(
                                        if (equipment != exercisesState.equipToFiler) equipment
                                        else Equipment.EVERYTHING
                                    )
                                )
                            },
                            label = { Text(stringResource(equipment.equipmentNameResource)) },
                            leadingIcon = if (equipment == exercisesState.equipToFiler) {
                                {
                                    Icon(
                                        Icons.Default.Done,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = ReversedCornersShape(
                    MaterialTheme.shapes.medium.topStart,
                    corners = setOf(ReversedCorner.BottomStart, ReversedCorner.BottomEnd)
                ),
                modifier = Modifier.zIndex(1f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        AsyncImage(
                            model = R.drawable.full_body,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(30.dp)
                                .padding(4.dp)
                                .clip(MaterialTheme.shapes.small)
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(
                            items = Exercise.Muscle.entries.drop(1),
                            key = { _, m -> m.ordinal }
                        ) { _, muscle ->
                            FilterChip(
                                selected = muscle == selectedMuscle,
                                onClick = {
                                    selectedMuscle = if (muscle != selectedMuscle) muscle
                                    else Exercise.Muscle.EVERYTHING
                                },
                                label = { Text(stringResource(muscle.muscleNameResource)) },
                                leadingIcon = if (muscle == selectedMuscle) {
                                    {
                                        Icon(
                                            Icons.Default.Done,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }

            LazyColumn(
                state = listState,
//            contentPadding = innerPadding + PaddingValues(bottom = 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (newExercises.size > 1) {
                    item {
                        TextButton(
                            onClick = {
                                localExcludedIds =
                                    localExcludedIds + newExercises.map { it.exerciseId }.toSet()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(stringResource(R.string.exclude_all_n_results, newExercises.size))
                        }
                    }
                }
                lazyGroupedCard(
                    colors = cardColors
                ) {
                    exercises.forEach { exercise ->
                        val isExcluded = exercise.exerciseId in localExcludedIds
                        subCard(onClick = {
                            localExcludedIds = if (isExcluded)
                                localExcludedIds - exercise.exerciseId
                            else
                                localExcludedIds + exercise.exerciseId
                        }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()/*
                            .padding(horizontal = 16.dp)
                            .padding(vertical = 8.dp)*/,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = exercise.image,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(MaterialTheme.shapes.small)
                                )
                                Column(
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        exercise.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        stringResource(exercise.primaryMuscle.muscleNameResource),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Checkbox(
                                    checked = isExcluded,
                                    onCheckedChange = {
                                        localExcludedIds = if (isExcluded)
                                            localExcludedIds - exercise.exerciseId
                                        else
                                            localExcludedIds + exercise.exerciseId
                                    }
                                )

                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun CustomSplitEditorScreen(
    navigator: DestinationsNavigator,
    viewModel: CustomizePlanViewModel = hiltViewModel()
) {
    // Each day is a set of selected muscle ordinals (excluding EVERYTHING at index 0)
    val days by viewModel.customDays.collectAsState()
    val muscles = remember { Exercise.Muscle.entries.drop(1) } // exclude EVERYTHING
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()) { false }

    val noValidDays = remember(days) { days.all { it.muscleOrdinals.isEmpty() } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = { Text(stringResource(R.string.custom_split_editor_title)) },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(
                        onClick = { navigator.navigateUp() },
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.close_icon)
                        )
                    }
                },
                actions = {
                    FilledIconButton(
                        onClick = {
                            navigator.navigateUp()
                        },
                        enabled = !noValidDays,
                        shapes = IconButtonDefaults.shapes(
                            MaterialTheme.shapes.small,
                            MaterialTheme.shapes.extraLarge
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = stringResource(R.string.done_icon)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val cardColors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
        val haptics = LocalHapticFeedback.current
        val lazyListState = rememberLazyListState()
        val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
            viewModel.reorderDays(
                from.key.toString().toLongOrNull() ?: 0L,
                to.key.toString().toLongOrNull() ?: 0L
            )

            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
        LazyColumn(
            state = lazyListState,
            contentPadding = innerPadding + PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            lazyGroupedCard(
                colors = cardColors
            ) {
                days.forEach { day ->
                    reorderableSubCard(day.id, reorderableLazyListState, onClick = {}) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.custom_split_day, day.dayIndex + 1),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            AnimatedVisibility(
                                visible = true,//days.size > 1,
                                enter = scaleIn() + fadeIn(),
                                exit = scaleOut() + fadeOut()
                            ) {
                                FilledIconButton(
                                    onClick = {
                                        viewModel.removeCustomDay(day.dayIndex)
                                    },
                                    shapes = IconButtonDefaults.shapes()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
//                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
//                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            muscles.forEach { muscle ->
                                val isSelected = muscle.ordinal in day.muscleOrdinals
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.toggleMuscleToDay(day.dayIndex, muscle.ordinal)
                                    },
                                    label = { Text(stringResource(muscle.muscleNameResource)) },
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                Icons.Default.Done,
                                                contentDescription = null,
                                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }
                if (days.size < 7) {
                    subCard(onClick = {
                        viewModel.addCustomDay()
                    }) {
                        TextButton(
                            onClick = {
                                viewModel.addCustomDay()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text(stringResource(R.string.custom_split_add_day))
                        }
                    }
                }
            }
        }
    }
}