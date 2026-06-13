package agdesigns.elevatefitness.ui.screens.home

import agdesigns.elevatefitness.data.PhoneWorkoutRepository
import agdesigns.elevatefitness.data.PreferenceRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.db.entity.ExerciseRecordAndInfo
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseAndInfo
import agdesigns.elevatefitness.data.db.entity.WorkoutProgram
import agdesigns.elevatefitness.shared.grpc.Workout
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.util.concurrent.CancellationException
import javax.inject.Inject

data class HomeState(
    val currentPlan: Long? = 0,
    val currentProgram: Int? = null,
    val programs: List<WorkoutProgram>? = null,
    val exercisesAndInfo: Map<Long, List<ProgramExerciseAndInfo>> = emptyMap(),
    val openAddProgramDialogue: Boolean = false,
    val currentWorkout: Long? = null,
    val animationTick: Int = 0,
    val planCycleCount: Int = 0,
    val showPlanChangeReminder: Boolean = false,
    val resumedWorkoutExercises: List<ExerciseRecordAndInfo> = emptyList()
)

sealed class HomeEvent{
    data object ResetCurrentWorkout: HomeEvent()
    data object DismissPlanChangeReminder: HomeEvent()
}

@OptIn(ExperimentalHorologistApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: Repository,
    private val preferences: PreferenceRepository,
    private val phoneWorkoutRepository: PhoneWorkoutRepository
): ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private var collectProgramsJob: Job? = null
    private var collectCurrentProgram: Job? = null
    private var getProgramExercisesJob: Job? = null
    private var animateJob: Job? = null
    private var retrieveResumeWorkoutJob: Job? = null


    init {
        viewModelScope.launch {
            // if we are in home, there is no workout. Make sure it is sent to wear
            // (this could happen if phone app is abruptly terminated)
            if (phoneWorkoutRepository.apiIsAvailable()) {
                phoneWorkoutRepository.wearWorkoutStaticDeferred.await().urgentUpdateData {
                    Workout.WorkoutStaticData.newBuilder()
                        .setActiveWorkout(false)
                        .build()
                }
            }
        }
        viewModelScope.launch {
            preferences.getCurrentPlan().collect { currentPlanId ->
                _state.update { it.copy(currentPlan = currentPlanId) }
                val currentPlan = currentPlanId?.let { repository.getPlan(it).first() }
                if (currentPlanId != null && currentPlan != null) {
                    collectCurrentProgram?.cancel()
                    collectCurrentProgram = this.launch {
                        repository.getPlan(currentPlanId).collect { currentPlan ->
                            currentPlan?.let {
                                _state.update { it.copy(currentProgram = currentPlan.currentProgram) }
                            }
                        }
                    }
                    collectProgramsJob?.cancel()
                    collectProgramsJob = this.launch {
                        repository.getPrograms(currentPlanId).collect { programs ->
                            _state.update { it.copy(
                                programs = programs.sortedBy { it1 -> it1.orderInWorkoutPlan }
                            ) }
                            getProgramExercisesJob?.cancel()
                            getProgramExercisesJob = this.launch {
                                repository.getProgramExercisesAndInfo(programs.map { prg -> prg.programId })
                                    .collect { exList ->
                                        _state.update { it.copy(
                                            exercisesAndInfo = exList.groupBy { ex -> ex.extProgramId }
                                        ) }
                                    }
                            }
                            // Calculate plan cycle count and check if reminder should be shown
                            viewModelScope.launch {
                                val cycleCount = repository.getPlanCycleCount(currentPlanId)
                                val hasDiminishingReturns = repository.isPlanShowingDiminishingReturns(currentPlanId)
                                val isDismissed = preferences.isDismissedPlanChangeReminder(currentPlanId).first()
                                val shouldShowReminder = (cycleCount >= 8 || hasDiminishingReturns) && !isDismissed

                                _state.update {
                                    it.copy(
                                        planCycleCount = cycleCount,
                                        showPlanChangeReminder = shouldShowReminder
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // current plan is null, very likely means there are no plans in db
                    // however, if user restores db without preferences plans are there
                    val plans = repository.getPlans().first()
                    if (plans.isNotEmpty()) {
                        val lastPlan = plans.minByOrNull { it.creationDate }
                        if (lastPlan != null) {
                            preferences.setCurrentPlanIfNone(lastPlan.planId, overrideValue = false)
                        }
                    }
                }

            }
        }
        viewModelScope.launch {
            preferences.getCurrentWorkout().collect{ workout ->
                _state.update {
                    it.copy(
                        currentWorkout = workout
                    )
                }
                if (workout != null) {
                    retrieveResumeWorkoutJob?.cancel()
                    retrieveResumeWorkoutJob = this.launch {
                        repository.getWorkoutExerciseRecordsAndInfo(workout).collect { exs ->
                            _state.update {
                                it.copy(
                                    resumedWorkoutExercises = exs
                                )
                            }
                        }
                    }
                }
            }
        }
        animateJob?.cancel(CancellationException("Duplicate call"))
        animateJob = flow {
            var counter = 0
            while (true) {
                emit(counter++)
                delay(2000)
            }
        }.onEach {_state.update { it.copy(animationTick = it.animationTick+1)} }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: HomeEvent){
        when(event){
            is HomeEvent.ResetCurrentWorkout -> {
                viewModelScope.launch {
                    preferences.setCurrentWorkout(null)
                }
            }
            is HomeEvent.DismissPlanChangeReminder -> {
                viewModelScope.launch {
                    state.value.currentPlan?.let { planId ->
                        preferences.dismissPlanChangeReminder(planId)
                    }
                    _state.update { it.copy(showPlanChangeReminder = false) }
                }
            }
        }
    }

}
