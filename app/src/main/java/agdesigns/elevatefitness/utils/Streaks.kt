package agdesigns.elevatefitness.utils

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class StreakWorkout(
    val date: LocalDate,
    val planId: Long? // null for workouts not bound to a plan
)

data class Streaks(val current: Int, val longest: Int)

/**
 * Streaks are counted in weeks: a week is kept when the user completed at least as many workouts
 * as there are programs in the plan they were following that week (e.g., a 2-day plan requires
 * 2 workouts per week). Without a known plan, a single workout keeps the week.
 *
 * The plan followed in a past week is the one most used that week, or the last one followed before
 * it. For the current week the user's current plan wins, so switching plan applies immediately.
 * The current week never breaks the streak since it is still in progress.
 */
fun calculateWeeklyStreaks(
    workouts: List<StreakWorkout>,
    planSizes: Map<Long, Int>,
    currentPlanId: Long?,
    firstDayOfWeek: DayOfWeek,
    today: LocalDate = LocalDate.now()
): Streaks {
    if (workouts.isEmpty()) return Streaks(0, 0)
    fun weekStartOf(date: LocalDate) = date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))

    val workoutsByWeek = workouts.groupBy { weekStartOf(it.date) }
    val currentWeek = weekStartOf(today)
    var week = workoutsByWeek.keys.min()

    var activePlan: Long? = null
    var current = 0
    var longest = 0
    while (!week.isAfter(currentWeek)) {
        val weekWorkouts = workoutsByWeek[week].orEmpty()
        weekWorkouts.mapNotNull { it.planId }
            .groupingBy { it }.eachCount()
            .maxByOrNull { it.value }?.key
            ?.let { activePlan = it }
        val plan = if (week == currentWeek) currentPlanId ?: activePlan else activePlan
        val target = plan?.let { planSizes[it] }?.coerceAtLeast(1) ?: 1

        if (weekWorkouts.size >= target) {
            current++
            longest = maxOf(longest, current)
        } else if (week != currentWeek) {
            current = 0
        }
        week = week.plusWeeks(1)
    }
    return Streaks(current, longest)
}
