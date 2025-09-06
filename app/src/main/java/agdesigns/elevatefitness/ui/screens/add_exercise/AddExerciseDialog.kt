package agdesigns.elevatefitness.ui.screens.add_exercise

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
import agdesigns.elevatefitness.data.db.entity.getVariation
import agdesigns.elevatefitness.navigation.ChangePlanGraph
import agdesigns.elevatefitness.navigation.FullscreenDialogTransition
import agdesigns.elevatefitness.ui.common.InfoDialog
import agdesigns.elevatefitness.ui.common.ResetExerciseProbabilityDialog
import agdesigns.elevatefitness.ui.screens.home.components.ValueSuggestionRow
import agdesigns.elevatefitness.ui.screens.workout.components.TextFieldWithButtons
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Remove
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.input.ImeAction
import coil3.compose.AsyncImage
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.ExerciseStatsDestination
import com.ramcosta.composedestinations.generated.destinations.ExercisesByMuscleDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import kotlin.math.max

@Destination<ChangePlanGraph>(style = FullscreenDialogTransition::class)
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
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
    // TODO: if user changed a value and goes back without saving, show an alert
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
        Text(stringResource(R.string.probability_info))
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
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.navigationBarsPadding()) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = { Text(addExerciseState.exercise?.name ?: "") },
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
                }, actions = {
                    val keyboardController = LocalSoftwareKeyboardController.current
                    val fillString = stringResource(R.string.fill_every_field)
                    FilledTonalButton(onClick = {
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
                        .fillMaxSize()
                        .imePadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        AsyncImage(
                            addExerciseState.exercise!!.image,
                            stringResource(R.string.exercise_image),
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
                            Icon(Icons.Default.AutoAwesome,
                                stringResource(R.string.magic_generation)
                            )
                            Spacer(Modifier.width(8.dp))
                            val currentProbability = addExerciseState.exercise!!.probability
                            // FIXME: does this work with string resource??
                            Text(stringResource(R.string.current_probability_2f, currentProbability))
                            TextButton(onClick = { resetProbabilityDialogOpen = true }) {
                                Text(stringResource(R.string.reset))
                            }
                            IconButton(onClick = { awesomeDialogOpen = true }) {
                                Icon(Icons.AutoMirrored.Filled.HelpOutline,
                                    stringResource(R.string.more_info)
                                )
                            }
                            // TODO: check overflow on long probabilities
                        }
                    }
                    item {
                        OutlinedTextField(
                            shape = MaterialTheme.shapes.large,
                            value = addExerciseState.note,
                            onValueChange = { viewModel.onEvent(AddExerciseEvent.UpdateNotes(it)) },
                            label = { Text(stringResource(R.string.notes)) },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                    if (addExerciseState.exercise!!.variationsResKeys.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(dimensionResource(R.dimen.screen_edge_padding)),
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
                                        value = stringResource(
                                            getVariation(
                                                addExerciseState.variationResKey
                                            )
                                        ).replaceFirstChar { it.uppercaseChar() },
                                        singleLine = true,
                                        onValueChange = {
                                            viewModel.onEvent(
                                                AddExerciseEvent.UpdateVariationResKey(
                                                    it
                                                )
                                            )
                                        },
                                        label = { Text(stringResource(R.string.variation)) },
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
                                        addExerciseState.exercise!!.variationsResKeys.plus("no_variation")
                                            .forEach { selectionOption ->
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(getVariation(selectionOption))) },
                                                    onClick = {
                                                        viewModel.onEvent(
                                                            AddExerciseEvent.UpdateVariationResKey(
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
                                FilledTonalIconButton(
                                    shapes = IconButtonDefaults.shapes(
                                        shape = MaterialTheme.shapes.medium,
                                        pressedShape = MaterialTheme.shapes.extraExtraLarge
                                    ),
                                    onClick = {
                                    viewModel.onEvent(
                                        AddExerciseEvent.UpdateSets(
                                            max(1, addExerciseState.repsArray.size-1).toUInt()
                                        )
                                    )
                                }
                                ) {
                                    Icon(Icons.Default.Remove,
                                        stringResource(R.string.decrease_sets)
                                    )
                                }
                                Column {
                                    Text(stringResource(R.string.sets), style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(
                                        Alignment.CenterHorizontally
                                    ))
                                    Text(addExerciseState.repsArray.size.toString(), modifier = Modifier.align(
                                        Alignment.CenterHorizontally
                                    ))
                                }
                                FilledTonalIconButton(
                                    shapes = IconButtonDefaults.shapes(
                                        shape = MaterialTheme.shapes.medium,
                                        pressedShape = MaterialTheme.shapes.extraExtraLarge
                                    ),
                                    onClick = {
                                    viewModel.onEvent(
                                        AddExerciseEvent.UpdateSets(
                                            (addExerciseState.repsArray.size + 1).toUInt()
                                        )
                                    )
                                }) {
                                    Icon(Icons.Default.Add, stringResource(R.string.increase_sets))
                                }
                            }
                            Row(
                                verticalAlignment = CenterVertically,
                                modifier = Modifier.weight(0.5f)
                            ) {
                                Text(stringResource(R.string.advanced_sets))
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
                            // reps/rest when advanced sets if off
                            var repsBeingFocussed by remember { mutableStateOf(false) }
                            val repsTextFieldState = rememberTextFieldState(addExerciseState.repsArray.first().toString())
                            var restBeingFocussed by remember { mutableStateOf(false) }
                            val restTextFieldState = rememberTextFieldState(addExerciseState.restArray.first().toString())
                            var repsTextIsValid by remember { mutableStateOf(true) }
                            LaunchedEffect(repsTextFieldState.text) {
                                val reps = repsTextFieldState.text.toString().toUIntOrNull()
                                repsTextIsValid = reps?.let { it > 0U } == true
                                if (reps?.toUInt()?.let{it  > 0U} == true) {
                                    viewModel.onEvent(AddExerciseEvent.UpdateReps(reps))
                                }
                            }
                            var restTextIsValid by remember { mutableStateOf(true) }
                            LaunchedEffect(restTextFieldState.text) {
                                val rest = restTextFieldState.text.toString().toUIntOrNull()
                                restTextIsValid = restTextFieldState.text.toString().toUIntOrNull() != null
                                if (rest != null) {
                                    viewModel.onEvent(AddExerciseEvent.UpdateRest(rest))
                                }
                            }
                            ValueSuggestionRow(
                                shouldBeShown = repsBeingFocussed,
                                options = (1..50).toList(),
                                onClick = {
                                    repsTextFieldState.setTextAndPlaceCursorAtEnd(it.toString())
                                },
                                valueIsSelected = {
                                    it.toUInt() == repsTextFieldState.text.toString().toUIntOrNull()
                                }
                            )
                            ValueSuggestionRow(
                                shouldBeShown = restBeingFocussed,
                                options = (15..120 step 15).toList(),
                                onClick = {
                                    restTextFieldState.setTextAndPlaceCursorAtEnd(it.toString())
                                },
                                valueIsSelected = {
                                    it.toUInt() == restTextFieldState.text.toString().toUIntOrNull()
                                }
                            )
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
                                    TextField(
                                        state = repsTextFieldState,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Done
                                        ),
                                        label = { Text(stringResource(R.string.reps)) },
                                        isError = !repsTextIsValid,
                                        supportingText = if (!repsTextIsValid) {
                                            { Text(stringResource(R.string.please_enter_a_valid_number)) }
                                        } else { null },
                                        modifier = Modifier.onFocusChanged { newFocus ->
                                            repsBeingFocussed = newFocus.isFocused
                                        }
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
                                    TextField(
                                        state = restTextFieldState,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Done
                                        ),
                                        label = { Text(stringResource(R.string.rest)) },
                                        isError = !restTextIsValid,
                                        supportingText = if (!restTextIsValid) {
                                            { Text(stringResource(R.string.please_enter_a_valid_number)) }
                                        } else { null },
                                        suffix = {
                                            Text(stringResource(R.string.sec))
                                        },
                                        modifier = Modifier.onFocusChanged { newFocus ->
                                            restBeingFocussed = newFocus.isFocused
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            Spacer(Modifier.height(16.dp))
                        }
                        itemsIndexed(items = addExerciseState.repsArray, { i, _ -> i }) { index, reps ->
                            // reps/rest when advanced sets if off
                            var repsBeingFocussed by remember { mutableStateOf(false) }
                            val repsTextFieldState = rememberTextFieldState(reps.toString())
                            var restBeingFocussed by remember { mutableStateOf(false) }
                            val restTextFieldState = rememberTextFieldState(addExerciseState.restArray[index].toString())
                            var repsTextIsValid by remember { mutableStateOf(true) }
                            LaunchedEffect(repsTextFieldState.text) {
                                val reps = repsTextFieldState.text.toString().toUIntOrNull()
                                repsTextIsValid = reps?.let { it > 0U } == true
                                if (reps?.toUInt()?.let{it  > 0U} == true) {
                                    viewModel.onEvent(AddExerciseEvent.UpdateRepsAtIndex(reps, index))
                                }
                            }
                            var restTextIsValid by remember { mutableStateOf(true) }
                            LaunchedEffect(restTextFieldState.text) {
                                val rest = restTextFieldState.text.toString().toUIntOrNull()
                                restTextIsValid = restTextFieldState.text.toString().toUIntOrNull() != null
                                if (rest != null) {
                                    viewModel.onEvent(AddExerciseEvent.UpdateRestAtIndex(rest, index))
                                }
                            }
                            ValueSuggestionRow(
                                shouldBeShown = repsBeingFocussed,
                                options = (1..50).toList(),
                                onClick = {
                                    repsTextFieldState.setTextAndPlaceCursorAtEnd(it.toString())
                                },
                                valueIsSelected = {
                                    it.toUInt() == repsTextFieldState.text.toString().toUIntOrNull()
                                }
                            )
                            ValueSuggestionRow(
                                shouldBeShown = restBeingFocussed,
                                options = (15..120 step 15).toList(),
                                onClick = {
                                    restTextFieldState.setTextAndPlaceCursorAtEnd(it.toString())
                                },
                                valueIsSelected = {
                                    it.toUInt() == restTextFieldState.text.toString().toUIntOrNull()
                                }
                            )
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
                                    // FIXME: should register when textfield gets focus
                                    TextFieldWithButtons(
                                        prompt = stringResource(R.string.reps),
                                        text = { repsTextFieldState.text.toString() },
                                        onNewText = {
                                            repsTextFieldState.setTextAndPlaceCursorAtEnd(it)
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
                                            val reps = (repsTextFieldState.text.toString().toUIntOrNull() ?: 0U) + 1U
                                            repsTextFieldState.setTextAndPlaceCursorAtEnd(reps.toString())
                                            viewModel.onEvent(
                                                AddExerciseEvent.UpdateRepsAtIndex(
                                                    reps,
                                                    index
                                                )
                                            )
                                        },
                                        onDecrement = {
                                            var reps = repsTextFieldState.text.toString().toUIntOrNull() ?: 0U
                                            if (reps < 2U)
                                                reps = 1U
                                            else
                                                reps -= 1U

                                            repsTextFieldState.setTextAndPlaceCursorAtEnd(reps.toString())
                                            viewModel.onEvent(
                                                AddExerciseEvent.UpdateRepsAtIndex(
                                                    reps,
                                                    index
                                                )
                                            )
                                        },
                                        textIsValid = { repsTextIsValid },
                                        contentDescription = stringResource(
                                            R.string.reps_for_set,
                                            index+1
                                        )
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Row(Modifier.weight(1f)) {
                                    TextField(
                                        state = restTextFieldState,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Done
                                        ),
                                        label = { Text(stringResource(R.string.rest)) },
                                        isError = !restTextIsValid,
                                        supportingText = if (!restTextIsValid) {
                                            { Text(stringResource(R.string.please_enter_a_valid_number)) }
                                        } else { null },
                                        suffix = {
                                            Text(stringResource(R.string.sec))
                                        },
                                        modifier = Modifier.onFocusChanged { newFocus ->
                                            restBeingFocussed = newFocus.isFocused
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        FilledTonalButton({
                            navigator.navigate(ExerciseStatsDestination(exerciseId))
                        }, modifier = Modifier.padding(16.dp)) {
                            Row {
                                Icon(
                                    Icons.AutoMirrored.Filled.ShowChart,
                                    stringResource(R.string.chart_icon),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.view_exercise_history_and_stats))
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