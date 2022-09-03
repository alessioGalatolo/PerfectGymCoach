package com.anexus.perfectgymcoach.ui.screens

import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.ripple.rememberRipple
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.ColorUtils
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.data.exercise.Exercise
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import com.anexus.perfectgymcoach.ui.MainScreen
import com.anexus.perfectgymcoach.viewmodels.ExercisesEvent
import com.anexus.perfectgymcoach.viewmodels.ExercisesViewModel
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExercisesByMuscle(navController: NavHostController, programName: String,
                      programId: Long
) {
    // scroll behaviour for top bar
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )
    Scaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(title = { Text("Add exercise to $programName") },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                }
            )
        }, content = { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp)) {
                item {
                    // search bar
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = NavigationBarDefaults.Elevation,  // should use card elevation but it is private
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = rememberRipple(),
                                    onClick = {
                                        navController.navigate(
                                            "${MainScreen.ViewExercises.route}/${programName}/${programId}/" +
                                                    "${Exercise.Muscle.EVERYTHING.ordinal}/${true}"
                                        )
                                    }
                                ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    top = 8.dp,
                                    bottom = 8.dp
                                ),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "Search exercise",
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(16.dp),
//                            style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                items(items = Exercise.Muscle.values(), key = { it.ordinal }) {
                    Card(
                        onClick = {
                            navController.navigate(
                                "${MainScreen.ViewExercises.route}/${programName}/${programId}/" +
                                        "${it.ordinal}/${false}"
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
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


                            Column(modifier = Modifier.align(CenterVertically)) {
                                Text(text = it.muscleName, fontWeight = FontWeight.Bold)
//                                Spacer(modifier = Modifier.height(4.dp))
//                                Text(text = "Some exercise names...") // TODO
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.navigationBarsPadding())
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun ViewExercises(navController: NavHostController, programName: String,
                  programId: Long, muscleOrdinal: Int, focusSearch: Boolean,
                  viewModel: ExercisesViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val toFocus = rememberSaveable { mutableStateOf(focusSearch) }

    viewModel.onEvent(ExercisesEvent.GetExercises(Exercise.Muscle.values()[muscleOrdinal]))
    AddExerciseDialogue(
        viewModel.state.value.exerciseToAdd,
        viewModel.state.value.openAddExerciseDialogue,
        { viewModel.onEvent(ExercisesEvent.ToggleExerciseDialogue()) },
        { eId, reps, rest ->
            viewModel.onEvent(ExercisesEvent.AddWorkoutExercise(
                WorkoutExercise(
                    extProgramId = programId,
                    extExerciseId = eId,
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.navigationBarsPadding()) },
        topBar = {
            SmallTopAppBar(
                title = { Text(Exercise.Muscle.values()[muscleOrdinal].muscleName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }, content = { innerPadding ->
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = innerPadding,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                item{
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = NavigationBarDefaults.Elevation,  // should use card elevation but it is private
                        modifier = Modifier.padding(vertical = 8.dp)
                    ){
                        Row(verticalAlignment = CenterVertically,
                            modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                            tint = MaterialTheme.colorScheme.outline
                            )
                            var searchText by remember { mutableStateOf("") }
                            TextField(
                                value = searchText,
                                onValueChange = {
                                    searchText = it
                                    viewModel.onEvent(ExercisesEvent.FilterExercise(searchText))
                                },
                                placeholder = { Text("Search exercise") },
                                singleLine = true,
                                colors = TextFieldDefaults.textFieldColors(
                                    containerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Search
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        viewModel.onEvent(ExercisesEvent.FilterExercise(searchText))
                                        keyboardController?.hide()
                                    }
                                ), modifier = Modifier
                                    .focusRequester(focusRequester)
                                    .onFocusChanged {
                                        if (it.isFocused) {
                                            keyboardController?.show()
                                        }
                                    }
                            )
                        }
                        if (toFocus.value){
                            LaunchedEffect(focusRequester) {
                                awaitFrame()
                                awaitFrame()
                                awaitFrame()
                                awaitFrame()
                                focusRequester.requestFocus()
                                toFocus.value = false
                            }
                        }
                    }
                }
                items(viewModel.state.value.exercisesToDisplay ?: emptyList(), key = { it.exerciseId }) { exercise ->
                    ElevatedCard(
                        onClick = {
                            viewModel.onEvent(ExercisesEvent.ToggleExerciseDialogue(exercise))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItemPlacement()
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(exercise.image)
                                .crossfade(true)
                                .build(),
                            contentScale = ContentScale.Crop,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(LocalConfiguration.current.screenWidthDp.dp / 4)
                                .align(Alignment.CenterHorizontally)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Column (Modifier.padding(8.dp)){
                            Text(text = exercise.name, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = buildAnnotatedString {
                                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                    append("Primary muscle: ")
                                }
                                append(exercise.primaryMuscle.muscleName)
                            })
                            if (exercise.secondaryMuscles.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Secondary muscles: ${
                                    exercise.secondaryMuscles.joinToString(
                                        ", "
                                    ) { it.muscleName }
                                }") // TODO
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                item {
                    Spacer(Modifier.navigationBarsPadding())
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
    addExerciseAndClose: (Long, List<Int>, Int) -> Unit
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
                decorFitsSystemWindows = true,
                usePlatformDefaultWidth = false // experimental
            )
        ) {
            Scaffold(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding(),
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
                            val keyboardController = LocalSoftwareKeyboardController.current
                            TextButton(onClick = {
                                // TODO: add mark on required fields
                                if (setsText.value.isEmpty() ||
                                    repsText.value.isEmpty() ||
                                    restText.value.isEmpty()){
                                    scope.launch {
                                        keyboardController?.hide()
                                        snackbarHostState.showSnackbar(
                                            "Please fill every field in order to save"
                                        )
                                    }
                                } else {
                                    addExerciseAndClose(
                                        exercise!!.exerciseId,
                                        // TODO: add option to have different reps number
                                        List(setsText.value.toInt()) { repsText.value.toInt() },
                                        restText.value.toInt())
                                    setsText.value = ""
                                    repsText.value = ""
                                    restText.value = ""
                                    notesText.value = "" // FIXME notes are not used
                                }
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
                            painterResource(id = exercise!!.image),
                            null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(AbsoluteRoundedCornerShape(12.dp))
                        )
                        Row (modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly){
                            MyDropdownMenu( // FIXME: should check is int
                                prompt = "Sets*",
                                options = (1..6).map { "$it" },
                                text = setsText,
                                keyboardType = KeyboardType.Number
                            )
                            Spacer(Modifier.width(8.dp))
                            MyDropdownMenu( // FIXME: should check is int
                                prompt = "Reps*",
                                options = (1..12).map { "$it" },
                                text = repsText,
                                keyboardType = KeyboardType.Number
                            )

                        }
                        MyDropdownMenu(prompt = "Rest*",
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