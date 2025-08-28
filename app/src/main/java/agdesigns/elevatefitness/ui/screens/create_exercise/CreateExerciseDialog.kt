package agdesigns.elevatefitness.ui.screens.create_exercise

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.Exercise
import agdesigns.elevatefitness.navigation.ChangePlanGraph
import agdesigns.elevatefitness.navigation.FullscreenDialogTransition
import com.agdesignes.shared.Equipment
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@Destination<ChangePlanGraph>(style = FullscreenDialogTransition::class)
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreateExerciseDialog(
    navigator: DestinationsNavigator,
    viewModel: CreateExerciseViewModel = hiltViewModel()
) {
    // FIXME: bad ime reaction
    val exerciseState by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    // make topappbar opaque
    scrollBehavior.state.contentOffset = scrollBehavior.state.heightOffsetLimit
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.navigationBarsPadding()) },
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.create_a_new_exercise)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.close_icon)
                        )
                    }
                }, actions = {
                    val keyboardController = LocalSoftwareKeyboardController.current
                    val fillString = stringResource(R.string.fill_every_field)
                    TextButton(onClick = {
                        if (!viewModel.onEvent(CreateExerciseEvent.TryCreateExercise))
                            scope.launch {
                                keyboardController?.hide()
                                snackbarHostState.showSnackbar(fillString)
                            }
                        else {
                            navigator.navigateUp()
                        }
                    }, modifier = Modifier.align(CenterVertically)) {
                        Text(text = stringResource(R.string.save))
                    }
                }, scrollBehavior = scrollBehavior
            )
        }, content = { innerPadding ->
            LazyColumn(contentPadding = innerPadding,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()){
                item {
                    OutlinedTextField(
                        value = exerciseState.name,
                        onValueChange = { viewModel.onEvent(CreateExerciseEvent.UpdateName(it)) },
                        label = { Text(stringResource(R.string.enter_exercise_name))},
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.select_equipment))
                    // Note that Modifier.selectableGroup() is essential to ensure correct accessibility behavior
                    Column(Modifier.selectableGroup()) {
                        Equipment.entries.toTypedArray().slice(1 until Equipment.entries.size)
                            .map { it.equipmentNameResource } .forEachIndexed { index, textRes ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .selectable(
                                        selected = (textRes == exerciseState.equipment.equipmentNameResource),
                                        onClick = {
                                            viewModel.onEvent(
                                                CreateExerciseEvent.UpdateEquipment(
                                                    Equipment.entries[index + 1]
                                                )
                                            )
                                        },
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = CenterVertically
                            ) {
                                RadioButton(
                                    selected = (textRes == exerciseState.equipment.equipmentNameResource),
                                    onClick = null // null recommended for accessibility with screenreaders
                                )
                                Text(
                                    text = stringResource(textRes),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.select_difficulty))
                    Column(Modifier.selectableGroup()) {
                        Exercise.ExerciseDifficulty.entries.map { it.difficultyResource } .forEachIndexed { index, textRes ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .selectable(
                                            selected = (textRes == exerciseState.difficulty.difficultyResource),
                                            onClick = {
                                                viewModel.onEvent(
                                                    CreateExerciseEvent.UpdateDifficulty(
                                                        Exercise.ExerciseDifficulty.entries[index]
                                                    )
                                                )
                                            },
                                            role = Role.RadioButton
                                        )
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (textRes == exerciseState.difficulty.difficultyResource),
                                        onClick = null // null recommended for accessibility with screenreaders
                                    )
                                    Text(
                                        text = stringResource(textRes),
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            }
                    }
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.select_primary_muscle))
                    Column(Modifier.selectableGroup()) {
                        Exercise.Muscle.entries.toTypedArray().slice(1 until Exercise.Muscle.entries.size)
                            .map { it.muscleNameResource } .forEachIndexed { index, textRes ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .selectable(
                                            selected = (textRes == exerciseState.primaryMuscle.muscleNameResource),
                                            onClick = {
                                                viewModel.onEvent(
                                                    CreateExerciseEvent.UpdatePrimaryMuscle(
                                                        Exercise.Muscle.entries[index + 1]
                                                    )
                                                )
                                            },
                                            role = Role.RadioButton
                                        )
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (textRes == exerciseState.primaryMuscle.muscleNameResource),
                                        onClick = null // null recommended for accessibility with screenreaders
                                    )
                                    Text(
                                        text = stringResource(textRes),
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            }
                    }
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.select_secondary_muscle_s))
                    Column(Modifier.selectableGroup()) {
                        Exercise.Muscle.entries.toTypedArray().slice(1 until Exercise.Muscle.entries.size)
                            .map { it.muscleNameResource } .forEachIndexed { index, textRes ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .selectable(
                                            selected = exerciseState.secondaryMuscles[index],
                                            onClick = {
                                                viewModel.onEvent(
                                                    CreateExerciseEvent.ToggleSecondaryMuscle(
                                                        index
                                                    )
                                                )
                                            },
                                            role = Role.RadioButton
                                        )
                                        .padding(horizontal = 5.dp), // manually set-up to align checkbox and radio button
                                    verticalAlignment = CenterVertically
                                ) {
                                    Checkbox (
                                        checked = exerciseState.secondaryMuscles[index],
                                        onCheckedChange = {
                                            viewModel.onEvent(
                                                CreateExerciseEvent.UpdateSecondaryMuscle(it, index)
                                            )}
                                    )
                                    Text(
                                        text = stringResource(textRes),
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            }
                    }
                }
            }
        }
    )
}