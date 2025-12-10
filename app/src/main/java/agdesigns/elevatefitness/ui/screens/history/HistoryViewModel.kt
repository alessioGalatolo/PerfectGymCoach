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
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

data class HistoryScreenListItem(
    val workout: WorkoutRecordAndName,
    val showYearHeader: Boolean,
    val year: Int,
    val showWeekHeader: Boolean,
    val week: Int
)

data class HistoryScreenCalendarItem(
    val week: Int,
    val year: Int,
    val workouts: Int,
    val showYearHeader: Boolean
)

data class HistoryState(
    val workoutRecords: Map<Int, Map<Int, List<WorkoutRecordAndName>>> = emptyMap(),
    val mainList: List<HistoryScreenListItem> = emptyList(),
    val calendarList: List<HistoryScreenCalendarItem> = emptyList(),
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
                val groupByYear = filteredRecords.groupBy { record -> record.startDate?.year ?:
                // BUG: some old records may have a null start date; try to infer it from the exercise records
                (
                        repository.getWorkoutExerciseRecords(record.workoutId)
                            .first()
                            .firstOrNull()?.date?.year ?: 2025
                ) }
                val weekField = WeekFields.of(Locale.getDefault()).weekOfYear()
                val yearToWeekToRecord = groupByYear.mapValues {
                    it.value.groupBy { record -> record.startDate?.get(weekField) ?:
                    (
                            repository.getWorkoutExerciseRecords(record.workoutId)
                                .first()
                                .firstOrNull()?.date?.get(weekField) ?: 42
                    ) }
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
                        records.forEach {
                            mainList.add(
                                HistoryScreenListItem(
                                    workout = it,
                                    showYearHeader = showYearHeader,
                                    showWeekHeader = showWeekHeader,
                                    year = year,
                                    week = week
                                )
                            )
                            showWeekHeader = false
                            showYearHeader = false
                        }
                    }
                }

                // create the calendar list

                // Find the earliest workout across all years
                val earliestYear = yearToWeekToRecord.keys.minOrNull() ?: currentYear
                val earliestWeek = yearToWeekToRecord[earliestYear]?.keys?.minOrNull() ?: 1
                // Generate list of (year, week) pairs from current to earliest
                val calendarList = buildList {
                    var year = currentYear
                    var week = currentWeek
                    var showYearHeader = false

                    while (year > earliestYear || (year == earliestYear && week >= earliestWeek)) {
                        add(
                            HistoryScreenCalendarItem(
                                year = year,
                                week = week,
                                workouts = yearToWeekToRecord[year]?.get(week)?.size ?: 0,
                                showYearHeader = showYearHeader
                            )
                        )
                        showYearHeader = false
                        week--
                        if (week < 1) {
                            showYearHeader = true
                            year--
                            if (year >= earliestYear) {
                                // Get the last week of the previous year
                                val lastDayOfYear = ZonedDateTime.of(year, 12, 31, 0, 0, 0, 0,
                                    ZoneId.systemDefault())
                                week = lastDayOfYear.get(weekField)
                            }
                        }
                    }
                }
                _state.update { it.copy(
                    workoutRecords = yearToWeekToRecord,
                    mainList = mainList,
                    calendarList = calendarList
                ) }
            }
        }
    }

    fun onEvent(event: HistoryEvent){
        when (event) {

            else -> {}
        }
    }

}
