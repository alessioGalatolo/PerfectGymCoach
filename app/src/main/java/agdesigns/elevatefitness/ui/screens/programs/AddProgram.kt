package agdesigns.elevatefitness.ui.screens.programs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.WorkoutProgram
import agdesigns.elevatefitness.data.db.entity.WorkoutProgramRename
import agdesigns.elevatefitness.data.db.entity.WorkoutProgramReorder
import agdesigns.elevatefitness.data.db.entity.getPlanDisplayName
import agdesigns.elevatefitness.data.db.entity.getProgramDisplayName
import agdesigns.elevatefitness.navigation.ChangePlanGraph
import agdesigns.elevatefitness.navigation.SlideTransition
import agdesigns.elevatefitness.ui.common.EmptyScreenInfo
import agdesigns.elevatefitness.ui.common.InsertNameDialog
import agdesigns.elevatefitness.ui.common.WorkoutCard
import agdesigns.elevatefitness.ui.screens.programs.components.ProgramCard
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.AddProgramExerciseDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.android.awaitFrame
import sh.calvin.reorderable.rememberReorderableLazyListState

@Destination<ChangePlanGraph>(style = SlideTransition::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun AddProgram(
    navigator: DestinationsNavigator,
    planId: Long,
    openDialogNow: Boolean = false,
    viewModel: ProgramsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(planId) {
        viewModel.onEvent(ProgramsEvent.InitProgramView(planId))
    }
    val haptic = LocalHapticFeedback.current

    InsertNameDialog(
        prompt = stringResource(R.string.new_program_prompt),
        dialogueIsOpen = state.openAddProgramDialog,
        toggleDialog = { viewModel.onEvent(ProgramsEvent.ToggleAddProgramDialog) },
        insertName = { programName ->
            viewModel.onEvent(ProgramsEvent.AddProgram(WorkoutProgram(
                extPlanId = planId,
                name = programName,
                orderInWorkoutPlan = state.programs.size
            ))) }
    )
    // rename program
    InsertNameDialog(
        prompt = stringResource(R.string.rename_program_prompt),
        dialogueIsOpen = state.openChangeNameDialog,
        toggleDialog = { viewModel.onEvent(ProgramsEvent.ToggleChangeNameDialog()) },
        oldName = state.programs.firstOrNull {
            it.programId == state.programToBeChanged
        }?.name?.let { getProgramDisplayName(it)},
        insertName = { viewModel.onEvent(ProgramsEvent.RenameProgram(
            WorkoutProgramRename(
                programId = state.programToBeChanged,
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
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = { Text(
                    getPlanDisplayName(state.planName)
                ) },
                navigationIcon = {
                    IconButton(
                        onClick = { navigator.navigateUp() },
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back_icon)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }, floatingActionButton = {
            MediumFloatingActionButton (
                onClick = {
                    viewModel.onEvent(ProgramsEvent.ToggleAddProgramDialog)
                },
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_program),
                    modifier = Modifier.size(FloatingActionButtonDefaults.MediumIconSize)
                )
            }
        }, content = { innerPadding ->
            if (state.programs.isEmpty()) {
                // if you have no programs
                EmptyScreenInfo(
                    Icons.Default.Description,
                    R.string.empty_no_programs,
                    titleRes = R.string.empty_no_programs,
                    subtitleRes = R.string.empty_home_program
                )
            } else {
                // if you have some programs
                val isDragging = remember { mutableStateOf(false) }
                val listState = rememberLazyListState()
                val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
                    val toIndex = state.programs.find { it.programId == to.key }!!.orderInWorkoutPlan
                    val fromIndex = state.programs.find { it.programId == from.key }!!.orderInWorkoutPlan
                    while (viewModel.reorderCompleted.tryReceive().isSuccess);

                    viewModel.onEvent(ProgramsEvent.ReorderProgram(listOf(
                        WorkoutProgramReorder(
                            from.key as Long,
                            toIndex
                        ),
                        WorkoutProgramReorder(
                            to.key as Long,
                            fromIndex
                        )
                    )))
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                    while (viewModel.reorderCompleted.receive()) {
                        // check reorder completed
                        if (state.programs.find { it.programId == from.key }!!.orderInWorkoutPlan == toIndex &&
                            state.programs.find { it.programId == to.key }!!.orderInWorkoutPlan == fromIndex
                        )
                            break
                    }
                }
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = innerPadding
                ) {
                    itemsIndexed(items = state.programs, key = { _, it -> it.programId }) { index, programEntry ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .animateItem(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column (horizontalAlignment = Alignment.CenterHorizontally){
                                IconButton(onClick = {
                                    viewModel.onEvent(ProgramsEvent.ReorderProgram(listOf(
                                        WorkoutProgramReorder(programEntry.programId, programEntry.orderInWorkoutPlan-1),
                                        WorkoutProgramReorder(state.programs[index-1].programId, programEntry.orderInWorkoutPlan)
                                    )))
                                }, enabled = index > 0) {
                                    Icon(Icons.Default.ArrowUpward,
                                        stringResource(R.string.move_program_up_info)
                                    )
                                }
                                Text(stringResource(R.string.day), fontWeight = FontWeight.SemiBold, fontStyle = FontStyle.Italic)
                                Text("${programEntry.orderInWorkoutPlan+1}",
                                    fontWeight = FontWeight.SemiBold, fontStyle = FontStyle.Italic)
                                IconButton(onClick = {
                                    viewModel.onEvent(ProgramsEvent.ReorderProgram(listOf(
                                        WorkoutProgramReorder(programEntry.programId, programEntry.orderInWorkoutPlan+1),
                                        WorkoutProgramReorder(state.programs[index+1].programId, programEntry.orderInWorkoutPlan)
                                    ))) }, enabled = index+1 < state.programs.size) {
                                    Icon(Icons.Default.ArrowDownward,
                                        stringResource(R.string.move_program_down_info)
                                    )
                                }
                            }
                            ProgramCard(
                                navigator = navigator,
                                reorderableState = reorderableState,
                                isDragging = isDragging,
                                program = programEntry,
                                exercises = state.exercisesAndInfo[programEntry.programId]
                                    ?: emptyList(),
                                onCardClick = {
                                    navigator.navigate(
                                        AddProgramExerciseDestination(
                                            programName = programEntry.name,
                                            programId = programEntry.programId
                                        )
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
                                onDuplicate = {
                                    viewModel.onEvent(ProgramsEvent.DuplicateProgram(programEntry.programId))
                                },
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        }
                    }
                    item{
                        var finalSpacerSize = 80.dp + 16.dp // large fab size + its padding FIXME: not hardcode
                        finalSpacerSize += 8.dp
                        Spacer(Modifier.height(finalSpacerSize))
                    }
                }
            }
        })
}

