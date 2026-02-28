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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.Exercise
import agdesigns.elevatefitness.data.db.entity.getVariation
import agdesigns.elevatefitness.navigation.ChangePlanGraph
import agdesigns.elevatefitness.navigation.FullscreenDialogTransition
import agdesigns.elevatefitness.ui.common.DiscardChangesDialog
import agdesigns.elevatefitness.ui.common.InfoDialog
import agdesigns.elevatefitness.ui.common.ResetExerciseProbabilityDialog
import agdesigns.elevatefitness.ui.common.SharedElementKey
import agdesigns.elevatefitness.ui.common.SharedElementType
import agdesigns.elevatefitness.ui.screens.home.components.ValueSuggestionRow
import agdesigns.elevatefitness.ui.screens.workout.components.TextFieldWithButtons
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Remove
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import coil3.compose.AsyncImage
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.ExerciseStatsDestination
import com.ramcosta.composedestinations.generated.destinations.ExercisesByMuscleDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import kotlin.math.max

@Destination<ChangePlanGraph>
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class
)
@Composable
fun SharedTransitionScope.AddExerciseDialog(
    animatedVisibilityScope: AnimatedVisibilityScope,
    navigator: DestinationsNavigator,
    previewExercise: Exercise,
    programId: Long = 0L, // programId != 0L means we are adding an exercise to a program (and maybe a current workout)
    workoutId: Long = 0L, // workoutId != 0L we're adding to a ongoing workout (and maybe a program)
    programExerciseId: Long = 0L,  // != 0L if we are changing an existing exercise
    programName: String = "",
    returnAfterAdding: Boolean = false,  // if adding a single exercise to workout, return to workout instead of program
    continueAdding: Boolean = true,  // if true, expects user to continue adding exercise,
    insertAtPosition: Int? = null,
    viewModel: AddExerciseViewModel = hiltViewModel()
) {
    assert(workoutId != 0L || programId != 0L)
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.onEvent(
            AddExerciseEvent.StartRetrievingData(
                exerciseId = previewExercise.exerciseId,
                programId = programId,
                workoutId = workoutId,
                insertAtPosition = insertAtPosition,
                programExerciseId = programExerciseId
            )
        )
    }
    val exercise = remember(previewExercise, state.exercise) {
        if (state.exercise == null)
            previewExercise
        else
            state.exercise!!
    }

    var somethingHasChanged by remember { mutableStateOf(false) }
    LaunchedEffect(
        state.programExercise,
        state.note,
        state.variationResKey,
        state.repsArray,
        state.restArray,
        state.advancedSets
    ) {
        if (state.programExercise != null) {
            somethingHasChanged = state.programExercise!!.note != state.note ||
                    state.programExercise!!.variationResKey != state.variationResKey ||
                    state.programExercise!!.reps.map{ it.toUInt() } != state.repsArray ||
                    state.programExercise!!.rest.map{ it.toUInt() } != state.restArray
        }
    }

    // used to animate dialog alpha for predictive back
    var discardChangesDialogProgress by rememberSaveable { mutableFloatStateOf(0f) }
    PredictiveBackHandler(
        enabled = somethingHasChanged && discardChangesDialogProgress < 0.5f
    ) { backFlow ->
        try {
            backFlow.collect { back ->
                discardChangesDialogProgress = back.progress
            }
            discardChangesDialogProgress = 1f
        } catch (_: CancellationException) {
            discardChangesDialogProgress = 0f
        }
    }
    DiscardChangesDialog(
        dialogueOpenProgress = discardChangesDialogProgress,
        dismissDialog = { discardChangesDialogProgress = 0f },
        confirmExit = {
            navigator.navigateUp()
        }
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
        resetExercise = { viewModel.onEvent(AddExerciseEvent.ResetProbability(state.exercise!!.exerciseId)) },
        resetAllExercises = { viewModel.onEvent(AddExerciseEvent.ResetProbability()) }
    )

    val imageWidth = with (LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    val imageHeight = imageWidth/3*2
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    // make topappbar opaque
    scrollBehavior.state.contentOffset = scrollBehavior.state.heightOffsetLimit

    Scaffold (
        modifier = Modifier.sharedBounds(
            rememberSharedContentState(
                SharedElementKey(
                    "AddExerciseDialog",
                    SharedElementType.Bounds,
                    idLong = exercise.exerciseId
                )
            ),
            animatedVisibilityScope,
            boundsTransform = BoundsTransform { _, _ ->
                MotionScheme.expressive().slowSpatialSpec()
            }
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.navigationBarsPadding()) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = { Text(
                    exercise.name,
                    modifier = Modifier.sharedElement(
                        rememberSharedContentState(
                            SharedElementKey(
                                "AddExerciseDialog",
                                SharedElementType.Title,
                                idLong = exercise.exerciseId
                            )
                        ),
                        animatedVisibilityScope,
                        boundsTransform = { _, _ ->
                            MotionScheme.expressive().slowSpatialSpec()
                        }
                    )
                ) },
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
                    FilledTonalButton(
                        shapes = ButtonDefaults.shapes(),
                        onClick = {
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
                    }, enabled = state.exercise != null,
                    modifier = Modifier.align(CenterVertically)) {
                        Text(text = stringResource(R.string.save))
                    }
                })
        }, content = { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    AsyncImage(
                        exercise.image,
                        stringResource(R.string.exercise_image),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(imageHeight)
                            .padding(bottom = 16.dp)
                            .sharedElement(
                                rememberSharedContentState(
                                    SharedElementKey(
                                        "AddExerciseDialog",
                                        SharedElementType.Image,
                                        idLong = exercise.exerciseId
                                    )
                                ),
                                animatedVisibilityScope,
                                boundsTransform = { _, _ ->
                                    MotionScheme.expressive().slowSpatialSpec()
                                }
                            )
                            .clip(AbsoluteRoundedCornerShape(0.dp, 0.dp, 12.dp, 12.dp))
                    )
                }

                item {
                    Row(
                        verticalAlignment = CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .animateItem()
                            .padding(horizontal = 16.dp)
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome,
                            stringResource(R.string.magic_generation)
                        )
                        Spacer(Modifier.width(8.dp))
                        val currentProbability = exercise.probability
                        Text(stringResource(R.string.current_probability_2f, currentProbability))
                        TextButton(onClick = { resetProbabilityDialogOpen = true }) {
                            Text(stringResource(R.string.reset))
                        }
                        IconButton(onClick = { awesomeDialogOpen = true }) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline,
                                stringResource(R.string.more_info)
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        shape = MaterialTheme.shapes.large,
                        value = state.note,
                        onValueChange = { viewModel.onEvent(AddExerciseEvent.UpdateNotes(it)) },
                        label = { Text(stringResource(R.string.notes)) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
                if (exercise.variationsResKeys.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .animateItem()
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
                                            state.variationResKey
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
                                    exercise.variationsResKeys.plus("no_variation")
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
                            .animateItem()
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
                                            max(1, state.repsArray.size-1).toUInt()
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
                                Text(state.repsArray.size.toString(), modifier = Modifier.align(
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
                                        (state.repsArray.size + 1).toUInt()
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
                                checked = state.advancedSets,
                                onCheckedChange = { viewModel.onEvent(AddExerciseEvent.ToggleAdvancedSets) }
                            )
                        }
                    }
                }
                if (!state.advancedSets && !state.isLoading) {
                    item {
                        // reps/rest when advanced sets if off
                        var repsBeingFocussed by remember { mutableStateOf(false) }
                        val repsTextFieldState = rememberTextFieldState(
                            state.repsArray.firstOrNull()?.toString() ?: "0"
                        )
                        var restBeingFocussed by remember { mutableStateOf(false) }
                        val restTextFieldState = rememberTextFieldState(
                            state.restArray.firstOrNull()?.toString() ?: "0"
                        )
                        var repsTextIsValid by remember { mutableStateOf(true) }
                        LaunchedEffect(repsTextFieldState.text) {
                            val reps = repsTextFieldState.text.toString().toUIntOrNull()
                            repsTextIsValid = reps?.let { it > 0U } == true
                            if (repsTextIsValid) {
                                viewModel.onEvent(AddExerciseEvent.UpdateReps(reps!!))
                            }
                        }
                        var restTextIsValid by remember { mutableStateOf(true) }
                        LaunchedEffect(restTextFieldState.text) {
                            val rest = restTextFieldState.text.toString().toUIntOrNull()
                            restTextIsValid = restTextFieldState.text.toString().toUIntOrNull() != null
                            if (restTextIsValid) {
                                viewModel.onEvent(AddExerciseEvent.UpdateRest(rest!!))
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
                                .animateItem()
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
                } else if (state.advancedSets && !state.isLoading) {
                    item {
                        Spacer(Modifier.height(16.dp))
                    }
                    itemsIndexed(items = state.repsArray, { i, _ -> i }) { index, reps ->
                        // reps/rest when advanced sets if off
                        var repsBeingFocussed by remember { mutableStateOf(false) }
                        val repsTextFieldState = rememberTextFieldState(reps.toString())
                        var restBeingFocussed by remember { mutableStateOf(false) }
                        val restTextFieldState = rememberTextFieldState(state.restArray[index].toString())
                        var repsTextIsValid by remember { mutableStateOf(true) }
                        LaunchedEffect(repsTextFieldState.text) {
                            val reps = repsTextFieldState.text.toString().toUIntOrNull()
                            repsTextIsValid = reps?.let { it > 0U } == true
                            if (repsTextIsValid) {
                                viewModel.onEvent(AddExerciseEvent.UpdateRepsAtIndex(reps!!, index))
                            }
                        }
                        var restTextIsValid by remember { mutableStateOf(true) }
                        LaunchedEffect(restTextFieldState.text) {
                            val rest = restTextFieldState.text.toString().toUIntOrNull()
                            restTextIsValid = restTextFieldState.text.toString().toUIntOrNull() != null
                            if (restTextIsValid) {
                                viewModel.onEvent(AddExerciseEvent.UpdateRestAtIndex(rest!!, index))
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
                        navigator.navigate(ExerciseStatsDestination(exercise.exerciseId))
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
    )
}