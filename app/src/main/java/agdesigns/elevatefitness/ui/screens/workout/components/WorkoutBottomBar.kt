package agdesigns.elevatefitness.ui.screens.workout.components

import agdesigns.elevatefitness.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import agdesigns.elevatefitness.ui.screens.workout.WorkoutState
import agdesigns.elevatefitness.ui.screens.workout.CurrentExerciseState
import androidx.compose.foundation.background
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import agdesignes.elevatefitness.shared.Equipment

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutBottomBar(
    workoutState: WorkoutState,
    currentExerciseState: CurrentExerciseState,
    contentPadding: PaddingValues,
    startWorkout: () -> Unit,
    completeWorkout: () -> Unit,
    completeSet: () -> Unit,
    addSet: () -> Unit,
    goToNextExercise: () -> Unit,
    updateReps: (String) -> Unit,
    updateWeight: (String) -> Unit,
    autoStepWeight: (String, Equipment, Boolean) -> Unit
) {
    val imeVisible = WindowInsets.isImeVisible
    Column(
        Modifier
            .background(NavigationBarDefaults.containerColor)
            .padding(contentPadding)
            .padding(horizontal = 16.dp)
    ) {
        if (!workoutState.workoutStarted) {
            // workout has not started
            Button(
                onClick = startWorkout,
                Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.start_workout))
            }
        } else if (currentExerciseState.currentExercise == null) {
            // workout has started and it is on the end page
            Button(
                onClick = completeWorkout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.complete_workout))
            }
        } else if (currentExerciseState.setsDone >= currentExerciseState.currentExercise.reps.size) {
            // workout started and the user has done all the sets in the page
            OutlinedButton(
                onClick = addSet,
                Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.add_set))
            }
            Button(
                onClick = goToNextExercise,
                Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.next_exercise))
            }

        } else {
            // normal case
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextFieldWithButtons(
                    stringResource(R.string.reps),
                    hapticsEnabled = true,
                    text = { currentExerciseState.repsBottomBar },
                    onNewText = { new -> updateReps(new) },
                    onIncrement = { updateReps(((currentExerciseState.repsBottomBar.toIntOrNull() ?: 0) + 1).toString()) },
                    onDecrement = { updateReps(((currentExerciseState.repsBottomBar.toIntOrNull() ?: 0) - 1).toString()) },
                    contentDescription = stringResource(R.string.reps),
                    textIsValid = { currentExerciseState.repsIsValid }
                )
                Spacer(Modifier.width(8.dp))
                TextFieldWithButtons(
                    stringResource(R.string.weight),
                    hapticsEnabled = true,
                    text = { currentExerciseState.weightBottomBar },
                    onNewText = { new -> updateWeight(new) },
                    onIncrement = { autoStepWeight(
                        currentExerciseState.weightBottomBar,
                        currentExerciseState.currentExercise.equipment,
                        false
                    )},
                    onDecrement = { autoStepWeight(
                        currentExerciseState.weightBottomBar,
                        currentExerciseState.currentExercise.equipment,
                        true
                    )},
                    contentDescription = stringResource(R.string.weight),
                    textIsValid = { currentExerciseState.weightIsValid }
                )
            }

            /*
             FIXME: bottom padding if ime is needed otherwise button will be below keyboard
             but it's bad to hardcode the padding. There are also still some bugs with keyboard and
             this bottom bar.
             */
            Row(Modifier.fillMaxWidth()) {
                Button(
                    enabled = currentExerciseState.repsIsValid && currentExerciseState.weightIsValid,
                    onClick = completeSet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (imeVisible) 48.dp else 0.dp)
                ) {
                    Text(stringResource(R.string.complete_set))
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RowScope.TextFieldWithButtons(
    prompt: String,
    hapticsEnabled: Boolean = false,
    text: () -> String,
    onNewText: (String) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    textIsValid: (String) -> Boolean = { true },
    contentDescription: String = ""
) {
    val haptics = LocalHapticFeedback.current
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, true)
    ) {
        IconButton(onClick = {
            if (hapticsEnabled)
                haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
            onDecrement()
        }, modifier = Modifier
            .weight(0.3f)
            .minimumInteractiveComponentSize()) {
            Icon(
                Icons.Filled.Remove,
                stringResource(
                    R.string.decrease_i, contentDescription
                ))
        }
        OutlinedTextField(
            shape = MaterialTheme.shapes.large,
            value = text(),
            onValueChange = onNewText,
            singleLine = true,
            label = { Text(prompt) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = !textIsValid(text()),
            modifier = Modifier
                .widthIn(1.dp, Dp.Infinity)
                .heightIn(1.dp, Dp.Infinity)
                .weight(0.5f)
        )
        IconButton(onClick = {
            if (hapticsEnabled)
                haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
            onIncrement()
        }, modifier = Modifier
            .weight(0.3f)
            .minimumInteractiveComponentSize()) {
            Icon(Icons.Filled.Add, stringResource(R.string.increase_i, contentDescription))
        }
    }
}