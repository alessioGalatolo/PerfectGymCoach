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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddExerciseState(
    val exercise: Exercise? = null
)

sealed class AddExerciseEvent{
    data class GetExercise(val exerciseId: Long): AddExerciseEvent()

    data class AddWorkoutExercise(val workoutExercise: WorkoutExercise): AddExerciseEvent()

}

@HiltViewModel
class AddExerciseViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = mutableStateOf(AddExerciseState())
    val state: State<AddExerciseState> = _state

    fun onEvent(event: AddExerciseEvent){
        when (event) {
            is AddExerciseEvent.AddWorkoutExercise -> {
                viewModelScope.launch {
                    repository.addWorkoutExercise(event.workoutExercise)
                }
            }
            is AddExerciseEvent.GetExercise -> {
                viewModelScope.launch {
                    _state.value = state.value.copy(
                        exercise = repository.getExercise(event.exerciseId).first()
                    )
                }

            }
        }
    }

}
