package com.anexus.perfectgymcoach.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anexus.perfectgymcoach.data.Repository
import com.anexus.perfectgymcoach.data.exercise.ExerciseRecord
import com.anexus.perfectgymcoach.data.exercise.WorkoutExerciseAndInfo
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

data class WorkoutState(
    val cancelWorkoutDialogOpen: Boolean = false,
    val workoutExercisesAndInfo: List<WorkoutExerciseAndInfo> = emptyList(),
    val allRecords: Map<Long, List<ExerciseRecord>> = emptyMap(), // old records
    val workoutTime: Long? = null, // in seconds
    val workoutId: Long = 0,
    val repsBottomBar: Int = 0, // reps to be displayed in bottom bar
    val weightBottomBar: Float = 0f // weight to be displayed in bottom bar
)

sealed class WorkoutEvent{
    data class StartWorkout(val programId: Long): WorkoutEvent()

    object FinishWorkout: WorkoutEvent()

    object CancelWorkout: WorkoutEvent()

    object DeleteCurrentRecords: WorkoutEvent()

    data class CompleteSet(
        val exerciseInWorkout: Int
    ): WorkoutEvent()

    object ToggleCancelWorkoutDialog : WorkoutEvent()

    data class GetWorkoutExercises(val programId: Long): WorkoutEvent()

    data class AddSetToExercise(val exerciseInWorkout: Int): WorkoutEvent()

    data class UpdateReps(val newValue: Int): WorkoutEvent()

    data class UpdateWeight(val newValue: Float): WorkoutEvent()

}

@HiltViewModel
class WorkoutViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = mutableStateOf(WorkoutState())
    val state: State<WorkoutState> = _state

    private var getExercisesJob: Job? = null // FIXME: is it needed?
    private var getExerciseRecordsJob: Job? = null
    private var timerJob: Job? = null

    fun onEvent(event: WorkoutEvent){
        when (event) {
            is WorkoutEvent.ToggleCancelWorkoutDialog -> {
                _state.value = state.value.copy(
                    cancelWorkoutDialogOpen = !state.value.cancelWorkoutDialogOpen
                )
            }
            is WorkoutEvent.GetWorkoutExercises -> {
                if (state.value.workoutExercisesAndInfo.isEmpty()) { // only retrieve once
                    viewModelScope.launch {
                        _state.value = state.value.copy(
                            workoutExercisesAndInfo = repository.getWorkoutExercisesAndInfo(event.programId).first()
                        ) // TODO: sort
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
                        extProgramId = event.programId,
                        startDate = Calendar.getInstance()
                    ))
                    _state.value = state.value.copy(workoutId = workoutId)
                }
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
            is WorkoutEvent.CompleteSet -> {
                viewModelScope.launch {
                    val record = state.value.allRecords[
                            state.value.workoutExercisesAndInfo[event.exerciseInWorkout].extExerciseId
                    ]?.find {
                        it.extWorkoutId == state.value.workoutId && it.exerciseInWorkout == event.exerciseInWorkout
                    }  // FIXME: same find is repeated elsewhere
                    if (record == null) {
                        repository.addExerciseRecord(
                            ExerciseRecord(
                                extWorkoutId = state.value.workoutId,
                                extExerciseId = state.value.workoutExercisesAndInfo[event.exerciseInWorkout].extExerciseId,
                                exerciseInWorkout = event.exerciseInWorkout,
                                date = Calendar.getInstance(),
                                reps = listOf(state.value.repsBottomBar),
                                weights = listOf(state.value.weightBottomBar)
                            )
                        )
                    } else {
                        repository.addExerciseRecord(record.copy(
                            reps = record.reps.plus(state.value.repsBottomBar),
                            weights = record.weights.plus(state.value.weightBottomBar)
                        ))
                    }
                }
            }
            is WorkoutEvent.FinishWorkout -> {
                viewModelScope.launch {
                    repository.completeWorkoutRecord(
                        WorkoutRecordFinish(
                            state.value.workoutId,
                            state.value.workoutTime!!
                        )
                    )
                }
            }
            is WorkoutEvent.CancelWorkout -> { /*TODO()*/ }
            is WorkoutEvent.DeleteCurrentRecords -> { /*TODO()*/ }
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
        }
    }

}
