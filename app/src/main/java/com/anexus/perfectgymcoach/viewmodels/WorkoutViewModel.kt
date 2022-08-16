package com.anexus.perfectgymcoach.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anexus.perfectgymcoach.data.exercise.Exercise
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import com.anexus.perfectgymcoach.data.Repository
import com.anexus.perfectgymcoach.data.exercise.ExerciseRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import javax.inject.Inject

data class WorkoutState(
    val workoutExercises: List<WorkoutExercise> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val currentExercise: Int? = null,
    val currentExerciseRecords: List<ExerciseRecord> = emptyList(),
    val workoutStarted: Long? = null
)

sealed class WorkoutEvent{
    object StartWorkout: WorkoutEvent()

    object NextExercise: WorkoutEvent()

    object PreviousExercise: WorkoutEvent()

    data class ToggleExerciseDialogue(val exercise: Exercise? = null) : WorkoutEvent()

    data class GetWorkoutExercises(val programId: Long): WorkoutEvent()

    data class GetExercises(val muscle: Exercise.Muscle): WorkoutEvent()

    data class GetExerciseRecords(val exerciseId: Long): WorkoutEvent()

    data class AddWorkoutExercise(val workoutExercise: WorkoutExercise): WorkoutEvent()

}

@HiltViewModel
class WorkoutViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = mutableStateOf(WorkoutState())
    val state: State<WorkoutState> = _state

    private var getExercisesJob: Job? = null // FIXME
    private var timerJob: Job? = null

    fun onEvent(event: WorkoutEvent){
        when (event) {
            is WorkoutEvent.AddWorkoutExercise -> {

                viewModelScope.launch {
                    repository.addWorkoutExercise(event.workoutExercise)
                }
            }
            is WorkoutEvent.ToggleExerciseDialogue -> {

            }
            is WorkoutEvent.GetWorkoutExercises -> {
                viewModelScope.launch {
                    repository.getWorkoutExercises(event.programId).collect {
                        _state.value = state.value.copy(
                            workoutExercises = it,
                            currentExercise = state.value.currentExercise ?: 0
                        )

                    }
                }
            }
            is WorkoutEvent.GetExercises -> {
                viewModelScope.launch {
                    repository.getExercises(event.muscle).collect {
                        _state.value = state.value.copy(
                            exercises = it
                        )
                    }
                }
            }
            is WorkoutEvent.StartWorkout -> {
                _state.value = state.value.copy(workoutStarted = 0)
                timerJob?.cancel(CancellationException("Duplicate call"))
                timerJob = flow {
                    var counter = 0
                    while (true) {
                        emit(counter++)
                        delay(1000)
                    }
                }.onEach {_state.value = state.value.copy(workoutStarted = state.value.workoutStarted!!+1)}
                .launchIn(viewModelScope)
            }
            is WorkoutEvent.NextExercise ->
                _state.value = state.value.copy(currentExercise = state.value.currentExercise!!+1)
            is WorkoutEvent.PreviousExercise ->
                _state.value = state.value.copy(currentExercise = state.value.currentExercise!!-1)
            is WorkoutEvent.GetExerciseRecords -> {
                viewModelScope.launch {
                    repository.getExerciseRecords(event.exerciseId).collect {
                        // TODO: sort by date before putting in
                        // TODO: implement some sort of caching
                        _state.value = state.value.copy(
                            currentExerciseRecords = it
                        )
                    }
                }
            }
        }
    }

}
