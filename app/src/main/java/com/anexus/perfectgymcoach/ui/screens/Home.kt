package com.anexus.perfectgymcoach.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.ui.MainScreen
import com.anexus.perfectgymcoach.ui.components.WorkoutCard
import com.anexus.perfectgymcoach.viewmodels.HomeViewModel


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun Home(navController: NavHostController,
         viewModel: HomeViewModel = hiltViewModel()
         ) {
    val haptic = LocalHapticFeedback.current

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
    } else if (viewModel.state.value.programs?.isEmpty() == true) {
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
            TextButton(
                onClick = { navController.navigate("${MainScreen.ChangePlan.route}/${false}" ) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text(stringResource(R.string.change_workout_plan)) }
            Spacer(modifier = Modifier.height(8.dp))

        }
    } else if (viewModel.state.value.programs?.isNotEmpty() == true
        && viewModel.state.value.currentProgram != null
    ) {
        LazyColumn(Modifier.padding(horizontal = 16.dp)){
            val currentProgram = viewModel.state.value.programs!![viewModel.state.value.currentProgram!!]
            val currentExercises =
                viewModel.state.value.exercisesAndInfo[currentProgram.programId]?.sortedBy {
                    it.workoutExerciseId
                } ?: emptyList()
            item{
                // Coming next
                Text(text = stringResource(id = R.string.coming_next), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                WorkoutCard(
                    program = currentProgram,
                    exercises = currentExercises,
                    // TODO: add message when no exercises in the program
                    onCardClick = { navController.navigate("${MainScreen.Workout.route}/${currentProgram.programId}/${false}") },
                    onCardLongPress = {
                        navController.navigate(
                            "${MainScreen.AddExercise.route}/" +
                                    "${currentProgram.name}/" +
                                    "${currentProgram.programId}"
                        )
                    }, navController = navController
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (viewModel.state.value.programs!!.size > 1) {
                    Text(
                        text = stringResource(id = R.string.other_programs),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            items(items = viewModel.state.value.programs!!.minus(currentProgram), key = { it }){
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .combinedClickable(onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            navController.navigate(
                                "${MainScreen.AddExercise.route}/" +
                                        "${it.name}/" +
                                        "${it.programId}"
                            )
                        }) {
                            navController.navigate("${MainScreen.Workout.route}/${it.programId}/${false}")
                        }
                ) {
                    Image(
                        painter = painterResource(R.drawable.full_body),
                        contentDescription = "Contact profile picture",
                        modifier = Modifier
                            // Set image size to 40 d
                            .size(60.dp)
                            .padding(all = 4.dp)
                            // Clip image to be shaped as a circle
                            .clip(CircleShape)
                    )

                    Column(modifier = Modifier.align(Alignment.CenterVertically)) {
                        Text(text = it.name)
                    }
                    IconButton(onClick = {
                        navController.navigate("${MainScreen.Workout.route}/${it.programId}/${true}")
                    }) {
                        Icon(Icons.Default.RocketLaunch, null)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            item {
                Column (horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()){
                    TextButton(
                        onClick = { navController.navigate("${MainScreen.ChangePlan.route}/${false}") },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) { Text(stringResource(R.string.change_workout_plan)) }
                    TextButton(onClick = {
                        navController.navigate( // FIXME: empty plan name
                            "${MainScreen.AddProgram.route}/ /${viewModel.state.value.currentPlan!!}/${true}")
                    }) {
                        Text(stringResource(id = R.string.add_program))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}