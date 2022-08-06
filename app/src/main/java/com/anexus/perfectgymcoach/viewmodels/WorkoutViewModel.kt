package com.anexus.perfectgymcoach.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anexus.perfectgymcoach.data.exercise.Exercise
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import com.anexus.perfectgymcoach.data.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutState(
    val workoutExercises: List<WorkoutExercise> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val currentExercise: Int? = null
)

sealed class WorkoutEvent{
    data class ToggleExerciseDialogue(val exercise: Exercise? = null) : WorkoutEvent()

    data class GetWorkoutExercises(val programId: Long): WorkoutEvent()

    data class GetExercises(val muscle: Exercise.Muscle): WorkoutEvent()

    data class AddWorkoutExercise(val workoutExercise: WorkoutExercise): WorkoutEvent()

}

@HiltViewModel
class WorkoutViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = mutableStateOf(WorkoutState())
    val state: State<WorkoutState> = _state

    private var getExercisesJob: Job? = null

    fun onEvent(event: WorkoutEvent){
        when (event) {
            is WorkoutEvent.AddWorkoutExercise -> {
                getExercisesJob?.cancel()
                getExercisesJob = viewModelScope.launch {
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
        }
    }

}
