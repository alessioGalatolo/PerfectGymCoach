package agdesigns.elevatefitness.ui.screens.history

import agdesigns.elevatefitness.data.PreferenceRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.db.entity.WorkoutRecordAndName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

data class HistoryScreenListItem(
    val workout: WorkoutRecordAndName,
    val showYearHeader: Boolean,
    val year: Int,
    val showWeekHeader: Boolean,
    val week: Int,
    // first day of the week the workout belongs to, as used by the heatmap; null if the date is unknown
    val weekStart: LocalDate?,
    // set on the first workout done with a new plan, null if the plan did not change
    val planChange: PlanChange? = null
)

data class PlanChange(
    val fromPlanId: Long,
    val fromPlanName: String,
    val toPlanId: Long,
    val toPlanName: String
)

data class HeatmapWorkout(
    val workoutId: Long,
    val name: String,
    val date: ZonedDateTime,
    val durationSeconds: Long
)

data class HeatmapWeek(
    val weekStart: LocalDate,
    val workouts: Int,
    val isFuture: Boolean
)

// one block per year: weeks grouped by the month in which they start
data class HeatmapYear(
    val year: Int,
    val months: List<List<HeatmapWeek>>
)

data class HistoryState(
    val workoutRecords: Map<Int, Map<Int, List<WorkoutRecordAndName>>> = emptyMap(),
    val mainList: List<HistoryScreenListItem> = emptyList(),
    val heatmap: List<HeatmapYear> = emptyList(),
    val maxWeeklyWorkouts: Int = 0,
    // workouts of each week (keyed by its first day), oldest first
    val weekWorkouts: Map<LocalDate, List<HeatmapWorkout>> = emptyMap(),
    val useImperialSystem: Boolean = false
)

sealed class HistoryEvent{

}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: Repository,
    private val preferences: PreferenceRepository
): ViewModel() {
    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.getImperialSystem().collect { imperialSystem ->
                _state.update { it.copy(useImperialSystem = imperialSystem) }
            }
        }
        viewModelScope.launch {
            repository.getWorkoutHistoryAndName().collect { records ->
                val filteredRecords = records
                    .filter { it.durationSeconds > 0 }
                // BUG: some old records may have a null start date; try to infer it from the exercise records
                val recordDates = filteredRecords.associate { record ->
                    record.workoutId to (record.startDate ?: repository.getWorkoutExerciseRecords(record.workoutId)
                        .first()
                        .firstOrNull()?.date)
                }
                val groupByYear = filteredRecords.groupBy { record ->
                    recordDates[record.workoutId]?.year ?: 2025
                }
                val weekField = WeekFields.of(Locale.getDefault()).weekOfYear()
                val yearToWeekToRecord = groupByYear.mapValues {
                    it.value.groupBy { record ->
                        recordDates[record.workoutId]?.get(weekField) ?: 42
                    }
                }

                // a workout marks a plan change when its plan differs from the one of the previous
                // plan-bound workout; unbound workouts are ignored
                val planChanges = mutableMapOf<Long, PlanChange>()
                var previous: WorkoutRecordAndName? = null
                filteredRecords.sortedBy { recordDates[it.workoutId] }.forEach { record ->
                    val planId = record.extPlanId ?: return@forEach
                    val previousPlanId = previous?.extPlanId
                    if (previousPlanId != null && planId != previousPlanId)
                        planChanges[record.workoutId] = PlanChange(
                            fromPlanId = previousPlanId,
                            fromPlanName = previous?.planName ?: "",
                            toPlanId = planId,
                            toPlanName = record.planName ?: ""
                        )
                    previous = record
                }

                // create the main history list of records
                val mainList = mutableListOf<HistoryScreenListItem>()

                val currentYear = ZonedDateTime.now().year
                val currentWeek = ZonedDateTime.now().get(weekField)

                var yearIteration = currentYear
                var weekIteration = currentWeek
                for ((year, weekRecords) in yearToWeekToRecord.toSortedMap(compareByDescending { it })) {
                    var showYearHeader = if (year != yearIteration) {
                        yearIteration = year
                        // we are changing year, update weekIteration
                        // Get the last week of the previous year
                        val lastDayOfYear = ZonedDateTime.of(year, 12, 31, 0, 0, 0, 0,
                            ZoneId.systemDefault())
                        weekIteration = lastDayOfYear.get(weekField)+1  // +1 so that we show the week header

                        true
                    } else false
                    for ((week, records) in weekRecords.toSortedMap(compareByDescending { it })) {
                        var showWeekHeader = if (week != weekIteration) {
                            weekIteration = week
                            true
                        } else false
                        records.sortedByDescending { it.startDate }.forEach {
                            mainList.add(
                                HistoryScreenListItem(
                                    workout = it,
                                    showYearHeader = showYearHeader,
                                    showWeekHeader = showWeekHeader,
                                    year = year,
                                    week = week,
                                    weekStart = recordDates[it.workoutId]?.toLocalDate()?.let(::weekStartOf),
                                    planChange = planChanges[it.workoutId]
                                )
                            )
                            showWeekHeader = false
                            showYearHeader = false
                        }
                    }
                }

                val weekWorkouts = filteredRecords
                    .mapNotNull { record ->
                        recordDates[record.workoutId]?.let { date ->
                            HeatmapWorkout(record.workoutId, record.name, date, record.durationSeconds)
                        }
                    }
                    .sortedBy { it.date }
                    .groupBy { weekStartOf(it.date.toLocalDate()) }
                val weeklyCounts = weekWorkouts.mapValues { it.value.size }
                _state.update { it.copy(
                    workoutRecords = yearToWeekToRecord,
                    mainList = mainList,
                    heatmap = buildHeatmap(weeklyCounts),
                    maxWeeklyWorkouts = weeklyCounts.values.maxOrNull() ?: 0,
                    weekWorkouts = weekWorkouts
                ) }
            }
        }
    }

    private fun weekStartOf(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(WeekFields.of(Locale.getDefault()).firstDayOfWeek))

    private fun buildHeatmap(weeklyCounts: Map<LocalDate, Int>): List<HeatmapYear> {
        val currentWeekStart = weekStartOf(LocalDate.now())
        val earliestYear = weeklyCounts.keys.minOfOrNull { it.year } ?: return emptyList()
        return (currentWeekStart.year downTo earliestYear).map { year ->
            // every week is assigned to the year and month of its first day
            var weekStart = weekStartOf(LocalDate.of(year, 1, 1))
            if (weekStart.year < year) weekStart = weekStart.plusWeeks(1)
            val weeks = buildList {
                while (weekStart.year == year) {
                    add(HeatmapWeek(
                        weekStart = weekStart,
                        workouts = weeklyCounts[weekStart] ?: 0,
                        isFuture = weekStart.isAfter(currentWeekStart)
                    ))
                    weekStart = weekStart.plusWeeks(1)
                }
            }
            HeatmapYear(
                year = year,
                months = (1..12).map { month -> weeks.filter { it.weekStart.monthValue == month } }
            )
        }
    }

    fun onEvent(event: HistoryEvent){
        when (event) {

            else -> {}
        }
    }

}
