package com.anexus.perfectgymcoach.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.anexus.perfectgymcoach.viewmodels.AddExerciseEvent
import com.anexus.perfectgymcoach.viewmodels.AddExerciseViewModel
import kotlinx.coroutines.launch


@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@Composable
fun AddExerciseDialogue(
    navHostController: NavHostController,
    exerciseId: Long,
    programId: Long,
    viewModel: AddExerciseViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val notesText = remember { mutableStateOf("") }
    val setsText = remember { mutableStateOf("") }
    val repsText = remember { mutableStateOf("") }
    val restText = remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    viewModel.onEvent(AddExerciseEvent.GetExercise(exerciseId))
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    scrollBehavior.state.contentOffset = scrollBehavior.state.heightOffsetLimit
    Scaffold(
//        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
//        contentWindowInsets = WindowInsets.ime,
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
                        // TODO: add mark on required fields
                        if (setsText.value.isEmpty() ||
                            repsText.value.isEmpty() ||
                            restText.value.isEmpty()){
                            scope.launch {
                                keyboardController?.hide()
                                snackbarHostState.showSnackbar(fillString)
                            }
                        } else {
                            viewModel.onEvent(
                                AddExerciseEvent.AddWorkoutExercise(
                                    WorkoutExercise(
                                        extProgramId = programId,
                                        extExerciseId = exerciseId,
                                        reps = List(setsText.value.toInt()) { repsText.value.toInt() },
                                        rest = restText.value.toInt()
                                    )
                                    // TODO: add option to have different reps number
                                )
                            )
                            setsText.value = ""
                            repsText.value = ""
                            restText.value = ""
                            notesText.value = "" // FIXME notes are not used
                            scope.launch {
                                snackbarHostState.showSnackbar("Some very useless message")
                            }
                            navHostController.popBackStack()
                        }
                    }, modifier = Modifier.align(Alignment.CenterVertically)) {
                        Text(text = stringResource(R.string.save))
                    }
                })
        }, content = { innerPadding ->
            if (viewModel.state.value.exercise != null) {
                Column(
                    modifier = Modifier
//                        .imePadding()
                        .padding(innerPadding)
                        .fillMaxSize(),
//                        verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(
                        Modifier
                            .height(140.dp)
                            .padding(WindowInsets.ime.asPaddingValues()))  // FIXME: remove, only debugging
                    Image(
                        painterResource(id = viewModel.state.value.exercise!!.image),
                        null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .padding(WindowInsets.ime.asPaddingValues())
                            .clip(AbsoluteRoundedCornerShape(12.dp))
                    )
                    OutlinedTextField(
                        value = notesText.value,
                        onValueChange = { notesText.value = it },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp).padding(WindowInsets.ime.asPaddingValues())
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp).padding(WindowInsets.ime.asPaddingValues()),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
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
//                    MyDropdownMenu(
//                        prompt = "Rest*",
//                        options = (15..120 step 15).map { "$it" },
//                        text = restText,
//                        keyboardType = KeyboardType.Number
//                    ) {
//                        Text("sec")
//                    }
                    Spacer(
                        Modifier
                            .height(16.dp).padding(WindowInsets.ime.asPaddingValues()))
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