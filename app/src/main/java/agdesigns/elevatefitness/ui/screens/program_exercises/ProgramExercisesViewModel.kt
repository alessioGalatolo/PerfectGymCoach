package agdesigns.elevatefitness.ui.screens.program_exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.db.entity.Exercise
import agdesigns.elevatefitness.data.db.entity.ProgramExercise
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseReorder
import agdesigns.elevatefitness.data.db.entity.UpdateExerciseSuperset
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgramExercisesState(
    val programId: Long = -1L,
    val programExercises: List<ProgramExercise> = emptyList(),
    val exercises: List<Exercise?> = emptyList()
)

sealed class ProgramExercisesEvent{
    data class GetProgramExercises(val programId: Long): ProgramExercisesEvent()

    data class UpdateSuperset(val index1: Int, val index2: Int): ProgramExercisesEvent()

    data class ReorderExercises(val programExerciseReorders: List<ProgramExerciseReorder>): ProgramExercisesEvent()

    data class DeleteExercise(val programExerciseId: Long): ProgramExercisesEvent()

}

@HiltViewModel
class ProgramExercisesViewModel @Inject constructor(
    private val repository: Repository,
): ViewModel() {
    private val _state = MutableStateFlow(ProgramExercisesState())
    val state: StateFlow<ProgramExercisesState> = _state.asStateFlow()

    private var getProgramExercisesJob: Job? = null

    fun onEvent(event: ProgramExercisesEvent){
        when (event) {
            is ProgramExercisesEvent.GetProgramExercises -> {
                if (event.programId == state.value.programId) {
                    return
                }
                _state.update { it.copy(programId = event.programId) }
                getProgramExercisesJob?.cancel()
                getProgramExercisesJob = viewModelScope.launch {
                    repository.getProgramExercisesWithExercise(event.programId).collect { exsPairs ->
                        Log.d("ProgramExercises", "ProgramExercises = $exsPairs")
                        val sorted = exsPairs.sortedBy { it.first.orderInProgram }
                        _state.update { state -> state.copy(
                            programExercises = sorted.map { it.first },
                            exercises = sorted.map { it.second }
                        ) }
                    }
                }
            }
            is ProgramExercisesEvent.ReorderExercises -> {
                // TODO: check that doesn't break supersets (probably does)
                viewModelScope.launch {
                    repository.reorderProgramExercises(event.programExerciseReorders)
                }
            }
            is ProgramExercisesEvent.DeleteExercise -> {
                viewModelScope.launch {
                    repository.deleteProgramExercise(event.programExerciseId)
                }
            }
            is ProgramExercisesEvent.UpdateSuperset -> {
                val exercise1 = state.value.programExercises[event.index1]
                val exercise2 = state.value.programExercises[event.index2]
                val exercisesToUpdate = mutableListOf<UpdateExerciseSuperset>()
                if (exercise1.supersetExercise != null){
                    val otherExercise = state.value.programExercises.find {
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
                    val otherExercise = state.value.programExercises.find {
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
                        if (exercise1.supersetExercise != exercise2.programExerciseId)
                            exercise2.programExerciseId else null
                    )
                )
                exercisesToUpdate.add(
                    UpdateExerciseSuperset(
                        exercise2.programExerciseId,
                        if (exercise2.supersetExercise != exercise1.programExerciseId)
                            exercise1.programExerciseId else null
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
