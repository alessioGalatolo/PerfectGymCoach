package agdesigns.elevatefitness.ui.components

import agdesigns.elevatefitness.R
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
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
                Text(text = stringResource(R.string.enter_name_for, prompt.lowercase()))
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
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        toggleDialog()
                    }
                ) {
                    Text(stringResource(R.string.dialog_cancel))
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
                Text(text = stringResource(R.string.resume_unfinished_workout))
            },
            text = {
               Text(stringResource(R.string.resume_workout_info))
            },
            confirmButton = {
                TextButton(
                    onClick = resumeWorkout
                ) {
                    Text(stringResource(R.string.resume))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = discardWorkout
                ) {
                    Text(stringResource(R.string.discard_workout))
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
    deleteData: () -> Unit,
    hasRecords: Boolean
) {
    val (cancelData, onStateChange) = remember { mutableStateOf(false) }
    if (dialogueIsOpen) {
        AlertDialog(
            onDismissRequest = {
                toggleDialog()
            },
            title = {
                Text(text = stringResource(R.string.cancel_workout_title))
            },
            text = {
                Column {
                    Text(text = stringResource(R.string.cancel_workout_info))
                    if (hasRecords)
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
                                text = stringResource(R.string.cancel_workout_and_records),
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
                    Text(stringResource(R.string.cancel_workout_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        toggleDialog()
                    }
                ) {
                    Text(stringResource(R.string.cancel_workout_cancel))
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
            icon = { Icon(Icons.Outlined.Info, stringResource(R.string.info))},
            text = {
                infoText()
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        toggleDialogue()
                    }
                ) {
                    Text(stringResource(R.string.info_dialog_confirm))
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
            title = { Text(stringResource(R.string.change_reps_weight_title)) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = reps,
                        onValueChange = {reps = it},
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text(stringResource(R.string.new_reps_value)) }
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = {weight = it},
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text(stringResource(R.string.new_weight_value)) }
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { toggleDialog() }
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
            confirmButton = {
                TextButton(
                    enabled = reps.toUIntOrNull() != null && weight.toFloatOrNull() != null,
                    onClick = {
                        toggleDialog()
                        updateValues(reps.toInt(), weight.toFloat())
                    }
                ) {
                    Text(stringResource(R.string.change_reps_weight_confirm))
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
    var text by rememberSaveable { mutableStateOf("") }
    var isValid by remember { mutableStateOf(false) }

    // Validate input
    LaunchedEffect(text) {
        isValid = text.toFloatOrNull()?.let { it >= 0 } == true
    }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    if (dialogIsOpen) {
        AlertDialog(
            onDismissRequest = {
                toggleDialog()
            },
            title = {
                Text(
                    text = stringResource(R.string.enter_barbell_weight_title),
                    style = MaterialTheme.typography.headlineSmall
                )
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
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    isError = text.isNotEmpty() && !isValid,
                    supportingText = if (text.isNotEmpty() && !isValid) {
                        { Text("Please enter a valid weight") }
                    } else null,
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
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        toggleDialog()
                    }
                ) {
                    Text(stringResource(R.string.dialog_cancel))
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
    val (dontAskAgainChecked, onStateChange) = remember { mutableStateOf(false) }
    if (dialogIsOpen) {
        AlertDialog(
            onDismissRequest = {
                toggleDialog()
            },
            title = {
                Text(text = stringResource(R.string.request_notification_access_title))
            },
            text = {
                Column {
                    Text(text = stringResource(R.string.request_notification_access_info))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .toggleable(
                                value = dontAskAgainChecked,
                                onValueChange = { onStateChange(!dontAskAgainChecked) },
                                role = Role.Checkbox
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = dontAskAgainChecked,
                            onCheckedChange = null // null recommended for accessibility with screenreaders
                        )
                        Text(
                            text = stringResource(R.string.don_t_ask_again),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        openPermissionRequest()
                        toggleDialog()
                    }
                ) {
                    Text(stringResource(R.string.request_notification_access_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (dontAskAgainChecked)
                            dontAskAgain()
                        toggleDialog()
                    }
                ) {
                    Text(stringResource(R.string.request_notification_access_cancel))
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
                Text(text = stringResource(R.string.reset_exercise_probability_title))
            },
            text = {
                Column {
                    Text(text = stringResource(R.string.reset_exercise_probability_info))
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
                            text = stringResource(R.string.reset_all_exercises_probability),
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
                    Text(stringResource(R.string.reset))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        toggleDialog()
                    }
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}