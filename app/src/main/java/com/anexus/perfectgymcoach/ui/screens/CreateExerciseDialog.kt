package com.anexus.perfectgymcoach.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.data.exercise.Exercise
import com.anexus.perfectgymcoach.viewmodels.CreateExerciseEvent
import com.anexus.perfectgymcoach.viewmodels.CreateExerciseViewModel
import kotlinx.coroutines.launch


@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreateExerciseDialogue(
    navHostController: NavHostController,
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
            SmallTopAppBar(title = { Text("Create a new Exercise") },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
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
                            navHostController.popBackStack()
                            // FIXME: goes back a second time cause the list of exercises isn't updated
                            navHostController.popBackStack()
                        }
                    }, modifier = Modifier.align(CenterVertically)) {
                        Text(text = stringResource(R.string.save))
                    }
                })
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
                                verticalAlignment = Alignment.CenterVertically
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MyDropdownMenus(
    prompt: String,
    options: List<String>,
    text: String,
    onTextChange: (String) -> Unit,
    expanded: MutableState<Boolean> = rememberSaveable { mutableStateOf(false) },
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingIcon: (@Composable () -> Unit)? = null
){
    val keyboardController = LocalSoftwareKeyboardController.current

    // the 3 variable below are used to make the keyboard appear after two taps on the textfield
    // meaning that on tap 1 we only show the dropdown menu and only on second tap we show the keyboard
    var hasBeenFocused by rememberSaveable { mutableStateOf(true) }
    var hasBeenFalse by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester.Default }
    ExposedDropdownMenuBox(
        expanded = expanded.value,
        onExpandedChange = {
            expanded.value = !expanded.value
            hasBeenFocused = hasBeenFocused || expanded.value
            if (!expanded.value) {
                focusRequester.requestFocus()
                keyboardController?.show()
            }

        }
    ) {
        OutlinedTextField(
//            enabled = !expanded.value,
            readOnly = !hasBeenFocused,
            value = text,
            singleLine = true,
            onValueChange = onTextChange,
            label = { Text(prompt) },
            trailingIcon = {
                Row (verticalAlignment = Alignment.CenterVertically){
                    trailingIcon?.invoke()
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value)
                }
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            keyboardActions = KeyboardActions(onDone = {
                keyboardController?.hide()
            }),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier
                .widthIn(1.dp, Dp.Infinity)
                .onGloballyPositioned {
                    hasBeenFocused = false || (hasBeenFalse && hasBeenFocused)
                    hasBeenFalse = true
                }
                .focusRequester(focusRequester)
        )
        // filter options based on text field value
//        val filteringOptions = options.filter { it.contains(text, ignoreCase = true) }
        if (options.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false },
            ) {
                options.forEach { selectionOption ->  // FIXME: not using filteringOptions
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            onTextChange(selectionOption)
                            expanded.value = false
                        }
                    )
                }
            }
        }
    }
}