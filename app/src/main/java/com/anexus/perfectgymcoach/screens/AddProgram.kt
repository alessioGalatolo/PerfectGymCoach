package com.anexus.perfectgymcoach.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import com.anexus.perfectgymcoach.viewmodels.ProgramsEvent
import com.anexus.perfectgymcoach.viewmodels.ProgramsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProgram(navController: NavHostController, name: String, planId: Long,
               openDialogNow: Boolean,
               viewModel: ProgramsViewModel = hiltViewModel()) {
    viewModel.onEvent(ProgramsEvent.GetPrograms(planId))
    InsertNameDialog(
        prompt = "Name of the new program",
        dialogueIsOpen = viewModel.state.value.openAddProgramDialog,
        toggleDialogue = { viewModel.onEvent(ProgramsEvent.ToggleAddProgramDialog) },
        insertName = { programName ->
            viewModel.onEvent(ProgramsEvent.AddProgram(WorkoutProgram(
                extPlanId = planId,
                name = programName
            ))) }
    )
    InsertNameDialog(
        prompt = "New name of the program",
        dialogueIsOpen = viewModel.state.value.openChangeNameDialog,
        toggleDialogue = { viewModel.onEvent(ProgramsEvent.ToggleChangeNameDialog()) },
        insertName = { viewModel.onEvent(ProgramsEvent.RenameProgram(
            WorkoutProgram(
                programId = viewModel.state.value.programToBeChanged,
                extPlanId = planId,
                name = it
            )
        )) }
    )
    val openDialog = rememberSaveable { mutableStateOf(openDialogNow) }
    if (openDialog.value){
        viewModel.onEvent(ProgramsEvent.ToggleAddProgramDialog)
        openDialog.value = false
    }
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
                    viewModel.onEvent(ProgramsEvent.ToggleAddProgramDialog)
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
                        WorkoutCard(
                            programEntry,
                            viewModel.state.value.exercises[index],
                            onCardClick = {
                                navController.navigate(
                                    "${MainScreen.AddExercise.route}/" +
                                            "${programEntry.name}/" +
                                            "${programEntry.programId}"
                                )
                            }, onCardLongPress = {
                                viewModel.onEvent(ProgramsEvent.ToggleChangeNameDialog(programEntry.programId))
                            }, navController = navController
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        })
}

