package agdesigns.elevatefitness.ui.screens.create_exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.db.entity.Exercise
import agdesignes.elevatefitness.shared.Equipment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class ExerciseState(
    val name: String = "",
    val equipment: Equipment = Equipment.BARBELL,
    val primaryMuscle: Exercise.Muscle = Exercise.Muscle.ABS,
    val secondaryMuscles: List<Boolean> = List(Exercise.Muscle.entries.size-1) { false },
    val difficulty: Exercise.ExerciseDifficulty = Exercise.ExerciseDifficulty.INTERMEDIATE
)

sealed class CreateExerciseEvent{
    data class Init(val muscleOrdinal: Int, val filterEquipment: Equipment?): CreateExerciseEvent()

    data class UpdateName(val newName: String): CreateExerciseEvent()

    data class UpdateEquipment(val newEquipment: Equipment): CreateExerciseEvent()

    data class UpdateDifficulty(val newDifficulty: Exercise.ExerciseDifficulty): CreateExerciseEvent()

    data class UpdatePrimaryMuscle(val newMuscle: Exercise.Muscle): CreateExerciseEvent()

    data class UpdateSecondaryMuscle(val checkStatus: Boolean, val index: Int): CreateExerciseEvent()

    data class ToggleSecondaryMuscle(val index: Int): CreateExerciseEvent()

    data object TryCreateExercise: CreateExerciseEvent()
}

@HiltViewModel
class CreateExerciseViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = MutableStateFlow(ExerciseState())
    val state: StateFlow<ExerciseState> = _state.asStateFlow()


    fun onEvent(event: CreateExerciseEvent): Boolean {
        when (event) {
            is CreateExerciseEvent.Init -> {
                _state.update {
                    val muscle = Exercise.Muscle.entries[event.muscleOrdinal]
                    val equipment = event.filterEquipment ?: Equipment.EVERYTHING
                    it.copy(
                        equipment = if (equipment != Equipment.EVERYTHING)
                            equipment
                        else
                            it.equipment,
                        primaryMuscle = if (muscle != Exercise.Muscle.EVERYTHING)
                            muscle
                        else
                            it.primaryMuscle
                    )
                }
            }
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
                                nameResKey = "",
                                equipment = state.value.equipment,
                                primaryMuscle = state.value.primaryMuscle,
                                secondaryMuscles = secondaryMuscles,
                                difficulty = state.value.difficulty,
                                userDefined = true
                            )
                        )
                    }
                } else
                    return false
            }
            is CreateExerciseEvent.UpdateName -> {
                _state.update { it.copy(name = event.newName) }
            }
            is CreateExerciseEvent.UpdateEquipment -> {
                _state.update { it.copy(equipment = event.newEquipment) }
            }
            is CreateExerciseEvent.UpdatePrimaryMuscle -> {
                _state.update { it.copy(primaryMuscle = event.newMuscle) }
            }
            is CreateExerciseEvent.UpdateSecondaryMuscle -> {
                val mutable = state.value.secondaryMuscles.toMutableList()
                mutable[event.index] = event.checkStatus
                _state.update { it.copy(secondaryMuscles = mutable.toList()) }
            }
            is CreateExerciseEvent.ToggleSecondaryMuscle -> {
                val mutable = state.value.secondaryMuscles.toMutableList()
                mutable[event.index] = !mutable[event.index]
                _state.update { it.copy(secondaryMuscles = mutable.toList()) }
            }
            is CreateExerciseEvent.UpdateDifficulty -> {
                _state.update { it.copy(difficulty = event.newDifficulty) }
            }
        }
        return true
    }
}
