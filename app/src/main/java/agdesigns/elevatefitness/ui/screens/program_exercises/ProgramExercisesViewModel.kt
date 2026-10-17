package agdesigns.elevatefitness.ui.screens.program_exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.PreferenceRepository
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.db.entity.Exercise
import agdesigns.elevatefitness.data.db.entity.ProgramExercise
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseReorder
import agdesigns.elevatefitness.data.db.entity.UpdateExerciseSuperset
import agdesigns.elevatefitness.data.db.entity.WorkoutExercise
import agdesigns.elevatefitness.data.db.entity.WorkoutRecord
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgramExercisesState(
    val programId: Long = -1L,
    val programExercises: List<ProgramExercise> = emptyList(),
    val exercises: List<Exercise?> = emptyList(),
    // modifications done in the last workout that also happened in a previous one
    val permanentModificationProposals: List<PermanentModificationProposal> = emptyList(),
)

data class PermanentModificationProposal(
    val modification: WorkoutRecord.WorkoutModification,
    val sourceName: String,
    // exercise that was added or used as replacement, null when skipped
    val targetWorkoutExercise: WorkoutExercise? = null,
)

sealed class ProgramExercisesEvent{
    data class GetProgramExercises(val programId: Long): ProgramExercisesEvent()

    data class UpdateSuperset(val index1: Int, val index2: Int): ProgramExercisesEvent()

    data class ReorderExercises(val programExerciseReorders: List<ProgramExerciseReorder>): ProgramExercisesEvent()

    data class DeleteExercise(val programExerciseId: Long): ProgramExercisesEvent()

    data class DuplicateExercise(val programExerciseId: Long): ProgramExercisesEvent()

    data class ApplyPermanentModifications(
        val accepted: List<PermanentModificationProposal>
    ): ProgramExercisesEvent()

}

@HiltViewModel
class ProgramExercisesViewModel @Inject constructor(
    private val repository: Repository,
    private val preferences: PreferenceRepository,
): ViewModel() {
    private val _state = MutableStateFlow(ProgramExercisesState())
    val state: StateFlow<ProgramExercisesState> = _state.asStateFlow()

    val reorderCompleted: Channel<Boolean> = Channel()

    private var getProgramExercisesJob: Job? = null
    private var gatherModificationsJob: Job? = null
    // workout from which the current proposals were gathered
    private var proposalsWorkoutId: Long? = null

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
                        val sorted = exsPairs.sortedBy { it.first.orderInProgram }
                        _state.update { state -> state.copy(
                            programExercises = sorted.map { it.first },
                            exercises = sorted.map { it.second }
                        ) }
                        reorderCompleted.trySend(true)
                    }
                }
                gatherModificationsJob?.cancel()
                gatherModificationsJob = viewModelScope.launch {
                    gatherPermanentModifications(event.programId)
                }
            }
            is ProgramExercisesEvent.ReorderExercises -> {
                viewModelScope.launch {
                    // check if exercise were in superset
                    // we assume programExerciseReorders.size = 2 for the following logic, log if not
                    // we also assume that an exercise can only be move forward or backwards by 1 place
                    if (event.programExerciseReorders.size > 2) {
                        Log.w("ProgramExercisesViewModel", "When reordering exercises, got programExerciseReorders.size ${event.programExerciseReorders.size}")
                    }
                    // it's okay if swapping between two superset exercises
                    val ex1 = state.value.programExercises[event.programExerciseReorders[0].orderInProgram]
                    val ex2 = state.value.programExercises[event.programExerciseReorders[1].orderInProgram]
                    if (ex1.supersetExercise == ex2.programExerciseId && ex2.supersetExercise == ex1.programExerciseId){
                        repository.reorderProgramExercises(event.programExerciseReorders)
                        return@launch
                    }
                    // reset both exercise's supersets
                    repository.updateExerciseSuperset(
                        listOf(
                            UpdateExerciseSuperset(
                                ex1.programExerciseId,
                                null
                            ),
                            UpdateExerciseSuperset(
                                ex2.programExerciseId,
                                null
                            )
                        )
                    )
                    repository.reorderProgramExercises(event.programExerciseReorders)
                }
            }
            is ProgramExercisesEvent.DeleteExercise -> {
                viewModelScope.launch {
                    val exercise = state.value.programExercises.find {
                        it.programExerciseId == event.programExerciseId
                    }
                    if (exercise == null) {
                        Log.w("ProgramExercisesViewModel", "Trying to delete exercise that doesn't exist")
                        return@launch
                    }
                    val exerciseOrderInProgram = exercise.orderInProgram
                    val exercisesToBeUpdated = state.value.programExercises.filter {
                        it.orderInProgram > exerciseOrderInProgram
                    }
                    val updates = exercisesToBeUpdated.map {
                        ProgramExerciseReorder(
                            it.programExerciseId,
                            it.orderInProgram - 1
                        )
                    }
                    repository.deleteProgramExercise(event.programExerciseId)
                    // if not last exercise, we need to update all the others' orderInProgram
                    repository.reorderProgramExercises(updates)
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
            is ProgramExercisesEvent.DuplicateExercise -> {
                viewModelScope.launch {
                    repository.duplicateProgramExercise(event.programExerciseId)
                }
            }
            is ProgramExercisesEvent.ApplyPermanentModifications -> {
                val programId = state.value.programId
                val workoutId = proposalsWorkoutId
                _state.update { it.copy(permanentModificationProposals = emptyList()) }
                viewModelScope.launch {
                    if (event.accepted.isNotEmpty()) {
                        applyPermanentModifications(programId, event.accepted)
                    }
                    // rejected ones are not proposed again for this same workout
                    if (workoutId != null) {
                        preferences.setReviewedModificationsWorkout(programId, workoutId)
                    }
                }
            }
        }
    }

    private data class ModificationKey(
        val type: WorkoutRecord.ModificationType,
        val sourceProgramExerciseId: Long?,
        val targetExerciseId: Long?,
    )

    private fun WorkoutRecord.WorkoutModification.key() = ModificationKey(
        modificationType,
        sourceProgramExerciseId,
        if (modificationType == WorkoutRecord.ModificationType.EXERCISE_SKIPPED) null
        else targetExerciseId
    )

    /**
     * Proposes as permanent the modifications done in the last completed workout of the program
     * that were also done in at least one of the previous ones.
     */
    private suspend fun gatherPermanentModifications(programId: Long) {
        if (!preferences.getSuggestWorkoutModifications().first())
            return
        val records = repository.getWorkoutRecordsByProgram(programId).first()
            // only consider workouts that were completed
            .filter { it.durationSeconds > 0L && it.startDate != null }
            .sortedBy { it.startDate }
        val lastRecord = records.lastOrNull() ?: return
        if (preferences.getReviewedModificationsWorkout(programId).first() == lastRecord.workoutId)
            return
        val previousKeys = records.dropLast(1)
            .flatMap { it.workoutModifications }
            .map { it.key() }
            .toSet()
        val programExercises = repository.getProgramExercisesWithExercise(programId).first()
            .sortedBy { it.first.orderInProgram }
        val proposals = lastRecord.workoutModifications
            // modifications of exercises not in the program cannot be made permanent
            .filter { it.sourceProgramExerciseId != null && it.key() in previousKeys }
            .distinctBy { it.key() }
            .mapNotNull { modification ->
                val sourceIndex = programExercises.indexOfFirst {
                    it.first.programExerciseId == modification.sourceProgramExerciseId
                }
                if (sourceIndex == -1)
                    return@mapNotNull null
                val (source, sourceExercise) = programExercises[sourceIndex]
                val sourceName = sourceExercise?.name ?: return@mapNotNull null
                if (modification.modificationType == WorkoutRecord.ModificationType.EXERCISE_SKIPPED)
                    return@mapNotNull PermanentModificationProposal(modification, sourceName)
                val target = modification.targetWorkoutExerciseId?.let {
                    repository.getWorkoutExercise(it).first()
                } ?: return@mapNotNull null
                val alreadyApplied = when (modification.modificationType) {
                    WorkoutRecord.ModificationType.EXERCISE_ADDED ->
                        programExercises.getOrNull(sourceIndex + 1)
                            ?.first?.extExerciseId == target.extExerciseId
                    else -> source.extExerciseId == target.extExerciseId
                }
                if (alreadyApplied)
                    null
                else
                    PermanentModificationProposal(modification, sourceName, target)
            }
        proposalsWorkoutId = lastRecord.workoutId
        _state.update { it.copy(permanentModificationProposals = proposals) }
    }

    private suspend fun applyPermanentModifications(
        programId: Long,
        accepted: List<PermanentModificationProposal>
    ) {
        val current = repository.getProgramExercises(programId).first()
            .sortedBy { it.orderInProgram }
        val byType = accepted.groupBy { it.modification.modificationType }
        val skipped = byType[WorkoutRecord.ModificationType.EXERCISE_SKIPPED].orEmpty()
            .mapNotNull { it.modification.sourceProgramExerciseId }
            .toSet()
        val replaced = byType[WorkoutRecord.ModificationType.EXERCISE_REPLACED].orEmpty()
            .associateBy { it.modification.sourceProgramExerciseId }
        val added = byType[WorkoutRecord.ModificationType.EXERCISE_ADDED].orEmpty()
            .groupBy { it.modification.sourceProgramExerciseId }

        fun WorkoutExercise.toProgramExercise(base: ProgramExercise?) = (base ?: ProgramExercise(
            extProgramId = programId,
            extExerciseId = extExerciseId,
            orderInProgram = 0,
            reps = reps,
            rest = rest,
            overriddenDurationBased = overriddenDurationBased
        )).copy(
            extExerciseId = extExerciseId,
            reps = reps,
            rest = rest,
            note = note,
            variation = variation,
            variationResKey = variationResKey,
            overriddenDurationBased = overriddenDurationBased,
            setTypes = setTypes
        )

        // rebuild program order, new exercises have id 0
        val newOrder = mutableListOf<ProgramExercise>()
        val supersetResets = mutableListOf<UpdateExerciseSuperset>()
        current.forEachIndexed { index, exercise ->
            if (exercise.programExerciseId !in skipped) {
                val replacement = replaced[exercise.programExerciseId]?.targetWorkoutExercise
                newOrder.add(replacement?.toProgramExercise(exercise) ?: exercise)
            }
            val toAdd = added[exercise.programExerciseId].orEmpty()
                .mapNotNull { it.targetWorkoutExercise?.toProgramExercise(null) }
            if (toAdd.isEmpty())
                return@forEachIndexed
            // adding in between a superset breaks it
            val next = current.getOrNull(index + 1)
            if (next != null && exercise.supersetExercise == next.programExerciseId) {
                supersetResets.add(UpdateExerciseSuperset(exercise.programExerciseId, null))
                supersetResets.add(UpdateExerciseSuperset(next.programExerciseId, null))
            }
            newOrder.addAll(toAdd)
        }

        skipped.forEach { repository.deleteProgramExercise(it) }
        replaced.values
            .filter { it.modification.sourceProgramExerciseId !in skipped }
            .forEach { proposal ->
                newOrder.find {
                    it.programExerciseId == proposal.modification.sourceProgramExerciseId
                }?.let { repository.updateProgramExercise(it) }
            }
        if (supersetResets.isNotEmpty())
            repository.updateExerciseSuperset(supersetResets)
        val reorders = mutableListOf<ProgramExerciseReorder>()
        newOrder.forEachIndexed { index, exercise ->
            if (exercise.programExerciseId == 0L)
                repository.addProgramExercise(exercise.copy(orderInProgram = index))
            else if (exercise.orderInProgram != index)
                reorders.add(ProgramExerciseReorder(exercise.programExerciseId, index))
        }
        repository.reorderProgramExercises(reorders)
    }
}
