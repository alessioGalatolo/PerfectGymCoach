package agdesigns.elevatefitness.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.exercise.ProgramExerciseAndInfo
import agdesigns.elevatefitness.data.workout_program.WorkoutProgram
import agdesigns.elevatefitness.data.workout_program.WorkoutProgramRename
import agdesigns.elevatefitness.data.workout_program.WorkoutProgramReorder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgramsState(
    val programs: List<WorkoutProgram> = emptyList(),
    val exercisesAndInfo: Map<Long, List<ProgramExerciseAndInfo>> = emptyMap(),
    val openAddProgramDialog: Boolean = false,
    val openChangeNameDialog: Boolean = false,
    val programToBeChanged: Long = 0
)

sealed class ProgramsEvent{
    data object ToggleAddProgramDialog : ProgramsEvent()

    data class ToggleChangeNameDialog(val programId: Long = 0) : ProgramsEvent()

    data class GetPrograms(val planId: Long): ProgramsEvent()

    data class AddProgram(val workoutProgram: WorkoutProgram): ProgramsEvent()

    data class RenameProgram(val workoutProgramRename: WorkoutProgramRename): ProgramsEvent()

    data class ReorderProgram(val workoutProgramReorders: List<WorkoutProgramReorder>): ProgramsEvent()

    data class DeleteProgram(val programId: Long): ProgramsEvent()

}

@HiltViewModel
class ProgramsViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = MutableStateFlow(ProgramsState())
    val state: StateFlow<ProgramsState> = _state.asStateFlow()

    private var getProgramsJob: Job? = null
    private var getProgramExercisesJob: Job? = null

    fun onEvent(event: ProgramsEvent){
        when (event) {
            is ProgramsEvent.GetPrograms -> {
                getProgramsJob?.cancel()
                getProgramsJob = viewModelScope.launch {
                    repository.getPrograms(event.planId).collect { programs ->
                        _state.update { it.copy(
                            programs = programs.sortedBy { prog -> prog.orderInWorkoutPlan }
                        ) }
                        getProgramExercisesJob?.cancel()
                        getProgramExercisesJob = this.launch {
                            repository.getProgramExercisesAndInfo(programs.map { prg -> prg.programId }).collect{ exList ->
                                _state.update { it.copy(
                                    exercisesAndInfo = exList.groupBy { ex -> ex.extProgramId }  // FIXME: should sort each list
                                ) }
                            }
                        }
                    }
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
                    repository.removeProgramFromPlan(event.programId)
                }
            }
        }
    }

}
