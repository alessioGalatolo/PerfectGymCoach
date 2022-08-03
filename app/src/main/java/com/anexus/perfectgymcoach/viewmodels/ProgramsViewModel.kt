package com.anexus.perfectgymcoach.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import com.anexus.perfectgymcoach.data.Repository
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgramsState(
    val programs: List<WorkoutProgram> = emptyList(),
    val exercises: List<List<WorkoutExercise>> = emptyList(),
    val openAddProgramDialogue: Boolean = false
)

sealed class ProgramsEvent{
    object ToggleProgramDialogue : ProgramsEvent()

    data class GetPrograms(val planId: Long): ProgramsEvent()

    data class AddProgram(val workoutProgram: WorkoutProgram): ProgramsEvent()

    // TODO: ChangeOrder
    // TODO: RemovePlan
}

@HiltViewModel
class ProgramsViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = mutableStateOf(ProgramsState())
    val state: State<ProgramsState> = _state

    private var getProgramsJob: Job? = null

    fun onEvent(event: ProgramsEvent){
        when (event) {
            is ProgramsEvent.GetPrograms -> {
                getProgramsJob?.cancel()
                getProgramsJob = viewModelScope.launch {
                    repository.getPrograms(event.planId).collect {
                        _state.value = state.value.copy(
                            programs = it.keys.toList(),
                            exercises = it.values.toList()
                        )
                    }
                }
            }
            is ProgramsEvent.AddProgram -> {
                viewModelScope.launch {
                    repository.addProgram(event.workoutProgram)
                }
            }
            is ProgramsEvent.ToggleProgramDialogue -> {
                _state.value = state.value.copy(
                    openAddProgramDialogue = !state.value.openAddProgramDialogue
                )
            }

        }
    }

}
