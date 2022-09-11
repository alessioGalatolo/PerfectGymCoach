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
import okhttp3.internal.toImmutableList
import java.lang.Integer.min
import javax.inject.Inject

data class AddExerciseState(
    val exercise: Exercise? = null,
    val workoutExerciseId: Long = 0,
    val sets: String = "5",
    val reps: String = "8",
    val rest: String = "90",
    val note: String = "",
    val repsArray: List<String> = List(5) { "8" },
    val restArray: List<String> = List(5) { "90" },
    val advancedSets: Boolean = false
    )

sealed class AddExerciseEvent{
    data class GetExercise(val exerciseId: Long): AddExerciseEvent()

    data class GetWorkoutExercise(val workoutExerciseId: Long): AddExerciseEvent()

    object ToggleAdvancedSets: AddExerciseEvent()

    data class TryAddExercise(val programId: Long): AddExerciseEvent()

    data class UpdateNotes(val newNote: String): AddExerciseEvent()

    data class UpdateSets(val newSets: String): AddExerciseEvent()

    data class UpdateReps(val newReps: String): AddExerciseEvent()

    data class UpdateRepsAtIndex(val newReps: String, val index: Int): AddExerciseEvent()

    data class UpdateRest(val newRest: String): AddExerciseEvent()

    data class UpdateRestAtIndex(val newRest: String, val index: Int): AddExerciseEvent()
}

@HiltViewModel
class AddExerciseViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = mutableStateOf(AddExerciseState())
    val state: State<AddExerciseState> = _state

    fun onEvent(event: AddExerciseEvent): Boolean{
        when (event) {
            is AddExerciseEvent.GetExercise -> {
                viewModelScope.launch {
                    if (state.value.exercise == null) {
                        _state.value = state.value.copy(
                            exercise = repository.getExercise(event.exerciseId).first()
                        )
                    }
                }
            }
            is AddExerciseEvent.GetWorkoutExercise -> {
                viewModelScope.launch {
                    if (state.value.workoutExerciseId == 0L) {
                        val ex = repository.getWorkoutExercise(event.workoutExerciseId).first()
                        _state.value = state.value.copy(
                            workoutExerciseId = ex.workoutExerciseId,
                            sets = ex.reps.size.toString(), // don't use toString(), need a copy
                            reps = "${ex.reps[0]}",
                            rest = "${ex.rest}", // fixme when rest becomes an array
                            repsArray = List(ex.reps.size) { "${ex.reps[it]}" },
                            restArray = List(ex.reps.size) { "${ex.rest}" }, // fixme when rest becomes an array
                            note = ex.note,
                            advancedSets = ex.reps.distinct().size > 1
                        )
                    }
                }
            }
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
                    val exercises = repository.getWorkoutExercisesAndInfo(event.programId).first() // fixme: very expensive
                    val order = if (state.value.workoutExerciseId == 0L)
                        exercises.size
                    else
                        exercises.find { it.workoutExerciseId == state.value.workoutExerciseId }!!.orderInProgram
                    repository.addWorkoutExercise(
                        WorkoutExercise(
                            workoutExerciseId = state.value.workoutExerciseId,
                            extProgramId = event.programId,
                            extExerciseId = state.value.exercise!!.exerciseId,
                            orderInProgram = order,
                            reps =
                            if (state.value.advancedSets)
                                state.value.repsArray.map { it.toInt() }
                            else
                                List(state.value.sets.toInt()) { state.value.reps.toInt() },
                            rest = state.value.rest.toInt(), //state.value.restArray.map { it.toInt() }[0], // FIXME: pass whole array
                            note = state.value.note
                        )
                    )
                }
            }
            is AddExerciseEvent.UpdateNotes -> {
                _state.value = state.value.copy(note = event.newNote)
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
                _state.value = state.value.copy(
                    reps = event.newReps,
                    repsArray = repsArray
                )
            }
            is AddExerciseEvent.UpdateRepsAtIndex -> {
                _state.value = state.value.copy(
                    reps = if (event.index == 0) event.newReps else state.value.reps,
                    repsArray = state.value.repsArray.mapIndexed {
                            index, s -> if (index == event.index) event.newReps else s
                    })
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
                _state.value = state.value.copy(
                    rest = event.newRest,
                    restArray = restArray
                )
            }
            is AddExerciseEvent.UpdateRestAtIndex -> {
                _state.value = state.value.copy(
                    rest = if (event.index == 0) event.newRest else state.value.rest,
                    restArray = state.value.restArray.mapIndexed {
                        index, s -> if (index == event.index) event.newRest else s
                    })
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
                    restArray = restArrayMutable.toImmutableList()


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
                    repsArray = repsArrayMutable.toImmutableList()
                }
                _state.value = state.value.copy(
                    sets = event.newSets,
                    restArray = restArray,
                    repsArray = repsArray
                )
            }
            is AddExerciseEvent.ToggleAdvancedSets -> {
                _state.value = state.value.copy(
                    advancedSets = !state.value.advancedSets
                )
            }
        }
        return true
    }

}
