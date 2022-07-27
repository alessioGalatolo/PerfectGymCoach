package com.anexus.perfectgymcoach.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlan
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlansState(
    val workoutPlans: List<WorkoutPlan> = emptyList(),
    val openAddPlanDialogue: Boolean = false
)

sealed class PlansEvent{
    object TogglePlanDialogue : PlansEvent()

    data class AddPlan(val name: String): PlansEvent()

    // TODO: ChangeOrder
    // TODO: RemovePlan
}

@HiltViewModel
class PlansViewModel @Inject constructor(private val repository: WorkoutPlanRepository): ViewModel() {
    private val _state = mutableStateOf(PlansState())
    val state: State<PlansState> = _state

    init {
        viewModelScope.launch {
            repository.getPlans().collect{
                _state.value = state.value.copy(
                    workoutPlans = it
                )
            }
        }
    }

    fun onEvent(event: PlansEvent){
        when (event) {
            is PlansEvent.AddPlan -> {
                viewModelScope.launch {
                    repository.addPlan(WorkoutPlan(name = event.name))
                }
            }
            is PlansEvent.TogglePlanDialogue -> {
                _state.value = state.value.copy(
                    openAddPlanDialogue = !state.value.openAddPlanDialogue
                )
            }

        }
    }

}
