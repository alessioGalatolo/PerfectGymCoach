package agdesigns.elevatefitness.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlan
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.workout_program.WorkoutProgram
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlansState(
    val workoutPlanMapPrograms: List<Pair<WorkoutPlan, List<WorkoutProgram>>> = emptyList(),
    val archivedPlans: List<Pair<WorkoutPlan, List<WorkoutProgram>>> = emptyList(),
    val openAddPlanDialogue: Boolean = false,
    val currentPlanId: Long? = null
)

sealed class PlansEvent{
    data object TogglePlanDialogue : PlansEvent()

    data class AddPlan(val workoutPlan: WorkoutPlan): PlansEvent()

    data class SetCurrentPlan(val planId: Long): PlansEvent()

    data class ArchivePlan(val planId: Long): PlansEvent()

    data class UnarchivePlan(val planId: Long): PlansEvent()

    // TODO: ChangeOrder
}

@HiltViewModel
class PlansViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = mutableStateOf(PlansState())
    val state: State<PlansState> = _state

    private fun updatePlans(
        currentPlanId: Long? = state.value.currentPlanId,
        workoutPlanMapPrograms: List<Pair<WorkoutPlan, List<WorkoutProgram>>> = state.value.workoutPlanMapPrograms
    ){
        var plans = workoutPlanMapPrograms
        val archivedPlans = workoutPlanMapPrograms.filter { (plan, _) -> plan.archived }
        plans = plans.filter { (plan, _) -> !plan.archived }
        // most recently created plans go first
        plans = plans.sortedByDescending { plan ->
            plan.first.planId
        }
        if(currentPlanId != null){
            plans = plans.sortedByDescending {plan ->
                if (plan.first.planId == currentPlanId) 1 else 0
            }
        }
        _state.value = state.value.copy(
            workoutPlanMapPrograms = plans,
            archivedPlans = archivedPlans,
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

            is PlansEvent.ArchivePlan -> {
                viewModelScope.launch {
                    repository.archivePlan(event.planId)
                }
            }

            is PlansEvent.UnarchivePlan -> {
                viewModelScope.launch {
                    repository.unarchivePlan(event.planId)
                }
            }
        }
    }

}
