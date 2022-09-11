package com.anexus.perfectgymcoach.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.data.exercise.Exercise
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import com.anexus.perfectgymcoach.ui.components.TextFieldWithButtons
import com.anexus.perfectgymcoach.viewmodels.AddExerciseEvent
import com.anexus.perfectgymcoach.viewmodels.AddExerciseViewModel
import kotlinx.coroutines.launch
import kotlin.math.exp


@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@Composable
fun AddExerciseDialogue(
    navHostController: NavHostController,
    programId: Long,
    exerciseId: Long = 0L,
    workoutExerciseId: Long = 0L,
    viewModel: AddExerciseViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    if (exerciseId != 0L)
        viewModel.onEvent(AddExerciseEvent.GetExercise(exerciseId))
    if (workoutExerciseId != 0L)
        viewModel.onEvent(AddExerciseEvent.GetWorkoutExercise(workoutExerciseId))

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    // make topappbar opaque
    scrollBehavior.state.contentOffset = scrollBehavior.state.heightOffsetLimit
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.navigationBarsPadding()) },
        topBar = {
            SmallTopAppBar(title = { Text(viewModel.state.value.exercise?.name ?: "") },
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
                        if (!viewModel.onEvent(AddExerciseEvent.TryAddExercise(programId)))
                            scope.launch {
                                keyboardController?.hide()
                                snackbarHostState.showSnackbar(fillString)
                            }
                        else {
                            // todo: display snackbar in previous screen
                            navHostController.popBackStack()
                        }
                    }, modifier = Modifier.align(CenterVertically)) {
                        Text(text = stringResource(R.string.save))
                    }
                })
        }, content = { innerPadding ->
            if (viewModel.state.value.exercise != null) {
                LazyColumn(
                    contentPadding = innerPadding,
                    modifier = Modifier
                        .imePadding()
//                        .imeNestedScroll()
//                        .verticalScroll(rememberScrollState())
//                        .padding(innerPadding)
                        .fillMaxSize(),
//                        verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Image(
                            painterResource(id = viewModel.state.value.exercise!!.image),
                            null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clip(AbsoluteRoundedCornerShape(0.dp, 0.dp, 12.dp, 12.dp))
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = viewModel.state.value.note,
                            onValueChange = { viewModel.onEvent(AddExerciseEvent.UpdateNotes(it)) },
                            label = { Text("Notes (optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                    item {
                        Row(
                            verticalAlignment = CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = CenterVertically,
                                modifier = Modifier.weight(0.5f)
                            ) {
                                TextFieldWithButtons(
                                    prompt = "Sets",
                                    text = { viewModel.state.value.sets },
                                    onNewText = { viewModel.onEvent(AddExerciseEvent.UpdateSets(it)) },
                                    onIncrement = {
                                        viewModel.onEvent(AddExerciseEvent.UpdateSets(
                                            viewModel.state.value.sets.toIntOrNull()?.plus(1).toString()
                                        ))
                                    },
                                    onDecrement = {
                                        viewModel.onEvent(AddExerciseEvent.UpdateSets(
                                            viewModel.state.value.sets.toIntOrNull()?.minus(1).toString()
                                        ))
                                    },
                                )
                            }
                            Row(
                                verticalAlignment = CenterVertically,
                                modifier = Modifier.weight(0.5f)
                            ) {
                                Text("Advanced sets")
                                Spacer(Modifier.width(8.dp))
                                Switch(
                                    checked = viewModel.state.value.advancedSets,
                                    onCheckedChange = { viewModel.onEvent(AddExerciseEvent.ToggleAdvancedSets) }
                                )
                            }
                        }
                    }
                    if (!viewModel.state.value.advancedSets) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 8.dp),
//                            horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(0.5f)
                                ) {
                                    MyDropdownMenu( // FIXME: should check is int
                                        prompt = "Reps" + "*",
                                        options = (1..12).map { "$it" },
                                        text = viewModel.state.value.reps,
                                        onTextChange = { viewModel.onEvent(AddExerciseEvent.UpdateReps(it)) },
                                        keyboardType = KeyboardType.Number
                                    )
                                }
                                Spacer(
                                    Modifier
                                        .height(8.dp)
                                        .weight(0.05f)
                                )
                                Row(
                                    verticalAlignment = CenterVertically,
                                    modifier = Modifier.weight(0.5f)
                                ) {
                                    MyDropdownMenu(
                                        prompt = "Rest" + "*",
                                        options = (15..120 step 15).map { "$it" },
                                        text = viewModel.state.value.rest,
                                        onTextChange = { viewModel.onEvent(AddExerciseEvent.UpdateRest(it)) },
                                        keyboardType = KeyboardType.Number
                                    ) {
                                        Text("sec")
                                    }
                                }
                            }
                        }
                        item {
                            Spacer(Modifier.height(160.dp))
                        }
                    } else {
                        item {
                            Spacer(Modifier.height(16.dp))
                        }
                        itemsIndexed(items = viewModel.state.value.repsArray, { i, _ -> i }) { index, reps ->
                            Row(
                                verticalAlignment = CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
//                                    .padding(bottom = 8.dp)
                            ) {
                                Row(Modifier.weight(1.5f), verticalAlignment = CenterVertically) {
                                    FilledIconToggleButton(checked = false,
                                        onCheckedChange = { }) {
                                        Text((index + 1).toString())
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    TextFieldWithButtons(
                                        prompt = "Reps",
                                        text = { reps },
                                        onNewText = { viewModel.onEvent(AddExerciseEvent.UpdateRepsAtIndex(it, index)) },
                                        onIncrement = {
                                            viewModel.onEvent(AddExerciseEvent.UpdateRepsAtIndex(
                                                reps.toIntOrNull()?.plus(1).toString(),
                                                index
                                            ))
                                        },
                                        onDecrement = {
                                            viewModel.onEvent(AddExerciseEvent.UpdateRepsAtIndex(
                                                reps.toIntOrNull()?.minus(1).toString(),
                                                index
                                            ))
                                        }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Row(Modifier.weight(1f)) {
                                    MyDropdownMenu(
                                        prompt = "Rest" + "*",
                                        options = (15..120 step 15).map { "$it" },
                                        text = viewModel.state.value.restArray[index],
                                        onTextChange = { viewModel.onEvent(AddExerciseEvent.UpdateRestAtIndex(it, index)) },
                                        keyboardType = KeyboardType.Number
                                    ) {
                                        Text("sec")
                                    }
                                }
                            }
                        }
                        item {
                            Spacer(Modifier.height(160.dp))
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MyDropdownMenu(
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