package com.anexus.perfectgymcoach.ui.components

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