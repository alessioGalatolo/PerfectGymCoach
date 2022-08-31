package com.anexus.perfectgymcoach.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anexus.perfectgymcoach.data.Repository
import com.anexus.perfectgymcoach.data.workout_record.WorkoutRecordAndName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryState(
    val workoutRecords: List<WorkoutRecordAndName> = emptyList()
)

sealed class HistoryEvent{

}

@HiltViewModel
class HistoryViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = mutableStateOf(HistoryState())
    val state: State<HistoryState> = _state

    init {
        viewModelScope.launch {
            repository.getWorkoutHistoryAndName().collect{
                _state.value = state.value.copy(
                    workoutRecords = it.sortedByDescending { record -> record.startDate }
                )
            }
        }
    }

    fun onEvent(event: HistoryEvent){
        when (event) {

            else -> {}
        }
    }

}
