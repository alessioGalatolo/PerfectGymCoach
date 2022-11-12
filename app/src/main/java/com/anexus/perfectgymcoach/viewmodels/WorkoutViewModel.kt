package com.anexus.perfectgymcoach.viewmodels

import android.text.format.DateUtils
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anexus.perfectgymcoach.data.Repository
import com.anexus.perfectgymcoach.data.exercise.ExerciseRecord
import com.anexus.perfectgymcoach.data.exercise.WorkoutExerciseAndInfo
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlanUpdateProgram
import com.anexus.perfectgymcoach.data.workout_record.WorkoutRecord
import com.anexus.perfectgymcoach.data.workout_record.WorkoutRecordFinish
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlin.math.max

data class WorkoutState(
    val cancelWorkoutDialogOpen: Boolean = false,
    val programId: Long = 0L,
    val workoutExercisesAndInfo: List<WorkoutExerciseAndInfo> = emptyList(),
    val allRecords: Map<Long, List<ExerciseRecord>> = emptyMap(), // old records
    val workoutTime: Long? = null, // in seconds
    val restTimestamp: Long? = null, // workout time of end of rest
    val workoutId: Long = 0L,
    val repsBottomBar: String = "0", // reps to be displayed in bottom bar
    val weightBottomBar: String = "0.0" // weight to be displayed in bottom bar
)

sealed class WorkoutEvent{
    object StartWorkout: WorkoutEvent()

    data class FinishWorkout(val workoutIntensity: WorkoutRecord.WorkoutIntensity): WorkoutEvent()

    object ResumeWorkout: WorkoutEvent()

    object CancelWorkout: WorkoutEvent()

    object DeleteCurrentRecords: WorkoutEvent()

    data class TryCompleteSet(
        val exerciseInWorkout: Int,
        val exerciseRest: Long
    ): WorkoutEvent()

    object ToggleCancelWorkoutDialog : WorkoutEvent()

    data class GetWorkoutExercises(val programId: Long): WorkoutEvent()

    data class AddSetToExercise(val exerciseInWorkout: Int): WorkoutEvent()

    data class UpdateReps(val newValue: String): WorkoutEvent()

    data class UpdateWeight(val newValue: String): WorkoutEvent()

}

@HiltViewModel
class WorkoutViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = mutableStateOf(WorkoutState())
    val state: State<WorkoutState> = _state

    private var getExercisesJob: Job? = null // FIXME: is it needed?
    private var getExerciseRecordsJob: Job? = null
    private var timerJob: Job? = null

    fun onEvent(event: WorkoutEvent): Boolean{
        when (event) {
            is WorkoutEvent.ToggleCancelWorkoutDialog -> {
                _state.value = state.value.copy(
                    cancelWorkoutDialogOpen = !state.value.cancelWorkoutDialogOpen
                )
            }
            is WorkoutEvent.GetWorkoutExercises -> {
                if (state.value.workoutExercisesAndInfo.isEmpty()) { // only retrieve once
                    _state.value = state.value.copy(programId = event.programId)
                    viewModelScope.launch {
                        _state.value = state.value.copy(
                            workoutExercisesAndInfo = repository.getWorkoutExercisesAndInfo(event.programId).first()
                                .sortedBy { it.orderInProgram }
                        )
                        repository.getExerciseRecords(
                            state.value.workoutExercisesAndInfo.map { it.extExerciseId }
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
            is WorkoutEvent.StartWorkout -> {
                _state.value = state.value.copy(workoutTime = 0)
                viewModelScope.launch {
                    val workoutId = repository.addWorkoutRecord(WorkoutRecord(
                        extProgramId = state.value.programId,
                        startDate = Calendar.getInstance()
                    ))
                    _state.value = state.value.copy(workoutId = workoutId)
                    repository.setCurrentWorkout(workoutId)
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
                            state.value.workoutExercisesAndInfo[event.exerciseInWorkout].extExerciseId
                    ]?.find {
                        it.extWorkoutId == state.value.workoutId && it.exerciseInWorkout == event.exerciseInWorkout
                    }  // FIXME: same find is repeated elsewhere

                    _state.value = state.value.copy(restTimestamp = state.value.workoutTime!!+event.exerciseRest)
                    if (record == null) {
                        repository.addExerciseRecord(
                            ExerciseRecord(
                                extWorkoutId = state.value.workoutId,
                                extExerciseId = state.value.workoutExercisesAndInfo[event.exerciseInWorkout].extExerciseId,
                                exerciseInWorkout = event.exerciseInWorkout,
                                date = Calendar.getInstance(),
                                reps = listOf(state.value.repsBottomBar.toInt()),
                                weights = listOf(state.value.weightBottomBar.toFloat())
                            )
                        )
                    } else {
                        repository.addExerciseRecord(record.copy(
                            reps = record.reps.plus(state.value.repsBottomBar.toInt()),
                            weights = record.weights.plus(state.value.weightBottomBar.toFloat())
                        ))
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
                val newExs = state.value.workoutExercisesAndInfo
                val newEx = newExs[event.exerciseInWorkout].copy(
                    reps = newExs[event.exerciseInWorkout].reps.plus(newExs[event.exerciseInWorkout].reps.last())
                )
                _state.value = state.value.copy(
                    workoutExercisesAndInfo = newExs.map { if (it.workoutExerciseId == newEx.workoutExerciseId) newEx else it }
                )
            }
            is WorkoutEvent.UpdateReps -> {
                _state.value = state.value.copy(repsBottomBar = event.newValue)
            }
            is WorkoutEvent.UpdateWeight -> {
                _state.value = state.value.copy(weightBottomBar = event.newValue)
            }
            is WorkoutEvent.ResumeWorkout -> {
                viewModelScope.launch{
                    val workoutId = repository.getCurrentWorkout().first()
                    if (workoutId != null) {
                        _state.value = state.value.copy(
                            workoutId = workoutId
                        )
                        val workout = repository.getWorkoutRecord(state.value.workoutId).first()
                        onEvent(WorkoutEvent.GetWorkoutExercises(workout.extProgramId))  // FIXME: should store workoutExercises
                        _state.value = state.value.copy(
                            workoutTime = (Calendar.getInstance().timeInMillis - workout.startDate.timeInMillis) / 1000
                        )
                        startTimer()
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
