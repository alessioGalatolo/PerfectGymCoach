package com.anexus.perfectgymcoach.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlan
import com.anexus.perfectgymcoach.data.Repository
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GeneratePlanState(
    val workoutPlanMapPrograms: List<Pair<WorkoutPlan, List<WorkoutProgram>>> = emptyList(),
    val openAddPlanDialogue: Boolean = false,
    val currentPlanId: Long? = null
)

sealed class GeneratePlanEvent{
//    object TogglePlanDialogue : PlansEvent()
//
//    data class AddPlan(val workoutPlan: WorkoutPlan): PlansEvent()
//
//    data class SetCurrentPlan(val planId: Long): PlansEvent()
//
//    // TODO: ChangeOrder
//    // TODO: RemovePlan
}

@HiltViewModel
class GeneratePlanViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = mutableStateOf(PlansState())
    val state: State<PlansState> = _state

    private fun updatePlans(
        currentPlanId: Long? = state.value.currentPlanId,
        workoutPlanMapPrograms: List<Pair<WorkoutPlan, List<WorkoutProgram>>> = state.value.workoutPlanMapPrograms
    ){
        var plans = workoutPlanMapPrograms
        if(currentPlanId != null){
            plans = workoutPlanMapPrograms.sortedByDescending {plan ->
                if (plan.first.planId == currentPlanId) 1 else 0
            }
        }
        _state.value = state.value.copy(
            workoutPlanMapPrograms = plans,
            currentPlanId = currentPlanId
        )
    }

    init {
        viewModelScope.launch {
            repository.getPlanMapPrograms().collect{
                updatePlans(workoutPlanMapPrograms = it.toList())
            }
        }
        viewModelScope.launch {
            repository.getCurrentPlan().collect {
                updatePlans(currentPlanId = it)
            }
        }
    }

    fun onEvent(event: GeneratePlanEvent){
        when (event) {

            else -> {}
        }
    }

}
