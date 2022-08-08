package com.anexus.perfectgymcoach.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.viewmodels.ExercisesEvent
import com.anexus.perfectgymcoach.viewmodels.ExercisesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExercise(navController: NavHostController, programName: String, programId: Long,
                viewModel: ExercisesViewModel = hiltViewModel()) {
    viewModel.onEvent(ExercisesEvent.GetWorkoutExercises(programId))
    Scaffold(
        topBar = {
            SmallTopAppBar(title = { Text(programName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                }, modifier = Modifier.statusBarsPadding())
        }, floatingActionButton = {
            LargeFloatingActionButton (
                onClick = {
                    navController.navigate("${MainScreen.ExercisesByMuscle.route}/$programName/$programId")
                },
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add exercise",
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                )
            }
        }, content = { innerPadding ->
            if (viewModel.state.value.workoutExercises.isEmpty()) {
                // if you have no exercises
                Column(
                    modifier = Modifier.padding(innerPadding).fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.FitnessCenter,
                        contentDescription = "",
                        modifier = Modifier.size(160.dp)
                    )
                    Text(
                        stringResource(id = R.string.empty_exercises),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                // if you have some plans
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = innerPadding
                ) {
                    items(items = viewModel.state.value.workoutExercises, key = { it }) { exercise ->
                        Card(
                            onClick = {
//                                    navController.navigate(
//                                        "${MainScreen.AddExercise.route}/${program.name}/${program.id}"
//                                    )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Row {
                                Image(
                                    painter = painterResource(R.drawable.full_body),
                                    contentDescription = "Contact profile picture",
                                    modifier = Modifier
                                        // Set image size to 40 dp
                                        .size(40.dp)
                                        .padding(all = 4.dp)
                                        // Clip image to be shaped as a circle
                                        .clip(CircleShape)
                                )

                                // Add a horizontal space between the image and the column
//                Spacer(modifier = Modifier.width(8.dp))

                                Column {
                                    Text(text = exercise.name)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Sets: ${exercise.sets} " +
                                            "Reps: ${exercise.reps} " +
                                            "Rest: ${exercise.rest}") // TODO
                                }
                            }
                        }
                    }
                }
            }
        })
}
