package com.anexus.perfectgymcoach.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anexus.perfectgymcoach.data.exercise.Exercise
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import com.anexus.perfectgymcoach.data.Repository
import com.anexus.perfectgymcoach.data.exercise.ExerciseRecord
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
    val workoutExercises: List<WorkoutExercise> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val currentExerciseRecords: List<ExerciseRecord> = emptyList(), // old records
    val currentExerciseCurrentRecord: ExerciseRecord? = null, // records collected in the current workout
    val workoutTime: Long? = null, // in seconds
    val workoutId: Long = 0
)

sealed class WorkoutEvent{
    data class StartWorkout(val programId: Long): WorkoutEvent()

    object FinishWorkout: WorkoutEvent()

    object CancelWorkout: WorkoutEvent()

    object DeleteCurrentRecords: WorkoutEvent()

    data class CompleteSet(
        val reps: Int,
        val weight: Float,
        val exerciseId: Long, // FIXME: may be redundant as can be get with exerciseInWorkout
        val exerciseInWorkout: Int
    ): WorkoutEvent()

    object ToggleCancelWorkoutDialog : WorkoutEvent()

    data class GetWorkoutExercises(val programId: Long): WorkoutEvent()

    data class AddSetToExercise(val exerciseInWorkout: Int): WorkoutEvent()

    data class GetExercises(val muscle: Exercise.Muscle): WorkoutEvent()

    data class GetExerciseRecords(val exerciseId: Long, val exerciseInWorkout: Int): WorkoutEvent()

    data class AddWorkoutExercise(val workoutExercise: WorkoutExercise): WorkoutEvent()

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
            is WorkoutEvent.AddWorkoutExercise -> {

                viewModelScope.launch {
                    repository.addWorkoutExercise(event.workoutExercise)
                }
            }
            is WorkoutEvent.ToggleCancelWorkoutDialog -> {
                _state.value = state.value.copy(
                    cancelWorkoutDialogOpen = !state.value.cancelWorkoutDialogOpen
                )
            }
            is WorkoutEvent.GetWorkoutExercises -> {
                if (state.value.workoutExercises.isEmpty()) { // only retrieve once
                    viewModelScope.launch {
                        repository.getWorkoutExercises(event.programId)
                            .first { // fixme: maybe just first emission
                                _state.value = state.value.copy(
                                    workoutExercises = it // todo: sort
                                )
                                true
                            }
                    }
                }
            }
            is WorkoutEvent.GetExercises -> {
                viewModelScope.launch {
                    repository.getExercises(event.muscle).collect {
                        _state.value = state.value.copy(
                            exercises = it
                        )
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
            is WorkoutEvent.GetExerciseRecords -> {
                getExerciseRecordsJob?.cancel() // TODO: could check if it has changed before cancelling
                getExerciseRecordsJob = viewModelScope.launch {
                    repository.getExerciseRecords(event.exerciseId).collect { records ->
                        // TODO: sort by date before putting in
                        // TODO: implement some sort of caching
                        val currentRecord: ExerciseRecord? = records.find {
                            it.extWorkoutId == state.value.workoutId && it.exerciseInWorkout == event.exerciseInWorkout
                        }
                        val oldRecords = if (currentRecord != null) records.minus(currentRecord) else records
                        records.sortedByDescending { it.date }
                        _state.value = state.value.copy(
                            currentExerciseRecords = oldRecords,
                            currentExerciseCurrentRecord = currentRecord
                        )
                    }
                }
            }
            is WorkoutEvent.CompleteSet -> {
                viewModelScope.launch {
                    val record = state.value.currentExerciseCurrentRecord
                    if (record == null) {
                        repository.addExerciseRecord(
                            ExerciseRecord(
                                extWorkoutId = state.value.workoutId,
                                extExerciseId = event.exerciseId,
                                exerciseInWorkout = event.exerciseInWorkout,
                                date = Calendar.getInstance(),
                                reps = listOf(event.reps),
                                weights = listOf(event.weight)
                            )
                        )
                    } else {
                        repository.addExerciseRecord(record.copy(
                            reps = record.reps.plus(event.reps),
                            weights = record.weights.plus(event.weight)
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
                val newExs = state.value.workoutExercises
                val newEx = newExs[event.exerciseInWorkout].copy(
                    reps = newExs[event.exerciseInWorkout].reps.plus(newExs[event.exerciseInWorkout].reps.last())
                )
                _state.value = state.value.copy(
                    workoutExercises = newExs.map { if (it.workoutExerciseId == newEx.workoutExerciseId) newEx else it }
                )
            }
        }
    }

}
