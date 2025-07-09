package agdesigns.elevatefitness.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.exercise.Exercise
import agdesigns.elevatefitness.data.exercise.ProgramExercise
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.exercise.ExerciseRecord
import agdesigns.elevatefitness.data.exercise.ExerciseRecordAndEquipment
import agdesigns.elevatefitness.data.workout_exercise.WorkoutExercise
import agdesigns.elevatefitness.ui.OneRepMaxFormula
import agdesigns.elevatefitness.ui.estimate1RM
import agdesigns.elevatefitness.ui.generateVolumeProgressionData
import android.util.Log
import com.jaikeerthick.composable_graphs.composables.line.model.LineData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.lang.Integer.min
import javax.inject.Inject
import kotlin.math.max

data class ExerciseStatsState(
    val imperialSystem: Boolean = false,
    val exercise: Exercise? = null,
    val exerciseRecords: List<ExerciseRecordAndEquipment> = emptyList(),
    val oneRepMaxFormula: OneRepMaxFormula = OneRepMaxFormula.EPLEY,
    val volumeProgression: List<LineData> = emptyList(),
    val maxWeights: List<Float> = emptyList(),
    val maxReps: List<Int> = emptyList(),
    val avgWeight: List<Float> = emptyList(),
    val avgReps: List<Float> = emptyList(),
    val oneRepMaxs: List<Float> = emptyList()
)

sealed class ExerciseStatsEvent{
    data class StartRetrievingData(
        val exerciseId: Long
    ): ExerciseStatsEvent()

    data class ChangeOneRepMaxFormula(
        val newFormula: OneRepMaxFormula
    ): ExerciseStatsEvent()
}

@HiltViewModel
class ExerciseStatsViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = MutableStateFlow(ExerciseStatsState())
    val state: StateFlow<ExerciseStatsState> = _state.asStateFlow()

    private var getDataJob: Job? = null

    init {
        viewModelScope.launch {
            repository.getImperialSystem().collect { imperialSystem ->
                _state.update {
                    it.copy(
                        imperialSystem = imperialSystem
                    )
                }
            }
        }
    }

    fun onEvent(event: ExerciseStatsEvent): Boolean {
        when (event) {
            is ExerciseStatsEvent.StartRetrievingData -> {
                if (getDataJob == null) {
                    getDataJob = viewModelScope.launch {
                        retrieveData(
                            event.exerciseId,
                        )
                    }
                }
            }

            is ExerciseStatsEvent.ChangeOneRepMaxFormula -> {
                _state.update {
                    it.copy(
                        oneRepMaxFormula = event.newFormula
                    )
                }
                computeStats()
            }
        }
        return true
    }

    private suspend fun retrieveData(exerciseId: Long) {
        // retrieve exercise records
        combine(
            repository.getExercise(exerciseId),
            repository.getExerciseRecordsAndEquipment(exerciseId)
        ) { exercise, exerciseRecords ->

            _state.update {
                it.copy(
                    exercise = exercise,
                    exerciseRecords = exerciseRecords,
                )
            }
            computeStats()
        }.collect()
    }

    private fun computeStats() {
        // computeStats
        if (state.value.exerciseRecords.isNotEmpty()) {
            val exerciseRecords = state.value.exerciseRecords
            // exerciseRecords may be A LOT. If more than 20, split plots
            val maxRecordsPerPlot = 20
            val maxLabelsPerPlot = 5
            var numPlots = exerciseRecords.size / maxRecordsPerPlot
            if (exerciseRecords.size % maxRecordsPerPlot != 0) {
                numPlots++
            }
            val volumeProgression = generateVolumeProgressionData(
                exerciseRecords,
                maxRecords = Int.MAX_VALUE,
                maxLabels = numPlots * maxLabelsPerPlot
            )
            val maxWeights = exerciseRecords.map { it.weights.max() + it.tare }
            val maxReps = exerciseRecords.map { it.reps.max() }
            val avgWeight = exerciseRecords.map { it.weights.average().toFloat() + it.tare }
            val avgReps = exerciseRecords.map { it.reps.average().toFloat() }
            val oneRepMaxs = exerciseRecords.map { estimate1RM(it, state.value.oneRepMaxFormula) }
            _state.update {
                it.copy(
                    volumeProgression = volumeProgression,
                    maxWeights = maxWeights,
                    maxReps = maxReps,
                    avgWeight = avgWeight,
                    avgReps = avgReps,
                    oneRepMaxs = oneRepMaxs
                )
            }
        }
    }
}
