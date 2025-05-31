package agdesigns.elevatefitness.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.exercise.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class ExercisesState(
    val programExercisesAndInfo: List<ProgramExerciseAndInfo> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val exercisesFilterEquip: List<Exercise>? = null,
    val exercisesToDisplay: List<Exercise>? = null,
)

sealed class ExercisesEvent{
    data class ReorderExercises(val programExerciseReorders: List<ProgramExerciseReorder>): ExercisesEvent()

    data class DeleteExercise(val programExerciseId: Long): ExercisesEvent()

    data class GetProgramExercises(val programId: Long): ExercisesEvent()

    data class GetExercises(val muscle: Exercise.Muscle): ExercisesEvent()

    data class AddProgramExercise(val programExercise: ProgramExercise): ExercisesEvent()

    data class FilterExercise(val query: String): ExercisesEvent()

    data class FilterExerciseEquipment(val query: Exercise.Equipment): ExercisesEvent()

    data class UpdateSuperset(val index1: Int, val index2: Int): ExercisesEvent()

}

@HiltViewModel
class ExercisesViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = MutableStateFlow(ExercisesState())
    val state: StateFlow<ExercisesState> = _state.asStateFlow()

    private var getExercisesJob: Job? = null
    private var getProgramExercisesJob: Job? = null
    private var searchJob: Job? = null
    private var filterJob: Job? = null

    fun onEvent(event: ExercisesEvent){
        when (event) {
            is ExercisesEvent.AddProgramExercise -> {
                viewModelScope.launch {
                    repository.addProgramExercise(event.programExercise)
                }
            }
            is ExercisesEvent.GetProgramExercises -> {
                getProgramExercisesJob?.cancel()
                getProgramExercisesJob = viewModelScope.launch {
                    repository.getProgramExercisesAndInfo(event.programId).collect { programExercisesAndInfo ->
                        _state.update { it.copy(
                            programExercisesAndInfo = programExercisesAndInfo.sortedBy { it2 -> it2.orderInProgram }
                        ) }
                    }
                }
            }
            is ExercisesEvent.GetExercises -> {
                getExercisesJob?.cancel()
                getExercisesJob = viewModelScope.launch {
                    repository.getExercises(event.muscle).collect {
                        val sorted = it.sortedBy { ex -> ex.name }
                        _state.update { it.copy(
                            exercises = sorted,
                            exercisesFilterEquip = state.value.exercisesFilterEquip ?: sorted,
                            exercisesToDisplay = state.value.exercisesToDisplay ?: sorted
                        ) }
                    }
                }
            }
            is ExercisesEvent.FilterExercise -> {
                searchJob?.cancel()
                searchJob = viewModelScope.launch {  // TODO: improve search
                    _state.update { it.copy(exercisesToDisplay = it.exercisesFilterEquip!!.filter { ex ->
                        ex.name.contains(event.query, ignoreCase = true)
                                || ex.primaryMuscle.muscleName.contains(event.query, ignoreCase = true)
                                || ex.variations.any { it1 -> it1.contains(event.query, ignoreCase = true) }
                                || ex.equipment.equipmentName.contains(event.query, ignoreCase = true)
                                || ex.secondaryMuscles.any { it1 -> it1.muscleName.contains(event.query, ignoreCase = true) }
                    }) }
                }
            }
            is ExercisesEvent.FilterExerciseEquipment -> {
                // FIXME: if search, than change equipment, initial search is lost
                val filtered = state.value.exercises.filter {
                    event.query == Exercise.Equipment.EVERYTHING || it.equipment == event.query
                }
                _state.update { it.copy(exercisesFilterEquip = filtered,
                    exercisesToDisplay = filtered) }
            }
            is ExercisesEvent.ReorderExercises -> {
                // TODO: check that doesn't break supersets (probably does)
                viewModelScope.launch {
                    repository.reorderProgramExercises(event.programExerciseReorders)
                }
            }
            is ExercisesEvent.DeleteExercise -> {
                viewModelScope.launch {
                    repository.deleteProgramExercise(event.programExerciseId)
                }
            }
            is ExercisesEvent.UpdateSuperset -> {
                val exercise1 = state.value.programExercisesAndInfo[event.index1]
                val exercise2 = state.value.programExercisesAndInfo[event.index2]
                val exercisesToUpdate = mutableListOf<UpdateExerciseSuperset>()
                if (exercise1.supersetExercise != null){
                    val otherExercise = state.value.programExercisesAndInfo.find {
                        it.programExerciseId == exercise1.supersetExercise
                    }
                    if (otherExercise != null)
                        exercisesToUpdate.add(
                            UpdateExerciseSuperset(
                                otherExercise.programExerciseId,
                                null
                            )
                        )
                }
                if (exercise2.supersetExercise != null){
                    val otherExercise = state.value.programExercisesAndInfo.find {
                        it.programExerciseId == exercise2.supersetExercise
                    }
                    if (otherExercise != null)
                        exercisesToUpdate.add(
                            UpdateExerciseSuperset(
                                otherExercise.programExerciseId,
                                null
                            )
                        )
                }
                exercisesToUpdate.add(
                    UpdateExerciseSuperset(
                        exercise1.programExerciseId,
                        if (exercise1.supersetExercise != exercise2.programExerciseId) exercise2.programExerciseId else null
                    )
                )
                exercisesToUpdate.add(
                    UpdateExerciseSuperset(
                        exercise2.programExerciseId,
                        if (exercise2.supersetExercise != exercise1.programExerciseId) exercise1.programExerciseId else null
                    )
                )
                viewModelScope.launch {
                    repository.updateExerciseSuperset(
                        exercisesToUpdate
                    )
                }
            }
        }
    }
}
