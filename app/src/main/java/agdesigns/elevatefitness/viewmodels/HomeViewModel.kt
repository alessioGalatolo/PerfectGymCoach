package agdesigns.elevatefitness.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.exercise.ProgramExerciseAndInfo
import agdesigns.elevatefitness.data.workout_program.WorkoutProgram
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
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
    data object ResetCurrentWorkout: HomeEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private var collectProgramsJob: Job? = null
    private var collectCurrentProgram: Job? = null
    private var getProgramExercisesJob: Job? = null
    private var animateJob: Job? = null

    init {
        viewModelScope.launch {
            repository.getCurrentPlan().collect { currentPlan ->
                _state.update { it.copy(currentPlan = currentPlan) }
                if (currentPlan != null) {
                    collectCurrentProgram?.cancel()
                    collectCurrentProgram = this.launch {
                        repository.getPlan(currentPlan).collect { currentPlan ->
                            _state.update { it.copy(currentProgram = currentPlan.currentProgram) }
                        }
                    }
                    collectProgramsJob?.cancel()
                    collectProgramsJob = this.launch {
                        repository.getPrograms(currentPlan).collect { programs ->
                            _state.update { it.copy(
                                programs = programs.sortedBy { it1 -> it1.orderInWorkoutPlan }
                            ) }
                            getProgramExercisesJob?.cancel()
                            getProgramExercisesJob = this.launch {
                                repository.getProgramExercisesAndInfo(programs.map { prg -> prg.programId })
                                    .collect { exList ->
                                        _state.update { it.copy(
                                            exercisesAndInfo = exList.groupBy { ex -> ex.extProgramId }
                                        ) }
                                    }
                            }
                        }
                    }
                }

            }
        }
        viewModelScope.launch {
            repository.getCurrentWorkout().collect{ workout ->
                _state.update { it.copy(
                    currentWorkout = workout
                ) }
            }
        }
        animateJob?.cancel(CancellationException("Duplicate call"))
        animateJob = flow {
            var counter = 0
            while (true) {
                emit(counter++)
                delay(2000)
            }
        }.onEach {_state.update { it.copy(animationTick = it.animationTick+1)} }
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
