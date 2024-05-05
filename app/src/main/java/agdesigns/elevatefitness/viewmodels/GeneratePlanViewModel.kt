package agdesigns.elevatefitness.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlan
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlanDifficulty
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlanGoal
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlanSplit
import agdesigns.elevatefitness.data.workout_program.WorkoutProgram
import agdesigns.elevatefitness.ui.generatePlan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GeneratePlanState(
    val generatedPlan: WorkoutPlan? = null,
    val workoutPlanMapPrograms: List<Pair<WorkoutPlan, List<WorkoutProgram>>> = emptyList(),
    val openAddPlanDialogue: Boolean = false,
    val currentPlanId: Long? = null
)

sealed class GeneratePlanEvent{
    data class GeneratePlan(
        val goalChoice: WorkoutPlanGoal,
        val expertiseLevel: WorkoutPlanDifficulty,
        val workoutSplit: WorkoutPlanSplit
    ): GeneratePlanEvent()

}

@HiltViewModel
class GeneratePlanViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = mutableStateOf(GeneratePlanState())
    val state: State<GeneratePlanState> = _state
    private var generatePlanJob: Job? = null
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
        // TODO: use this retrieved stuff to improve plan generation
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
            is GeneratePlanEvent.GeneratePlan -> {
                if (generatePlanJob == null) {
                    generatePlanJob = viewModelScope.launch {
                        val planId = generatePlan(
                            repository,
                            event.goalChoice,
                            event.expertiseLevel,
                            event.workoutSplit
                        )
                        repository.setCurrentPlan(planId, true)  // FIXME: I don't remember why I would need override

                        _state.value = state.value.copy(
                            generatedPlan = repository.getPlan(planId).first()
                        )
                        // todo: set planId as currentPlan
                    }
                }
            }
        }
    }

}
