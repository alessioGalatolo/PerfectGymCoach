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
    private val _state = mutableStateOf(RecapState())
    val state: State<RecapState> = _state

    private var retrieveWorkoutRecordJob: Job? = null
    private var retrieveRecordsJob: MutableList<Job> = mutableListOf()

    init {
        viewModelScope.launch {
            repository.getImperialSystem().collect{
                _state.value = state.value.copy(
                    imperialSystem = it
                )
            }
        }
    }

    fun onEvent(event: RecapEvent){
        when (event) {
            is RecapEvent.SetWorkoutId -> {
                if (event.workoutId != state.value.workoutId) {
                    _state.value = state.value.copy(workoutId = event.workoutId)
                    retrieveWorkoutRecordJob?.cancel()
                    retrieveWorkoutRecordJob = viewModelScope.launch {
                        repository.getWorkoutRecord(event.workoutId).collect{ workoutRecord ->
                            _state.value = state.value.copy(
                                workoutRecord = workoutRecord
                            )
                            for (job in retrieveRecordsJob) {
                                job.cancel()
                            }
                            retrieveRecordsJob = mutableListOf()
                            retrieveRecordsJob.add(this.launch {
                                repository.getWorkoutRecordsByProgram(state.value.workoutRecord!!.extProgramId).collect{
                                    _state.value = state.value.copy(
                                        olderRecords = it.filter { it1 -> it1.duration > 0 }
                                            .sortedBy { it1 -> it1.startDate }
                                    )
                                }
                            })
                            retrieveRecordsJob.add(this.launch {
                                repository.getWorkoutRecord(event.workoutId).collect {
                                    _state.value = state.value.copy(
                                        workoutRecord = it
                                    )
                                }
                            })
                            retrieveRecordsJob.add(this.launch {
                                repository.getWorkoutExerciseRecordsAndInfo(event.workoutId).collect{
                                    _state.value = state.value.copy(
                                        exerciseRecords = it.distinct().sortedBy { it1 -> it1.exerciseInWorkout }
                                    )
                                }
                            })
                        }


                    }
                }
            }
        }
    }

}
