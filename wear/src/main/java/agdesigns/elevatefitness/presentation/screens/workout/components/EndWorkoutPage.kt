package agdesigns.elevatefitness.presentation.screens.workout.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton

@Composable
fun EndWorkoutPage(
    contentPadding: PaddingValues,
    endWorkout: () -> Unit
) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        TextButton(onClick = endWorkout, modifier = Modifier.align(Alignment.Center).fillMaxSize()) {
            Text("End Workout")
        }
    }
}