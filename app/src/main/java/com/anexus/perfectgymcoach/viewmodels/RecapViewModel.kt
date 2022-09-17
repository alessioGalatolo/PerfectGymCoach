package com.anexus.perfectgymcoach.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anexus.perfectgymcoach.data.Repository
import com.anexus.perfectgymcoach.data.exercise.ExerciseRecord
import com.anexus.perfectgymcoach.data.exercise.ExerciseRecordAndInfo
import com.anexus.perfectgymcoach.data.workout_record.WorkoutRecord
import com.anexus.perfectgymcoach.data.workout_record.WorkoutRecordAndName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecapState(
    val workoutId: Long = 0L,
    val workoutRecord: WorkoutRecord? = null,
    val olderRecords: List<WorkoutRecord> = emptyList(),
    val exerciseRecords: List<ExerciseRecordAndInfo> = emptyList()
)

sealed class RecapEvent{
    data class SetWorkoutId(val workoutId: Long): RecapEvent()
}

@HiltViewModel
class RecapViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = mutableStateOf(RecapState())
    val state: State<RecapState> = _state

    private var retrieveRecordJob: Job? = null


    fun onEvent(event: RecapEvent){
        when (event) {
            is RecapEvent.SetWorkoutId -> {
                if (event.workoutId != state.value.workoutId) {
                    _state.value = state.value.copy(workoutId = event.workoutId)
                    retrieveRecordJob?.cancel()
                    retrieveRecordJob = viewModelScope.launch {
                        _state.value = state.value.copy(
                            workoutId = event.workoutId,
                            workoutRecord = repository.getWorkoutRecord(event.workoutId).first(),
                            exerciseRecords = repository.getWorkoutExerciseRecordsAndInfo(event.workoutId)
                                .first().distinct().sortedBy { it.exerciseInWorkout }
                        )
                        this.launch {
                            repository.getWorkoutRecordsByProgram(state.value.workoutRecord!!.extProgramId).collect{
                                _state.value = state.value.copy(
                                    olderRecords = it.filter { it1 -> it1.duration > 0 }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}
