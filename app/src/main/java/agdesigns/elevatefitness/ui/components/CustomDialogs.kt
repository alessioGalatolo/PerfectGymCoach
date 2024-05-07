package agdesigns.elevatefitness.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.android.awaitFrame

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InsertNameDialog(
    prompt: String,
    dialogueIsOpen: Boolean,
    toggleDialog: () -> Unit,
    insertName: (String) -> Unit
) {
    // alert dialogue to enter the workout plan/program name

    var text by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    if (dialogueIsOpen) {
        AlertDialog(
            onDismissRequest = {
                // Dismiss the dialog when the user clicks outside the dialog or on the back
                // button. If you want to disable that functionality, simply use an empty
                // onDismissRequest.
                toggleDialog()
            },
            title = {
                Text(text = "Enter ${prompt.lowercase()}")
            },
            text = {

                TextField(value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            if (it.isFocused) {
                                keyboardController?.show()
                            }
                        },
                    label = { Text(prompt) },
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                    }),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    singleLine = true)
                LaunchedEffect(focusRequester) {
                    awaitFrame()
                    awaitFrame()
                    awaitFrame()
                    awaitFrame()
                    focusRequester.requestFocus()
                }
            },
            confirmButton = {
                TextButton(
                    enabled = text.isNotBlank(),
                    onClick = {
                        insertName(text.trim())
                        toggleDialog()
                        text = ""
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        toggleDialog()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ResumeWorkout(
    dialogueIsOpen: Boolean,
    discardWorkout: () -> Unit,
    resumeWorkout: () -> Unit
) {
    if (dialogueIsOpen) {
        AlertDialog(
            onDismissRequest = {
            },
            title = {
                Text(text = "Resume unfinished workout?")
            },
            text = {
               Text("We noticed you did not finish the last workout you started, do you want to finish it now?")
            },
            confirmButton = {
                TextButton(
                    onClick = resumeWorkout
                ) {
                    Text("Resume")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = discardWorkout
                ) {
                    Text("Discard workout")
                }
            }
        )
    }
}

@Composable
fun CancelWorkoutDialog(
    dialogueIsOpen: Boolean,
    toggleDialog: () -> Unit,
    cancelWorkout: () -> Unit,
    deleteData: () -> Unit
) {
    if (dialogueIsOpen) {
        AlertDialog(
            onDismissRequest = {
                toggleDialog()
            },
            title = {
                Text(text = "Delete workout data?")
            },
            text = {
                Text(text = "Do you want to delete the recorded exercise data as well as cancelling the workout? (Go back or tap anywhere outside this dialog to keep working out)")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        toggleDialog()
                        deleteData()
                        cancelWorkout()
                    }
                ) {
                    Text("Delete data and cancel workout")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        toggleDialog()
                        cancelWorkout()
                    }
                ) {
                    Text("Cancel workout")
                }
            }
        )
    }
}


@Composable
fun InfoDialog(dialogueIsOpen: Boolean, toggleDialogue: () -> Unit, infoText: @Composable () -> Unit) {
    if (dialogueIsOpen) {
        AlertDialog(
            onDismissRequest = {
                toggleDialogue()
            },
            icon = { Icon(Icons.Outlined.Info, null)},
            text = {
                infoText()
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        toggleDialogue()
                    }
                ) {
                    Text("Ok")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeRepsWeightDialog(
    dialogIsOpen: Boolean,
    toggleDialog: () -> Unit,
    initialReps: String,
    initialWeight: String,
    updateValues: (Int, Float) -> Unit
) {
    if (dialogIsOpen) {
        var reps by rememberSaveable { mutableStateOf(initialReps) }
        var weight by rememberSaveable { mutableStateOf(initialWeight) }
        AlertDialog(
            onDismissRequest = {
                toggleDialog()
            },
            title = { Text("Change reps/weight value") },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = reps,
                        onValueChange = {reps = it},
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("New reps value") }
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = {weight = it},
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("New weight value") }
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { toggleDialog() }
                ) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(
                    enabled = reps.toIntOrNull() != null && weight.toFloatOrNull() != null,
                    onClick = {
                        toggleDialog()
                        updateValues(reps.toInt(), weight.toFloat())
                    }
                ) {
                    Text("Update values")
                }
            }
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InputOtherEquipmentDialog(
    dialogIsOpen: Boolean,
    toggleDialog: () -> Unit,
    weightUnit: String, // kg or lb
    updateTare: (Float) -> Unit
) {
    // alert dialogue to enter the workout plan/program name

    var text by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    if (dialogIsOpen) {
        AlertDialog(
            onDismissRequest = {
                toggleDialog()
            },
            title = {
                Text(text = "Enter other barbell weight")
            },
            text = {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            if (it.isFocused) {
                                keyboardController?.show()
                            }
                        },
                    placeholder = { Text("0") },
                    trailingIcon = { Text(weightUnit) },
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                    }),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                LaunchedEffect(focusRequester) {
                    awaitFrame()
                    awaitFrame()
                    awaitFrame()
                    awaitFrame()
                    focusRequester.requestFocus()
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        updateTare(text.trim().toFloatOrNull() ?: 0f)
                        text = ""
                        toggleDialog()
                    },
                    enabled = text.toFloatOrNull() != null
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        toggleDialog()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}