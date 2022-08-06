package com.anexus.perfectgymcoach.screens

import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import com.anexus.perfectgymcoach.viewmodels.WorkoutEvent
import com.anexus.perfectgymcoach.viewmodels.WorkoutViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Workout(navController: NavHostController, programId: Long,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    viewModel.onEvent(WorkoutEvent.GetWorkoutExercises(programId))
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        decayAnimationSpec,
        rememberTopAppBarState()
    )
    var currentExercise: WorkoutExercise? = null
    if (viewModel.state.value.currentExercise != null){
        currentExercise = viewModel.state.value.workoutExercises[viewModel.state.value.currentExercise!!]
    }
    Box {

        Scaffold(
            containerColor = Transparent,
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                CenterAlignedTopAppBar(title = {
                    Box (Modifier.fillMaxWidth().wrapContentHeight()) {
//                        Image(
//                            painterResource(id = R.drawable.sample_image),
//                            null,
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .clip(AbsoluteRoundedCornerShape(12.dp))
//                        ) // TODO: image of exercise
                        Text(currentExercise?.name ?: "")
                    }
                },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Transparent, scrolledContainerColor = Transparent),
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "App settings"
                            )
                        }
                    })
            }, content = { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {

                    repeat(20) {
                        Text("Some very long text", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }, bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .wrapContentHeight(CenterVertically)
                ) {
                    val reps = remember { mutableStateOf(currentExercise?.reps ?: 0) } // fixme
                    TextFieldWithButtons(
                        "Reps",
                        initialValue = reps,
                        increment = 1
                    )
                    TextFieldWithButtons(
                        "Weight",
                        initialValue = mutableStateOf(0),
                        2
                    ) // FIXME: equipment2increment[currentExercise?]
                }
            })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextFieldWithButtons(prompt: String, initialValue: MutableState<Int> = mutableStateOf(0), increment: Int) {
    Row(verticalAlignment = CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        var text by remember { mutableStateOf("${initialValue.value}") }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            label = { Text(prompt) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.widthIn(1.dp, Dp.Infinity)
        )
        IconButton(onClick = { text = "${text.toInt() - increment}" }) {
            Icon(Icons.Filled.Remove, null)
        }
        IconButton(onClick = { text = "${text.toInt() + increment}" }) {
            Icon(Icons.Filled.Add, null)
        }
    }
}
