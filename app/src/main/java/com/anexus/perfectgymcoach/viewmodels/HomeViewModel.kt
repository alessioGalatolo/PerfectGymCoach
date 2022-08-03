package com.anexus.perfectgymcoach.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import com.anexus.perfectgymcoach.data.Repository
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class HomeState(
    val currentPlan: Long? = null,
    val programs: List<WorkoutProgram> = emptyList(),
    val exercises: List<List<WorkoutExercise>> = emptyList(),
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

    init {
        viewModelScope.launch {
            repository.getCurrentPlan().collect { currentPlan ->
                _state.value = state.value.copy(currentPlan = currentPlan)
                collectProgramsJob?.cancel()
                collectProgramsJob = this.launch {
                    repository.getPrograms(currentPlan?: 0L).collect {
                        _state.value = state.value.copy(
                            programs = it.keys.toList(),
                            exercises = it.values.toList()
                        )
                    }
                }

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
