package agdesigns.elevatefitness.ui.screens.history

import agdesigns.elevatefitness.data.PreferenceRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.db.entity.WorkoutRecordAndName
import agdesigns.elevatefitness.data.db.entity.WorkoutAchievementAndInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

// minimum number of days without workouts for a break to be marked in the history
private const val MIN_BREAK_DAYS = 7L

data class HistoryScreenListItem(
    val workout: WorkoutRecordAndName,
    // set on the first (most recent) workout of a year other than the current one
    val yearHeader: Int? = null,
    // set on the first (most recent) workout of each month
    val monthHeader: MonthSummary? = null,
    // first day of the week the workout belongs to, as used by the heatmap; null if the date is unknown
    val weekStart: LocalDate?,
    // set on the first workout done with a new plan, null if the plan did not change
    val planChange: PlanChange? = null,
    // days without workouts between this workout and the previous one, null if the break is short
    val breakDays: Long? = null
)

data class MonthSummary(
    val month: YearMonth,
    val workouts: Int,
    val volume: Float
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
    val mainList: List<HistoryScreenListItem> = emptyList(),
    val heatmap: List<HeatmapYear> = emptyList(),
    val maxWeeklyWorkouts: Int = 0,
    // workouts of each week (keyed by its first day), oldest first
    val weekWorkouts: Map<LocalDate, List<HeatmapWorkout>> = emptyMap(),
    // achievements of each workout, keyed by workoutId
    val achievements: Map<Long, List<WorkoutAchievementAndInfo>> = emptyMap(),
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
            repository.getAllWorkoutAchievements().collect { achievements ->
                _state.update { it.copy(achievements = achievements.groupBy { a -> a.extWorkoutId }) }
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

                // create the main history list of records, most recent first; workouts with an
                // unknown date go at the end, without headers
                val sortedRecords = filteredRecords.sortedWith(
                    compareByDescending(nullsFirst()) { recordDates[it.workoutId] }
                )
                val monthSummaries = sortedRecords
                    .groupBy { recordDates[it.workoutId]?.let(YearMonth::from) }
                    .mapNotNull { (month, records) ->
                        month?.let { MonthSummary(it, records.size, records.sumOf { r -> r.volume.toDouble() }.toFloat()) }
                    }
                    .associateBy { it.month }
                val currentYear = LocalDate.now().year
                val mainList = sortedRecords.mapIndexed { i, record ->
                    val date = recordDates[record.workoutId]?.toLocalDate()
                    val newer = sortedRecords.getOrNull(i - 1)?.let { recordDates[it.workoutId]?.toLocalDate() }
                    val older = sortedRecords.getOrNull(i + 1)?.let { recordDates[it.workoutId]?.toLocalDate() }
                    val month = date?.let(YearMonth::from)
                    val newMonth = month != null && month != newer?.let(YearMonth::from)
                    val newYear = date != null && date.year != (newer?.year ?: currentYear)
                    HistoryScreenListItem(
                        workout = record,
                        yearHeader = if (newYear) date.year else null,
                        monthHeader = if (newMonth) monthSummaries[month] else null,
                        weekStart = date?.let(::weekStartOf),
                        planChange = planChanges[record.workoutId],
                        breakDays = if (date != null && older != null)
                            (ChronoUnit.DAYS.between(older, date) - 1).takeIf { it >= MIN_BREAK_DAYS }
                        else null
                    )
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
