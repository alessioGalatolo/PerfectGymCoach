package com.anexus.perfectgymcoach.screens

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardCapitalization

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AddNameDialogue(
    name: String,
    dialogueIsOpen: Boolean,
    toggleDialogue: () -> Unit,
    addName: (String) -> Unit
) {
    // alert dialogue to enter the workout plan/program name

    var text by rememberSaveable { mutableStateOf("") }
    if (dialogueIsOpen) {
        AlertDialog(
            onDismissRequest = {
                // Dismiss the dialog when the user clicks outside the dialog or on the back
                // button. If you want to disable that functionality, simply use an empty
                // onDismissRequest.
                toggleDialogue()
            },
            title = {
                Text(text = "Create workout $name")
            },
            text = {
                val keyboardController = LocalSoftwareKeyboardController.current

                TextField(value = text,
                    onValueChange = { text = it },
                    label = { Text("Name of the $name" ) },
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                    }),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    singleLine = true)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        addName(text.trim())
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