package agdesigns.elevatefitness.ui.screens.create_exercise

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.Exercise
import agdesigns.elevatefitness.navigation.DestinationsNavigator
import agdesigns.elevatefitness.ui.common.SelectableCard
import agdesigns.elevatefitness.utils.plus
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Cable
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.PrecisionManufacturing
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.times
import coil3.compose.AsyncImage
import agdesigns.elevatefitness.shared.Equipment
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun CreateExerciseDialog(
    navigator: DestinationsNavigator,
    muscleOrdinal: Int = 0,  // used for init
    filterEquipment: Equipment? = null,
    viewModel: CreateExerciseViewModel = hiltViewModel()
) {
    val exerciseState by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(Unit) {
        viewModel.onEvent(
            CreateExerciseEvent.Init(muscleOrdinal, filterEquipment)
        )
    }

    // Make top app bar opaque
    scrollBehavior.state.contentOffset = scrollBehavior.state.heightOffsetLimit

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                Modifier.navigationBarsPadding()
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Row {
                        Text(
                            stringResource(R.string.create_a_new_exercise),
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                            autoSize = TextAutoSize.StepBased(
                                maxFontSize = MaterialTheme.typography.headlineSmall.fontSize
                            )
                        )
                        Spacer(Modifier.width(16.dp))
                    }
                },
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
                    val keyboardController = LocalSoftwareKeyboardController.current
                    val fillString = stringResource(R.string.fill_every_field)

                    FilledTonalButton(
                        shapes = ButtonDefaults.shapes(),
                        enabled = exerciseState.name.isNotBlank(),
                        onClick = {
                            if (!viewModel.onEvent(CreateExerciseEvent.TryCreateExercise)) {
                                scope.launch {
                                    keyboardController?.hide()
                                    snackbarHostState.showSnackbar(fillString)
                                }
                            } else {
                                navigator.navigateUp()
                            }
                        },
                        modifier = Modifier.align(CenterVertically).padding(4.dp)
                    ) {
                        Text(stringResource(R.string.save))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding + PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // Exercise Name Section
            item {
                ExerciseNameSection(
                    name = exerciseState.name,
                    onNameChange = { viewModel.onEvent(CreateExerciseEvent.UpdateName(it)) }
                )
            }

            // Equipment Section
            item {
                Card(
                    shape = MaterialTheme.shapes.large.copy(
                        bottomStart = CornerSize(4.dp),
                        bottomEnd = CornerSize(4.dp)
                    ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    EquipmentSection(
                        selectedEquipment = exerciseState.equipment,
                        onEquipmentSelected = { equipment ->
                            viewModel.onEvent(CreateExerciseEvent.UpdateEquipment(equipment))
                        }
                    )
                }
            }

            // Difficulty Section
            item {
                Card(
                    shape = MaterialTheme.shapes.large.copy(
                        topStart = CornerSize(4.dp),
                        topEnd = CornerSize(4.dp),
                        bottomStart = CornerSize(4.dp),
                        bottomEnd = CornerSize(4.dp)
                    ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                )  {
                    DifficultySection(
                        selectedDifficulty = exerciseState.difficulty,
                        onDifficultySelected = { difficulty ->
                            viewModel.onEvent(CreateExerciseEvent.UpdateDifficulty(difficulty))
                        }
                    )
                }
            }

            // Primary Muscle Section
            item {
                Card(
                    shape = MaterialTheme.shapes.large.copy(
                        topStart = CornerSize(4.dp),
                        topEnd = CornerSize(4.dp),
                        bottomStart = CornerSize(4.dp),
                        bottomEnd = CornerSize(4.dp)
                    ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                )  {
                    PrimaryMuscleSection(
                        selectedMuscle = exerciseState.primaryMuscle,
                        onMuscleSelected = { muscle ->
                            viewModel.onEvent(CreateExerciseEvent.UpdatePrimaryMuscle(muscle))
                        }
                    )
                }
            }

            // Secondary Muscles Section
            item {
                Card(
                    shape = MaterialTheme.shapes.large.copy(
                        topStart = CornerSize(4.dp),
                        topEnd = CornerSize(4.dp),
                    ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    SecondaryMusclesSection(
                        selectedMuscles = exerciseState.secondaryMuscles,
                        onMuscleToggled = { index ->
                            viewModel.onEvent(CreateExerciseEvent.ToggleSecondaryMuscle(index))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseNameSection(
    name: String,
    onNameChange: (String) -> Unit
) {
    Column(Modifier.padding(32.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.enter_exercise_name)) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        )
    }
}

@Composable
private fun EquipmentSection(
    selectedEquipment: Equipment,
    onEquipmentSelected: (Equipment) -> Unit
) {
    val lazyRowState = rememberLazyListState()
    // FIXME: should only scroll once, after init
    LaunchedEffect(selectedEquipment) {
        lazyRowState.animateScrollToItem(selectedEquipment.ordinal-1)
    }
    Column {
        SectionHeader(
            title = stringResource(R.string.select_equipment),
        )
        Spacer(Modifier.height(8.dp))

        LazyRow(
            state = lazyRowState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(Equipment.entries.drop(1)) { equipment ->
                EquipmentCard(
                    equipment = equipment,
                    isSelected = equipment == selectedEquipment,
                    onClick = { onEquipmentSelected(equipment) }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DifficultySection(
    selectedDifficulty: Exercise.ExerciseDifficulty,
    onDifficultySelected: (Exercise.ExerciseDifficulty) -> Unit
) {
    Column {
        SectionHeader(
            title = stringResource(R.string.select_difficulty),
        )
        Spacer(Modifier.height(8.dp))

        Row(
            Modifier
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            Exercise.ExerciseDifficulty.entries.forEachIndexed { index, difficulty ->
                val modifier = if (difficulty == selectedDifficulty)
                    Modifier.weight(1f + ButtonGroupDefaults.ExpandedRatio) // expanded
                else Modifier.weight(1f)

                var textAlign = when (index) {
                    0 -> TextAlign.End
                    Exercise.ExerciseDifficulty.entries.lastIndex -> TextAlign.Start
                    else -> TextAlign.Center
                }
                if (difficulty == selectedDifficulty) {
                    textAlign = TextAlign.Center
                }
                ToggleButton(
                    checked = difficulty == selectedDifficulty,
                    onCheckedChange = {
                        onDifficultySelected(difficulty)
                    },
                    modifier = modifier,
                    shapes =
                        when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            Exercise.ExerciseDifficulty.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                ) {
                    Text(
                        stringResource(difficulty.difficultyResource),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = textAlign,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PrimaryMuscleSection(
    selectedMuscle: Exercise.Muscle,
    onMuscleSelected: (Exercise.Muscle) -> Unit
) {
    val lazyRowState = rememberLazyListState()
    // FIXME: should only scroll once, after init
    LaunchedEffect(selectedMuscle) {
        lazyRowState.animateScrollToItem(selectedMuscle.ordinal - 1)
    }
    Column {
        SectionHeader(
            title = stringResource(R.string.select_primary_muscle),
        )
        Spacer(Modifier.height(8.dp))

        LazyRow (
            state = lazyRowState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(Exercise.Muscle.entries.drop(1)) { muscle ->
                MuscleCard(
                    muscle = muscle,
                    isSelected = muscle == selectedMuscle,
                    onClick = { onMuscleSelected(muscle) }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SecondaryMusclesSection(
    selectedMuscles: List<Boolean>,
    onMuscleToggled: (Int) -> Unit
) {
    Column {
        SectionHeader(
            title = stringResource(R.string.select_secondary_muscle_s),
            subtitle = stringResource(R.string.select_secondary_muscle_subtitle)
        )
        Spacer(Modifier.height(8.dp))

        // need a fixed height because of nested lazy lists
        val columnHeight = Exercise.Muscle.entries.drop(1).size / 2 * 80.dp +
                8.dp * (Exercise.Muscle.entries.drop(1).size / 2 - 1)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(columnHeight).padding(horizontal = 16.dp)
        ) {
            itemsIndexed(Exercise.Muscle.entries.drop(1)) { index, muscle ->
                SecondaryMuscleCard(
                    muscle = muscle,
                    isSelected = selectedMuscles.getOrElse(index) { false },
                    onClick = { onMuscleToggled(index) },
                    height = 80.dp
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null
) {
    Column(modifier = Modifier
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EquipmentCard(
    equipment: Equipment,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val equipmentIcon = getEquipmentIcon(equipment)
    val equipmentImageId = getEquipmentImage(equipment)
    SelectableCard(
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier
            .width(120.dp)
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceDim
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            if (equipmentIcon != null) {
                Icon(
                    imageVector = equipmentIcon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (equipmentImageId != null) {
                AsyncImage(
                    model = equipmentImageId,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    colorFilter = if (isSelected) {
                        ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)
                    } else {
                        ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(equipment.equipmentNameResource),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MuscleCard(
    muscle: Exercise.Muscle,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    SelectableCard(
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .width(120.dp)
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceDim
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Use muscle.imageRes if available, otherwise use a default icon
            AsyncImage(
                model = muscle.imageRes,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                placeholder = painterResource(R.drawable.full_body), // fallback
                error = painterResource(R.drawable.full_body)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(muscle.muscleNameResource),
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SecondaryMuscleCard(
    muscle: Exercise.Muscle,
    isSelected: Boolean,
    onClick: () -> Unit,
    height: Dp = 80.dp
) {
    SelectableCard(
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        )
    ) {
        Row(
            verticalAlignment = CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.secondary
                )
            )
            Spacer(Modifier.width(8.dp))
            AsyncImage(
                model = muscle.imageRes,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                placeholder = painterResource(R.drawable.full_body),
                error = painterResource(R.drawable.full_body)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(muscle.muscleNameResource),
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun getEquipmentIcon(equipment: Equipment): ImageVector? {
    return when (equipment) {
        Equipment.BARBELL -> Icons.Outlined.FitnessCenter
        Equipment.MACHINE -> Icons.Outlined.PrecisionManufacturing
        Equipment.CABLES -> Icons.Outlined.Cable
        Equipment.BODY_WEIGHT -> Icons.AutoMirrored.Outlined.DirectionsRun
        else -> null
    }
}

@Composable fun getEquipmentImage(equipment: Equipment): Int? {
    return when (equipment) {
        Equipment.DUMBBELL -> R.drawable.icon_dumbbell
        else -> null
    }
}