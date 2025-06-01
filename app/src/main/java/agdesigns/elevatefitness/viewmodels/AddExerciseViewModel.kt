package agdesigns.elevatefitness.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.exercise.Exercise
import agdesigns.elevatefitness.data.exercise.ProgramExercise
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.workout_exercise.WorkoutExercise
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.lang.Integer.min
import javax.inject.Inject

data class AddExerciseState(
    val exercise: Exercise? = null,
    val programId: Long = 0L,
    val workoutId: Long = 0L,
    val programExerciseId: Long = 0L,
    val exerciseNumber: Int = 0,
    // below are values changeable by user
    val note: String = "",
    val variation: String = "No variation",  // FIXME: should not hardcode, also used below
    val repsArray: List<UInt> = List(5) { 8U },
    val restArray: List<UInt> = List(5) { 90U },
    val advancedSets: Boolean = false,
)

sealed class AddExerciseEvent{
    // if exerciseId is null, will reset all the exercises probability
    data class ResetProbability(val exerciseId: Long? = null): AddExerciseEvent()

    data class StartRetrievingData(
        val exerciseId: Long,
        val programId: Long = 0L,
        val workoutId: Long = 0L,
        val programExerciseId: Long = 0L
    ): AddExerciseEvent()

    data object ToggleAdvancedSets: AddExerciseEvent()

    data object TryAddExercise: AddExerciseEvent()

    data class UpdateNotes(val newNote: String): AddExerciseEvent()

    data class UpdateVariation(val newVariation: String): AddExerciseEvent()

    data class UpdateSets(val newSets: UInt): AddExerciseEvent()

    data class UpdateReps(val newReps: UInt): AddExerciseEvent()

    data class UpdateRepsAtIndex(val newReps: UInt, val index: Int): AddExerciseEvent()

    data class UpdateRest(val newRest: UInt): AddExerciseEvent()

    data class UpdateRestAtIndex(val newRest: UInt, val index: Int): AddExerciseEvent()
}

@HiltViewModel
class AddExerciseViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = MutableStateFlow(AddExerciseState())
    val state: StateFlow<AddExerciseState> = _state.asStateFlow()

    private var getDataJob: Job? = null


    fun onEvent(event: AddExerciseEvent): Boolean {
        when (event) {
            is AddExerciseEvent.StartRetrievingData -> {
                if (getDataJob == null) {
                    getDataJob = viewModelScope.launch {
                        retrieveData(
                            event.exerciseId,
                            event.programId,
                            event.workoutId,
                            event.programExerciseId
                        )
                    }
                }
            }

            is AddExerciseEvent.TryAddExercise -> {
                // check reps array's values > 0
                if (state.value.repsArray.any { it == 0U })
                    return false
                // while very very unlikely, it can happen this event is called before the data is retrieved
                if (state.value.exercise == null)
                    return false

                viewModelScope.launch {
                    if (state.value.workoutId != 0L) {
                        // need to add exercise to workout
                        repository.addWorkoutExercise(
                            WorkoutExercise(
                                extWorkoutId = state.value.workoutId,
                                extExerciseId = state.value.exercise!!.exerciseId,
                                name = state.value.exercise!!.name,
                                image = state.value.exercise!!.image,
                                description = state.value.exercise!!.description,
                                equipment = state.value.exercise!!.equipment,
                                orderInProgram = state.value.exerciseNumber,
                                reps = state.value.repsArray.map { it.toInt() },
                                rest = state.value.restArray.map { it.toInt() },
                                note = state.value.note,
                                variation = if (state.value.variation == "No variation")
                                    ""
                                else
                                    " (${state.value.variation.lowercase()})",
                            )
                        )
                    }
                    // could also need to add to program, these conditions are NOT mutually exclusive
                    if (state.value.programId != 0L) {
                        repository.addProgramExercise(
                            ProgramExercise(
                                programExerciseId = state.value.programExerciseId,
                                extProgramId = state.value.programId,
                                extExerciseId = state.value.exercise!!.exerciseId,
                                orderInProgram = state.value.exerciseNumber,
                                reps = state.value.repsArray.map { it.toInt() },
                                rest = state.value.restArray.map { it.toInt() },
                                note = state.value.note,
                                variation = if (state.value.variation == "No variation")
                                    ""
                                else
                                    " (${state.value.variation.lowercase()})"
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
            is AddExerciseEvent.UpdateSets -> {
                // Needs to update repsArray and restArray
                if (event.newSets == 0U) // cannot have less than 1 set
                    return false
                if (state.value.restArray.size.toUInt() >= event.newSets) {
                    _state.update {
                        it.copy(
                            restArray = it.restArray.subList(0, event.newSets.toInt()),
                            repsArray = it.repsArray.subList(0, event.newSets.toInt())
                        )
                    }
                } else {
                    _state.update { oldState ->
                        val newRestArray = oldState.restArray.plus(
                            List(event.newSets.toInt() - oldState.restArray.size) { oldState.restArray.last() }
                        )
                        val newRepsArray = oldState.repsArray.plus(
                            List(event.newSets.toInt() - oldState.repsArray.size) { oldState.repsArray.last() }
                        )
                        oldState.copy(
                            restArray = newRestArray,
                            repsArray = newRepsArray
                        )
                    }
                }
            }
            is AddExerciseEvent.UpdateReps -> {
                if (event.newReps == 0U) // cannot have less than 1 rep
                    return false
                _state.update { it.copy(
                    repsArray = it.repsArray.map { event.newReps }
                ) }
            }
            is AddExerciseEvent.UpdateRepsAtIndex -> {
                if (event.newReps == 0U) // cannot have less than 1 rep
                    return false
                _state.update {
                    it.copy(
                        repsArray = it.repsArray.mapIndexed { index, s ->
                            if (index == event.index) event.newReps else s
                        }
                    )
                }
            }
            is AddExerciseEvent.UpdateRest -> {
                _state.update {
                    it.copy(
                        restArray = state.value.restArray.map { event.newRest }
                    )
                }
            }
            is AddExerciseEvent.UpdateRestAtIndex -> {
                _state.update {
                    it.copy(
                        restArray = it.restArray.mapIndexed { index, s ->
                            if (index == event.index) event.newRest else s
                        }
                    )
                }
            }
            is AddExerciseEvent.ToggleAdvancedSets -> {
                _state.update {
                    var newRepsArray = it.repsArray
                    var newRestArray = it.restArray
                    if (it.advancedSets) {
                        // was in advanced sets, now not
                        newRepsArray = newRepsArray.map { newRepsArray.first() }
                        newRestArray = newRestArray.map { newRestArray.first() }
                    }
                    it.copy(
                        advancedSets = !it.advancedSets,
                        repsArray = newRepsArray,
                        restArray = newRestArray
                    )
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

    private suspend fun retrieveData(exerciseId: Long, programId: Long, workoutId: Long, programExerciseId: Long) {
        // NOTE: we could retrieve exercise and then one of the other 3 without using combine
        // but this way we only need to keep track of one job
        if (programExerciseId != 0L) {
            // changing an existing exercise
            combine(
                repository.getExercise(exerciseId),
                repository.getProgramExercise(programExerciseId)
            ) { exercise, programExercise ->
                Log.d("AddExerciseViewModel", "retrieved data: $exercise $programExercise")
                val variation = programExercise.variation.ifBlank { "No variation" }
                    .replace("(", "")
                    .replace(")", "")
                    .trim()
                    .replaceFirstChar { it.uppercaseChar() }
                _state.update {
                    it.copy(
                        exercise = exercise,
                        programExerciseId = programExerciseId,
                        programId = programExercise.extProgramId,
                        exerciseNumber = programExercise.orderInProgram,
                        note = programExercise.note,
                        variation = variation,
                        repsArray = programExercise.reps.map { it.toUInt() },
                        restArray = programExercise.rest.map { it.toUInt() },
                        advancedSets = (programExercise.reps.distinct().size + programExercise.rest.distinct().size) > 2,
                    )
                }
            }.collect()
        } else if (programId != 0L) {
            // adding to workout and program
            combine(
                repository.getExercise(exerciseId),
                repository.getProgramMapExercises(programId),
            ) { exercise, programMapExercises ->
                Log.d("AddExerciseViewModel", "retrieved data: $exercise $programMapExercises")
                // adding to workout and program is only possible if program is empty
                // thus, the number of exercises in program and workout are the same
                val exerciseNumber = programMapExercises.values.first().size
                _state.update {
                    it.copy(
                        exercise = exercise,
                        exerciseNumber = exerciseNumber,
                        programId = programId,
                        workoutId = workoutId,
                    )
                }
            }.collect()
        } else if (workoutId != 0L) {
            // adding to workout
            combine (
                repository.getExercise(exerciseId),
                repository.getWorkoutExercises(workoutId)
            ) { exercise, workoutExercises ->
                Log.d("AddExerciseViewModel", "retrieved data: $exercise $workoutExercises")
                _state.update {
                    it.copy(
                        exercise = exercise,
                        exerciseNumber = workoutExercises.size,
                        workoutId = workoutId
                    )
                }
            }.collect()
        } else {
            // should not happen
            Log.w("AddExerciseViewModel", "retrieveData got programId = 0, workoutId = 0, programExerciseId = 0")
        }
    }
}
