package agdesigns.elevatefitness.data.db

import agdesigns.elevatefitness.data.db.entity.ExerciseRecordAndEquipment
import agdesigns.elevatefitness.data.db.entity.WorkoutRecord
import agdesigns.elevatefitness.utils.StreakWorkout
import agdesigns.elevatefitness.utils.calculateWeeklyStreaks
import agdesigns.elevatefitness.utils.computeAchievements
import kotlinx.coroutines.flow.first
import java.time.temporal.WeekFields
import java.util.Locale

private suspend fun WorkoutDatabase.longestStreakCalculator(): (List<WorkoutRecord>, WorkoutRecord, Long?) -> Int {
    val planPrograms = workoutPlanDao.getPlanMapPrograms().first()
    val programToPlan = planPrograms.flatMap { (plan, programs) ->
        programs.map { it.programId to plan.planId }
    }.toMap()
    val planSizes = planPrograms.entries.associate { it.key.planId to it.value.size }
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    // longest streak as of the day of [workout]
    return { workouts, workout, currentPlanId ->
        calculateWeeklyStreaks(
            workouts = workouts.map {
                StreakWorkout(it.startDate!!.toLocalDate(), programToPlan[it.extProgramId])
            },
            planSizes = planSizes,
            currentPlanId = currentPlanId,
            firstDayOfWeek = firstDayOfWeek,
            today = workout.startDate!!.toLocalDate()
        ).longest
    }
}

// workouts that can be compared against, i.e., finished and with a known date
private fun List<WorkoutRecord>.finished() = filter { it.durationSeconds > 0 && it.startDate != null }

private suspend fun WorkoutDatabase.storeAchievements(
    workout: WorkoutRecord,
    workoutRecords: List<ExerciseRecordAndEquipment>,
    previousWorkouts: List<WorkoutRecord>,
    previousRecords: List<ExerciseRecordAndEquipment>,
    longestStreak: (List<WorkoutRecord>, WorkoutRecord, Long?) -> Int,
    currentPlanId: Long?
) {
    val achievements = computeAchievements(
        workout = workout,
        workoutRecords = workoutRecords,
        previousRecords = previousRecords,
        previousWorkouts = previousWorkouts,
        previousLongestStreak = longestStreak(previousWorkouts, workout, currentPlanId),
        newLongestStreak = longestStreak(previousWorkouts + workout, workout, currentPlanId)
    )
    workoutAchievementDao.insertIfAbsent(workout.workoutId, achievements)
}

/**
 * Computes and stores the achievements of a finished workout. Does nothing if the workout
 * already has achievements stored, they are never recomputed.
 */
suspend fun WorkoutDatabase.computeWorkoutAchievements(workoutId: Long, currentPlanId: Long?) {
    if (workoutAchievementDao.countByWorkout(workoutId) > 0) return
    val workout = workoutRecordDao.getRecord(workoutId).first() ?: return
    val startDate = workout.startDate ?: return
    val previousWorkouts = workoutRecordDao.getRecords().first().finished().filter {
        it.workoutId != workoutId && it.startDate!!.isBefore(startDate)
    }
    val previousWorkoutIds = previousWorkouts.map { it.workoutId }.toSet()
    val records = exerciseRecordDao.getRecordsWithEquipment(
        exerciseRecordDao.getByWorkout(workoutId).first().map { it.extExerciseId }.distinct()
    ).first()
    storeAchievements(
        workout = workout,
        workoutRecords = records.filter { it.extWorkoutId == workoutId },
        previousWorkouts = previousWorkouts,
        previousRecords = records.filter { it.extWorkoutId in previousWorkoutIds },
        longestStreak = longestStreakCalculator(),
        currentPlanId = currentPlanId
    )
}

/**
 * Computes the achievements of all finished workouts that have none stored (e.g., done before
 * achievements existed), each one as if it had just been finished: only workouts done before it
 * are taken into account.
 */
suspend fun WorkoutDatabase.backfillWorkoutAchievements() {
    val withAchievements = workoutAchievementDao.getWorkoutIds().toSet()
    val workouts = workoutRecordDao.getRecords().first().finished().sortedBy { it.startDate }
    val recordsByWorkout = exerciseRecordDao.getAllRecordsWithEquipment().first().groupBy { it.extWorkoutId }
    val longestStreak = longestStreakCalculator()
    val previousRecords = mutableListOf<ExerciseRecordAndEquipment>()
    workouts.forEachIndexed { index, workout ->
        val workoutRecords = recordsByWorkout[workout.workoutId].orEmpty()
        if (workout.workoutId !in withAchievements)
            storeAchievements(
                workout = workout,
                workoutRecords = workoutRecords,
                previousWorkouts = workouts.subList(0, index),
                previousRecords = previousRecords,
                longestStreak = longestStreak,
                // the plan followed at the time is inferred from the workouts done
                currentPlanId = null
            )
        previousRecords.addAll(workoutRecords)
    }
}
