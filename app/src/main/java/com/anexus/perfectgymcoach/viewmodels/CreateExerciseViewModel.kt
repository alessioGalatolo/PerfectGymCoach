package com.anexus.perfectgymcoach.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anexus.perfectgymcoach.data.Repository
import com.anexus.perfectgymcoach.data.exercise.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


data class ExerciseState(
    val name: String = "",
    val equipment: Exercise.Equipment = Exercise.Equipment.BARBELL,
    val primaryMuscle: Exercise.Muscle = Exercise.Muscle.ABS,
    val secondaryMuscles: List<Boolean> = List(Exercise.Muscle.entries.size-1) { false },
    val difficulty: Exercise.ExerciseDifficulty = Exercise.ExerciseDifficulty.MEDIUM
)

sealed class CreateExerciseEvent{
    data class UpdateName(val newName: String): CreateExerciseEvent()

    data class UpdateEquipment(val newEquipment: Exercise.Equipment): CreateExerciseEvent()

    data class UpdateDifficulty(val newDifficulty: Exercise.ExerciseDifficulty): CreateExerciseEvent()

    data class UpdatePrimaryMuscle(val newMuscle: Exercise.Muscle): CreateExerciseEvent()

    data class UpdateSecondaryMuscle(val checkStatus: Boolean, val index: Int): CreateExerciseEvent()

    data class ToggleSecondaryMuscle(val index: Int): CreateExerciseEvent()

    data object TryCreateExercise: CreateExerciseEvent()
}

@HiltViewModel
class CreateExerciseViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = mutableStateOf(ExerciseState())
    val state: State<ExerciseState> = _state


    fun onEvent(event: CreateExerciseEvent): Boolean {
        when (event) {
            is CreateExerciseEvent.TryCreateExercise -> {
                if(state.value.name.isNotBlank()) {
                    val secondaryMuscles = mutableListOf<Exercise.Muscle>()
                    state.value.secondaryMuscles.forEachIndexed { index, muscle ->
                        if (muscle)
                            secondaryMuscles.add(Exercise.Muscle.entries[index+1])
                    }
                    viewModelScope.launch {
                        repository.addExercise(
                            Exercise(
                                name = state.value.name,
                                equipment = state.value.equipment,
                                primaryMuscle = state.value.primaryMuscle,
                                secondaryMuscles = secondaryMuscles,
                                difficulty = state.value.difficulty
                            )
                        )
                    }
                } else
                    return false
            }
            is CreateExerciseEvent.UpdateName -> {
                _state.value = state.value.copy(name = event.newName)
            }
            is CreateExerciseEvent.UpdateEquipment -> {
                _state.value = state.value.copy(equipment = event.newEquipment)
            }
            is CreateExerciseEvent.UpdatePrimaryMuscle -> {
                _state.value = state.value.copy(primaryMuscle = event.newMuscle)
            }
            is CreateExerciseEvent.UpdateSecondaryMuscle -> {
                val mutable = state.value.secondaryMuscles.toMutableList()
                mutable[event.index] = event.checkStatus
                _state.value = state.value.copy(secondaryMuscles = mutable.toList())
            }
            is CreateExerciseEvent.ToggleSecondaryMuscle -> {
                val mutable = state.value.secondaryMuscles.toMutableList()
                mutable[event.index] = !mutable[event.index]
                _state.value = state.value.copy(secondaryMuscles = mutable.toList())
            }
            is CreateExerciseEvent.UpdateDifficulty -> {
                _state.value = state.value.copy(difficulty = event.newDifficulty)
            }
        }
        return true
    }
}
