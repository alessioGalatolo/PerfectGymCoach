package com.anexus.perfectgymcoach.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anexus.perfectgymcoach.data.Repository
import com.anexus.perfectgymcoach.data.exercise.Exercise
import com.anexus.perfectgymcoach.data.exercise.ExerciseRecord
import com.anexus.perfectgymcoach.data.exercise.ExerciseRecordAndEquipment
import com.anexus.perfectgymcoach.data.exercise.ExerciseRecordAndInfo
import com.anexus.perfectgymcoach.data.workout_exercise.WorkoutExercise
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlanUpdateProgram
import com.anexus.perfectgymcoach.data.workout_record.WorkoutRecord
import com.anexus.perfectgymcoach.data.workout_record.WorkoutRecordFinish
import com.anexus.perfectgymcoach.data.workout_record.WorkoutRecordStart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.internal.toImmutableList
import java.util.*
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlin.math.max

data class WorkoutState(
    val cancelWorkoutDialogOpen: Boolean = false,
    val programId: Long = 0L,
    val workoutExercises: List<WorkoutExercise> = emptyList(),
    val allRecords: Map<Long, List<ExerciseRecordAndEquipment>> = emptyMap(), // old records
    val workoutTime: Long? = null, // in seconds
    val restTimestamp: Long? = null, // workout time of end of rest
    val workoutId: Long = 0L,
    val tare: Float = 0f,
    val repsBottomBar: String = "0", // reps to be displayed in bottom bar
    val weightBottomBar: String = "0.0" // weight to be displayed in bottom bar
)

sealed class WorkoutEvent{
    object StartWorkout: WorkoutEvent()

    object StartRetrievingExercises: WorkoutEvent()

    data class FinishWorkout(val workoutIntensity: WorkoutRecord.WorkoutIntensity): WorkoutEvent()

    object ResumeWorkout: WorkoutEvent()

    object CancelWorkout: WorkoutEvent()

    object DeleteCurrentRecords: WorkoutEvent()

    data class TryCompleteSet(
        val exerciseInWorkout: Int,
        val exerciseRest: Long
    ): WorkoutEvent()

    object ToggleCancelWorkoutDialog : WorkoutEvent()

    data class InitWorkout(val programId: Long): WorkoutEvent()

    data class AddSetToExercise(val exerciseInWorkout: Int): WorkoutEvent()

    data class UpdateReps(val newValue: String): WorkoutEvent()

    data class UpdateWeight(val newValue: String): WorkoutEvent()

    data class UpdateTare(val newValue: Float): WorkoutEvent()

    data class EditSetRecord(
        val reps: Int,
        val weight: Float,
        val exerciseInWorkout: Int,
        val set: Int
    ): WorkoutEvent()

}

@HiltViewModel
class WorkoutViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = mutableStateOf(WorkoutState())
    val state: State<WorkoutState> = _state

    private var retrieveExercises: Job? = null
    private var retrieveExercisesRecords: Job? = null
    private var timerJob: Job? = null

    fun onEvent(event: WorkoutEvent): Boolean{
        when (event) {
            is WorkoutEvent.ToggleCancelWorkoutDialog -> {
                _state.value = state.value.copy(
                    cancelWorkoutDialogOpen = !state.value.cancelWorkoutDialogOpen
                )
            }
            is WorkoutEvent.InitWorkout -> {
                if (retrieveExercises == null) { // only retrieve once
                    _state.value = state.value.copy(programId = event.programId)
                    retrieveExercises = viewModelScope.launch {
                        _state.value = state.value.copy(
                            workoutId = repository.addWorkoutRecord(
                                WorkoutRecord(extProgramId = event.programId)
                            )
                        )
                        onEvent(WorkoutEvent.StartRetrievingExercises)
                    }
                    viewModelScope.launch {
                        val exercises = repository.getProgramExercisesAndInfo(event.programId)
                            .first().sortedBy { it.orderInProgram }
                        val workoutExercises = exercises.map {
                            WorkoutExercise(
                                extExerciseId = it.extExerciseId,
                                extWorkoutId = state.value.workoutId,
                                extProgramExerciseId = it.programExerciseId,
                                orderInProgram = it.orderInProgram,
                                variation = it.variation,
                                name = it.name,
                                image = it.image,
                                equipment = it.equipment,
                                note = it.note,
                                reps = it.reps.toImmutableList(),
                                rest = it.rest,
                                supersetExercise = it.supersetExercise
                            )
                        }
                        repository.addWorkoutExercises(workoutExercises)
                    }
                }
            }
            is WorkoutEvent.StartRetrievingExercises -> {
                viewModelScope.launch {
                    repository.getWorkoutExercises(state.value.workoutId).collect{ exs ->
                        _state.value = state.value.copy(workoutExercises = exs)
                        retrieveExercisesRecords?.cancel()
                        retrieveExercisesRecords = this.launch {
                            repository.getExerciseRecordsAndEquipment(
                                exs.map { it.extExerciseId }
                            ).collect { records ->
                                val allRecords = records.groupBy { it.extExerciseId }
                                // TODO: sort by date before putting in
                                _state.value = state.value.copy(
                                    allRecords = allRecords
                                )
                            }
                        }
                    }
                }
            }
            is WorkoutEvent.StartWorkout -> {
                _state.value = state.value.copy(workoutTime = 0)
                viewModelScope.launch {
                    repository.startWorkout(WorkoutRecordStart(
                        state.value.workoutId,
                        startDate = Calendar.getInstance()
                    ))
                    repository.setCurrentWorkout(state.value.workoutId)
                }
                startTimer()
            }
            is WorkoutEvent.TryCompleteSet -> {
                // TODO: check if superset and if
                if (state.value.repsBottomBar.toIntOrNull() == null ||
                        state.value.weightBottomBar.toFloatOrNull() == null)
                    return false
                viewModelScope.launch {
                    val record = state.value.allRecords[
                            state.value.workoutExercises[event.exerciseInWorkout].extExerciseId
                    ]?.find {
                        it.extWorkoutId == state.value.workoutId && it.exerciseInWorkout == event.exerciseInWorkout
                    }  // FIXME: same find is repeated elsewhere

                    _state.value = state.value.copy(restTimestamp = state.value.workoutTime!!+event.exerciseRest)
                    if (record == null) {
                        if (state.value.workoutExercises[event.exerciseInWorkout].equipment == Exercise.Equipment.BODY_WEIGHT)
                            _state.value = state.value.copy(tare = repository.getUserWeight().first())
                        repository.addExerciseRecord(
                            ExerciseRecord(
                                extWorkoutId = state.value.workoutId,
                                extExerciseId = state.value.workoutExercises[event.exerciseInWorkout].extExerciseId,
                                exerciseInWorkout = event.exerciseInWorkout,
                                date = Calendar.getInstance(),
                                reps = listOf(state.value.repsBottomBar.toInt()),
                                weights = listOf(state.value.weightBottomBar.toFloat()),
                                tare = state.value.tare
                            )
                        )
                    } else {
                        repository.addExerciseRecord(
                            ExerciseRecord(
                                record.recordId,
                                record.extExerciseId,
                                record.extWorkoutId,
                                record.exerciseInWorkout,
                                record.date,
                                record.reps.plus(state.value.repsBottomBar.toInt()),
                                record.weights.plus(state.value.weightBottomBar.toFloat()),
                                record.tare
                            )
                        )
                    }
                }
            }
            is WorkoutEvent.FinishWorkout -> {
                viewModelScope.launch {
                    val exercises = repository.getWorkoutExerciseRecordsAndInfo(state.value.workoutId).first().distinct()
                    repository.completeWorkoutRecord(
                        WorkoutRecordFinish(
                            state.value.workoutId,
                            event.workoutIntensity,
                            state.value.workoutTime!!,
                            volume = exercises.sumOf {
                                (it.tare * it.reps.size +
                                        it.weights.mapIndexed { index, i -> i * it.reps[index] }.sum()).toDouble()
                            },
                            activeTime = max(0L, state.value.workoutTime!! -
                                    exercises.sumOf { it.rest * it.reps.size }),
                            calories = event.workoutIntensity.metValue *
                                    repository.getUserWeight().first() *
                                    state.value.workoutTime!! / 3600
                        )
                    )
                    val planPrograms = repository.getPlanMapPrograms().first().entries.find {
                        it.value.find { it1 -> it1.programId == state.value.programId } != null
                    }!!

                    repository.updateCurrentPlan(WorkoutPlanUpdateProgram(
                        planId = planPrograms.key.planId,
                        currentProgram = (planPrograms.key.currentProgram+1) % planPrograms.value.size
                    ))
                    repository.setCurrentWorkout(null)
                }
            }
            is WorkoutEvent.CancelWorkout -> {
                viewModelScope.launch {
                    repository.setCurrentWorkout(null)
                }
            }
            is WorkoutEvent.DeleteCurrentRecords -> {
                viewModelScope.launch {
                    repository.deleteWorkoutExerciseRecords(state.value.workoutId)
                }
            }
            is WorkoutEvent.AddSetToExercise -> {
                // FIXME: probably there is a better way of doing this
                val newExs = state.value.workoutExercises
                val newEx = newExs[event.exerciseInWorkout].copy(
                    reps = newExs[event.exerciseInWorkout].reps.plus(newExs[event.exerciseInWorkout].reps.last())
                )
                _state.value = state.value.copy(
                    workoutExercises = newExs.map { if (it.workoutExerciseId == newEx.workoutExerciseId) newEx else it }
                )
            }
            is WorkoutEvent.UpdateReps -> {
                _state.value = state.value.copy(repsBottomBar = event.newValue)
            }
            is WorkoutEvent.UpdateWeight -> {
                _state.value = state.value.copy(weightBottomBar = event.newValue)
            }
            is WorkoutEvent.UpdateTare -> {
                _state.value = state.value.copy(tare = event.newValue)
            }
            is WorkoutEvent.ResumeWorkout -> {
                viewModelScope.launch{
                    val workoutId = repository.getCurrentWorkout().first()
                    if (workoutId != null) {
                        _state.value = state.value.copy(
                            workoutId = workoutId
                        )
                        val workout = repository.getWorkoutRecord(state.value.workoutId).first()
                        onEvent(WorkoutEvent.StartRetrievingExercises)
                        _state.value = state.value.copy(
                            workoutTime = (Calendar.getInstance().timeInMillis - workout.startDate!!.timeInMillis) / 1000
                        )
                        startTimer()
                    }
                }

            }
            is WorkoutEvent.EditSetRecord -> {
                viewModelScope.launch {
                    val record = state.value.allRecords[
                            state.value.workoutExercises[event.exerciseInWorkout].extExerciseId
                    ]?.find {
                        it.extWorkoutId == state.value.workoutId && it.exerciseInWorkout == event.exerciseInWorkout
                    }  // FIXME: same find is repeated elsewhere

                    if (record == null) {
                        // There is a problem
                    } else {
                        val reps = record.reps.toMutableList()
                        val weights = record.weights.toMutableList()
                        reps[event.set] = event.reps
                        weights[event.set] = event.weight
                        repository.addExerciseRecord(
                            ExerciseRecord(
                                record.recordId,
                                record.extExerciseId,
                                record.extWorkoutId,
                                record.exerciseInWorkout,
                                record.date,
                                reps,
                                weights,
                                record.tare
                            )
                        )
                    }
                }
            }
        }
        return true
    }

    private fun startTimer(){
        timerJob?.cancel(CancellationException("Duplicate call"))
        timerJob = flow {
            var counter = 0
            while (true) {
                emit(counter++)
                delay(1000)
            }
        }.onEach {_state.value = state.value.copy(workoutTime = state.value.workoutTime!!+1)}
            .launchIn(viewModelScope)
    }
}
