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

data class ExercisesState(
    val workoutExercises: List<WorkoutExercise> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val openAddExerciseDialogue: Boolean = false,
    val exerciseToAdd: Exercise? = null
)

sealed class ExercisesEvent{
    data class ToggleExerciseDialogue(val exercise: Exercise? = null) : ExercisesEvent()

    data class GetWorkoutExercises(val programId: Long): ExercisesEvent()

    data class GetExercises(val muscle: Exercise.Muscle): ExercisesEvent()

    data class AddWorkoutExercise(val workoutExercise: WorkoutExercise): ExercisesEvent()

}

@HiltViewModel
class ExercisesViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = mutableStateOf(ExercisesState())
    val state: State<ExercisesState> = _state

    private var getExercisesJob: Job? = null

    fun onEvent(event: ExercisesEvent){
        when (event) {
            is ExercisesEvent.AddWorkoutExercise -> {
                getExercisesJob?.cancel()
                getExercisesJob = viewModelScope.launch {
                    repository.addWorkoutExercise(event.workoutExercise)
                }
            }
            is ExercisesEvent.ToggleExerciseDialogue -> {
                _state.value = state.value.copy(
                    openAddExerciseDialogue = !state.value.openAddExerciseDialogue,
                    exerciseToAdd = event.exercise
                )
            }
            is ExercisesEvent.GetWorkoutExercises -> {
                viewModelScope.launch {
                    repository.getWorkoutExercises(event.programId).collect {
                        _state.value = state.value.copy(
                            workoutExercises = it
                        )
                    }
                }
            }
            is ExercisesEvent.GetExercises -> {
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
