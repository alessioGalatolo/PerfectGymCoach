package com.anexus.perfectgymcoach.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import com.anexus.perfectgymcoach.viewmodels.ProgramsEvent
import com.anexus.perfectgymcoach.viewmodels.ProgramsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProgram(navController: NavHostController, name: String, planId: Int,
               viewModel: ProgramsViewModel = hiltViewModel()) {
    viewModel.onEvent(ProgramsEvent.GetPrograms(planId))
    AddNameDialogue(
        name = "program",
        dialogueIsOpen = viewModel.state.value.openAddProgramDialogue,
        toggleDialogue = { viewModel.onEvent(ProgramsEvent.ToggleProgramDialogue) },
        addName = { programName ->
            viewModel.onEvent(ProgramsEvent.AddProgram(WorkoutProgram(
                extPlanId = planId,
                name = programName
            ))) }
    )
    Scaffold(
        topBar = {
            SmallTopAppBar(title = { Text(name) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                })
        }, floatingActionButton = {
            LargeFloatingActionButton (
                onClick = {
                    viewModel.onEvent(ProgramsEvent.ToggleProgramDialogue)
                },
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add program",
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                )
            }
        }, content = { innerPadding ->
            if (viewModel.state.value.programs.isEmpty()) {
                // if you have no programs
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = "",
                        modifier = Modifier.size(160.dp)
                    )
                    Text(
                        stringResource(id = R.string.empty_programs),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                // if you have some programs
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = innerPadding
                ) {
                    itemsIndexed(viewModel.state.value.programs, key = { _, it -> it }) { index, programEntry ->
                        ElevatedCard(
                            onClick = {
                                navController.navigate(
                                    "${MainScreen.AddExercise.route}/" +
                                            "${programEntry.name}/" +
                                            "${programEntry.programId}"
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Column (){
                                Image(
                                    painter = painterResource(R.drawable.sample_image),
                                    contentDescription = "Contact profile picture",
                                    alignment = Center,
                                    modifier = Modifier
                                        // Set image size to 40 dp
                                        .fillMaxWidth()
//                                        .size(160.dp)
                                        .align(CenterHorizontally)
                                        // Clip image to be shaped as a circle
                                        .clip(AbsoluteRoundedCornerShape(12.dp))
                                )

                                Text(text = programEntry.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                viewModel.state.value.exercises[index].forEach {
                                    Text(text = it.name,
                                        modifier = Modifier.padding(horizontal = 8.dp))
                                    Text(text = "Sets: ${it.sets} Reps: ${it.reps} Rest: ${it.rest}s",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(horizontal = 8.dp))
                                }// TODO
                                Button(onClick = { /*TODO*/ },
                                    modifier = Modifier.padding(8.dp)) {
                                    Text("Start workout")
                                }

                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        })
}

