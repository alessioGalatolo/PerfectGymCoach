package com.anexus.perfectgymcoach.screens

import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import com.anexus.perfectgymcoach.viewmodels.ExercisesEvent
import com.anexus.perfectgymcoach.viewmodels.ExercisesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesByMuscle(navController: NavHostController, programName: String,
                      programId: Long/*, viewModel: ExercisesViewModel = hiltViewModel()*/
) {
    // scroll behaviour for top bar
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        decayAnimationSpec,
        rememberTopAppBarState()
    )
    Scaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val backgroundColors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            val backgroundColor = backgroundColors.containerColor(
                colorTransitionFraction = scrollBehavior.state.collapsedFraction
            ).value
            val foregroundColors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            )
            Box (modifier = Modifier.background(backgroundColor)) {
                LargeTopAppBar(title = { Text("Add exercise to $programName") },
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Go back"
                            )
                        }
                    }, modifier = Modifier.statusBarsPadding(),
                    colors = foregroundColors
                )
            }
        }, content = { innerPadding ->
            Column(modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())){
                Exercise.Muscle.values().forEach {
                    Card(
                        onClick = {
                            navController.navigate(
                                "${MainScreen.ViewExercises.route}/${programName}/${programId}/${it.ordinal}"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row {
                            Image(
                                painter = painterResource(R.drawable.full_body),
                                contentDescription = "Contact profile picture",
                                modifier = Modifier
                                    // Set image size to 40 dp
                                    .size(80.dp)
                                    .padding(all = 4.dp)
                                    // Clip image to be shaped as a circle
                                    .clip(CircleShape)
                            )

                            // Add a horizontal space between the image and the column
//                Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.align(CenterVertically)) {
                                Text(text = it.muscleName, fontWeight = FontWeight.Bold)
//                                Spacer(modifier = Modifier.height(4.dp))
//                                Text(text = "Some exercise names...") // TODO
                            }
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewExercises(navController: NavHostController, programName: String,
                  programId: Long, muscleOrdinal: Int, viewModel: ExercisesViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    viewModel.onEvent(ExercisesEvent.GetExercises(Exercise.Muscle.values()[muscleOrdinal]))
    AddExerciseDialogue(
        viewModel.state.value.exerciseToAdd,
        viewModel.state.value.openAddExerciseDialogue,
        { viewModel.onEvent(ExercisesEvent.ToggleExerciseDialogue()) },
        { eId, name, sets, reps, rest ->
            viewModel.onEvent(ExercisesEvent.AddWorkoutExercise(
                WorkoutExercise(
                extProgramId = programId,
                extExerciseId = eId,
                name = name,
                sets = sets,
                reps = reps,
                rest = rest
            )))
            viewModel.onEvent(ExercisesEvent.ToggleExerciseDialogue())
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Exercise was added to the program, you can continue adding"
                )
            }
        }

    )
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SmallTopAppBar(title = { Text(Exercise.Muscle.values()[muscleOrdinal].muscleName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                }, modifier = Modifier.statusBarsPadding())
        }, content = { innerPadding ->
            // if you have some plans
            LazyColumn(
                contentPadding = innerPadding
            ) {
                items(items = viewModel.state.value.exercises, key = { it }) { exercise ->
                    Card(
                        onClick = {
                            viewModel.onEvent(ExercisesEvent.ToggleExerciseDialogue(exercise))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row {
                            Image(
                                painter = painterResource(R.drawable.full_body),
                                contentDescription = "Contact profile picture",
                                modifier = Modifier
                                    // Set image size to 40 dp
                                    .size(40.dp)
                                    .padding(all = 4.dp)
                                    .align(Alignment.Companion.CenterVertically)
                                    // Clip image to be shaped as a circle
                                    .clip(CircleShape)
                            )

                            // Add a horizontal space between the image and the column

                            Column {
                                Text(text = exercise.name)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Some exercise parameters...") // TODO
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        })
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseDialogue(
    exercise: Exercise?,
    dialogueIsOpen: Boolean,
    toggleDialogue: () -> Unit,
    addExerciseAndClose: (Long, String, Int, Int, Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val notesText = remember { mutableStateOf("") }
    val setsText = remember { mutableStateOf("") }
    val repsText = remember { mutableStateOf("") }
    val restText = remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    if (dialogueIsOpen) {
        Dialog(
            onDismissRequest = {
                toggleDialogue()
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false // experimental
            )
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    SmallTopAppBar(title = { Text(exercise!!.name) },
                        navigationIcon = {
                            IconButton(onClick = { toggleDialogue() }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close"
                                )
                            }
                        }, actions = {
                            TextButton(onClick = {
                                if (setsText.value.isEmpty() ||
                                    repsText.value.isEmpty() ||
                                    restText.value.isEmpty()){
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Please fill every field in order to save"
                                        )
                                    }
                                } else {
                                    addExerciseAndClose(
                                        exercise!!.exerciseId,
                                        exercise.name,
                                        setsText.value.toInt(),
                                        repsText.value.toInt(),
                                        restText.value.toInt())
                                }
                                setsText.value = ""
                                repsText.value = ""
                                restText.value = ""
                                notesText.value = "" // FIXME notes are not used
                            }, modifier = Modifier.align(CenterVertically)) {
                                Text(text = "Save")
                            }
                        })
                }, content = { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
//                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painterResource(id = R.drawable.sample_image),
                            null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(AbsoluteRoundedCornerShape(12.dp))
                        ) // TODO: image of exercise
                        Row (modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly){
                            MyDropdownMenu( // FIXME: should check is int
                                prompt = "Sets",
                                options = (1..6).map { "$it" },
                                text = setsText,
                                keyboardType = KeyboardType.Number
                            )
                            Spacer(Modifier.width(8.dp))
                            MyDropdownMenu( // FIXME: should check is int
                                prompt = "Reps",
                                options = (1..12).map { "$it" },
                                text = repsText,
                                keyboardType = KeyboardType.Number
                            )

                        }
                        MyDropdownMenu(prompt = "Rest",
                            options = (15..120 step 15).map { "$it" },
                            text = restText,
                            keyboardType = KeyboardType.Number){
                            Text("sec")
                        }
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = notesText.value,
                            onValueChange = { notesText.value = it },
                            label = { Text("Notes (optional)") }
                        )
                    }
                })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MyDropdownMenu(
    prompt: String,
    options: List<String>,
    text: MutableState<String> = rememberSaveable { mutableStateOf("") },
    expanded: MutableState<Boolean> = rememberSaveable { mutableStateOf(false) },
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingIcon: (@Composable () -> Unit)? = null
){
    val keyboardController = LocalSoftwareKeyboardController.current
    ExposedDropdownMenuBox(
        expanded = expanded.value,
        onExpandedChange = { expanded.value = !expanded.value },
    ) {
        OutlinedTextField(
            value = text.value,
            singleLine = true,
            onValueChange = { text.value = it },
            label = { Text(prompt) },
            trailingIcon = {
                Row (verticalAlignment = CenterVertically){
                    trailingIcon?.invoke()
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value)
                }},
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            keyboardActions = KeyboardActions(onDone = {
                keyboardController?.hide()
            }),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.widthIn(1.dp, Dp.Infinity)
        )
        // filter options based on text field value
        val filteringOptions = options.filter { it.contains(text.value, ignoreCase = true) }
        if (filteringOptions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false },
            ) {
                filteringOptions.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            text.value = selectionOption
                            expanded.value = false
                        }
                    )
                }
            }
        }
    }
}