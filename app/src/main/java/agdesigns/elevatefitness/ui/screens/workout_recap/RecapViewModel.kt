package agdesigns.elevatefitness.ui.screens.workout_recap

import agdesigns.elevatefitness.data.PreferenceRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.db.entity.ExerciseRecordAndInfo
import agdesigns.elevatefitness.data.db.entity.WorkoutRecord
import agdesigns.elevatefitness.ui.common.CurrentColumnKey
import agdesigns.elevatefitness.ui.common.highlightSeriesKey
import android.util.Log
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject

data class RecapState(
    val workoutId: Long = 0L,
    val workoutRecord: WorkoutRecord? = null,
    val olderRecords: List<WorkoutRecord> = emptyList(),
    val exerciseRecords: List<ExerciseRecordAndInfo> = emptyList(),
    val volumeChartProducer: CartesianChartModelProducer = CartesianChartModelProducer(),
    val caloriesChartProducer: CartesianChartModelProducer = CartesianChartModelProducer(),
    val timeChartProducer: CartesianChartModelProducer = CartesianChartModelProducer(),
    val imperialSystem: Boolean = false,
    val index2date: Map<Int, ZonedDateTime> = emptyMap()
)

sealed class RecapEvent{
    data class SetWorkoutId(val workoutId: Long): RecapEvent()
}

@HiltViewModel
class RecapViewModel @Inject constructor(
    private val repository: Repository,
    private val preferences: PreferenceRepository
): ViewModel() {
    private val _state = MutableStateFlow(RecapState())
    val state: StateFlow<RecapState> = _state.asStateFlow()

    private var retrieveWorkoutRecordJob: Job? = null

    init {
        viewModelScope.launch {
            preferences.getImperialSystem().collect{ imperialSystem ->
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
                            combine(
                                repository.getWorkoutRecordsByProgram(state.value.workoutRecord!!.extProgramId),
                                repository.getWorkoutExerciseRecordsAndInfo(event.workoutId),
                                repository.getWorkoutRecord(event.workoutId)
                            ) { olderRecords, exerciseRecords, workoutRecord ->
                                val sortedRecords = olderRecords
                                    .filter { it.durationSeconds > 0 }
                                    .sortedBy { it.startDate }
                                val sortedDistinctExercises = exerciseRecords
                                    .distinct()
                                    .sortedBy { it.exerciseInWorkout }
                                val index2date = sortedRecords.mapIndexed { index, workoutRecord ->
                                    index to (workoutRecord.startDate ?: (state.value.exerciseRecords.firstOrNull()?.date ?: ZonedDateTime.now()))
                                }.toMap()
                                state.value.volumeChartProducer.runTransaction {
                                    lineSeries {
                                        series(sortedRecords.indices.toList(), sortedRecords.map{it.volume})
                                        series(listOf(sortedRecords.indexOf(workoutRecord)), listOf(workoutRecord.volume))
                                    }
                                    extras {
                                        it[highlightSeriesKey] = listOf(1)
                                    }
                                }
                                Log.d ("RecapViewModel", "Calories chart data: ${sortedRecords.map{it.calories}}")
                                state.value.caloriesChartProducer.runTransaction {
                                    columnSeries {
                                        series(sortedRecords.indices.toList(), sortedRecords.map{it.calories})
                                    }
                                    extras {
                                        it[CurrentColumnKey] = sortedRecords.indexOf(workoutRecord)
                                        it[highlightSeriesKey] = listOf(1)  // doesn't make sense but is used to compute legend
                                    }
                                }
                                state.value.timeChartProducer.runTransaction {
                                    lineSeries {
                                        series(sortedRecords.indices.toList(), sortedRecords.map{it.durationSeconds})
                                        series(sortedRecords.indices.toList(), sortedRecords.map{it.activeTimeSeconds})
                                        series(listOf(sortedRecords.indexOf(workoutRecord)), listOf(workoutRecord.durationSeconds))
                                        series(listOf(sortedRecords.indexOf(workoutRecord)), listOf(workoutRecord.activeTimeSeconds))
                                    }
                                    extras {
                                        it[highlightSeriesKey] = listOf(2, 3)
                                    }
                                }
                                _state.update { it.copy(
                                    olderRecords = sortedRecords,
                                    exerciseRecords = sortedDistinctExercises,
                                    workoutRecord = workoutRecord,
                                    index2date = index2date
                                ) }

                            }.collect()
                        }
                    }
                }
            }
        }
    }

}
