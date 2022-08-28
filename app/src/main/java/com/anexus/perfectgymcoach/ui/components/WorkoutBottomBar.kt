package com.anexus.perfectgymcoach.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anexus.perfectgymcoach.data.exercise.Exercise
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import com.anexus.perfectgymcoach.data.exercise.WorkoutExerciseAndInfo
import com.anexus.perfectgymcoach.viewmodels.WorkoutEvent
import kotlinx.coroutines.launch

@Composable
fun WorkoutBottomBar(
    contentPadding: PaddingValues,
    workoutStarted: Boolean,
    startWorkout: () -> Unit,
    currentExercise: WorkoutExerciseAndInfo?,
    completeWorkout: () -> Unit,
    completeSet: () -> Unit,
    setsFinished: Boolean,
    addSet: () -> Unit,
    goToNextExercise: () -> Unit,
    repsToDisplay: Int,
    updateReps: (Int) -> Unit,
    weightToDisplay: Float,
    updateWeight: (Float) -> Unit
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
                    text = { repsToDisplay.toString() },
                    onNewText = { new -> updateReps(new.toInt()) },
                    onIncrement = { updateReps(repsToDisplay + 1) },
                    onDecrement = { updateReps(repsToDisplay - 1) }
                )
                Spacer(Modifier.width(8.dp))
                TextFieldWithButtons(
                    "Weight",
                    text = { weightToDisplay.toString() },
                    onNewText = { new -> updateWeight(new.toFloat()) },
                    onIncrement = { updateWeight(
                                weightToDisplay + Exercise.equipment2increment[currentExercise.equipment]!!
                    )},
                    onDecrement = { updateWeight(
                        weightToDisplay - Exercise.equipment2increment[currentExercise.equipment]!!
                    )}
                )
            }
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
    onDecrement: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, true)
    ) {
        IconButton(onClick = onDecrement, modifier = Modifier.weight(0.3f)) {
            Icon(Icons.Filled.Remove, null)
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
        IconButton(onClick = onIncrement, modifier = Modifier.weight(0.3f)) {
            Icon(Icons.Filled.Add, null)
        }
    }
}