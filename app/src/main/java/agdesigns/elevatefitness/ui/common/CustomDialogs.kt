package agdesigns.elevatefitness.ui.common

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.ui.screens.workout.components.TextFieldWithButtons
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndSelectAll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.android.awaitFrame

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InsertNameDialog(
    prompt: String,
    dialogueIsOpen: Boolean,
    oldName: String? = null,
    toggleDialog: () -> Unit,
    insertName: (String) -> Unit,
) {
    // alert dialogue to enter the workout plan/program name
    val textFieldState = rememberTextFieldState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(oldName) {
        if (oldName != null) {
            textFieldState.setTextAndSelectAll(oldName)
        }
    }
    LaunchedEffect(dialogueIsOpen) {
        if (dialogueIsOpen) {
            awaitFrame()
            awaitFrame()
            awaitFrame()
            awaitFrame()
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    val saveAndClose = {
        if (textFieldState.text.isNotBlank()) {
            if (textFieldState.text.toString() != oldName) {
                insertName(textFieldState.text.toString().trim())
                textFieldState.clearText()
            }
            toggleDialog()
        }
    }
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
                TextField(
                    state = textFieldState,
                    isError = textFieldState.text.isBlank(),
                    label = { Text(prompt) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                        showKeyboardOnFocus = true
                    ),
                    onKeyboardAction = KeyboardActionHandler {
                        saveAndClose()
                    },
                    modifier = Modifier.focusRequester(focusRequester)
                )
            },
            confirmButton = {
                TextButton(
                    enabled = textFieldState.text.isNotBlank(),
                    onClick = {
                        saveAndClose()
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

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UpdateWeightDialog(
    prompt: String,
    dialogueIsOpen: Boolean,
    toggleDialog: () -> Unit,
    updateWeight: (Float) -> Unit,
) {
    // alert dialogue to enter the workout plan/program name
    val textFieldState = rememberTextFieldState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(dialogueIsOpen) {
        if (dialogueIsOpen) {
            awaitFrame()
            awaitFrame()
            awaitFrame()
            awaitFrame()
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    val saveAndClose = {
        val newWeight = textFieldState.text.toString().toFloatOrNull()
        if (newWeight != null) {
            updateWeight(newWeight)
            textFieldState.clearText()
            toggleDialog()
        }
    }
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
                TextField(
                    state = textFieldState,
                    isError = textFieldState.text.toString().toFloatOrNull() == null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                        showKeyboardOnFocus = true
                    ),
                    onKeyboardAction = KeyboardActionHandler {
                        saveAndClose()
                    },
                    modifier = Modifier.focusRequester(focusRequester)
                )
            },
            confirmButton = {
                TextButton(
                    enabled = textFieldState.text.toString().toFloatOrNull() != null,
                    onClick = {
                        saveAndClose()
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CancelWorkoutDialog(
    dialogueOpenProgress: Float,
    dismissDialog: () -> Unit,
    cancelWorkout: () -> Unit,
    deleteData: () -> Unit,
    hasRecords: Boolean
) {
    val (cancelData, onStateChange) = remember { mutableStateOf(false) }

    BackHandler(enabled = dialogueOpenProgress == 1f) {
        dismissDialog()
    }

    val animatedProgress by animateFloatAsState(
        targetValue = dialogueOpenProgress,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "dialog_progress"
    )
    if (animatedProgress > 0f) {
        // custom implementation of alert dialog otherwise it doesn't work well with predictive back
        Box (
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f * animatedProgress))
                .zIndex(10f) // should be above everything
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    dismissDialog()
                }
                .sizeIn(minWidth = 280.dp, maxWidth = 560.dp),  // default for alert dialog
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .padding(36.dp)
                    .alpha(0.5f + (0.5f * animatedProgress))
                    .scale(0.8f + (0.2f * animatedProgress)), // Optional scale effect
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = stringResource(R.string.cancel_workout_title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = stringResource(R.string.cancel_workout_info),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    if (hasRecords) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = cancelData,
                                    onValueChange = { onStateChange(!cancelData) },
                                    role = Role.Checkbox
                                )
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 24.dp),
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
                    FlowRow(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = {
                                dismissDialog()
                            }
                        ) {
                            Text(stringResource(R.string.cancel_workout_cancel))
                        }

                        TextButton(
                            onClick = {
                                dismissDialog()
                                if (cancelData)
                                    deleteData()
                                cancelWorkout()
                            }
                        ) {
                            Text(stringResource(R.string.cancel_workout_confirm))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DiscardChangesDialog(
    dialogueOpenProgress: Float,
    dismissDialog: () -> Unit,
    confirmExit: () -> Unit
) {

    BackHandler(enabled = dialogueOpenProgress == 1f) {
        dismissDialog()
    }

    val animatedProgress by animateFloatAsState(
        targetValue = dialogueOpenProgress,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "dialog_progress"
    )
    if (animatedProgress > 0f) {
        // custom implementation of alert dialog otherwise it doesn't work well with predictive back
        Box (
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f * animatedProgress))
                .zIndex(10f) // should be above everything
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    dismissDialog()
                }
                .sizeIn(minWidth = 280.dp, maxWidth = 560.dp),  // default for alert dialog
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .padding(36.dp)
                    .alpha(0.5f + (0.5f * animatedProgress))
                    .scale(0.8f + (0.2f * animatedProgress)), // Optional scale effect
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = stringResource(R.string.discard_data_dialog_title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = stringResource(R.string.discard_data_dialog_info),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = {
                                dismissDialog()
                            }
                        ) {
                            Text(stringResource(R.string.discard_data_dialog_cancel))
                        }

                        TextButton(
                            onClick = {
                                dismissDialog()
                                confirmExit()
                            }
                        ) {
                            Text(stringResource(R.string.discard_data_dialog_confirm))
                        }
                    }
                }
            }
        }
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
    updateValues: (Int, Float) -> Unit,
    deleteSet: () -> Unit
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
                    TextFieldWithButtons(
                        prompt = stringResource(R.string.new_reps_value),
                        text = { reps },
                        onNewText = { reps = it },
                        onIncrement = { reps = ((reps.toUIntOrNull() ?: 0U) + 1U).toString() },
                        onDecrement = { reps = ((reps.toUIntOrNull() ?: 0U) - 1U).toString() },
                        textIsValid = { reps -> reps.toUIntOrNull() != null }
                    )
                    TextFieldWithButtons(
                        prompt = stringResource(R.string.new_weight_value),
                        text = { weight },
                        onNewText = { weight = it },
                        onIncrement = { weight = ((weight.toFloatOrNull() ?: 0f) + 0.5f).toString() },
                        onDecrement = { weight = ((weight.toFloatOrNull() ?: 0f) - 0.5f).toString() },
                        textIsValid = { weight -> weight.toFloatOrNull() != null }
                    )
                    Card (
                        onClick = {
                            toggleDialog()
                            deleteSet()
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                stringResource(R.string.delete),
                                Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text(
                                stringResource(R.string.workout_delete_completed_set),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
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
                        { Text(stringResource(R.string.please_enter_a_valid_number)) }
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