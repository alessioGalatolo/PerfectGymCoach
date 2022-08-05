package com.anexus.perfectgymcoach.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlan
import com.anexus.perfectgymcoach.data.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlansState(
    val workoutPlans: List<WorkoutPlan> = emptyList(),
    val openAddPlanDialogue: Boolean = false,
    val currentPlanId: Long? = null
)

sealed class PlansEvent{
    object TogglePlanDialogue : PlansEvent()

    data class AddPlan(val workoutPlan: WorkoutPlan): PlansEvent()

    data class SetCurrentPlan(val planId: Long): PlansEvent()

    // TODO: ChangeOrder
    // TODO: RemovePlan
}

@HiltViewModel
class PlansViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = mutableStateOf(PlansState())
    val state: State<PlansState> = _state

    init {
        viewModelScope.launch {
            repository.getPlans().collect{
                var ordered = it
                if (state.value.currentPlanId != null){
                    ordered = it.sortedByDescending { plan ->
                        if (plan.planId == state.value.currentPlanId) 1 else 0
                    }
                }
                _state.value = state.value.copy(
                    workoutPlans = ordered
                )
            }
        }
        viewModelScope.launch {
            repository.getCurrentPlan().collect {
                var ordered = state.value.workoutPlans
                if (it != null){
                    ordered = ordered.sortedByDescending { plan ->
                        if (plan.planId == it) 1 else 0
                    }
                }
                _state.value = state.value.copy(
                    workoutPlans = ordered,
                    currentPlanId = it
                )
            }
        }
    }

    fun onEvent(event: PlansEvent){
        when (event) {
            is PlansEvent.AddPlan -> {
                viewModelScope.launch {
                    repository.setCurrentPlan(repository.addPlan(event.workoutPlan), overrideValue = false)
                }
            }
            is PlansEvent.TogglePlanDialogue -> {
                _state.value = state.value.copy(
                    openAddPlanDialogue = !state.value.openAddPlanDialogue
                )
            }
            is PlansEvent.SetCurrentPlan -> {
                viewModelScope.launch{
                    repository.setCurrentPlan(event.planId, overrideValue = true)
                }
            }
        }
    }

}
