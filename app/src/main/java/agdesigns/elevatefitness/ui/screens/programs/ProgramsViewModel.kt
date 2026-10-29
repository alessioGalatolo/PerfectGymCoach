package agdesigns.elevatefitness.ui.screens.programs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseAndInfo
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanUpdateProgram
import agdesigns.elevatefitness.data.db.entity.WorkoutProgram
import agdesigns.elevatefitness.data.db.entity.WorkoutProgramRename
import agdesigns.elevatefitness.data.db.entity.WorkoutProgramReorder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgramsState(
    val programs: List<WorkoutProgram> = emptyList(),
    val planName: String = "",
    val exercisesAndInfo: Map<Long, List<ProgramExerciseAndInfo>> = emptyMap(),
    val openAddProgramDialog: Boolean = false,
    val openChangeNameDialog: Boolean = false,
    val programToBeChanged: Long = 0
)

sealed class ProgramsEvent{
    data object ToggleAddProgramDialog : ProgramsEvent()

    data class ToggleChangeNameDialog(val programId: Long = 0) : ProgramsEvent()

    data class InitProgramView(val planId: Long): ProgramsEvent()

    data class AddProgram(val workoutProgram: WorkoutProgram): ProgramsEvent()

    data class RenameProgram(val workoutProgramRename: WorkoutProgramRename): ProgramsEvent()

    data class ReorderProgram(val workoutProgramReorders: List<WorkoutProgramReorder>): ProgramsEvent()

    data class DeleteProgram(val programId: Long): ProgramsEvent()

    data class DuplicateProgram(val programId: Long): ProgramsEvent()

}

@HiltViewModel
class ProgramsViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = MutableStateFlow(ProgramsState())
    val state: StateFlow<ProgramsState> = _state.asStateFlow()

    private var getProgramsJob: Job? = null
    private var getProgramExercisesJob: Job? = null

    val reorderCompleted = Channel<Boolean>()

    fun onEvent(event: ProgramsEvent){
        when (event) {
            is ProgramsEvent.InitProgramView -> {
                getProgramsJob?.cancel()
                getProgramsJob = viewModelScope.launch {
                    combine(
                        repository.getPlan(event.planId),
                        repository.getPrograms(event.planId)
                    ) { plan, programs ->
                        _state.update { it.copy(
                            programs = programs.sortedBy { prog -> prog.orderInWorkoutPlan },
                            planName = plan?.name ?: ""
                        ) }
                        reorderCompleted.trySend(true)
                        getProgramExercisesJob?.cancel()
                        getProgramExercisesJob = this.launch {
                            repository.getProgramExercisesAndInfo(programs.map { prg -> prg.programId }).collect{ exList ->
                                _state.update { it.copy(
                                    exercisesAndInfo = exList.groupBy { ex -> ex.extProgramId }
                                        .mapValues { entry -> entry.value.sortedBy { ex -> ex.orderInProgram } }
                                ) }
                            }
                        }

                    }.collect()
                }
            }
            is ProgramsEvent.AddProgram -> {
                viewModelScope.launch {
                    repository.addProgram(event.workoutProgram)
                }
            }
            is ProgramsEvent.ToggleAddProgramDialog -> {
                _state.update { it.copy(
                    openAddProgramDialog = !state.value.openAddProgramDialog
                ) }
            }
            is ProgramsEvent.ToggleChangeNameDialog -> {
                _state.update { it.copy(
                    openChangeNameDialog = !state.value.openChangeNameDialog,
                    programToBeChanged = event.programId
                ) }

            }
            is ProgramsEvent.RenameProgram -> {
                viewModelScope.launch {
                    repository.renameProgram(event.workoutProgramRename)
                }
            }
            is ProgramsEvent.ReorderProgram -> {
                viewModelScope.launch {
                    repository.reorderPrograms(event.workoutProgramReorders)
                }
            }
            is ProgramsEvent.DeleteProgram -> {
                viewModelScope.launch {
                    // keep currentProgram pointing to the same program (or to the one following
                    // the deleted one), as the programs after the deleted one shift back by one
                    val plan = repository.getPlan(state.value.programs[0].extPlanId!!).first()
                    val program = state.value.programs.first { it.programId == event.programId }
                    val deletedOrder = program.orderInWorkoutPlan
                    val remainingPrograms = state.value.programs.size - 1
                    if (plan != null) {
                        val newCurrentProgram = when {
                            remainingPrograms <= 0 -> 0
                            plan.currentProgram > deletedOrder -> plan.currentProgram - 1
                            // the following program takes the deleted one's place, wrap if it was last
                            plan.currentProgram == deletedOrder -> deletedOrder % remainingPrograms
                            else -> plan.currentProgram
                        }
                        if (newCurrentProgram != plan.currentProgram) {
                            repository.updateCurrentPlan(WorkoutPlanUpdateProgram(
                                planId = plan.planId,
                                currentProgram = newCurrentProgram
                            ))
                        }
                    }
                    repository.removeProgramFromPlan(event.programId)
                    // reorder programs after this one
                    val programs2reorder = state.value.programs.filter { it.orderInWorkoutPlan > program.orderInWorkoutPlan }
                    repository.reorderPrograms(
                        programs2reorder.map {
                            WorkoutProgramReorder(it.programId, it.orderInWorkoutPlan-1)
                        }
                    )
                }
            }
            is ProgramsEvent.DuplicateProgram -> {
                viewModelScope.launch {
                    repository.duplicateProgram(event.programId)
                }
            }
        }
    }

}
