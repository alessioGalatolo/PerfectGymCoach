package agdesigns.elevatefitness.ui.screens

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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.ui.ChangePlanGraph
import agdesigns.elevatefitness.ui.components.TextFieldWithButtons
import agdesigns.elevatefitness.viewmodels.AddExerciseEvent
import agdesigns.elevatefitness.viewmodels.AddExerciseViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.ExercisesByMuscleDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch

@Destination<ChangePlanGraph>
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseDialog(
    navigator: DestinationsNavigator,
    programId: Long = 0L, // programId and workoutId are mutually exclusive
    workoutId: Long = 0L, // workoutId can only happen with exercise Id
    exerciseId: Long = 0L,
    programExerciseId: Long = 0L,
    viewModel: AddExerciseViewModel = hiltViewModel()
) {
    assert((workoutId != 0L && exerciseId != 0L) || (programId != 0L))

    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    if (workoutId != 0L) {
        viewModel.onEvent(AddExerciseEvent.GetWorkoutAndExercise(workoutId, programId, exerciseId))
    } else if (programExerciseId != 0L)  // FIXME: sometimes the reps/rest counter doesn't follow the exercise
        viewModel.onEvent(AddExerciseEvent.GetProgramAndProgramExercise(programId, programExerciseId, exerciseId))
    else if (exerciseId != 0L)
        viewModel.onEvent(AddExerciseEvent.GetProgramAndExercise(programId, exerciseId))

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    // make topappbar opaque
    scrollBehavior.state.contentOffset = scrollBehavior.state.heightOffsetLimit
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.navigationBarsPadding()) },
        topBar = {
            TopAppBar(title = { Text(viewModel.state.value.exercise?.name ?: "") },
                scrollBehavior = scrollBehavior,
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
                        if (!viewModel.onEvent(AddExerciseEvent.TryAddExercise))
                            scope.launch {
                                keyboardController?.hide()
                                snackbarHostState.showSnackbar(fillString)
                            }
                        else {
                            // FIXME:
                            navigator.navigateUp()
                            navigator.navigateUp()
                            navigator.navigateUp()
                            navigator.navigate(
                                ExercisesByMuscleDestination(
                                    programName = viewModel.state.value.programName,
                                    programId = programId,
                                    workoutId = workoutId,
                                    successfulAddExercise = true
                                )
                            )
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
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        AsyncImage(
                            viewModel.state.value.exercise!!.image,
                            "Exercise image",
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
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                    if (viewModel.state.value.exercise!!.variations.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 8.dp),
                                verticalAlignment = CenterVertically
                            ) {
                                val expanded = rememberSaveable { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = expanded.value,
                                    onExpandedChange = {
                                        expanded.value = !expanded.value
                                    }
                                ) {
                                    OutlinedTextField(
                                        readOnly = true,
                                        value = viewModel.state.value.variation,
                                        singleLine = true,
                                        onValueChange = {
                                            viewModel.onEvent(
                                                AddExerciseEvent.UpdateVariation(
                                                    it
                                                )
                                            )
                                        },
                                        label = { Text("Variation") },
                                        trailingIcon = {
                                            Row (verticalAlignment = CenterVertically){
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value)
                                            }
                                        },
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expanded.value,
                                        onDismissRequest = { expanded.value = false },
                                    ) {
                                        // TODO: add "add variation" to create a variation of the exercise
                                        viewModel.state.value.exercise!!.variations.plus("No variation")
                                            .forEach { selectionOption ->
                                                DropdownMenuItem(
                                                    text = { Text(selectionOption) },
                                                    onClick = {
                                                        viewModel.onEvent(
                                                            AddExerciseEvent.UpdateVariation(
                                                                selectionOption
                                                            )
                                                        )
                                                        expanded.value = false
                                                    },
                                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                                )
                                            }
                                    }
                                }
                            }
                        }
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
                                    contentDescription = "Sets"
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
                                verticalAlignment = CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = CenterVertically,
                                    modifier = Modifier.weight(0.5f)
                                ) {
                                    MyDropdownMenu(
                                        prompt = "Reps" + "*",  // FIXME: why is there a star?
                                        options = (1..12).map { "$it" },
                                        text = viewModel.state.value.reps,
                                        onTextChange = { if (it.toIntOrNull() != null) viewModel.onEvent(AddExerciseEvent.UpdateReps(it)) },
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
                                        onTextChange = { if (it.toIntOrNull() != null) viewModel.onEvent(AddExerciseEvent.UpdateRest(it)) },
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
                                        },
                                        contentDescription = "Reps for set ${index+1}"
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Row(Modifier.weight(1f)) {
                                    MyDropdownMenu(
                                        prompt = "Rest" + "*" + " ${index+1}",
                                        options = (15..120 step 15).map { "$it" },
                                        text = viewModel.state.value.restArray[index],
                                        onTextChange = {
                                            viewModel.onEvent(AddExerciseEvent.UpdateRestAtIndex(
                                                it,
                                                index)
                                            )
                                        },
                                        keyboardType = KeyboardType.Number
                                    ) {
                                        Text("sec")
                                    }
                                }
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
    val scope = rememberCoroutineScope()

    // the variable below is used to make the keyboard appear after two taps on the textfield
    // meaning that on tap 1 we only show the dropdown menu and only on second tap we show the keyboard
    // FIXME: not implemented optimally but is the best that can be done atm
    var keyboardIsShowing by rememberSaveable { mutableStateOf(true) }

    ExposedDropdownMenuBox(
        expanded = expanded.value,
        onExpandedChange = {
            if (!expanded.value) {
                expanded.value = true
                scope.launch {
                    awaitFrame()
                    awaitFrame()
                    keyboardController?.hide()
                    keyboardIsShowing = false
                }
            } else {
                if (keyboardIsShowing) {
                    expanded.value = false
                } else {
                    keyboardIsShowing = true
                }
            }

        }
    ) {
        OutlinedTextField(
            value = text,
            singleLine = true,
            onValueChange = onTextChange,
            label = { Text(prompt) },
            trailingIcon = {
                Row (verticalAlignment = CenterVertically){
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
                .menuAnchor()
        )

        if (options.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false },
            ) {
                options.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            onTextChange(selectionOption)
                            expanded.value = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}