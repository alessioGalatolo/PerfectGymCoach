package agdesigns.elevatefitness.ui.screens.statistics

import agdesigns.elevatefitness.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.db.entity.Exercise
import agdesigns.elevatefitness.data.db.entity.ExerciseRecordAndEquipment
import agdesigns.elevatefitness.data.db.entity.WorkoutRecord
import agdesigns.elevatefitness.data.db.entity.WorkoutRecord.WorkoutIntensity
import agdesigns.elevatefitness.ui.common.BestColumnKey
import agdesigns.elevatefitness.ui.common.MeanLineKey
import agdesigns.elevatefitness.utils.computeVolume
import agdesigns.elevatefitness.utils.generateVolumeProgressionData
import android.util.Log
import com.agdesignes.shared.Equipment
import com.agdesignes.shared.maybeKgToLb
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.jaikeerthick.composable_graphs.composables.donut.model.DonutData
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// StatisticsState.kt
data class StatisticsState(
    val isLoading: Boolean = true,
    val useImperialSystem: Boolean = false,
    val totalWorkouts: Int = 0,
    val totalVolume: Double = 0.0,
    val avgWorkoutDuration: Long = 0L,
    val totalCalories: Double = 0.0,
    val avgCalories: Double = 0.0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val volumeChartProducer: CartesianChartModelProducer = CartesianChartModelProducer(),
    val frequencyChartProducer: CartesianChartModelProducer = CartesianChartModelProducer(),
    val muscleGroupDistribution: List<Pair<Int, Float>> = emptyList(),
    val topExercises: List<ExerciseStats> = emptyList(),
    val recentPRs: List<PersonalRecord> = emptyList(),
    val equipmentUsage: List<Pair<Int, DonutData>> = emptyList(),
    val selectedTimeFrame: TimeFrame = TimeFrame.MONTH,
    val allExerciseRecords: List<ExerciseRecordAndEquipment> = emptyList(),
    val allWorkouts: List<WorkoutRecord> = emptyList(),
    val progressTextRes: Int = R.string.stats_loading_generic,
    val volumeIndex2Date: Map<Int, String> = emptyMap()
)

data class ExerciseStats(
    val exerciseId: Long,
    val exerciseName: String,
    val timesPerformed: Int,
    val totalVolume: Float,
    val maxWeight: Float
)

data class PersonalRecord(
    val exerciseId: Long,
    val exerciseName: String,
    val weight: Float,
    val reps: Int,
    val date: ZonedDateTime
)

enum class TimeFrame(val displayResKey: String) {
    WEEK("timeframes_week"),
    MONTH("timeframes_month"),
    YEAR("timeframes_year"),
    ALL_TIME("timeframes_all_time");

    val displayResource: Int
        get() = when (this) {
            WEEK -> R.string.timeframes_week
            MONTH -> R.string.timeframes_month
            YEAR -> R.string.timeframes_year
            ALL_TIME -> R.string.timeframes_all_time
        }
}

sealed class StatisticsEvent {
    data class OnTimeFrameChanged(val timeFrame: TimeFrame) : StatisticsEvent()
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {
    private val _state = MutableStateFlow(StatisticsState())
    val state: StateFlow<StatisticsState> = _state.asStateFlow()

    var computeStatisticsJob: Job? = null

    init {
        observeData()
    }

    fun onEvent(event: StatisticsEvent) {
        when (event) {
            is StatisticsEvent.OnTimeFrameChanged -> {
                _state.update {
                    it.copy(
                        selectedTimeFrame = event.timeFrame
                    )
                }
                computeStatistics()
            }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                repository.getImperialSystem(),
                repository.getWorkoutHistory(),
                repository.getAllExerciseRecordsAndEquipment()
            ) { useImperial, workouts, exerciseRecords ->
                _state.update { it.copy(
                    allExerciseRecords = exerciseRecords,
                    allWorkouts = workouts,
                    useImperialSystem = useImperial,
                ) }
                computeStatistics()
            }.collect()
        }
    }


    private fun computeStatistics() {
        computeStatisticsJob?.cancel()
        computeStatisticsJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val timeFrame = _state.value.selectedTimeFrame

            // Calculate date range based on selected time frame
            val endDate = ZonedDateTime.now()
            val startDate = when (timeFrame) {
                TimeFrame.WEEK -> endDate.minusWeeks(1)
                TimeFrame.MONTH -> endDate.minusMonths(1)
                TimeFrame.YEAR -> endDate.minusYears(1)
                TimeFrame.ALL_TIME -> ZonedDateTime.of(
                    2020,
                    1,
                    1,
                    0,
                    0,
                    0,
                    0,
                    ZoneId.systemDefault()
                )
            }
            _state.update { it.copy(progressTextRes = R.string.stats_loading_filtering) }
            val nonEmptyWorkouts = state.value.allWorkouts.filter { it.startDate != null }
                .filter { it.durationSeconds > 0 }
            val recordList = state.value.allExerciseRecords

            val workoutsDateFiltered = nonEmptyWorkouts.filter {
                it.startDate!!.isAfter(startDate) && it.startDate.isBefore(endDate)
            }
            val recordsDateFiltered = recordList.filter {
                it.date.isAfter(startDate) && it.date.isBefore(endDate)
            }

            // Calculate basic metrics
            _state.update { it.copy(progressTextRes = R.string.stats_loading_computing) }
            val totalWorkouts = workoutsDateFiltered.size
            val totalVolume = maybeKgToLb(recordsDateFiltered.sumOf {
                computeVolume(
                    it.weights,
                    it.reps,
                    it.tare
                ).toDouble()
            }, state.value.useImperialSystem)
            val avgDuration = if (nonEmptyWorkouts.isNotEmpty()) {
                nonEmptyWorkouts.map { it.durationSeconds }.average().toLong()
            } else 0L
            val totalCalories = nonEmptyWorkouts.sumOf { it.calories.toDouble() }
            val avgCalories = if (nonEmptyWorkouts.isNotEmpty()) {
                nonEmptyWorkouts.sumOf { it.calories.toDouble() } / nonEmptyWorkouts.size
            } else 0.0

            // Calculate streaks
            _state.update { it.copy(progressTextRes = R.string.stats_loading_calculating) }
            val (currentStreak, longestStreak) = calculateStreaks(workoutsDateFiltered)

            // Generate chart data
            _state.update { it.copy(progressTextRes = R.string.stats_loading_summing) }
            val volumeProgression = generateVolumeProgressionData(
                recordsDateFiltered,
                timeFrame,
                state.value.useImperialSystem
            )
            val volumeIndex2Date = volumeProgression.mapIndexed { index, pair -> index to pair.first }.toMap()
            val maxVolume = volumeProgression.maxOfOrNull { it.second }
            val maxIndex = volumeProgression.indexOfLast { it.second == maxVolume }
            viewModelScope.launch {
                if (volumeProgression.isEmpty())
                    return@launch
                state.value.volumeChartProducer.runTransaction {
                    columnSeries {
                        series(
                            volumeProgression.indices.toList(),
                            volumeProgression.map { it.second }
                        )
                    }
                    extras {
                        val nonZeroVolumeEntries = volumeProgression.filter { it.second > 0 }.size
                        it[MeanLineKey] = volumeProgression.map { it.second / nonZeroVolumeEntries }.sum().toDouble()
                        it[BestColumnKey] = maxIndex
                    }
                }
            }
            _state.update { it.copy(progressTextRes = R.string.stats_loading_counting) }
            val monthlyWorkouts = generateMonthlyWorkoutData(nonEmptyWorkouts)
            viewModelScope.launch {
                if (monthlyWorkouts.isEmpty())
                    return@launch
                state.value.frequencyChartProducer.runTransaction {
                    columnSeries {
                        series(
                            monthlyWorkouts.map { it.first },
                            monthlyWorkouts.map { it.second }
                        )
                    }
                }
            }

            _state.update { it.copy(progressTextRes = R.string.stats_loading_computing_muscle) }
            val muscleDistribution = generateMuscleDistribution(recordsDateFiltered)
            _state.update { it.copy(progressTextRes = R.string.stats_loading_deriving) }
            val topExercises = generateTopExercises(recordsDateFiltered, state.value.useImperialSystem)
            _state.update { it.copy(progressTextRes = R.string.stats_loading_prs) }
            val recentPRs = generateRecentPRs(recordsDateFiltered, state.value.useImperialSystem)
            _state.update { it.copy(progressTextRes = R.string.stats_loading_equipment) }
            val equipmentUsage = generateEquipmentUsage(recordsDateFiltered)

            _state.update {
                it.copy(
                    isLoading = false,
                    totalWorkouts = totalWorkouts,
                    totalVolume = totalVolume,
                    avgWorkoutDuration = avgDuration,
                    totalCalories = totalCalories,
                    avgCalories = avgCalories,
                    currentStreak = currentStreak,
                    longestStreak = longestStreak,
                    volumeIndex2Date = volumeIndex2Date,
                    muscleGroupDistribution = muscleDistribution,
                    topExercises = topExercises,
                    recentPRs = recentPRs,
                    equipmentUsage = equipmentUsage,
                    progressTextRes = R.string.stats_loading_done
                )
            }
            Log.d("StatisticsViewModel", "Statistics computed successfully")
        }
    }

    private fun calculateStreaks(workouts: List<WorkoutRecord>): Pair<Int, Int> {
        if (workouts.isEmpty()) return Pair(0, 0)

        val workoutDates = workouts
            .map { it.startDate!!.toLocalDate() }
            .distinct()
            .sortedDescending()

        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 0
        var previousDate = LocalDate.now()

        for (date in workoutDates) {
            val daysDiff = ChronoUnit.DAYS.between(date, previousDate)

            if (daysDiff <= 1) {
                tempStreak++
                if (date == LocalDate.now() || date == LocalDate.now().minusDays(1)) {
                    currentStreak = tempStreak
                }
            } else {
                longestStreak = maxOf(longestStreak, tempStreak)
                tempStreak = 1
            }
            previousDate = date
        }
        longestStreak = maxOf(longestStreak, tempStreak)

        return Pair(currentStreak, longestStreak)
    }

    private fun generateMonthlyWorkoutData(
        workouts: List<WorkoutRecord>
    ): List<Pair<Long, Float>> {
        val formatter = DateTimeFormatter.ofPattern("MMM yyyy")

        var monthlyData = workouts.sortedBy { it.startDate }.groupBy { it.startDate!!.format(formatter) }.toList()

        return monthlyData.map { pair ->
            Pair(
                pair.second[0].startDate!!.toInstant().toEpochMilli(),
                pair.second.size.toFloat()
            )
        }
    }

    private suspend fun generateTopExercises(records: List<ExerciseRecordAndEquipment>, useImperialSystem: Boolean): List<ExerciseStats> {
        val exerciseStats = mutableMapOf<Long, Triple<String, Int, Float>>()

        records.forEach { record ->
            val exercise = repository.getExercise(record.extExerciseId).first()
            exercise.let {
                val volume = computeVolume(record.weights, record.reps, record.tare)

                val current = exerciseStats[record.extExerciseId]
                exerciseStats[record.extExerciseId] = Triple(
                    it.name,
                    (current?.second ?: 0) + 1,
                    (current?.third ?: 0f) + volume
                )
            }
        }

        return exerciseStats.entries.map { it ->
            val id = it.key
            val tripleData = it.value
            val name = tripleData.first
            val count = tripleData.second
            val totalVolume = maybeKgToLb(tripleData.third, useImperialSystem)
            val maxWeight = maybeKgToLb(
                records
                    .filter { record -> repository.getExercise(record.extExerciseId).first().name == name }
                    .flatMap { it.weights }
                    .maxOrNull() ?: 0f,
                useImperialSystem
            )

            ExerciseStats(id, name, count, totalVolume, maxWeight)
        }.sortedByDescending { it.totalVolume }
        .take(5)
    }

    private suspend fun generateEquipmentUsage(records: List<ExerciseRecordAndEquipment>): List<Pair<Int, DonutData>> {
        val equipmentCount = mutableMapOf<Equipment, Int>()

        records.forEach { record ->
            val exercise = repository.getExercise(record.extExerciseId).first()
            exercise.let {
                equipmentCount[it.equipment] = equipmentCount.getOrDefault(it.equipment, 0) + 1
            }
        }
        return equipmentCount.entries.mapIndexed { index, (equipment, count) ->
            Pair(
                equipment.equipmentNameResource,
                DonutData(
                    value = count.toFloat()
                )
            )
        }
    }

    private suspend fun generateMuscleDistribution(records: List<ExerciseRecordAndEquipment>): List<Pair<Int, Float>> {
        val muscleCount = mutableMapOf<Exercise.Muscle, Int>()

        records.forEach { record ->
            val exercise = repository.getExercise(record.extExerciseId).first()
            exercise.let {
                muscleCount[it.primaryMuscle] = muscleCount.getOrDefault(it.primaryMuscle, 0) + 1
            }
        }

        val totalCount = muscleCount.values.sum().toFloat()
        return muscleCount.entries.mapIndexed { index, (muscle, count) ->
            Pair(
                muscle.muscleNameResource,
                count.toFloat() / totalCount * 100f,
            )
        }
    }

    private suspend fun generateRecentPRs(records: List<ExerciseRecordAndEquipment>, useImperialSystem: Boolean): List<PersonalRecord> {
        // filter records with no weights
        val filteredRecords = records.filter { it.weights.isNotEmpty() }

        return filteredRecords
            .filter { it.date.isAfter(ZonedDateTime.now().minusDays(30)) }
            .map { record ->
                val exercise = repository.getExercise(record.extExerciseId).first()
                exercise.let {
                    val maxWeight = record.weights.maxOrNull() ?: 0f
                    val repsAtMax = record.reps[record.weights.indexOf(maxWeight)]
                    val weight = maybeKgToLb(maxWeight + record.tare, useImperialSystem)
                    PersonalRecord(
                        exerciseId = record.extExerciseId,
                        exerciseName = it.name,
                        weight = weight,
                        reps = repsAtMax,
                        date = record.date
                    )
                }
            }
            .sortedByDescending { it.date }
            .take(5)
    }

    private fun getFakeExerciseRecords(): List<ExerciseRecordAndEquipment> {
        val now = ZonedDateTime.now()
        return listOf(
            // Recent - for PRs, streaks, weekly volume
            ExerciseRecordAndEquipment(
                recordId = 1L,
                extExerciseId = 1L,
                extWorkoutId = 1L,
                exerciseInWorkout = 0,
                date = now.minusDays(1),
                reps = listOf(10, 8),
                weights = listOf(60f, 65f),
                tare = 0f,
                variation = "Standard",
                variationResKey = "",
                rest = listOf(60, 90),
                equipment = Equipment.BARBELL
            ),
            ExerciseRecordAndEquipment(
                recordId = 2L,
                extExerciseId = 2L,
                extWorkoutId = 2L,
                exerciseInWorkout = 0,
                date = now.minusDays(2),
                reps = listOf(12),
                weights = listOf(50f),
                tare = 2f,
                variation = "Incline",
                variationResKey = "",
                rest = listOf(90),
                equipment = Equipment.DUMBBELL
            ),
            ExerciseRecordAndEquipment(
                recordId = 3L,
                extExerciseId = 3L,
                extWorkoutId = 3L,
                exerciseInWorkout = 0,
                date = now.minusDays(5),
                reps = listOf(4),
                weights = listOf(100f),
                tare = 5f,
                variation = "Wide grip",
                variationResKey = "",
                rest = listOf(120),
                equipment = Equipment.BARBELL
            ),

            // This month
            ExerciseRecordAndEquipment(
                recordId = 4L,
                extExerciseId = 4L,
                extWorkoutId = 4L,
                exerciseInWorkout = 0,
                date = now.minusDays(10),
                reps = listOf(6),
                weights = listOf(70f),
                tare = 2.5f,
                variation = "Standard",
                variationResKey = "",
                rest = listOf(90),
                equipment = Equipment.DUMBBELL
            ),
            ExerciseRecordAndEquipment(
                recordId = 5L,
                extExerciseId = 5L,
                extWorkoutId = 5L,
                exerciseInWorkout = 0,
                date = now.minusDays(15),
                reps = listOf(5, 5),
                weights = listOf(80f, 85f),
                tare = 0f,
                variation = "Paused",
                variationResKey = "",
                rest = listOf(90, 120),
                equipment = Equipment.BARBELL
            ),
            ExerciseRecordAndEquipment(
                recordId = 6L,
                extExerciseId = 6L,
                extWorkoutId = 5L,
                exerciseInWorkout = 1,
                date = now.minusDays(20),
                reps = listOf(15),
                weights = listOf(40f),
                tare = 0f,
                variation = "Tempo",
                variationResKey = "",
                rest = listOf(60),
                equipment = Equipment.MACHINE
            ),

            // Older but still relevant
            ExerciseRecordAndEquipment(
                recordId = 7L,
                extExerciseId = 7L,
                extWorkoutId = 6L,
                exerciseInWorkout = 0,
                date = now.minusMonths(1).plusDays(3),
                reps = listOf(6),
                weights = listOf(90f),
                tare = 0f,
                variation = "Standard",
                variationResKey = "",
                rest = listOf(90),
                equipment = Equipment.BARBELL
            ),
            ExerciseRecordAndEquipment(
                recordId = 8L,
                extExerciseId = 8L,
                extWorkoutId = 7L,
                exerciseInWorkout = 0,
                date = now.minusMonths(2).plusDays(6),
                reps = listOf(8),
                weights = listOf(55f),
                tare = 0f,
                variation = "Close grip",
                variationResKey = "",
                rest = listOf(75),
                equipment = Equipment.DUMBBELL
            ),
            ExerciseRecordAndEquipment(
                recordId = 9L,
                extExerciseId = 9L,
                extWorkoutId = 8L,
                exerciseInWorkout = 0,
                date = now.minusMonths(3).plusDays(1),
                reps = listOf(6, 6),
                weights = listOf(60f, 65f),
                tare = 2f,
                variation = "Standard",
                variationResKey = "",
                rest = listOf(90, 90),
                equipment = Equipment.CABLES
            ),

            // Variety / Distribution
            ExerciseRecordAndEquipment(
                recordId = 10L,
                extExerciseId = 10L,
                extWorkoutId = 4L,
                exerciseInWorkout = 1,
                date = now.minusDays(3),
                reps = listOf(3),
                weights = listOf(100f),
                tare = 0f,
                variation = "PR attempt",
                variationResKey = "",
                rest = listOf(150),
                equipment = Equipment.BARBELL
            ),
            ExerciseRecordAndEquipment(
                recordId = 11L,
                extExerciseId = 11L,
                extWorkoutId = 4L,
                exerciseInWorkout = 2,
                date = now.minusDays(7),
                reps = listOf(3, 3, 2),
                weights = listOf(120f, 130f, 140f),
                tare = 0f,
                variation = "Max volume",
                variationResKey = "",
                rest = listOf(120, 150, 180),
                equipment = Equipment.BARBELL
            ),

            // Edge case: old record
            ExerciseRecordAndEquipment(
                recordId = 12L,
                extExerciseId = 12L,
                extWorkoutId = 10L,
                exerciseInWorkout = 0,
                date = now.minusYears(1).minusDays(5),
                reps = listOf(10),
                weights = listOf(45f),
                tare = 1f,
                variation = "Standard",
                variationResKey = "",
                rest = listOf(90),
                equipment = Equipment.MACHINE
            ),

            // Invalid edge cases (included for filtering tests)
            ExerciseRecordAndEquipment(
                recordId = 13L,
                extExerciseId = 13L,
                extWorkoutId = 9L,
                exerciseInWorkout = 0,
                date = now.minusDays(4),
                reps = listOf(0),
                weights = listOf(75f),
                tare = 0f,
                variation = "Empty set",
                variationResKey = "",
                rest = listOf(90),
                equipment = Equipment.BARBELL
            ),
            ExerciseRecordAndEquipment(
                recordId = 14L,
                extExerciseId = 14L,
                extWorkoutId = 9L,
                exerciseInWorkout = 1,
                date = now.minusDays(4),
                reps = listOf(),
                weights = listOf(),
                tare = 0f,
                variation = "Missing data",
                variationResKey = "",
                rest = listOf(),
                equipment = Equipment.BARBELL
            )
        )
    }

    private fun getFakeWorkoutRecords(): List<WorkoutRecord> {
        val now = ZonedDateTime.now()
        return listOf(
            // Current week
            WorkoutRecord(1, 101, now.minusDays(1), WorkoutIntensity.NORMAL_INTENSITY, 3600, 1000.0, 3200, 350f),
            WorkoutRecord(2, 101, now.minusDays(2), WorkoutIntensity.HIGH_INTENSITY, 4500, 1500.0, 4000, 450f),
            WorkoutRecord(3, 101, now.minusDays(5), WorkoutIntensity.LOW_INTENSITY, 3000, 700.0, 2500, 290f),

            // This month
            WorkoutRecord(4, 102, now.minusDays(10), WorkoutIntensity.NORMAL_INTENSITY, 3900, 1200.0, 3300, 400f),
            WorkoutRecord(5, 102, now.minusDays(18), WorkoutIntensity.HIGH_INTENSITY, 3600, 1300.0, 3000, 420f),

            // Last 3 months
            WorkoutRecord(6, 103, now.minusMonths(1).plusDays(2), WorkoutIntensity.LOW_INTENSITY, 2800, 800.0, 2300, 310f),
            WorkoutRecord(7, 103, now.minusMonths(1).plusDays(5), WorkoutIntensity.NORMAL_INTENSITY, 3500, 1000.0, 2900, 370f),
            WorkoutRecord(8, 104, now.minusMonths(2).plusDays(4), WorkoutIntensity.HIGH_INTENSITY, 4000, 1400.0, 3600, 460f),
            WorkoutRecord(9, 104, now.minusMonths(3).plusDays(3), WorkoutIntensity.NORMAL_INTENSITY, 3000, 700.0, 2600, 300f),

            // Edge of year cutoff
            WorkoutRecord(10, 105, now.minusYears(1).plusDays(1), WorkoutIntensity.HIGH_INTENSITY, 4200, 1350.0, 3700, 440f),

            // Ancient workouts
            WorkoutRecord(11, 105, now.minusYears(2), WorkoutIntensity.NORMAL_INTENSITY, 3600, 1000.0, 3100, 340f),

            // Invalid
            WorkoutRecord(12, 106, null, WorkoutIntensity.LOW_INTENSITY, 3600, 800.0, 3000, 300f), // null date
            WorkoutRecord(13, 106, now.minusDays(4), WorkoutIntensity.LOW_INTENSITY, 0, 800.0, 3000, 300f) // zero duration
        )
    }
}