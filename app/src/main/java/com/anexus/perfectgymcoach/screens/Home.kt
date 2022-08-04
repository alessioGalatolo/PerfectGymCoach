package com.anexus.perfectgymcoach.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.viewmodels.HomeEvent
import com.anexus.perfectgymcoach.viewmodels.HomeViewModel
import com.anexus.perfectgymcoach.viewmodels.ProgramsViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(navController: NavHostController,
         viewModel: HomeViewModel = hiltViewModel()
         ) {

    if (viewModel.state.value.currentPlan == null) {
        Scaffold(
            floatingActionButton = {
                LargeFloatingActionButton(
                    onClick = { navController.navigate("${MainScreen.ChangePlan.route}/${true}") }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize)
                    )
                }
            }) {
            Column(modifier = Modifier.padding(it)) {
                Text(
                    stringResource(id = R.string.empty_home),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    } else {
        if (viewModel.state.value.programs.isEmpty()) {
            Column (horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = "",
                    modifier = Modifier.size(160.dp)
                )
                Text(
                    stringResource(id = R.string.empty_home_program),
                    modifier = Modifier.padding(16.dp)
                )
                Button(onClick = {
                    navController.navigate( // FIXME: empty plan name
                        "${MainScreen.AddProgram.route}/ /${viewModel.state.value.currentPlan!!}/${true}")
                }) {
                    Text(stringResource(id = R.string.add_program))
                }

            }
        } else {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)) {
                // Coming next
                Text(text = stringResource(id = R.string.coming_next), fontWeight = FontWeight.Bold)
                val currentProgram = viewModel.state.value.programs.find {
                    it.programId == viewModel.state.value.currentProgram!!
                }!!
                val currentExercises =
                    viewModel.state.value.exercises[viewModel.state.value.programs.indexOf(
                        currentProgram
                    )]
                WorkoutCard(
                    program = currentProgram,
                    exercises = currentExercises,
                    onCardClick = { navController.navigate(MainScreen.Workout.route) },
                    onCardLongPress = {
                        navController.navigate(
                            "${MainScreen.AddExercise.route}/" +
                                    "${currentProgram.name}/" +
                                    "${currentProgram.programId}"
                        )
                    }, navController = navController
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.other_programs),
                    fontWeight = FontWeight.Bold
                )
                repeat(6) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.full_body),
                            contentDescription = "Contact profile picture",
                            modifier = Modifier
                                // Set image size to 40 dp
                                .size(60.dp)
                                .padding(all = 4.dp)
                                // Clip image to be shaped as a circle
                                .clip(CircleShape)
                        )

                        // Add a horizontal space between the image and the column
                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.align(Alignment.CenterVertically)) {
                            Text(text = "msg.author")
                            // Add a vertical space between the author and message texts
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "msg.body")
                        }
                    }
//            Divider()

                    Spacer(modifier = Modifier.height(4.dp))
                }
                TextButton(
                    onClick = { navController.navigate("${MainScreen.ChangePlan.route}/${false}" ) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) { Text(stringResource(R.string.change_workout_plan)) }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}