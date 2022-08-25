package com.anexus.perfectgymcoach.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anexus.perfectgymcoach.data.Repository
import com.anexus.perfectgymcoach.data.exercise.WorkoutExerciseAndInfo
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

data class HomeState(
    val currentPlan: Long? = null,
    val currentProgram: Long? = null,
    val programs: List<WorkoutProgram> = emptyList(),
    val exercisesAndInfo: Map<Long, List<WorkoutExerciseAndInfo>> = emptyMap(),
    val openAddProgramDialogue: Boolean = false
)

sealed class HomeEvent{
//    object ToggleProgramDialogue : HomeEvent()

    object GetProgramsCurrentPlan: HomeEvent()

//    data class AddProgram(val workoutProgram: WorkoutProgram): HomeEvent()

    // TODO: ChangeOrder
    // TODO: RemovePlan
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = mutableStateOf(HomeState())
    val state: State<HomeState> = _state

    private var collectProgramsJob: Job? = null
    private var getWorkoutExercisesJob: Job? = null

    init {
        viewModelScope.launch {
            repository.getCurrentPlan().collect { currentPlan ->
                _state.value = state.value.copy(currentPlan = currentPlan)
                collectProgramsJob?.cancel()
                collectProgramsJob = this.launch {
                    repository.getPrograms(currentPlan?: 0L).collect {
                        _state.value = state.value.copy(
                            programs = it
                        )
                        getWorkoutExercisesJob?.cancel()
                        getWorkoutExercisesJob = this.launch {
                            repository.getWorkoutExercisesAndInfo(it.map { prg -> prg.programId }).collect{ exList ->
                                _state.value = state.value.copy(
                                    exercisesAndInfo = exList.groupBy { ex -> ex.extProgramId }
                                )
                            }
                        }
                    }
                }

            }
        }
        viewModelScope.launch {
            repository.getCurrentProgram().collect{
                _state.value = state.value.copy(currentProgram = it)
            }
        }
    }

    fun onEvent(event: HomeEvent){
        when(event){
            is HomeEvent.GetProgramsCurrentPlan -> {
//                if (state.value.currentPlan != null) {
//                    collectProgramsJob?.cancel()
//                    collectProgramsJob = viewModelScope.launch {
//                        repository.getPrograms(state.value.currentPlan!!).collect{
//
//                        }
//                    }
//                }
            }
        }
    }

}
