package com.anexus.perfectgymcoach.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
    toggleDialogue: () -> Unit,
    insertName: (String) -> Unit
) {
    // alert dialogue to enter the workout plan/program name

    var text by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    if (dialogueIsOpen) {
        AlertDialog(
            onDismissRequest = {
                // Dismiss the dialog when the user clicks outside the dialog or on the back
                // button. If you want to disable that functionality, simply use an empty
                // onDismissRequest.
                toggleDialogue()
            },
            title = {
                Text(text = "Enter ${prompt.lowercase()}")
            },
            text = {
                val keyboardController = LocalSoftwareKeyboardController.current

                TextField(value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.focusRequester(focusRequester).onFocusChanged {
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
                    focusRequester.requestFocus()
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        insertName(text.trim())
                        toggleDialogue()
                        text = ""
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        toggleDialogue()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
