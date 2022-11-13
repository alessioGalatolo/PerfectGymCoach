package com.anexus.perfectgymcoach.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anexus.perfectgymcoach.data.Repository
import com.anexus.perfectgymcoach.data.exercise.ProgramExerciseAndInfo
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.CancellationException
import javax.inject.Inject

data class HomeState(
    val currentPlan: Long? = 0,
    val currentProgram: Int? = null,
    val programs: List<WorkoutProgram>? = null,
    val exercisesAndInfo: Map<Long, List<ProgramExerciseAndInfo>> = emptyMap(),
    val openAddProgramDialogue: Boolean = false,
    val currentWorkout: Long? = null,
    val animationTick: Int = 0
)

sealed class HomeEvent{
    object ResetCurrentWorkout: HomeEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = mutableStateOf(HomeState())
    val state: State<HomeState> = _state

    private var collectProgramsJob: Job? = null
    private var collectCurrentProgram: Job? = null
    private var getProgramExercisesJob: Job? = null
    private var animateJob: Job? = null

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
                            getProgramExercisesJob?.cancel()
                            getProgramExercisesJob = this.launch {
                                repository.getProgramExercisesAndInfo(it.map { prg -> prg.programId })
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
        viewModelScope.launch {
            repository.getCurrentWorkout().collect{
                _state.value = state.value.copy(
                    currentWorkout = it
                )
            }
        }
        animateJob?.cancel(CancellationException("Duplicate call"))
        animateJob = flow {
            var counter = 0
            while (true) {
                emit(counter++)
                delay(2000)
            }
        }.onEach {_state.value = state.value.copy(animationTick = state.value.animationTick+1)}
            .launchIn(viewModelScope)
    }

    fun onEvent(event: HomeEvent){
        when(event){
            is HomeEvent.ResetCurrentWorkout -> {
                viewModelScope.launch {
                    repository.setCurrentWorkout(null)
                }
            }
        }
    }

}
