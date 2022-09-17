package com.anexus.perfectgymcoach.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anexus.perfectgymcoach.data.Repository
import com.anexus.perfectgymcoach.data.exercise.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExercisesState(
    val workoutExercisesAndInfo: List<WorkoutExerciseAndInfo> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val exercisesFilterEquip: List<Exercise>? = null,
    val exercisesToDisplay: List<Exercise>? = null,
)

sealed class ExercisesEvent{
    data class ReorderExercises(val workoutExerciseReorders: List<WorkoutExerciseReorder>): ExercisesEvent()

    data class DeleteExercise(val workoutExerciseId: Long): ExercisesEvent()

    data class GetWorkoutExercises(val programId: Long): ExercisesEvent()

    data class GetExercises(val muscle: Exercise.Muscle): ExercisesEvent()

    data class AddWorkoutExercise(val workoutExercise: WorkoutExercise): ExercisesEvent()

    data class FilterExercise(val query: String): ExercisesEvent()

    data class FilterExerciseEquipment(val query: Exercise.Equipment): ExercisesEvent()

    data class UpdateSuperset(val index1: Int, val index2: Int): ExercisesEvent()

}

@HiltViewModel
class ExercisesViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = mutableStateOf(ExercisesState())
    val state: State<ExercisesState> = _state

    private var getExercisesJob: Job? = null
    private var getWorkoutExercisesJob: Job? = null
    private var searchJob: Job? = null
    private var filterJob: Job? = null

    fun onEvent(event: ExercisesEvent){
        when (event) {
            is ExercisesEvent.AddWorkoutExercise -> {
                viewModelScope.launch {
                    repository.addWorkoutExercise(event.workoutExercise)
                }
            }
            is ExercisesEvent.GetWorkoutExercises -> {
                getWorkoutExercisesJob?.cancel()
                getWorkoutExercisesJob = viewModelScope.launch {
                    repository.getWorkoutExercisesAndInfo(event.programId).collect {
                        _state.value = state.value.copy(
                            workoutExercisesAndInfo = it.sortedBy { it2 -> it2.orderInProgram }
                        )
                    }
                }
            }
            is ExercisesEvent.GetExercises -> {
                getExercisesJob?.cancel()
                getExercisesJob = viewModelScope.launch {
                    repository.getExercises(event.muscle).collect {
                        val sorted = it.sortedBy { ex -> ex.name }
                        _state.value = state.value.copy(
                            exercises = sorted,
                            exercisesFilterEquip = state.value.exercisesFilterEquip ?: sorted,
                            exercisesToDisplay = state.value.exercisesToDisplay ?: sorted
                        )
                    }
                }
            }
            is ExercisesEvent.FilterExercise -> {
                searchJob?.cancel()
                searchJob = viewModelScope.launch {  // TODO: improve search
                    _state.value = state.value.copy(exercisesToDisplay = state.value.exercisesFilterEquip!!.filter {
                        it.name.contains(event.query, ignoreCase = true)
                                || it.primaryMuscle.muscleName.contains(event.query, ignoreCase = true)
                                || it.equipment.equipmentName.contains(event.query, ignoreCase = true)
                                || it.secondaryMuscles.any { it1 -> it1.muscleName.contains(event.query, ignoreCase = true) }
                    })
                }
            }
            is ExercisesEvent.FilterExerciseEquipment -> {
                val filtered = state.value.exercises.filter {
                    event.query == Exercise.Equipment.EVERYTHING || it.equipment == event.query
                }
                _state.value = state.value.copy(exercisesFilterEquip = filtered,
                    exercisesToDisplay = filtered)
            }
            is ExercisesEvent.ReorderExercises -> {
                // TODO: check that doesn't break supersets
                viewModelScope.launch {
                    repository.reorderWorkoutExercises(event.workoutExerciseReorders)
                }
            }
            is ExercisesEvent.DeleteExercise -> {
                viewModelScope.launch {
                    repository.deleteWorkoutExercise(event.workoutExerciseId)
                }
            }
            is ExercisesEvent.UpdateSuperset -> {
                val exercise1 = state.value.workoutExercisesAndInfo[event.index1]
                val exercise2 = state.value.workoutExercisesAndInfo[event.index2]
                val exercisesToUpdate = mutableListOf<UpdateExerciseSuperset>()
                if (exercise1.supersetExercise != null){
                    val otherExercise = state.value.workoutExercisesAndInfo.find {
                        it.workoutExerciseId == exercise1.supersetExercise
                    }
                    if (otherExercise != null)
                        exercisesToUpdate.add(
                            UpdateExerciseSuperset(
                                otherExercise.workoutExerciseId,
                                null
                            )
                        )
                }
                if (exercise2.supersetExercise != null){
                    val otherExercise = state.value.workoutExercisesAndInfo.find {
                        it.workoutExerciseId == exercise2.supersetExercise
                    }
                    if (otherExercise != null)
                        exercisesToUpdate.add(
                            UpdateExerciseSuperset(
                                otherExercise.workoutExerciseId,
                                null
                            )
                        )
                }
                exercisesToUpdate.add(
                    UpdateExerciseSuperset(
                        exercise1.workoutExerciseId,
                        if (exercise1.supersetExercise != exercise2.workoutExerciseId) exercise2.workoutExerciseId else null
                    )
                )
                exercisesToUpdate.add(
                    UpdateExerciseSuperset(
                        exercise2.workoutExerciseId,
                        if (exercise2.supersetExercise != exercise1.workoutExerciseId) exercise1.workoutExerciseId else null
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
