package agdesigns.elevatefitness.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.exercise.ExerciseRecordAndInfo
import agdesigns.elevatefitness.data.workout_record.WorkoutRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecapState(
    val workoutId: Long = 0L,
    val workoutRecord: WorkoutRecord? = null,
    val olderRecords: List<WorkoutRecord> = emptyList(),
    val exerciseRecords: List<ExerciseRecordAndInfo> = emptyList(),
    val imperialSystem: Boolean = false
)

sealed class RecapEvent{
    data class SetWorkoutId(val workoutId: Long): RecapEvent()
}

@HiltViewModel
class RecapViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = MutableStateFlow(RecapState())
    val state: StateFlow<RecapState> = _state.asStateFlow()

    private var retrieveWorkoutRecordJob: Job? = null
    private var retrieveRecordsJob: MutableList<Job> = mutableListOf()

    init {
        viewModelScope.launch {
            repository.getImperialSystem().collect{ imperialSystem ->
                _state.update { it.copy(
                    imperialSystem = imperialSystem
                ) }
            }
        }
    }

    fun onEvent(event: RecapEvent){
        when (event) {
            is RecapEvent.SetWorkoutId -> {
                if (event.workoutId != state.value.workoutId) {
                    _state.update { it.copy(workoutId = event.workoutId) }
                    retrieveWorkoutRecordJob?.cancel()
                    retrieveWorkoutRecordJob = viewModelScope.launch {
                        repository.getWorkoutRecord(event.workoutId).collect{ workoutRecord ->
                            _state.update { it.copy(
                                workoutRecord = workoutRecord
                            ) }
                            for (job in retrieveRecordsJob) {
                                job.cancel()
                            }
                            retrieveRecordsJob = mutableListOf()
                            retrieveRecordsJob.add(this.launch {
                                repository.getWorkoutRecordsByProgram(state.value.workoutRecord!!.extProgramId).collect{ olderRecords ->
                                    _state.update { it.copy(
                                        olderRecords = olderRecords.filter { it1 -> it1.durationSeconds > 0 }
                                            .sortedBy { it1 -> it1.startDate }
                                    ) }
                                }
                            })
                            retrieveRecordsJob.add(this.launch {
                                repository.getWorkoutRecord(event.workoutId).collect { workoutRecord ->
                                    _state.update { it.copy(
                                        workoutRecord = workoutRecord
                                    ) }
                                }
                            })
                            retrieveRecordsJob.add(this.launch {
                                repository.getWorkoutExerciseRecordsAndInfo(event.workoutId).collect{ exerciseRecords ->
                                    _state.update { it.copy(
                                        exerciseRecords = exerciseRecords.distinct().sortedBy { it1 -> it1.exerciseInWorkout }
                                    ) }
                                }
                            })
                        }


                    }
                }
            }
        }
    }

}
