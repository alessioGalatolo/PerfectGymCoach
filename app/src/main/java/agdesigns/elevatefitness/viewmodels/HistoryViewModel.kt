package agdesigns.elevatefitness.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.workout_record.WorkoutRecordAndName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

data class HistoryState(
    val workoutRecords: Map<Int, Map<Int, List<WorkoutRecordAndName>>> = emptyMap(),
    val useImperialSystem: Boolean = false
)

sealed class HistoryEvent{

}

@HiltViewModel
class HistoryViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getImperialSystem().collect { imperialSystem ->
                _state.update { it.copy(useImperialSystem = imperialSystem) }
            }
        }
        viewModelScope.launch {
            repository.getWorkoutHistoryAndName().collect { records ->
                val filteredRecords = records.filter { it.durationSeconds > 0 }
                val groupByYear = filteredRecords.groupBy { record -> record.startDate!!.year }
                val weekField = WeekFields.of(Locale.getDefault()).weekOfYear()
                val yearToWeekToRecord = groupByYear.mapValues {
                    it.value.groupBy { record -> record.startDate!!.get(weekField) }
                }
                _state.update { it.copy(
                    workoutRecords = yearToWeekToRecord
                ) }
            }
        }
    }

    fun onEvent(event: HistoryEvent){
        when (event) {

            else -> {}
        }
    }

}
