package agdesigns.elevatefitness.ui.screens.add_exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.db.entity.Exercise
import agdesigns.elevatefitness.data.db.entity.ProgramExercise
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.shared.SetType
import agdesigns.elevatefitness.data.db.entity.WorkoutExercise
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddExerciseState(
    val exercise: Exercise? = null,
    // keep track of original exercise to check if user changed something
    val programExercise: ProgramExercise? = null,
    val programId: Long = 0L,
    val workoutId: Long = 0L,
    val programExerciseId: Long = 0L,
    val exerciseNumber: Int = 0,
    // below are values changeable by user
    val note: String = "",
    val variationResKey: String = "no_variation",
    val repsArray: List<UInt> = List(5) { 8U },
    val restArray: List<UInt> = List(5) { 90U },
    val setTypesArray: List<SetType> = List(5) { SetType.NORMAL },
    val advancedSets: Boolean = false,
    val isLoading: Boolean = true,
    val insertAtPosition: Int? = null,
    // this gets set to exercise.isDurationBased but can be overridden by user
    val overriddenDurationBased: Boolean = false
)

sealed class AddExerciseEvent{
    data class StartRetrievingData(
        val exerciseId: Long,
        val programId: Long = 0L,
        val workoutId: Long = 0L,
        val insertAtPosition: Int? = null,
        val programExerciseId: Long? = null
    ): AddExerciseEvent()

    data object ToggleAdvancedSets: AddExerciseEvent()

    data object TryAddExercise: AddExerciseEvent()

    data class UpdateNotes(val newNote: String): AddExerciseEvent()

    data class UpdateVariationResKey(val newVariationResKey: String): AddExerciseEvent()

    data class UpdateSets(val newSets: UInt): AddExerciseEvent()

    data class UpdateReps(val newReps: UInt): AddExerciseEvent()

    data class UpdateRepsAtIndex(val newReps: UInt, val index: Int): AddExerciseEvent()

    data class UpdateRest(val newRest: UInt): AddExerciseEvent()

    data class UpdateRestAtIndex(val newRest: UInt, val index: Int): AddExerciseEvent()
    data class ChangeDurationBased(val isDurationBased: Boolean): AddExerciseEvent()
    data class UpdateSetTypeAtIndex(val setType: SetType, val index: Int): AddExerciseEvent()
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
                        _state.update {
                            it.copy(
                                insertAtPosition = event.insertAtPosition
                            )
                        }
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

                        if (state.value.insertAtPosition != null) {
                            repository.shiftWorkoutExercisesToRight(
                                state.value.workoutId,
                                state.value.insertAtPosition!!
                            )
                        }
                        val orderInProgram = state.value.insertAtPosition ?: state.value.exerciseNumber
                        repository.addWorkoutExercise(
                            WorkoutExercise(
                                extWorkoutId = state.value.workoutId,
                                extExerciseId = state.value.exercise!!.exerciseId,
                                name = state.value.exercise!!.name,
                                nameResKey = state.value.exercise!!.nameResKey,
                                image = state.value.exercise!!.image,
                                imageResKey = state.value.exercise!!.imageResKey,
                                description = state.value.exercise!!.description,
                                descriptionResKey = state.value.exercise!!.descriptionResKey,
                                equipment = state.value.exercise!!.equipment,
                                orderInProgram = orderInProgram,
                                reps = state.value.repsArray.map { it.toInt() },
                                rest = state.value.restArray.map { it.toInt() },
                                note = state.value.note,
                                variation = "",
                                variationResKey = if (state.value.variationResKey != "no_variation")
                                    state.value.variationResKey
                                else
                                    "",
                                userDefined = state.value.exercise!!.userDefined,
                                overriddenDurationBased = state.value.overriddenDurationBased,
                                setTypes = state.value.setTypesArray
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
                                variation = "",
                                variationResKey = if(state.value.variationResKey != "no_variation")
                                    state.value.variationResKey
                                else
                                    "",
                                overriddenDurationBased = state.value.overriddenDurationBased,
                                setTypes = state.value.setTypesArray
                            )
                        )
                    }
                }
            }
            is AddExerciseEvent.UpdateNotes -> {
                _state.update { it.copy(note = event.newNote) }
            }
            is AddExerciseEvent.UpdateVariationResKey -> {
                _state.update { it.copy(variationResKey = event.newVariationResKey) }
            }
            is AddExerciseEvent.UpdateSets -> {
                // Needs to update repsArray and restArray
                if (event.newSets == 0U) // cannot have less than 1 set
                    return false
                if (state.value.restArray.size.toUInt() >= event.newSets) {
                    _state.update {
                        it.copy(
                            restArray = it.restArray.subList(0, event.newSets.toInt()),
                            repsArray = it.repsArray.subList(0, event.newSets.toInt()),
                            setTypesArray = it.setTypesArray.subList(0, event.newSets.toInt())
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
                        val newSetTypesArray = oldState.setTypesArray.plus(
                            List(event.newSets.toInt() - oldState.setTypesArray.size) { SetType.NORMAL }
                        )
                        oldState.copy(
                            restArray = newRestArray,
                            repsArray = newRepsArray,
                            setTypesArray = newSetTypesArray
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
                    var newSetTypesArray = it.setTypesArray
                    if (it.advancedSets) {
                        // was in advanced sets, now not. Normalize all to first value
                        newRepsArray = newRepsArray.map { newRepsArray.first() }
                        newRestArray = newRestArray.map { newRestArray.first() }
                        newSetTypesArray = newSetTypesArray.map { SetType.NORMAL }
                    }
                    it.copy(
                        advancedSets = !it.advancedSets,
                        repsArray = newRepsArray,
                        restArray = newRestArray,
                        setTypesArray = newSetTypesArray
                    )
                }
            }
            is AddExerciseEvent.UpdateSetTypeAtIndex -> {
                // if old type is warmup, we need to make sure that it has no following warmup sets
                val currentType = state.value.setTypesArray.getOrElse(event.index) { SetType.NORMAL }
                if (currentType == SetType.WARMUP && event.setType == SetType.WARMUP)
                    return false
                var indexToChange = event.index
                var outcome = true
                if (currentType == SetType.WARMUP) {
                    // change latest warmup regardless of event.index
                    val lastWarmupSet = state.value.setTypesArray.lastIndexOf(SetType.WARMUP)
                    if (lastWarmupSet != -1 && lastWarmupSet != event.index) {
                        indexToChange = lastWarmupSet
                        outcome = false
                    }
                }
                _state.update {
                    it.copy(
                        setTypesArray = it.setTypesArray.mapIndexed { index, type ->
                            if (index == indexToChange) event.setType else type
                        }
                    )
                }
                return outcome
            }
            is AddExerciseEvent.ChangeDurationBased -> {
                _state.update { it.copy(overriddenDurationBased = event.isDurationBased) }
            }
        }
        return true
    }

    private suspend fun retrieveData(exerciseId: Long, programId: Long, workoutId: Long, programExerciseId: Long?) {
        // NOTE: we could retrieve exercise and then one of the other 3 without using combine
        // but this way we only need to keep track of one job
        if (programExerciseId != null) {
            // changing an existing exercise
            combine(
                repository.getExercise(exerciseId),
                repository.getProgramExercise(programExerciseId)
            ) { exercise, programExercise ->
                _state.update {
                    it.copy(
                        exercise = exercise,
                        programExercise = programExercise,
                        programExerciseId = programExerciseId,
                        programId = programExercise.extProgramId,
                        exerciseNumber = programExercise.orderInProgram,
                        note = programExercise.note,
                        // FIXME: once we allow custom variations, should also pass variation
                        variationResKey = programExercise.variationResKey,
                        repsArray = programExercise.reps.map { it.toUInt() },
                        restArray = programExercise.rest.map { it.toUInt() },
                        setTypesArray = programExercise.setTypes
                            ?: List(programExercise.reps.size) { SetType.NORMAL },
                        advancedSets = (programExercise.reps.distinct().size + programExercise.rest.distinct().size) > 2
                                || programExercise.setTypes?.any { it != SetType.NORMAL } == true,
                        isLoading = false,
                        overriddenDurationBased = programExercise.overriddenDurationBased
                    )
                }
            }.collect()
        } else if (programId != 0L) {
            // adding to workout and program
            combine(
                repository.getExercise(exerciseId),
                repository.getProgramMapExercises(programId),
            ) { exercise, programMapExercises ->
                // adding to workout and program is only possible if program is empty
                // thus, the number of exercises in program and workout are the same
                val exerciseNumber = programMapExercises.values.first().size
                _state.update {
                    it.copy(
                        exercise = exercise,
                        exerciseNumber = exerciseNumber,
                        programId = programId,
                        workoutId = workoutId,
                        isLoading = false,
                        overriddenDurationBased = exercise.isDurationBased
                    )
                }
            }.collect()
        } else if (workoutId != 0L) {
            // adding to workout
            combine (
                repository.getExercise(exerciseId),
                repository.getWorkoutExercises(workoutId)
            ) { exercise, workoutExercises ->
                _state.update {
                    it.copy(
                        exercise = exercise,
                        exerciseNumber = workoutExercises.size,
                        workoutId = workoutId,
                        isLoading = false,
                        overriddenDurationBased = exercise.isDurationBased
                    )
                }
            }.collect()
        } else {
            // should not happen
            Log.w("AddExerciseViewModel", "retrieveData got programId = 0, workoutId = 0, programExerciseId = 0")
        }
    }
}
