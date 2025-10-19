package agdesigns.elevatefitness.ui.screens.programs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseAndInfo
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanUpdateProgram
import agdesigns.elevatefitness.data.db.entity.WorkoutProgram
import agdesigns.elevatefitness.data.db.entity.WorkoutProgramRename
import agdesigns.elevatefitness.data.db.entity.WorkoutProgramReorder
import agdesigns.elevatefitness.genai.LLMWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgramsState(
    val programs: List<WorkoutProgram> = emptyList(),
    val planName: String = "",
    val exercisesAndInfo: Map<Long, List<ProgramExerciseAndInfo>> = emptyMap(),
    val openAddProgramDialog: Boolean = false,
    val openChangeNameDialog: Boolean = false,
    val programToBeChanged: Long = 0,
    val aiSummary: String = "",
    val aiEnabled: Boolean = true
)

sealed class ProgramsEvent{
    data object ToggleAddProgramDialog : ProgramsEvent()

    data class ToggleChangeNameDialog(val programId: Long = 0) : ProgramsEvent()

    data class InitProgramView(val planId: Long, val hasJustBeenGenerated: Boolean): ProgramsEvent()

    data class AddProgram(val workoutProgram: WorkoutProgram): ProgramsEvent()

    data class RenameProgram(val workoutProgramRename: WorkoutProgramRename): ProgramsEvent()

    data class ReorderProgram(val workoutProgramReorders: List<WorkoutProgramReorder>): ProgramsEvent()

    data class DeleteProgram(val programId: Long): ProgramsEvent()

}

@HiltViewModel
class ProgramsViewModel @Inject constructor(
    private val repository: Repository,
    private val llmWrapper: LLMWrapper
): ViewModel() {
    private val _state = MutableStateFlow(ProgramsState())
    val state: StateFlow<ProgramsState> = _state.asStateFlow()

    private var getProgramsJob: Job? = null
    private var getProgramExercisesJob: Job? = null

    init {
        _state.update {
            it.copy(
                aiEnabled = llmWrapper.modelIsAvailable()
            )
        }
    }

    fun onEvent(event: ProgramsEvent){
        when (event) {
            is ProgramsEvent.InitProgramView -> {
                getProgramsJob?.cancel()
                getProgramsJob = viewModelScope.launch {
                    combine(
                        repository.getPlan(event.planId),
                        repository.getPrograms(event.planId)
                    ) { plan, programs ->
                        _state.update { it.copy(
                            programs = programs.sortedBy { prog -> prog.orderInWorkoutPlan },
                            planName = plan?.name ?: ""
                        ) }
                        getProgramExercisesJob?.cancel()
                        getProgramExercisesJob = this.launch {
                            repository.getProgramExercisesAndInfo(programs.map { prg -> prg.programId }).collect{ exList ->
                                _state.update { it.copy(
                                    exercisesAndInfo = exList.groupBy { ex -> ex.extProgramId }
                                        .mapValues { entry -> entry.value.sortedBy { ex -> ex.orderInProgram } }
                                ) }
                            }
                        }

                    }.collect{
                        generatePlanSummary()
                    }
                }
            }
            is ProgramsEvent.AddProgram -> {
                viewModelScope.launch {
                    repository.addProgram(event.workoutProgram)
                }
            }
            is ProgramsEvent.ToggleAddProgramDialog -> {
                _state.update { it.copy(
                    openAddProgramDialog = !state.value.openAddProgramDialog
                ) }
            }
            is ProgramsEvent.ToggleChangeNameDialog -> {
                _state.update { it.copy(
                    openChangeNameDialog = !state.value.openChangeNameDialog,
                    programToBeChanged = event.programId
                ) }

            }
            is ProgramsEvent.RenameProgram -> {
                viewModelScope.launch {
                    repository.renameProgram(event.workoutProgramRename)
                }
            }
            is ProgramsEvent.ReorderProgram -> {
                viewModelScope.launch {
                    repository.reorderPrograms(event.workoutProgramReorders)
                }
            }
            is ProgramsEvent.DeleteProgram -> {
                viewModelScope.launch {
                    // check that currentProgram in plan is not the one we are eliminating
                    val plan = repository.getPlan(state.value.programs[0].extPlanId!!).first()
                    val program = state.value.programs.first { it.programId == event.programId }
                    if (plan?.currentProgram == program.orderInWorkoutPlan){
                        // it is, need to change it
                        var newCurrentProgram = if (state.value.programs.size == 1)
                            0
                        else
                            (plan.currentProgram+1) % (state.value.programs.size-1)
                        repository.updateCurrentPlan(WorkoutPlanUpdateProgram(
                            planId = plan.planId,
                            currentProgram = newCurrentProgram
                        ))
                    }
                    repository.removeProgramFromPlan(event.programId)
                    // reorder programs after this one
                    val programs2reorder = state.value.programs.filter { it.orderInWorkoutPlan > program.orderInWorkoutPlan }
                    repository.reorderPrograms(
                        programs2reorder.map {
                            WorkoutProgramReorder(it.programId, it.orderInWorkoutPlan-1)
                        }
                    )
                }
            }
        }
    }

    fun generatePlanSummary() {
        val basicPrompt = "You are an AI gym coach and you have just created a workout plan for the user. Now you need to **briefly** explain it to the user. Max one paragraph."
        val planPrompt = "$basicPrompt Begin by describing the overall plan schedule, goal and day division."
        val days = state.value.programs.map { prg ->
            prg.name
        }
        val input = "$planPrompt. Here is the plan name (raw name for you, don't mention it to the user): ${state.value.planName}. Here are the days: $days."
        // TODO: if already generated, don't regenerate
        viewModelScope.launch(Dispatchers.IO) {
            val result = llmWrapper.generateAsync(
                input,
                { text, _ ->
                    _state.update {
                        it.copy(aiSummary = it.aiSummary + text)
                    }
                }
            )
            _state.update { it.copy(aiSummary = result) }
            if (result.isEmpty()) {
                // TODO: error
            }
        }
    }
}
