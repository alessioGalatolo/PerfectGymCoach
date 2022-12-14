package com.anexus.perfectgymcoach.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.data.exercise.Exercise
import com.anexus.perfectgymcoach.viewmodels.CreateExerciseEvent
import com.anexus.perfectgymcoach.viewmodels.CreateExerciseViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@Destination
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreateExerciseDialog(
    navigator: DestinationsNavigator,
    viewModel: CreateExerciseViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    // make topappbar opaque
    scrollBehavior.state.contentOffset = scrollBehavior.state.heightOffsetLimit
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.navigationBarsPadding()) },
        topBar = {
            // FIXME: goes back a second time cause the list of exercises isn't updated
            TopAppBar(title = { Text("Create a new Exercise") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close"
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
                            // FIXME: goes back a second time cause the list of exercises isn't updated
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
                    .imePadding()
                    .padding(16.dp)
                    .fillMaxWidth()){
                item {
                    OutlinedTextField(
                        value = viewModel.state.value.name,
                        onValueChange = { viewModel.onEvent(CreateExerciseEvent.UpdateName(it)) },
                        label = { Text("Enter exercise name")},
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("Select equipment")
                    // Note that Modifier.selectableGroup() is essential to ensure correct accessibility behavior
                    Column(Modifier.selectableGroup()) {
                        Exercise.Equipment.values().slice(1 until Exercise.Equipment.values().size)
                            .map { it.equipmentName } .forEachIndexed { index, text ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .selectable(
                                        selected = (text == viewModel.state.value.equipment.equipmentName),
                                        onClick = { viewModel.onEvent(CreateExerciseEvent.UpdateEquipment(
                                            Exercise.Equipment.values()[index+1]
                                        )) },
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = CenterVertically
                            ) {
                                RadioButton(
                                    selected = (text == viewModel.state.value.equipment.equipmentName),
                                    onClick = null // null recommended for accessibility with screenreaders
                                )
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("Select difficulty")
                    Column(Modifier.selectableGroup()) {
                        Exercise.ExerciseDifficulty.values().map { it.difficulty } .forEachIndexed { index, text ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .selectable(
                                            selected = (text == viewModel.state.value.difficulty.difficulty),
                                            onClick = { viewModel.onEvent(CreateExerciseEvent.UpdateDifficulty(
                                                Exercise.ExerciseDifficulty.values()[index]
                                            )) },
                                            role = Role.RadioButton
                                        )
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (text == viewModel.state.value.difficulty.difficulty),
                                        onClick = null // null recommended for accessibility with screenreaders
                                    )
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            }
                    }
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("Select primary muscle")
                    Column(Modifier.selectableGroup()) {
                        Exercise.Muscle.values().slice(1 until Exercise.Muscle.values().size)
                            .map { it.muscleName } .forEachIndexed { index, text ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .selectable(
                                            selected = (text == viewModel.state.value.primaryMuscle.muscleName),
                                            onClick = { viewModel.onEvent(CreateExerciseEvent.UpdatePrimaryMuscle(
                                                Exercise.Muscle.values()[index+1]
                                            )) },
                                            role = Role.RadioButton
                                        )
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (text == viewModel.state.value.primaryMuscle.muscleName),
                                        onClick = null // null recommended for accessibility with screenreaders
                                    )
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            }
                    }
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("Select secondary muscle(s)")
                    Column(Modifier.selectableGroup()) {
                        Exercise.Muscle.values().slice(1 until Exercise.Muscle.values().size)
                            .map { it.muscleName } .forEachIndexed { index, text ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .selectable(
                                            selected = viewModel.state.value.secondaryMuscles[index],
                                            onClick = { viewModel.onEvent(CreateExerciseEvent.ToggleSecondaryMuscle(index)) },
                                            role = Role.RadioButton
                                        )
                                        .padding(horizontal = 5.dp), // manually set-up to align checkbox and radio button
                                    verticalAlignment = CenterVertically
                                ) {
                                    Checkbox (
                                        checked = viewModel.state.value.secondaryMuscles[index],
                                        onCheckedChange = {
                                            viewModel.onEvent(
                                                CreateExerciseEvent.UpdateSecondaryMuscle(it, index)
                                            )}
                                    )
                                    Text(
                                        text = text,
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