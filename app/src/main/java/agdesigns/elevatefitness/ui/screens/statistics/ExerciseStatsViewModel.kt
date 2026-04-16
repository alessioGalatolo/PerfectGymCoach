package agdesigns.elevatefitness.ui.screens.statistics

import agdesigns.elevatefitness.data.PreferenceRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.db.entity.Exercise
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.db.entity.ExerciseRecordAndEquipment
import agdesigns.elevatefitness.ui.common.BestColumnKey
import agdesigns.elevatefitness.ui.common.MeanLineKey
import agdesigns.elevatefitness.utils.OneRepMaxFormula
import agdesigns.elevatefitness.utils.computeVolume
import agdesigns.elevatefitness.utils.estimate1RM
import agdesigns.elevatefitness.utils.generateVolumeProgressionData
import agdesigns.elevatefitness.shared.Equipment
import agdesigns.elevatefitness.shared.SetType
import agdesigns.elevatefitness.shared.maybeKgToLb
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ExerciseStatsState(
    val imperialSystem: Boolean = false,
    val exercise: Exercise? = null,
    val exerciseRecords: List<ExerciseRecordAndEquipment> = emptyList(),
    val oneRepMaxFormula: OneRepMaxFormula = OneRepMaxFormula.EPLEY,
    val volumeProgressionAllProducer: CartesianChartModelProducer = CartesianChartModelProducer(),
    val volumeProgressionMonthProducer: CartesianChartModelProducer = CartesianChartModelProducer(),
    val maxWeightsProducer: CartesianChartModelProducer = CartesianChartModelProducer(),
    val maxRepsProducer: CartesianChartModelProducer = CartesianChartModelProducer(),
    val avgWeightProducer: CartesianChartModelProducer = CartesianChartModelProducer(),
    val avgRepsProducer: CartesianChartModelProducer = CartesianChartModelProducer(),
    val oneRepMaxsProducer: CartesianChartModelProducer = CartesianChartModelProducer(),
    val indices2dates: Map<Int, String> = emptyMap(),
    val volumeMonthIndex2Date: Map<Int, String> = emptyMap(),
    // Per-session date mapping for non-volume charts (one entry per workout session, ascending)
    val perSessionIndex2Date: Map<Int, String> = emptyMap(),
    // Summary stats
    val totalSessions: Int = 0,
    val personalBestWeight: Float = 0f,
    val totalVolume: Float = 0f,
    val bestOneRepMax: Float = 0f,
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
class ExerciseStatsViewModel @Inject constructor(
    private val repository: Repository,
    private val preferences: PreferenceRepository
): ViewModel() {
    private val _state = MutableStateFlow(ExerciseStatsState())
    val state: StateFlow<ExerciseStatsState> = _state.asStateFlow()

    private var getDataJob: Job? = null

    init {
        viewModelScope.launch {
            preferences.getImperialSystem().collect { imperialSystem ->
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
                    // only keep records with recorded reps/weights
                    exerciseRecords = exerciseRecords
                        .filter { it.reps.isNotEmpty() }
                        .sortedByDescending { it.date },
                )
            }
            computeStats()
        }.collect()
    }

    private fun computeStats() {
        if (state.value.exerciseRecords.isNotEmpty()) {
            val exerciseRecords = state.value.exerciseRecords
            val imperialSystem = state.value.imperialSystem

            // Per-session charts need ascending order (oldest → newest left to right)
            val sortedRecords = exerciseRecords.sortedBy { it.date }

            // Summary stats
            val totalSessions = sortedRecords.size
            val personalBestWeight = sortedRecords.maxOfOrNull {
                val weight = maybeKgToLb((it.weights.maxOrNull() ?: 0f), imperialSystem)
                if (it.equipment == Equipment.DUMBBELL || it.equipment == Equipment.BARBELL)
                    weight / 2
                else
                    weight
            } ?: 0f
            val totalVolume = sortedRecords.sumOf { record ->
                maybeKgToLb(
                    computeVolume(record.weights, record.reps, record.tare, record.equipment),
                    imperialSystem
                ).toDouble()
            }.toFloat()
            val bestOneRepMax = sortedRecords.maxOfOrNull {
                maybeKgToLb(estimate1RM(it, state.value.oneRepMaxFormula), imperialSystem)
            } ?: 0f

            // Date label for each session (ascending, one entry per workout session)
            val sessionDateFmt = DateTimeFormatter.ofPattern("d MMM")
            val perSessionIndex2Date = sortedRecords.mapIndexed { index, record ->
                index to record.date.format(sessionDateFmt)
            }.toMap()

            // Volume progression (groups by month)
            val volumeProgressionAll = generateVolumeProgressionData(
                exerciseRecords,
                TimeFrame.ALL_TIME,
                imperialSystem
            )
            val volumeAllIndex2Date = volumeProgressionAll.mapIndexed { index, pair -> index to pair.first }.toMap()
            val maxVolumeAll = volumeProgressionAll.maxOfOrNull { it.second } ?: 0f
            val maxVolumeAllIndex = volumeProgressionAll.indexOfLast { it.second == maxVolumeAll }
            viewModelScope.launch {
                if (volumeProgressionAll.isEmpty())
                    return@launch
                state.value.volumeProgressionAllProducer.runTransaction {
                    columnSeries {
                        series(
                            volumeProgressionAll.indices.toList(),
                            volumeProgressionAll.map { it.second }
                        )
                    }
                    extras {
                        val nonZeroVolumeEntries = volumeProgressionAll.filter { it.second > 0 }.size
                        it[BestColumnKey] = maxVolumeAllIndex
                        if (nonZeroVolumeEntries == 0) return@extras
                        it[MeanLineKey] = volumeProgressionAll.map { it.second / nonZeroVolumeEntries }.sum().toDouble()
                    }
                }
            }
            val startDate = ZonedDateTime.now().minusMonths(1)
            val volumeProgressionMonth = generateVolumeProgressionData(
                exerciseRecords.filter { it.date.isAfter(startDate) },
                TimeFrame.MONTH,
                imperialSystem
            )
            val volumeMonthIndex2Date = volumeProgressionMonth.mapIndexed { index, pair -> index to pair.first }.toMap()
            val maxVolumeMonth = volumeProgressionMonth.maxOfOrNull { it.second } ?: 0f
            val maxVolumeMonthIndex = volumeProgressionMonth.indexOfLast { it.second == maxVolumeMonth }
            viewModelScope.launch {
                if (volumeProgressionMonth.isEmpty())
                    return@launch
                state.value.volumeProgressionMonthProducer.runTransaction {
                    columnSeries {
                        series(
                            volumeProgressionMonth.indices.toList(),
                            volumeProgressionMonth.map { it.second }
                        )
                    }
                    extras {
                        val nonZeroVolumeEntries = volumeProgressionMonth.filter { it.second > 0 }.size
                        it[BestColumnKey] = maxVolumeMonthIndex
                        if (nonZeroVolumeEntries == 0) return@extras
                        it[MeanLineKey] = volumeProgressionMonth.map { it.second / nonZeroVolumeEntries }.sum().toDouble()
                    }
                }
            }

            // Per-session charts — use sortedRecords (ascending) for correct chronological order,
            // and apply imperial conversion to weight-based values
            val maxWeights = sortedRecords.map { maybeKgToLb((it.weights.maxOrNull() ?: 0f) + it.tare, imperialSystem) }
            viewModelScope.launch {
                if (maxWeights.isEmpty())
                    return@launch
                state.value.maxWeightsProducer.runTransaction {
                    columnSeries {
                        series(
                            maxWeights.indices.toList(),
                            maxWeights
                        )
                    }
                }
            }
            val maxReps = sortedRecords.map { it.reps.maxOrNull() ?: 0 }
            viewModelScope.launch {
                if (maxReps.isEmpty())
                    return@launch
                state.value.maxRepsProducer.runTransaction {
                    columnSeries {
                        series(
                            maxReps.indices.toList(),
                            maxReps
                        )
                    }
                }
            }
            val avgWeight = sortedRecords.map { maybeKgToLb(it.weights.average().toFloat() + it.tare, imperialSystem) }
            viewModelScope.launch {
                if (avgWeight.isEmpty())
                    return@launch
                state.value.avgWeightProducer.runTransaction {
                    columnSeries {
                        series(
                            avgWeight.indices.toList(),
                            avgWeight
                        )
                    }
                }
            }
            val avgReps = sortedRecords.map { it.reps.average().toFloat() }
            viewModelScope.launch {
                if (avgReps.isEmpty())
                    return@launch
                state.value.avgRepsProducer.runTransaction {
                    columnSeries {
                        series(
                            avgReps.indices.toList(),
                            avgReps
                        )
                    }
                }
            }
            val oneRepMaxs = sortedRecords.map { maybeKgToLb(estimate1RM(it, state.value.oneRepMaxFormula), imperialSystem) }
            viewModelScope.launch {
                if (oneRepMaxs.isEmpty())
                    return@launch
                state.value.oneRepMaxsProducer.runTransaction {
                    columnSeries {
                        series(
                            oneRepMaxs.indices.toList(),
                            oneRepMaxs
                        )
                    }
                }
            }
            _state.update {
                it.copy(
                    indices2dates = volumeAllIndex2Date,
                    volumeMonthIndex2Date = volumeMonthIndex2Date,
                    perSessionIndex2Date = perSessionIndex2Date,
                    totalSessions = totalSessions,
                    personalBestWeight = personalBestWeight,
                    totalVolume = totalVolume,
                    bestOneRepMax = bestOneRepMax,
                )
            }
        }
    }

    private fun getFakeRecords(): List<ExerciseRecordAndEquipment> {
        return buildList {
            val zone = ZoneId.systemDefault()

            // Create data for the last 2 years, ~ every 3–4 days
            var id = 1L
            val startDate = ZonedDateTime.now(zone).minusYears(2)
            var currentDate = startDate

            while (currentDate.isBefore(ZonedDateTime.now(zone))) {
                val reps = when ((1..3).random()) {
                    1 -> listOf(5, 5, 5)
                    2 -> listOf(10, 8, 6)
                    else -> listOf(12, 12, 12, 12)
                }
                val weights = reps.map { (40..120).random().toFloat() }
                val tare = listOf(0f, 20f).random()
                val rest = reps.map { listOf(60, 90, 120).random() }

                add(
                    ExerciseRecordAndEquipment(
                        recordId = id++,
                        extExerciseId = (1..5).random().toLong(), // simulate different exercises
                        extWorkoutId = (1..20).random().toLong(),
                        extWorkoutExerciseId = (1L..3L).random(),
                        exerciseInWorkout = (1..3).random(),
                        date = currentDate,
                        reps = reps,
                        weights = weights,
                        tare = tare,
                        variation = "variation_$id",
                        variationResKey = "variation_key_$id",
                        rest = rest,
                        equipment = Equipment.BARBELL,
                        overriddenDurationBased = false,
                        setTypes = List(reps.size) { SetType.NORMAL }
                    )
                )

                // progress time irregularly to make sparse data
                currentDate = currentDate.plusDays((2..6).random().toLong())
            }
        }
    }
}
