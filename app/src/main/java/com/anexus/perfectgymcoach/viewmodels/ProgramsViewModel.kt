package com.anexus.perfectgymcoach.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anexus.perfectgymcoach.data.Repository
import com.anexus.perfectgymcoach.data.exercise.ProgramExerciseAndInfo
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgramRename
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgramReorder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    object ToggleAddProgramDialog : ProgramsEvent()

    data class ToggleChangeNameDialog(val programId: Long = 0) : ProgramsEvent()

    data class GetPrograms(val planId: Long): ProgramsEvent()

    data class AddProgram(val workoutProgram: WorkoutProgram): ProgramsEvent()

    data class RenameProgram(val workoutProgramRename: WorkoutProgramRename): ProgramsEvent()

    data class ReorderProgram(val workoutProgramReorders: List<WorkoutProgramReorder>): ProgramsEvent()

    data class DeleteProgram(val programId: Long): ProgramsEvent()

}

@HiltViewModel
class ProgramsViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = mutableStateOf(ProgramsState())
    val state: State<ProgramsState> = _state

    private var getProgramsJob: Job? = null
    private var getWorkoutExercisesJob: Job? = null

    fun onEvent(event: ProgramsEvent){
        when (event) {
            is ProgramsEvent.GetPrograms -> {
                getProgramsJob?.cancel()
                getProgramsJob = viewModelScope.launch {
                    repository.getPrograms(event.planId).collect {
                        _state.value = state.value.copy(
                            programs = it.sortedBy { prog -> prog.orderInWorkoutPlan }
                        )
                        getWorkoutExercisesJob?.cancel()
                        getWorkoutExercisesJob = this.launch {
                            repository.getProgramExercisesAndInfo(it.map { prg -> prg.programId }).collect{ exList ->
                                _state.value = state.value.copy(
                                    exercisesAndInfo = exList.groupBy { ex -> ex.extProgramId }  // FIXME: should sort each list
                                )
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
                _state.value = state.value.copy(
                    openAddProgramDialog = !state.value.openAddProgramDialog
                )
            }
            is ProgramsEvent.ToggleChangeNameDialog -> {
                _state.value = state.value.copy(
                    openChangeNameDialog = !state.value.openChangeNameDialog,
                    programToBeChanged = event.programId
                )

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
                    repository.deleteProgram(event.programId)
                }
            }
        }
    }

}
