package com.anexus.perfectgymcoach.screens

import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
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
            LargeTopAppBar(title = { Text("Add exercise to $programName") },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                })
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
    viewModel.onEvent(ExercisesEvent.GetExercises(Exercise.Muscle.values()[muscleOrdinal]))
    AddExerciseDialogue(
        viewModel.state.value.openAddExerciseDialogue,
        { viewModel.onEvent(ExercisesEvent.ToggleExerciseDialogue) },
        { eId, name, sets, reps, rest ->
            viewModel.onEvent(ExercisesEvent.AddWorkoutExercise(
                WorkoutExercise(
                extProgramId = programId,
                extExerciseId = eId,
                name = name,
                sets = sets,
                reps = reps,
                rest = rest
            )
            ))
        }

    )
    Scaffold(
        topBar = {
            SmallTopAppBar(title = { Text(Exercise.Muscle.values()[muscleOrdinal].muscleName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                })
        }, content = { innerPadding ->
            // if you have some plans
            LazyColumn(
                contentPadding = innerPadding
            ) {
                items(items = viewModel.state.value.exercises, key = { it }) { exercise ->
                    Card(
                        onClick = {
                            viewModel.onEvent(ExercisesEvent.ToggleExerciseDialogue)
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
    dialogueIsOpen: Boolean,
    toggleDialogue: () -> Unit,
    addExercise: (Long, String, Int, Int, Int) -> Unit
) {
    // alert dialogue to enter the workout plan/program name

    var text by rememberSaveable { mutableStateOf("") }
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
                topBar = {
                    SmallTopAppBar(title = { Text("TODO") },
                        navigationIcon = {
                            IconButton(onClick = { toggleDialogue() }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close"
                                )
                            }
                        }, actions = {
                            TextButton(onClick = { /*TODO*/ }, modifier = Modifier.align(CenterVertically)) {
                                Text(text = "Save")
                            }
                        })
                }, content = { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(onClick = { addExercise(1, "Some exercise", 2, 3, 4); toggleDialogue() }) {
                            Text("CLICK ME!")
                        }
                        val keyboardController = LocalSoftwareKeyboardController.current
                        TextField(value = text,
                            onValueChange = { text = it },
                            label = { Text("Name of the " ) },
                            keyboardActions = KeyboardActions(onDone = {
                                keyboardController?.hide()
                            }),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            singleLine = true)

                    }
                })
        }

    }
}
//        AlertDialog(
//            onDismissRequest = {
//                // Dismiss the dialog when the user clicks outside the dialog or on the back
//                // button. If you want to disable that functionality, simply use an empty
//                // onDismissRequest.
//                toggleDialogue()
//            },
//            title = {
//                Text(text = "Add exercise")
//            },
//            text = {
//                val keyboardController = LocalSoftwareKeyboardController.current
//
//                TextField(value = text,
//                    onValueChange = { text = it },
//                    label = { Text("Name of the " ) },
//                    keyboardActions = KeyboardActions(onDone = {
//                        keyboardController?.hide()
//                    }),
//                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
//                    singleLine = true)
//
//            },
//            confirmButton = {
//                TextButton(
//                    onClick = {
////                        addExercise(text.trim())
//                        toggleDialogue()
//                        text = ""
//                    }
//                ) {
//                    Text("Confirm")
//                }
//            },
//            dismissButton = {
//                TextButton(
//                    onClick = {
//                        toggleDialogue()
//                    }
//                ) {
//                    Text("Cancel")
//                }
//            }
//        )

