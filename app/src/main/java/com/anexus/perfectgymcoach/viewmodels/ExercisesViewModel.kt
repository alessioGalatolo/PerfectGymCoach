package com.anexus.perfectgymcoach.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anexus.perfectgymcoach.data.exercise.Exercise
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import com.anexus.perfectgymcoach.data.Repository
import com.anexus.perfectgymcoach.data.exercise.WorkoutExerciseAndInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExercisesState(
    val workoutExercisesAndInfo: List<WorkoutExerciseAndInfo> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val exercisesToDisplay: List<Exercise>? = null,
    val openAddExerciseDialogue: Boolean = false,
    val exerciseToAdd: Exercise? = null
)

sealed class ExercisesEvent{
    data class ToggleExerciseDialogue(val exercise: Exercise? = null) : ExercisesEvent()

    data class GetWorkoutExercises(val programId: Long): ExercisesEvent()

    data class GetExercises(val muscle: Exercise.Muscle): ExercisesEvent()

    data class AddWorkoutExercise(val workoutExercise: WorkoutExercise): ExercisesEvent()

    data class FilterExercise(val query: String): ExercisesEvent()

}

@HiltViewModel
class ExercisesViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = mutableStateOf(ExercisesState())
    val state: State<ExercisesState> = _state

    private var getExercisesJob: Job? = null
    private var getWorkoutExercisesJob: Job? = null
    private var searchJob: Job? = null

    fun onEvent(event: ExercisesEvent){
        when (event) {
            is ExercisesEvent.AddWorkoutExercise -> {
                viewModelScope.launch {
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
                getWorkoutExercisesJob?.cancel()
                getWorkoutExercisesJob = viewModelScope.launch {
                    repository.getWorkoutExercisesAndInfo(event.programId).collect {
                        _state.value = state.value.copy(
                            workoutExercisesAndInfo = it
                        )
                    }
                }
            }
            is ExercisesEvent.GetExercises -> {
                getExercisesJob?.cancel()
                getExercisesJob = viewModelScope.launch {
                    repository.getExercises(event.muscle).collect {
                        _state.value = state.value.copy(
                            exercises = it.sortedBy { ex -> ex.name },
                            exercisesToDisplay = state.value.exercisesToDisplay ?: it.sortedBy { ex -> ex.name }
                        )
                    }
                }
            }
            is ExercisesEvent.FilterExercise -> {
                searchJob?.cancel()
                searchJob = viewModelScope.launch {  // TODO: improve search
                    _state.value = state.value.copy(exercisesToDisplay = state.value.exercises.filter {
                        it.name.contains(event.query)
                    })
                }
            }
        }
    }

}
