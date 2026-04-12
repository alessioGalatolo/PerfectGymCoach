package agdesigns.elevatefitness.ui.screens.workout_recap

import agdesigns.elevatefitness.data.HealthConnectRepository
import agdesigns.elevatefitness.data.PreferenceRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.db.entity.ExerciseRecordAndInfo
import agdesigns.elevatefitness.data.db.entity.WorkoutRecord
import agdesigns.elevatefitness.ui.common.CurrentColumnKey
import agdesigns.elevatefitness.ui.common.highlightSeriesKey
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject

enum class HealthConnectExportStatus {
    NOT_EXPORTED, EXPORTING, EXPORTED, ERROR
}

data class RecapState(
    val workoutId: Long = 0L,
    val workoutRecord: WorkoutRecord? = null,
    val programName: String = "",
    val olderRecords: List<WorkoutRecord> = emptyList(),
    val exerciseRecords: List<ExerciseRecordAndInfo> = emptyList(),
    val volumeChartProducer: CartesianChartModelProducer = CartesianChartModelProducer(),
    val caloriesChartProducer: CartesianChartModelProducer = CartesianChartModelProducer(),
    val timeChartProducer: CartesianChartModelProducer = CartesianChartModelProducer(),
    val hasHRData: Boolean = false,
    val maxHR: Double = 0.0,
    val minHR: Double = 0.0,
    val hrChartProducer: CartesianChartModelProducer = CartesianChartModelProducer(),
    val imperialSystem: Boolean = false,
    val index2date: Map<Int, ZonedDateTime> = emptyMap(),
    val isHealthConnectAvailable: Boolean = false,
    val hasHealthConnectPermissions: Boolean = false,
    val healthConnectExportStatus: HealthConnectExportStatus = HealthConnectExportStatus.NOT_EXPORTED,
)

sealed class RecapEvent{
    data class SetWorkoutId(val workoutId: Long): RecapEvent()
    data object ExportToHealthConnect : RecapEvent()
    data object RefreshHealthConnectPermissions : RecapEvent()
}

@HiltViewModel
class RecapViewModel @Inject constructor(
    private val repository: Repository,
    private val preferences: PreferenceRepository,
    private val healthConnectRepository: HealthConnectRepository
): ViewModel() {
    private val _state = MutableStateFlow(RecapState())
    val state: StateFlow<RecapState> = _state.asStateFlow()

    private var retrieveWorkoutRecordJob: Job? = null
    private var retrieveInfoJob: Job? = null


    init {
        viewModelScope.launch {
            preferences.getImperialSystem().collect{ imperialSystem ->
                _state.update { it.copy(imperialSystem = imperialSystem) }
            }
        }
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isHealthConnectAvailable = healthConnectRepository.isAvailable,
                    hasHealthConnectPermissions = healthConnectRepository.hasAllPermissions()
                )
            }
        }
        viewModelScope.launch {
            combine(
                state.map { it.workoutRecord }.distinctUntilChanged(),
                state.map { it.hasHealthConnectPermissions }.distinctUntilChanged(),
                state.map { it.programName }.distinctUntilChanged()
            ) { record, hasPermissions, programName ->
                tryExportToHealthConnect(
                    record,
                    hasPermissions,
                    programName
                )
            }.collect()
        }
    }

    fun onEvent(event: RecapEvent){
        when (event) {
            is RecapEvent.SetWorkoutId -> {
                if (event.workoutId != state.value.workoutId) {
                    _state.update { it.copy(workoutId = event.workoutId) }
                    retrieveWorkoutRecordJob?.cancel()
                    retrieveWorkoutRecordJob = viewModelScope.launch {
                        repository.getWorkoutRecord(event.workoutId).collect{ workoutRecord ->
                            _state.update { it.copy(
                                workoutRecord = workoutRecord
                            ) }
                            retrieveInfoJob?.cancel()
                            retrieveInfoJob = viewModelScope.launch {
                                combine(
                                    repository.getWorkoutRecordsByProgram(state.value.workoutRecord!!.extProgramId),
                                    repository.getWorkoutExerciseRecordsAndInfo(event.workoutId),
                                    repository.getWorkoutRecord(event.workoutId)
                                ) { olderRecords, exerciseRecords, workoutRecord ->
                                    // TODO: maybe limit max number of records?
                                    // TODO: also maybe get stats from similar workouts
                                    val sortedRecords = olderRecords
                                        .filter { it.durationSeconds > 0 }
                                        .sortedBy { it.startDate }
                                    val sortedDistinctExercises = exerciseRecords
                                        .distinct()
                                        .filter { it.reps.isNotEmpty() }  // only keep records with actual data inside
                                        .sortedBy { it.date }
                                    val index2date =
                                        sortedRecords.mapIndexed { index, workoutRecord ->
                                            index to (workoutRecord.startDate
                                                ?: (state.value.exerciseRecords.firstOrNull()?.date
                                                    ?: ZonedDateTime.now()))
                                        }.toMap()
                                    state.value.volumeChartProducer.runTransaction {
                                        lineSeries {
                                            series(
                                                sortedRecords.indices.toList(),
                                                sortedRecords.map { it.volume })
                                            series(
                                                listOf(sortedRecords.indexOf(workoutRecord)),
                                                listOf(workoutRecord.volume)
                                            )
                                        }
                                        extras {
                                            it[highlightSeriesKey] = listOf(1)
                                        }
                                    }
                                    state.value.caloriesChartProducer.runTransaction {
                                        columnSeries {
                                            series(
                                                sortedRecords.indices.toList(),
                                                sortedRecords.map { it.calories })
                                        }
                                        extras {
                                            it[CurrentColumnKey] =
                                                sortedRecords.indexOf(workoutRecord)
                                            it[highlightSeriesKey] =
                                                listOf(1)  // doesn't make sense but is used to compute legend
                                        }
                                    }
                                    state.value.timeChartProducer.runTransaction {
                                        lineSeries {
                                            series(
                                                sortedRecords.indices.toList(),
                                                sortedRecords.map { it.durationSeconds })
                                            series(
                                                sortedRecords.indices.toList(),
                                                sortedRecords.map { it.activeTimeSeconds })
                                            series(
                                                listOf(sortedRecords.indexOf(workoutRecord)),
                                                listOf(workoutRecord.durationSeconds)
                                            )
                                            series(
                                                listOf(sortedRecords.indexOf(workoutRecord)),
                                                listOf(workoutRecord.activeTimeSeconds)
                                            )
                                        }
                                        extras {
                                            it[highlightSeriesKey] = listOf(2, 3)
                                        }
                                    }
                                    var maxHR = 0.0
                                    var minHR = 0.0
                                    var hasHRData = false
                                    if (workoutRecord.heartRates != null) {
                                        val hrMax = workoutRecord.heartRates.max()
                                        val hrMin = workoutRecord.heartRates.min()
                                        hasHRData = true
                                        maxHR = (hrMax + (10 - hrMax % 10)).toDouble()
                                        minHR = (hrMin - (hrMin % 10)).toDouble()
                                        state.value.hrChartProducer.runTransaction {
                                            lineSeries {
                                                // heartRates can be a lot. Use max 100, samples equally spaced
                                                val finalSize = minOf(workoutRecord.heartRates.size, 50000)
                                                val samplingStep = maxOf(1, (workoutRecord.heartRates.size.toFloat() / finalSize.toFloat()).toInt())
                                                val newList = workoutRecord.heartRates.slice(
                                                    0 until workoutRecord.heartRates.size step samplingStep
                                                )
                                                series(
                                                    newList.indices.toList(),
                                                    newList
                                                )
                                            }
                                        }
                                    }
                                    // Fetch program name for Health Connect export
                                    val programName = runCatching {
                                        repository.getProgram(workoutRecord.extProgramId)
                                            .first().name
                                    }.getOrDefault("")
                                    _state.update {
                                        it.copy(
                                            olderRecords = sortedRecords,
                                            exerciseRecords = sortedDistinctExercises,
                                            workoutRecord = workoutRecord,
                                            programName = programName,
                                            index2date = index2date,
                                            hasHRData = hasHRData,
                                            maxHR = maxHR,
                                            minHR = minHR
                                        )
                                    }

                                }.collect()
                            }
                        }
                    }
                }
            }
            is RecapEvent.ExportToHealthConnect -> {
                tryExportToHealthConnect(
                    state.value.workoutRecord,
                    state.value.hasHealthConnectPermissions,
                    state.value.programName
                )
            }
            is RecapEvent.RefreshHealthConnectPermissions -> {
                viewModelScope.launch {
                    _state.update {
                        it.copy(hasHealthConnectPermissions = healthConnectRepository.hasAllPermissions())
                    }
                }
            }
        }
    }

    private fun tryExportToHealthConnect(record: WorkoutRecord?, hasPermissions: Boolean, programName: String) {
        viewModelScope.launch {
            if (record == null) return@launch
            if (!hasPermissions) return@launch
            if (record.healthRecordId != null) {
                _state.update { it.copy(healthConnectExportStatus = HealthConnectExportStatus.EXPORTED) }
                return@launch
            }
            if (programName.isEmpty()) return@launch
            _state.update { it.copy(healthConnectExportStatus = HealthConnectExportStatus.EXPORTING) }
            val healthRecordId = healthConnectRepository.writeWorkout(record, programName)
            _state.update {
                it.copy(
                    healthConnectExportStatus = if (healthRecordId != null)
                        HealthConnectExportStatus.EXPORTED
                    else
                        HealthConnectExportStatus.ERROR
                )
            }
            if (healthRecordId != null) {
                repository.updateWorkoutRecordHealthId(
                    record.workoutId,
                    healthRecordId
                )
            }
        }
    }
}
