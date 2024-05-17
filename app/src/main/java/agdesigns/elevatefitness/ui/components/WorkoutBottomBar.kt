package agdesigns.elevatefitness.ui.components

import agdesigns.elevatefitness.data.exercise.Exercise
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
import agdesigns.elevatefitness.data.workout_exercise.WorkoutExercise
import androidx.compose.material3.minimumInteractiveComponentSize

@Composable
fun WorkoutBottomBar(
    contentPadding: PaddingValues,
    workoutStarted: Boolean,
    startWorkout: () -> Unit,
    currentExercise: WorkoutExercise?,
    completeWorkout: () -> Unit,
    completeSet: () -> Unit,
    setsFinished: Boolean,
    addSet: () -> Unit,
    goToNextExercise: () -> Unit,
    repsToDisplay: String,
    updateReps: (String) -> Unit,
    weightToDisplay: String,
    updateWeight: (String) -> Unit,
    autoStepWeight: (String, Exercise.Equipment, Boolean) -> Unit
) {
    Column(
        Modifier
            .padding(contentPadding)
            .padding(horizontal = 16.dp)
    ) {
        if (workoutStarted) {
            // workout has not started
            Button(
                onClick = startWorkout,
                Modifier.fillMaxWidth()
            ) {
                Text("Start workout")
            }
        } else if (currentExercise == null) {
            // workout has started and it is on the end page
            Button(
                onClick = completeWorkout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Complete workout")
            }
        } else if (setsFinished) {
            // workout started and the user has done all the sets in the page
            OutlinedButton(
                onClick = addSet,
                Modifier.fillMaxWidth()
            ) {
                Text("Add set")
            }
            Button(
                onClick = goToNextExercise,
                Modifier.fillMaxWidth()
            ) {
                Text("Next exercise")
            }

        } else {
            // normal case
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextFieldWithButtons(
                    "Reps",
                    text = { repsToDisplay },
                    onNewText = { new -> updateReps(new) },
                    onIncrement = { updateReps(((repsToDisplay.toIntOrNull() ?: 0) + 1).toString()) },
                    onDecrement = { updateReps(((repsToDisplay.toIntOrNull() ?: 0) - 1).toString()) },
                    contentDescription = "reps"
                )
                Spacer(Modifier.width(8.dp))
                TextFieldWithButtons(
                    "Weight",
                    text = { weightToDisplay },
                    onNewText = { new -> updateWeight(new) },
                    onIncrement = { autoStepWeight(
                        weightToDisplay,
                        currentExercise.equipment,
                        false
                    )},
                    onDecrement = { autoStepWeight(
                        weightToDisplay,
                        currentExercise.equipment,
                        true
                    )},
                    contentDescription = "weight"
                )
            }

            // fixme: becomes hidden when keyboard out for typing in above fields
            Row(Modifier.fillMaxWidth()) {
                Button(
                    onClick = completeSet,
                    Modifier.fillMaxWidth()
                ) {
                    Text("Complete")
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.TextFieldWithButtons(
    prompt: String,
    text: () -> String,
    onNewText: (String) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    contentDescription: String = ""
) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, true)
    ) {
        IconButton(onClick = onDecrement, modifier = Modifier.weight(0.3f).minimumInteractiveComponentSize()) {
            Icon(Icons.Filled.Remove, "Decrease $contentDescription")  // FIXME: accessibility -> increase what?
        }
        OutlinedTextField(
            value = text(),
            onValueChange = onNewText,
            singleLine = true,
            label = { Text(prompt) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .widthIn(1.dp, Dp.Infinity)
                .heightIn(1.dp, Dp.Infinity)
                .weight(0.5f)
        )
        IconButton(onClick = onIncrement, modifier = Modifier.weight(0.3f).minimumInteractiveComponentSize()) {
            Icon(Icons.Filled.Add, "Increase $contentDescription")  // FIXME: accessibility -> decrease what?
        }
    }
}