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
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.ui.ChangePlanGraph
import agdesigns.elevatefitness.ui.FullscreenDialogTransition
import agdesigns.elevatefitness.ui.components.InfoDialog
import agdesigns.elevatefitness.ui.components.ResetExerciseProbabilityDialog
import agdesigns.elevatefitness.ui.components.TextFieldWithButtons
import agdesigns.elevatefitness.viewmodels.AddExerciseEvent
import agdesigns.elevatefitness.viewmodels.AddExerciseViewModel
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Remove
import androidx.compose.ui.Alignment
import coil3.compose.AsyncImage
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.ExercisesByMuscleDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.DestinationStyle
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import kotlin.math.max

@Destination<ChangePlanGraph>(style = FullscreenDialogTransition::class)
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseDialog(
    navigator: DestinationsNavigator,
    programId: Long = 0L, // programId != 0L means we are adding an exercise to a program (and maybe a current workout)
    workoutId: Long = 0L, // workoutId != 0L we're adding to a ongoing workout (and maybe a program)
    exerciseId: Long = 0L,  // should never be 0L
    programExerciseId: Long = 0L,  // != 0L if we are changing an existing exercise
    programName: String = "",
    returnAfterAdding: Boolean = false,  // if adding a single exercise to workout, return to workout instead of program
    continueAdding: Boolean = true,  // if true, expects user to continue adding exercise,
    viewModel: AddExerciseViewModel = hiltViewModel()
) {
    // FIXME: I don't like how ui reacts to ime
    assert((workoutId != 0L && exerciseId != 0L) || (programId != 0L))
    val addExerciseState by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    viewModel.onEvent(
        AddExerciseEvent.StartRetrievingData(
            exerciseId,
            programId,
            workoutId,
            programExerciseId
        )
    )

    var awesomeDialogOpen by rememberSaveable { mutableStateOf(false) }
    InfoDialog(
        dialogueIsOpen = awesomeDialogOpen,
        toggleDialogue = { awesomeDialogOpen = !awesomeDialogOpen }) {
        Text("This number is used to generate new plans. A number higher than 1 means a higher probability of being included in new workouts. A number lower than 1 means a lower probability. 1 is default.")
    }
    var resetProbabilityDialogOpen by rememberSaveable { mutableStateOf(false) }
    ResetExerciseProbabilityDialog(
        dialogIsOpen = resetProbabilityDialogOpen,
        toggleDialog = { resetProbabilityDialogOpen = !resetProbabilityDialogOpen },
        resetExercise = { viewModel.onEvent(AddExerciseEvent.ResetProbability(addExerciseState.exercise!!.exerciseId)) },
        resetAllExercises = { viewModel.onEvent(AddExerciseEvent.ResetProbability()) }
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    // make topappbar opaque
    scrollBehavior.state.contentOffset = scrollBehavior.state.heightOffsetLimit
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.navigationBarsPadding()) },
        topBar = {
            TopAppBar(title = { Text(addExerciseState.exercise?.name ?: "") },
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
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(fillString)
                            }
                        else {
                            // FIXME:
                            if (continueAdding) {
                                navigator.navigateUp()
                                navigator.navigateUp()
                                navigator.navigateUp()
                                navigator.navigate(
                                    ExercisesByMuscleDestination(
                                        programName = programName,
                                        programId = programId,
                                        workoutId = workoutId,
                                        successfulAddExercise = true,
                                        returnAfterAdding = returnAfterAdding
                                    )
                                )
                            } else {
                                // simply go back
                                navigator.navigateUp()
                            }
                        }
                    }, enabled = addExerciseState.exercise != null,
                    modifier = Modifier.align(CenterVertically)) {
                        Text(text = stringResource(R.string.save))
                    }
                })
        }, content = { innerPadding ->
            if (addExerciseState.exercise != null) {
                LazyColumn(
                    contentPadding = innerPadding,
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        AsyncImage(
                            addExerciseState.exercise!!.image,
                            "Exercise image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clip(AbsoluteRoundedCornerShape(0.dp, 0.dp, 12.dp, 12.dp))
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
                            Icon(Icons.Default.AutoAwesome, "Magic generation")
                            Spacer(Modifier.width(8.dp))
                            val currentProbability = addExerciseState.exercise!!.probability
                            Text("Current probability: %.2f".format(currentProbability))
                            TextButton(onClick = { resetProbabilityDialogOpen = true }) {
                                Text("Reset")
                            }
                            IconButton(onClick = { awesomeDialogOpen = true }) {
                                Icon(Icons.AutoMirrored.Filled.HelpOutline, "More info")
                            }
                            // TODO: check overflow on long probabilities
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = addExerciseState.note,
                            onValueChange = { viewModel.onEvent(AddExerciseEvent.UpdateNotes(it)) },
                            label = { Text("Notes (optional)") },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                    if (addExerciseState.exercise!!.variations.isNotEmpty()) {
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
                                        value = addExerciseState.variation,
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
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expanded.value,
                                        onDismissRequest = { expanded.value = false },
                                    ) {
                                        // TODO: add "add variation" to create a variation of the exercise
                                        addExerciseState.exercise!!.variations.plus("No variation")
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
                                IconButton({
                                    viewModel.onEvent(
                                        AddExerciseEvent.UpdateSets(
                                            max(1, addExerciseState.repsArray.size-1).toUInt()
                                        )
                                    )
                                }
                                ) {
                                    Icon(Icons.Default.Remove, "Decrease sets")
                                }
                                Column {
                                    Text("Sets:", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(
                                        Alignment.CenterHorizontally
                                    ))
                                    Text(addExerciseState.repsArray.size.toString(), modifier = Modifier.align(
                                        Alignment.CenterHorizontally
                                    ))
                                }
                                IconButton({
                                    viewModel.onEvent(
                                        AddExerciseEvent.UpdateSets(
                                            (addExerciseState.repsArray.size + 1).toUInt()
                                        )
                                    )
                                }

                                ) {
                                    Icon(Icons.Default.Add, "Increase sets")
                                }
                            }
                            Row(
                                verticalAlignment = CenterVertically,
                                modifier = Modifier.weight(0.5f)
                            ) {
                                Text("Advanced sets")
                                Spacer(Modifier.width(8.dp))
                                Switch(
                                    checked = addExerciseState.advancedSets,
                                    onCheckedChange = { viewModel.onEvent(AddExerciseEvent.ToggleAdvancedSets) }
                                )
                            }
                        }
                    }
                    if (!addExerciseState.advancedSets) {
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
                                    var repsText by remember { mutableStateOf(addExerciseState.repsArray.first().toString()) }
                                    MyDropdownMenu(
                                        prompt = "Reps",
                                        options = (1..12).map { "$it" },
                                        text = repsText,
                                        onTextChange = {
                                            repsText = it
                                            if (it.toUIntOrNull() != null && it.toUInt() > 0U) {
                                                viewModel.onEvent(AddExerciseEvent.UpdateReps(it.toUInt()))
                                            }
                                        }, keyboardType = KeyboardType.Number,
                                        textIsValid = { it.toUIntOrNull()?.let { it > 0U } == true }

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
                                    var restText by remember { mutableStateOf(addExerciseState.restArray.first().toString()) }
                                    MyDropdownMenu(
                                        prompt = "Rest",
                                        options = (15..120 step 15).map { "$it" },
                                        text = restText,
                                        onTextChange = {
                                            restText = it
                                            // try to update but don't mind an error
                                            if (it.toUIntOrNull() != null) {
                                                viewModel.onEvent(AddExerciseEvent.UpdateRest(it.toUInt()))
                                            }
                                        }, keyboardType = KeyboardType.Number
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
                        itemsIndexed(items = addExerciseState.repsArray, { i, _ -> i }) { index, reps ->
                            var repsText by remember { mutableStateOf(reps.toString()) }
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
                                        text = { repsText },
                                        onNewText = {
                                            repsText = it
                                            if (it.toUIntOrNull() != null && it.toUInt() > 0U) {
                                                viewModel.onEvent(
                                                    AddExerciseEvent.UpdateRepsAtIndex(
                                                        it.toUInt(),
                                                        index
                                                    )
                                                )
                                            }
                                        },
                                        onIncrement = {
                                            val reps = (repsText.toUIntOrNull() ?: 0U) + 1U
                                            repsText = reps.toString()
                                            viewModel.onEvent(
                                                AddExerciseEvent.UpdateRepsAtIndex(
                                                    repsText.toUInt(),
                                                    index
                                                )
                                            )
                                        },
                                        onDecrement = {
                                            var reps = repsText.toUIntOrNull() ?: 0U
                                            if (reps < 2U)
                                                reps = 1U
                                            else
                                                reps -= 1U

                                            repsText = reps.toString()
                                            viewModel.onEvent(
                                                AddExerciseEvent.UpdateRepsAtIndex(
                                                    repsText.toUInt(),
                                                    index
                                                )
                                            )
                                        },
                                        textIsValid = { it.toUIntOrNull()?.let { it > 0U } == true },
                                        contentDescription = "Reps for set ${index+1}"
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Row(Modifier.weight(1f)) {
                                    var restText by remember { mutableStateOf(addExerciseState.restArray[index].toString()) }
                                    MyDropdownMenu(
                                        prompt = "Rest" + " ${index+1}",
                                        options = (15..120 step 15).map { "$it" },
                                        text = restText,
                                        onTextChange = {
                                            restText = it
                                            if (it.toUIntOrNull() != null) {
                                                viewModel.onEvent(
                                                    AddExerciseEvent.UpdateRestAtIndex(
                                                        it.toUInt(),
                                                        index
                                                    )
                                                )
                                            }
                                        },
                                        keyboardType = KeyboardType.Number,
                                        textIsValid = { it.toUIntOrNull() != null }
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
    textIsValid: (String) -> Boolean = { true },
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
            isError = !textIsValid(text),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier
                .widthIn(1.dp, Dp.Infinity)
                .menuAnchor(MenuAnchorType.PrimaryEditable, true)
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