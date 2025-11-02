package agdesigns.elevatefitness.ui.screens.plans

import agdesigns.elevatefitness.data.PreferenceRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.db.entity.WorkoutPlan
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanDifficulty
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanGoal
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanSplit
import agdesigns.elevatefitness.data.db.entity.WorkoutProgram
import agdesigns.elevatefitness.genai.LLMWrapper
import agdesigns.elevatefitness.utils.generatePlan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GeneratePlanState(
    val generatedPlan: WorkoutPlan? = null,
    val workoutPlanMapPrograms: List<Pair<WorkoutPlan, List<WorkoutProgram>>> = emptyList(),
    val openAddPlanDialogue: Boolean = false,
    val currentPlanId: Long? = null,
    val isLoadingAI: Boolean = false
)

sealed class GeneratePlanEvent{
    data class GeneratePlan(
        val goalChoice: WorkoutPlanGoal,
        val expertiseLevel: WorkoutPlanDifficulty,
        val workoutSplit: WorkoutPlanSplit
    ): GeneratePlanEvent()

}

@HiltViewModel
class GeneratePlanViewModel @Inject constructor(
    private val repository: Repository,
    private val preferences: PreferenceRepository,
    private val llmWrapper: LLMWrapper
): ViewModel() {
    private val _state = MutableStateFlow(GeneratePlanState())
    val state: StateFlow<GeneratePlanState> = _state.asStateFlow()

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
        _state.update { it.copy(
            workoutPlanMapPrograms = plans,
            currentPlanId = currentPlanId
        )}
    }

    init {
        // TODO: use this retrieved stuff to improve plan generation
        viewModelScope.launch {
            repository.getPlanMapPrograms().collect{
                updatePlans(workoutPlanMapPrograms = it.toList())
            }
        }
        viewModelScope.launch {
            preferences.getCurrentPlan().collect {
                updatePlans(currentPlanId = it)
            }
        }
    }

    fun onEvent(event: GeneratePlanEvent){
        when (event) {
            is GeneratePlanEvent.GeneratePlan -> {
                if (generatePlanJob == null) {
                    generatePlanJob = viewModelScope.launch {
                        if (llmWrapper.modelIsAvailableFlow().first()) {
                            _state.update { it.copy(isLoadingAI = true) }
                        }
                        val planId = generatePlan(
                            repository,
                            preferences,
                            event.goalChoice,
                            event.expertiseLevel,
                            event.workoutSplit
                        )
                        preferences.setCurrentPlan(planId, true)  // FIXME: I don't remember why I would need override

                        _state.update { it.copy(
                            generatedPlan = repository.getPlan(planId).first(),
                        ) }
                        viewModelScope.launch(Dispatchers.IO) {
                            llmWrapper.start()
                            _state.update { it.copy(isLoadingAI = false) }
                        }
                    }
                }
            }
        }
    }

}
