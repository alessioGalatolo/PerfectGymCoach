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
    val currentPlan: Long? = 0,
    val currentProgram: Int? = null,
    val programs: List<WorkoutProgram>? = null,
    val exercisesAndInfo: Map<Long, List<WorkoutExerciseAndInfo>> = emptyMap(),
    val openAddProgramDialogue: Boolean = false
)

sealed class HomeEvent{

}

@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = mutableStateOf(HomeState())
    val state: State<HomeState> = _state

    private var collectProgramsJob: Job? = null
    private var collectCurrentProgram: Job? = null
    private var getWorkoutExercisesJob: Job? = null

    init {
        viewModelScope.launch {
            repository.getCurrentPlan().collect { currentPlan ->
                _state.value = state.value.copy(currentPlan = currentPlan)
                if (currentPlan != null) {
                    collectCurrentProgram?.cancel()
                    collectCurrentProgram = this.launch {
                        repository.getPlan(currentPlan).collect {
                            _state.value = state.value.copy(currentProgram = it.currentProgram)
                        }
                    }
                    collectProgramsJob?.cancel()
                    collectProgramsJob = this.launch {
                        repository.getPrograms(currentPlan).collect {
                            _state.value = state.value.copy(
                                programs = it.sortedBy { it1 -> it1.orderInWorkoutPlan }
                            )
                            getWorkoutExercisesJob?.cancel()
                            getWorkoutExercisesJob = this.launch {
                                repository.getWorkoutExercisesAndInfo(it.map { prg -> prg.programId })
                                    .collect { exList ->
                                        _state.value = state.value.copy(
                                            exercisesAndInfo = exList.groupBy { ex -> ex.extProgramId }
                                        )
                                    }
                            }
                        }
                    }
                }

            }
        }
    }

    fun onEvent(event: HomeEvent){

    }

}
