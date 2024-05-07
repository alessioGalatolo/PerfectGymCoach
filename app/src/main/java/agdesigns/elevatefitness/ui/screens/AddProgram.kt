package agdesigns.elevatefitness.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.workout_program.WorkoutProgram
import agdesigns.elevatefitness.data.workout_program.WorkoutProgramRename
import agdesigns.elevatefitness.data.workout_program.WorkoutProgramReorder
import agdesigns.elevatefitness.ui.ChangePlanGraph
import agdesigns.elevatefitness.ui.components.InsertNameDialog
import agdesigns.elevatefitness.ui.components.WorkoutCard
import agdesigns.elevatefitness.viewmodels.ProgramsEvent
import agdesigns.elevatefitness.viewmodels.ProgramsViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.AddProgramExerciseDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.android.awaitFrame

@Destination<ChangePlanGraph>
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddProgram(
    navigator: DestinationsNavigator,
    planName: String,
    planId: Long,
    openDialogNow: Boolean = false,
    viewModel: ProgramsViewModel = hiltViewModel()
) {
    viewModel.onEvent(ProgramsEvent.GetPrograms(planId))
    InsertNameDialog(
        prompt = "Name of the new program",
        dialogueIsOpen = viewModel.state.value.openAddProgramDialog,
        toggleDialog = { viewModel.onEvent(ProgramsEvent.ToggleAddProgramDialog) },
        insertName = { programName ->
            viewModel.onEvent(ProgramsEvent.AddProgram(WorkoutProgram(
                extPlanId = planId,
                name = programName,
                orderInWorkoutPlan = viewModel.state.value.programs.size
            ))) }
    )
    InsertNameDialog(
        prompt = "New name of the program",
        dialogueIsOpen = viewModel.state.value.openChangeNameDialog,
        toggleDialog = { viewModel.onEvent(ProgramsEvent.ToggleChangeNameDialog()) },
        insertName = { viewModel.onEvent(ProgramsEvent.RenameProgram(
            WorkoutProgramRename(
                programId = viewModel.state.value.programToBeChanged,
                name = it
            )
        )) }
    )
    val openDialog = rememberSaveable { mutableStateOf(openDialogNow) }
    LaunchedEffect(openDialog.value) {
        if (openDialog.value){
            awaitFrame()
            awaitFrame()
            viewModel.onEvent(ProgramsEvent.ToggleAddProgramDialog)
            openDialog.value = false
        }
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
        rememberTopAppBarState()
    )
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(planName) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }, floatingActionButton = {
            LargeFloatingActionButton (
                modifier = Modifier.navigationBarsPadding(),
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
                        imageVector = Icons.Outlined.Description,
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
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = innerPadding
                ) {
                    itemsIndexed(items = viewModel.state.value.programs, key = { _, it -> it.programId }) { index, programEntry ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .animateItemPlacement(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column (horizontalAlignment = Alignment.CenterHorizontally){
                                IconButton(onClick = {
                                    viewModel.onEvent(ProgramsEvent.ReorderProgram(listOf(
                                        WorkoutProgramReorder(programEntry.programId, programEntry.orderInWorkoutPlan-1),
                                        WorkoutProgramReorder(viewModel.state.value.programs[index-1].programId, programEntry.orderInWorkoutPlan)
                                    )))
                                }, enabled = index > 0) {
                                    Icon(Icons.Default.ArrowUpward, "Move program up (changing its order in the plan)")
                                }
                                Text("Day", fontWeight = FontWeight.SemiBold, fontStyle = FontStyle.Italic)
                                Text("${programEntry.orderInWorkoutPlan+1}",
                                    fontWeight = FontWeight.SemiBold, fontStyle = FontStyle.Italic)
                                IconButton(onClick = {
                                    viewModel.onEvent(ProgramsEvent.ReorderProgram(listOf(
                                        WorkoutProgramReorder(programEntry.programId, programEntry.orderInWorkoutPlan+1),
                                        WorkoutProgramReorder(viewModel.state.value.programs[index+1].programId, programEntry.orderInWorkoutPlan)
                                    ))) }, enabled = index+1 < viewModel.state.value.programs.size) {
                                    Icon(Icons.Default.ArrowDownward, "Move program down (changing its order in the plan)")
                                }
                            }
                            WorkoutCard(
                                navigator = navigator,
                                program = programEntry,
                                viewModel.state.value.exercisesAndInfo[programEntry.programId]
                                    ?: emptyList(),
                                onCardClick = {
                                    navigator.navigate(
                                        AddProgramExerciseDestination(
                                          programName = programEntry.name,
                                          programId = programEntry.programId
                                        ),
                                        onlyIfResumed = true
                                    )
                                }, onRename = {
                                    viewModel.onEvent(
                                        ProgramsEvent.ToggleChangeNameDialog(
                                            programEntry.programId
                                        )
                                    )
                                }, onDelete = {
                                    viewModel.onEvent(ProgramsEvent.DeleteProgram(programEntry.programId))
                                },
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        }
                    }
                    item{
                        var finalSpacerSize = 96.dp + 8.dp // large fab size + its padding FIXME: not hardcode
                        finalSpacerSize += 8.dp
                        Spacer(modifier = Modifier.navigationBarsPadding())
                        Spacer(Modifier.height(finalSpacerSize))
                    }
                }
            }
        })
}

