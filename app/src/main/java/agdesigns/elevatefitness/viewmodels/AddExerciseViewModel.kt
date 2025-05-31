package agdesigns.elevatefitness.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.exercise.Exercise
import agdesigns.elevatefitness.data.exercise.ProgramExercise
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.workout_exercise.WorkoutExercise
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.lang.Integer.min
import javax.inject.Inject

data class AddExerciseState(
    val exercise: Exercise? = null,
    val variation: String = "No variation",  // FIXME: no hardcode, also used below
    val programName: String = "",
    val programId: Long = 0L,
    val workoutId: Long = 0L,
    val programExerciseId: Long = 0L,
    val sets: String = "5",
    val reps: String = "8",
    val rest: String = "90",
    val note: String = "",
    val repsArray: List<String> = List(5) { "8" },
    val restArray: List<String> = List(5) { "90" },
    val advancedSets: Boolean = false,
    val exerciseNumber: Int = 0,
)

sealed class AddExerciseEvent{
    // if exerciseId is null, will reset all the exercises probability
    data class ResetProbability(val exerciseId: Long? = null): AddExerciseEvent()

    data class GetProgramAndExercise(val programId: Long, val exerciseId: Long): AddExerciseEvent()

    data class GetWorkoutAndExercise(val workoutId: Long, val programId: Long, val exerciseId: Long): AddExerciseEvent()

    data class GetProgramAndProgramExercise(
        val programId: Long,
        val programExerciseId: Long,
        val exerciseId: Long
    ): AddExerciseEvent()

    data object ToggleAdvancedSets: AddExerciseEvent()

    data object TryAddExercise: AddExerciseEvent()

    data class UpdateNotes(val newNote: String): AddExerciseEvent()

    data class UpdateVariation(val newVariation: String): AddExerciseEvent()

    data class UpdateSets(val newSets: String): AddExerciseEvent()

    data class UpdateReps(val newReps: String): AddExerciseEvent()

    data class UpdateRepsAtIndex(val newReps: String, val index: Int): AddExerciseEvent()

    data class UpdateRest(val newRest: String): AddExerciseEvent()

    data class UpdateRestAtIndex(val newRest: String, val index: Int): AddExerciseEvent()
}

@HiltViewModel
class AddExerciseViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = MutableStateFlow(AddExerciseState())
    val state: StateFlow<AddExerciseState> = _state.asStateFlow()

    private var getProgramJob: Job? = null
    private var getWorkoutJob: Job? = null


    fun onEvent(event: AddExerciseEvent): Boolean {
        when (event) {
            is AddExerciseEvent.TryAddExercise -> {
                if ((state.value.sets.toIntOrNull() ?: 0) <= 0)
                    return false
                if (state.value.advancedSets && (state.value.repsArray.any { it.isBlank() } ||
                            state.value.restArray.any { it.isBlank() } ))
                    return false
                if (!state.value.advancedSets && ((state.value.rest.toIntOrNull() ?: 0) < 0 ||
                            (state.value.reps.toIntOrNull() ?: 0) <= 0))
                    return false

                viewModelScope.launch {
                    if (state.value.workoutId != 0L) {
                        repository.addWorkoutExercise(
                            WorkoutExercise(
                                extExerciseId = state.value.exercise!!.exerciseId,
                                extWorkoutId = state.value.workoutId,
                                orderInProgram = state.value.exerciseNumber,
                                name = state.value.exercise!!.name,
                                description = state.value.exercise!!.description,
                                image = state.value.exercise!!.image,
                                equipment = state.value.exercise!!.equipment,
                                reps =
                                if (state.value.advancedSets)
                                    state.value.repsArray.map { it.toInt() }
                                else
                                    List(state.value.sets.toInt()) { state.value.reps.toInt() },
                                variation = if (state.value.variation == "No variation") ""
                                else
                                    " (${state.value.variation.lowercase()})",
                                rest =
                                if (state.value.advancedSets)
                                    state.value.restArray.map { it.toInt() }
                                else
                                    List(state.value.sets.toInt()) { state.value.rest.toInt() },
                                note = state.value.note
                            )
                        )
                    }
                    if (state.value.programId != 0L) {
                        repository.addProgramExercise(
                            ProgramExercise(
                                programExerciseId = state.value.programExerciseId,
                                extProgramId = state.value.programId,
                                extExerciseId = state.value.exercise!!.exerciseId,
                                orderInProgram = state.value.exerciseNumber,
                                reps =
                                if (state.value.advancedSets)
                                    state.value.repsArray.map { it.toInt() }
                                else
                                    List(state.value.sets.toInt()) { state.value.reps.toInt() },
                                variation = if (state.value.variation == "No variation") "" else " (${state.value.variation.lowercase()})",
                                rest =
                                if (state.value.advancedSets)
                                    state.value.restArray.map { it.toInt() }
                                else
                                    List(state.value.sets.toInt()) { state.value.rest.toInt() },
                                note = state.value.note
                            )
                        )
                    }
                }
            }
            is AddExerciseEvent.UpdateNotes -> {
                _state.update { it.copy(note = event.newNote) }
            }
            is AddExerciseEvent.UpdateVariation -> {
                _state.update { it.copy(variation = event.newVariation) }
            }
            is AddExerciseEvent.UpdateReps -> {
                var repsArray = emptyList<String>()
                if (event.newReps.toIntOrNull() != null &&
                    event.newReps.toInt() > 0 &&
                    state.value.sets.toIntOrNull() != null &&
                    state.value.sets.toInt() > 0
                ){
                    repsArray = List(state.value.sets.toInt()) { event.newReps }
                }
                _state.update { it.copy(
                    reps = event.newReps,
                    repsArray = repsArray
                ) }
            }
            is AddExerciseEvent.UpdateRepsAtIndex -> {
                _state.update {
                    it.copy(
                        reps = if (event.index == 0) event.newReps else state.value.reps,
                        repsArray = state.value.repsArray.mapIndexed { index, s ->
                            if (index == event.index) event.newReps else s
                        })
                }
            }
            is AddExerciseEvent.UpdateRest -> {
                var restArray = emptyList<String>()
                if (event.newRest.toIntOrNull() != null &&
                    event.newRest.toInt() > 0 &&
                    state.value.sets.toIntOrNull() != null &&
                    state.value.sets.toInt() > 0
                ){
                    restArray = List(state.value.sets.toInt()) { event.newRest }
                }
                _state.update { it.copy(
                    rest = event.newRest,
                    restArray = restArray
                )}
            }
            is AddExerciseEvent.UpdateRestAtIndex -> {
                _state.update {
                    it.copy(
                        rest = if (event.index == 0) event.newRest else state.value.rest,
                        restArray = state.value.restArray.mapIndexed { index, s ->
                            if (index == event.index) event.newRest else s
                        })
                }
            }
            is AddExerciseEvent.UpdateSets -> {
                var restArray = emptyList<String>()
                var repsArray = emptyList<String>()
                if(
                    event.newSets.toIntOrNull() != null &&
                    event.newSets.toInt() > 0
                ){
                    var restArrayMutable = state.value.restArray.subList(
                        0,
                        min(state.value.restArray.size, event.newSets.toInt())
                    ).toMutableList()
                    if (event.newSets.toInt() > state.value.restArray.size){
                        restArrayMutable.addAll(
                            List(event.newSets.toInt() - state.value.restArray.size) {
                                restArrayMutable.last()
                            }
                        )
                    } else if (event.newSets.toInt() < state.value.restArray.size) {
                        restArrayMutable = restArrayMutable.subList(0, event.newSets.toInt())
                    }
                    restArray = restArrayMutable.toList()


                    var repsArrayMutable = state.value.repsArray.subList(
                        0,
                        min(state.value.repsArray.size, event.newSets.toInt())
                    ).toMutableList()
                    if (event.newSets.toInt() > state.value.repsArray.size){
                        repsArrayMutable.addAll(
                            List(event.newSets.toInt() - state.value.repsArray.size) {
                                repsArrayMutable.last()
                            }
                        )
                    } else if (event.newSets.toInt() < state.value.repsArray.size) {
                        repsArrayMutable = repsArrayMutable.subList(0, event.newSets.toInt())
                    }
                    repsArray = repsArrayMutable.toList()
                }
                _state.update {
                    it.copy(
                        sets = event.newSets,
                        restArray = restArray,
                        repsArray = repsArray
                    )
                }
            }
            is AddExerciseEvent.ToggleAdvancedSets -> {
                _state.update {
                    it.copy(
                        advancedSets = !state.value.advancedSets
                    )
                }
            }
            is AddExerciseEvent.GetProgramAndExercise -> {
                // is adding an exercise
                if (getProgramJob == null) {
                    getProgramJob = viewModelScope.launch {
                        val programMapExercises =
                            repository.getProgramMapExercises(event.programId).first()

                        _state.update {
                            it.copy(
                                programId = event.programId,
                                programName = programMapExercises.keys.first().name,
                                exercise = repository.getExercise(event.exerciseId).first(),
                                exerciseNumber = programMapExercises.values.first().size
                            )
                        }

                        repository.getExercise(event.exerciseId).collect { exercise ->
                            _state.update { it.copy(exercise = exercise) }
                        }
                    }
                }
            }
            is AddExerciseEvent.GetWorkoutAndExercise -> {
                if (getWorkoutJob == null) {
                    getWorkoutJob = viewModelScope.launch {
                        _state.update { it.copy(
                            workoutId = event.workoutId,
                            exercise = repository.getExercise(event.exerciseId).first(),
                            exerciseNumber = repository.getWorkoutExercises(event.workoutId).first().size
                        ) }
                        if (event.programId != 0L) {
                            onEvent(AddExerciseEvent.GetProgramAndExercise(event.programId, event.exerciseId))
                        }
                    }
                }
            }
            is AddExerciseEvent.GetProgramAndProgramExercise -> {
                // is updating existing exercise
                if (getProgramJob == null) {
                    getProgramJob = viewModelScope.launch {
                        val programMapExercises =
                            repository.getProgramMapExercises(event.programId).first()

                        val ex = repository.getProgramExercise(event.programExerciseId).first()

                        _state.update { it.copy(
                            programExerciseId = ex.programExerciseId,
                            sets = ex.reps.size.toString(),
                            variation = ex.variation.ifBlank { "No variation" }
                                .replace("(", "")
                                .replace(")", "")
                                .trim()
                                .replaceFirstChar { it.uppercaseChar() },
                            reps = "${ex.reps[0]}",
                            rest = "${ex.rest[0]}",
                            repsArray = List(ex.reps.size) { "${ex.reps[it]}" },
                            restArray = List(ex.reps.size) { "${ex.rest[it]}" },
                            note = ex.note,
                            advancedSets = ex.reps.distinct().size + ex.rest.distinct().size > 2,
                            exercise = repository.getExercise(event.exerciseId).first(),
                            programId = event.programId,
                            programName = programMapExercises.keys.first().name,
                            exerciseNumber = programMapExercises.values.first().find {
                                it.programExerciseId == ex.programExerciseId
                            }!!.orderInProgram
                        )}
                    }
                }
            }
            is AddExerciseEvent.ResetProbability -> {
                viewModelScope.launch {
                    if (event.exerciseId != null)
                        repository.updateExerciseProbability(event.exerciseId)
                    else
                        repository.resetAllExerciseProbability()
                }
            }
        }
        return true
    }

}
