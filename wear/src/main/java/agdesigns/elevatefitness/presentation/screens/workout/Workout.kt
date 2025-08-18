package agdesigns.elevatefitness.presentation.screens.workout

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.presentation.screens.home.ArrowSwitcher
import agdesigns.elevatefitness.presentation.screens.home.VignetteImage
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ButtonGroup
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.TimeText
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlin.math.max

@Destination<RootGraph>
@Composable
fun Workout(
    navigator: DestinationsNavigator,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    BackHandler {
        viewModel.onEvent(WorkoutEvent.StopActivity)
        navigator.navigateUp()
    }
    val workoutState by viewModel.state.collectAsState()
    val listState = rememberScalingLazyListState()

    val currentRestMillis: Long? by remember { derivedStateOf {
        if (workoutState.restTimestamp != null)
            max(0L,
                workoutState.restTimestamp?.toInstant()?.toEpochMilli()?.minus(
                    workoutState.currentTime.toInstant().toEpochMilli()
                ) ?: 0L
            )
        else null
    }}
    // FIXME: if exercise is changed then rests change then progression is weird
    val restProgression by remember {
        derivedStateOf {
            if (workoutState.setsDone <= workoutState.rest.size && workoutState.rest.isNotEmpty()) {
                // rest can be 0, avoid div by 0
                if (workoutState.rest[max(0, workoutState.setsDone - 1)] > 0) {
                    currentRestMillis?.toFloat()
                        ?.div(workoutState.rest[max(0, workoutState.setsDone - 1)] * 1000)
                } else
                    null
            } else
                null
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        AppScaffold(Modifier.background(Color.Transparent)) {
            if (workoutState.imageBitmap != null) {
                VignetteImage(workoutState.imageBitmap!!.asImageBitmap())
            }
            if ((restProgression ?: 0f) > 0f) {
                // TODO: add vibration on counter finish
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    progress = { restProgression ?: 1f }, // should not happen but sometimes is null
                    startAngle = CircularProgressIndicatorDefaults.StartAngle + 20f,  // allow for clock in up center
                    endAngle = CircularProgressIndicatorDefaults.StartAngle - 20f,
                    strokeWidth = CircularProgressIndicatorDefaults.smallStrokeWidth

                )
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Transparent),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val nextUp by remember {
                        derivedStateOf {
                            if (workoutState.setsDone < workoutState.rest.size)
                                workoutState.exerciseName
                            else
                                workoutState.nextExerciseName
                        }
                    }
                    if (nextUp.isNotBlank()) {
                        Text(
                            text = "Next up:",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = nextUp,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 4.dp,
                                bottom = 16.dp
                            )
                        )
                    }
                    Text(
                        text = ((currentRestMillis ?: 0L) / 1000).toInt().toString(),
                        style = MaterialTheme.typography.displayLarge
                    )
                    TextButton({
                        viewModel.onEvent(WorkoutEvent.ResetRest)
                    }, Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)) {
                        Text(text = "Skip rest", textAlign = TextAlign.Center)
                    }
                }
            } else if (workoutState.exerciseName.isNotEmpty()) {
                ScreenScaffold(
                    modifier = Modifier.background(Color.Transparent),
                    scrollState = listState,
                    timeText = { TimeText() },
                    // Define custom spacing between [EdgeButton] and [ScalingLazyColumn].
                    edgeButtonSpacing = 4.dp,
                    edgeButton = {
                        EdgeButton(
                            onClick = {
                                viewModel.onEvent(WorkoutEvent.CompleteSet)
                            },
                            modifier =
                                // In case user starts scrolling from the EdgeButton.
                                Modifier.scrollable(
                                    listState,
                                    orientation = Orientation.Vertical,
                                    reverseDirection = true,
                                    // An overscroll effect should be applied to the EdgeButton for proper
                                    // scrolling behavior.
                                    overscrollEffect = rememberOverscrollEffect()
                                )
                        ) {
                            Icon(Icons.Filled.Done, contentDescription = "Done")
                        }
                    },
                ) { contentPadding ->
                    ScalingLazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        // Bottom spacing is derived from [ScreenScaffold.edgeButtonSpacing].
                        contentPadding = contentPadding,
                    ) {
                        item {
                            Text(
                                text = workoutState.exerciseName + " (${workoutState.setsDone + 1}/${workoutState.rest.size})",
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (workoutState.note.isNotEmpty()) {
                            item {
                                Text(
                                    text = workoutState.note,
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        item {
                            ListHeader {
                                Text("Reps")
                            }
                        }
                        item {
                            val interactionSource1 = remember { MutableInteractionSource() }
                            val interactionSource3 = remember { MutableInteractionSource() }
                            Box(contentAlignment = Alignment.Center) {
                                ButtonGroup(Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = { viewModel.onEvent(WorkoutEvent.ChangeReps(-1)) },
                                        modifier = Modifier.animateWidth(interactionSource1),
                                        interactionSource = interactionSource1
                                    ) {
                                        Box(
                                            Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Remove,
                                                contentDescription = "Decrease reps"
                                            )
                                        }
                                    }
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .weight(1.5f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            workoutState.currentReps.toString(),
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.onEvent(WorkoutEvent.ChangeReps(1))
                                        },
                                        modifier = Modifier.animateWidth(interactionSource3),
                                        interactionSource = interactionSource3
                                    ) {
                                        Box(
                                            Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Increase reps"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            ListHeader {
                                Text("Weight")
                            }
                        }
                        item {
                            val interactionSource1 = remember { MutableInteractionSource() }
                            val interactionSource3 = remember { MutableInteractionSource() }
                            Box(contentAlignment = Alignment.Center) {
                                ButtonGroup {
                                    Button(
                                        onClick = { viewModel.onEvent(WorkoutEvent.ChangeWeight(-1)) },
                                        modifier = Modifier.animateWidth(interactionSource1),
                                        interactionSource = interactionSource1
                                    ) {
                                        Box(
                                            Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Remove,
                                                contentDescription = "Decrease weight"
                                            )
                                        }
                                    }
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .weight(1.5f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            workoutState.weight.toString(),
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.onEvent(WorkoutEvent.ChangeWeight(1))
                                        },
                                        modifier = Modifier.animateWidth(interactionSource3),
                                        interactionSource = interactionSource3
                                    ) {
                                        Box(
                                            Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Increase weight"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (workoutState.equipment.lowercase().contains("barbell")) {
                            item {
                                ListHeader {
                                    Text("Barbell")
                                }
                            }
                            item {
                                ArrowSwitcher(
                                    // FIXME: should get imperial system
                                    items = List(workoutState.barbellNames.size) { i -> "${workoutState.barbellNames[i]} (${workoutState.barbellSizes[i]} ${if (workoutState.imperialSystem) "lb" else "kg"})" },
                                    currentIndex = workoutState.tareIndex,
                                    onIndexChanged = { index ->
                                        viewModel.onEvent(WorkoutEvent.ChangeTare(index))
                                    }
                                )
                            }
                        }
                        if (workoutState.nextExerciseName.isNotBlank()) {
                            item {
                                ListHeader {
                                    Text(
                                        "Next exercise: ",
                                        style = MaterialTheme.typography.labelMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            item {
                                Text(
                                    workoutState.nextExerciseName,
                                    style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Transparent),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Waiting for exercise data...",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        viewModel.onEvent(WorkoutEvent.StopActivity)
                        navigator.navigateUp()
                    }) {
                        Text(stringResource(R.string.go_back))
                    }
                }
            }
        }
    }
}