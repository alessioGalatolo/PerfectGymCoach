package agdesigns.elevatefitness.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
    val (cancelData, onStateChange) = remember { mutableStateOf(false) }
    if (dialogueIsOpen) {
        AlertDialog(
            onDismissRequest = {
                toggleDialog()
            },
            title = {
                Text(text = "Delete workout data?")
            },
            text = {
                Column {
                    Text(text = "Do you want to delete the recorded exercise data as well as cancelling the workout?")
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .toggleable(
                                value = cancelData,
                                onValueChange = { onStateChange(!cancelData) },
                                role = Role.Checkbox
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = cancelData,
                            onCheckedChange = null // null recommended for accessibility with screenreaders
                        )
                        Text(
                            text = "Delete exercise data",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        toggleDialog()
                        if (cancelData)
                            deleteData()
                        cancelWorkout()
                    }
                ) {
                    Text("Cancel workout")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        toggleDialog()
                    }
                ) {
                    Text("Keep doing workout")
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
            icon = { Icon(Icons.Outlined.Info, "Info")},
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

@Composable
fun RequestNotificationAccessDialog(
    dialogIsOpen: Boolean,
    toggleDialog: () -> Unit,
    openPermissionRequest: () -> Unit,
    dontAskAgain: () -> Unit
) {
    if (dialogIsOpen) {
        AlertDialog(
            onDismissRequest = {
                toggleDialog()
            },
            title = {
                Text(text = "Would you like to control your music during your workout?")
            },
            text = {
                Text(text = "If you want, you can enable this app to show and control your music while your workout is running. However, it needs access to your notification to do so. If you want to allow that, press 'Let's do it!' and look for this app in the list and enable the notification access.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        openPermissionRequest()
                        toggleDialog()
                    }
                ) {
                    Text("Let's do it!")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        dontAskAgain()
                        toggleDialog()
                    }
                ) {
                    Text("Don't ask again")
                }
            }
        )
    }
}

@Composable
fun ResetExerciseProbabilityDialog(
    dialogIsOpen: Boolean,
    toggleDialog: () -> Unit,
    resetExercise: () -> Unit,
    resetAllExercises: () -> Unit
) {
    val (resetAllChecked, onStateChange) = remember { mutableStateOf(false) }
    if (dialogIsOpen) {
        AlertDialog(
            onDismissRequest = {
                toggleDialog()
            },
            title = {
                Text(text = "Reset exercise probability?")
            },
            text = {
                Column {
                    Text(text = "By tapping on 'Reset' you will reset the exercise's probability of appearing in newly generated workouts. For a fresh start you can also reset the probability of all the exercises.")
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .toggleable(
                                value = resetAllChecked,
                                onValueChange = { onStateChange(!resetAllChecked) },
                                role = Role.Checkbox
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = resetAllChecked,
                            onCheckedChange = null // null recommended for accessibility with screenreaders
                        )
                        Text(
                            text = "Reset all exercises' probability",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (resetAllChecked) {
                            resetAllExercises()
                        } else {
                            resetExercise()
                        }
                        toggleDialog()
                    }
                ) {
                    Text("Reset")
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