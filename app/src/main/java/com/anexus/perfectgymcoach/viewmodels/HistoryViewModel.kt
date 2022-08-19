package com.anexus.perfectgymcoach.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlan
import com.anexus.perfectgymcoach.data.Repository
import com.anexus.perfectgymcoach.data.workout_record.WorkoutRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryState(
    val workoutRecords: List<WorkoutRecord> = emptyList()
)

sealed class HistoryEvent{

}

@HiltViewModel
class HistoryViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = mutableStateOf(HistoryState())
    val state: State<HistoryState> = _state

    init {
        viewModelScope.launch {
            repository.getWorkoutHistory().collect{
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
