package agdesigns.elevatefitness.ui.screens.plans

import agdesigns.elevatefitness.data.PreferenceRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.db.entity.WorkoutPlan
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanRename
import agdesigns.elevatefitness.data.db.entity.WorkoutProgram
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlansState(
    val workoutPlanMapPrograms: List<Pair<WorkoutPlan, List<WorkoutProgram>>> = emptyList(),
    val archivedPlans: List<Pair<WorkoutPlan, List<WorkoutProgram>>> = emptyList(),
    val openAddPlanDialogue: Boolean = false,
    val currentPlanId: Long? = null,
    val openChangeNameDialog: Boolean = false,
    val planToBeRenamed: Long? = null
)

sealed class PlansEvent{
    data object TogglePlanDialogue : PlansEvent()

    data class ToggleChangeNameDialog(val planId: Long?) : PlansEvent()

    data class AddPlan(val workoutPlan: WorkoutPlan): PlansEvent()

    data class SetCurrentPlan(val planId: Long): PlansEvent()

    data class ArchivePlan(val planId: Long): PlansEvent()

    data class UnarchivePlan(val planId: Long): PlansEvent()

    data class RenameProgram(val workoutProgramRename: WorkoutPlanRename): PlansEvent()
    // TODO: ChangeOrder
}

@HiltViewModel
class PlansViewModel @Inject constructor(
    private val repository: Repository,
    private val preferences: PreferenceRepository
): ViewModel() {
    private val _state = MutableStateFlow(PlansState())
    val state: StateFlow<PlansState> = _state.asStateFlow()

    private fun updatePlans(
        currentPlanId: Long? = state.value.currentPlanId,
        workoutPlanMapPrograms: List<Pair<WorkoutPlan, List<WorkoutProgram>>> = state.value.workoutPlanMapPrograms
    ) {
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
        _state.update { it.copy(
            workoutPlanMapPrograms = plans,
            archivedPlans = archivedPlans,
            currentPlanId = currentPlanId
        ) }
    }

    init {
        viewModelScope.launch {
            combine(
                preferences.getCurrentPlan(),
                repository.getPlanMapPrograms()
            ){ currentPlanId, workoutPlanMapPrograms ->
                updatePlans(
                    currentPlanId = currentPlanId,
                    workoutPlanMapPrograms = workoutPlanMapPrograms.toList()
                )
            }.collect()
        }
    }

    fun onEvent(event: PlansEvent){
        when (event) {
            is PlansEvent.AddPlan -> {
                viewModelScope.launch {
                    preferences.setCurrentPlan(repository.addPlan(event.workoutPlan), overrideValue = false)
                }
            }
            is PlansEvent.TogglePlanDialogue -> {
                _state.update { it.copy(
                    openAddPlanDialogue = !state.value.openAddPlanDialogue
                ) }
            }
            is PlansEvent.ToggleChangeNameDialog -> {
                _state.update { it.copy(
                    planToBeRenamed = event.planId ?: it.planToBeRenamed,
                    openChangeNameDialog = !state.value.openChangeNameDialog
                ) }
            }
            is PlansEvent.SetCurrentPlan -> {
                viewModelScope.launch{
                    preferences.setCurrentPlan(event.planId, overrideValue = true)
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

            is PlansEvent.RenameProgram -> {
                viewModelScope.launch {
                    repository.renamePlan(event.workoutProgramRename)
                }
            }
        }
    }

}
