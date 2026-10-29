package agdesigns.elevatefitness.utils

import agdesigns.elevatefitness.data.db.entity.AchievementType
import agdesigns.elevatefitness.data.db.entity.ExerciseRecordAndEquipment
import agdesigns.elevatefitness.data.db.entity.WorkoutAchievement
import agdesigns.elevatefitness.data.db.entity.WorkoutRecord
import agdesigns.elevatefitness.shared.Equipment
import agdesigns.elevatefitness.shared.SetType

// estimates from high rep sets are unreliable, they should not beat a real heavy set
private const val MAX_REPS_FOR_ONE_REP_MAX = 12
// improvements smaller than this are rounding noise (e.g., kg/lb conversions)
private const val EPSILON = 0.01

private val WORKOUT_COUNT_MILESTONES = listOf(1, 10, 25, 50, 75, 100, 150, 200, 250, 300, 400, 500, 750, 1000)
private const val WORKOUT_COUNT_MILESTONE_STEP = 500
private val LIFETIME_VOLUME_MILESTONES_KG = listOf(1_000.0, 10_000.0, 50_000.0, 100_000.0, 250_000.0, 500_000.0, 1_000_000.0)
private const val LIFETIME_VOLUME_MILESTONE_STEP_KG = 1_000_000.0
// a "streak" of a single week is not much of an achievement
private const val MIN_STREAK_WEEKS = 2

private data class SetData(val reps: Int, val weight: Float, val load: Float)

// warm up sets do not count towards records
private fun ExerciseRecordAndEquipment.workingSets(): List<SetData> =
    reps.indices
        .filter { setTypes?.getOrNull(it) != SetType.WARMUP }
        .mapNotNull { i ->
            val rep = reps[i]
            val weight = weights.getOrNull(i) ?: return@mapNotNull null
            if (rep <= 0) return@mapNotNull null
            // the tare of body weight exercises is the user's weight, which changes on its own
            val load = when (equipment) {
                Equipment.BARBELL, Equipment.DUMBBELL -> weight * 2 + tare
                Equipment.BODY_WEIGHT -> weight
                else -> weight + tare
            }
            SetData(rep, weight, load)
        }

private fun ExerciseRecordAndEquipment.workingVolume(): Float {
    val sets = reps.indices.filter { setTypes?.getOrNull(it) != SetType.WARMUP && it < weights.size }
    return computeVolume(sets.map { weights[it] }, sets.map { reps[it] }, tare, equipment)
}

private fun isWorkoutCountMilestone(count: Int) =
    count in WORKOUT_COUNT_MILESTONES ||
        (count > WORKOUT_COUNT_MILESTONES.last() && count % WORKOUT_COUNT_MILESTONE_STEP == 0)

// highest milestone in (previous, current], if any
private fun lifetimeVolumeMilestone(previous: Double, current: Double): Double? {
    val milestones = LIFETIME_VOLUME_MILESTONES_KG.toMutableList()
    var next = milestones.last() + LIFETIME_VOLUME_MILESTONE_STEP_KG
    while (next <= current) {
        milestones.add(next)
        next += LIFETIME_VOLUME_MILESTONE_STEP_KG
    }
    return milestones.lastOrNull { it > previous && it <= current }
}

/**
 * Computes the achievements unlocked by [workout].
 *
 * Records are only compared against [previousRecords] of the same exercise (from finished workouts
 * done before this one), so the first time an exercise is done there is no record. Exercises with
 * a weight or one rep max record do not also get a rep record, which would be redundant. Beating a
 * best of 0 (e.g., sets logged without weight) is not a weight or volume record.
 *
 * @param workoutRecords exercise records of [workout]
 * @param previousWorkouts finished workouts done before [workout]
 * @param previousLongestStreak/newLongestStreak longest weekly streak without/with [workout]
 */
fun computeAchievements(
    workout: WorkoutRecord,
    workoutRecords: List<ExerciseRecordAndEquipment>,
    previousRecords: List<ExerciseRecordAndEquipment>,
    previousWorkouts: List<WorkoutRecord>,
    previousLongestStreak: Int,
    newLongestStreak: Int
): List<WorkoutAchievement> {
    val achievements = mutableListOf<WorkoutAchievement>()
    fun add(
        type: AchievementType,
        value: Double,
        previousValue: Double? = null,
        exerciseId: Long? = null,
        secondaryValue: Double? = null
    ) = achievements.add(
        WorkoutAchievement(
            extWorkoutId = workout.workoutId,
            type = type,
            extExerciseId = exerciseId,
            value = value,
            previousValue = previousValue,
            secondaryValue = secondaryValue
        )
    )

    val previousByExercise = previousRecords.groupBy { it.extExerciseId }
    // keep the order in which exercises were done
    workoutRecords.sortedBy { it.date }.groupBy { it.extExerciseId }.forEach { (exerciseId, records) ->
        val history = previousByExercise[exerciseId].orEmpty()
        val previousSets = history.flatMap { it.workingSets() }
        val sets = records.flatMap { it.workingSets() }
        if (sets.isEmpty() || previousSets.isEmpty()) return@forEach

        if (records.any { it.overriddenDurationBased }) {
            // reps are seconds
            val longest = sets.maxOf { it.reps }
            val previousLongest = previousSets.maxOf { it.reps }
            if (longest > previousLongest)
                add(AchievementType.LONGEST_DURATION, longest.toDouble(), previousLongest.toDouble(), exerciseId)
            return@forEach
        }

        var hasWeightRecord = false
        val heaviest = sets.maxWith(compareBy<SetData> { it.load }.thenBy { it.reps })
        val previousHeaviest = previousSets.maxOf { it.load }
        if (previousHeaviest > EPSILON && heaviest.load - previousHeaviest > EPSILON) {
            add(
                AchievementType.HEAVIEST_WEIGHT,
                heaviest.load.toDouble(),
                previousHeaviest.toDouble(),
                exerciseId,
                secondaryValue = heaviest.reps.toDouble()
            )
            hasWeightRecord = true
        }

        fun List<SetData>.bestOneRepMax() = filter { it.reps <= MAX_REPS_FOR_ONE_REP_MAX && it.load > 0f }
            .maxOfOrNull { oneRepMax(it.load, it.reps, OneRepMaxFormula.EPLEY) }
        val oneRepMax = sets.bestOneRepMax()
        val previousOneRepMax = previousSets.bestOneRepMax()
        if (oneRepMax != null && previousOneRepMax != null && previousOneRepMax > EPSILON &&
            oneRepMax - previousOneRepMax > EPSILON) {
            add(AchievementType.ESTIMATED_ONE_REP_MAX, oneRepMax.toDouble(), previousOneRepMax.toDouble(), exerciseId)
            hasWeightRecord = true
        }

        if (!hasWeightRecord) {
            // most reps with at least a given load; report the heaviest such set
            val repRecord = sets
                .mapNotNull { set ->
                    val previousBest = previousSets.filter { it.load >= set.load - EPSILON }
                        .maxOfOrNull { it.reps } ?: return@mapNotNull null
                    if (set.reps > previousBest) set to previousBest else null
                }
                .maxWithOrNull(compareBy<Pair<SetData, Int>> { it.first.load }.thenBy { it.first.reps })
            if (repRecord != null)
                add(
                    AchievementType.REP_RECORD,
                    repRecord.first.reps.toDouble(),
                    repRecord.second.toDouble(),
                    exerciseId,
                    secondaryValue = repRecord.first.load.toDouble()
                )
        }

        val volume = records.sumOf { it.workingVolume().toDouble() }
        val previousVolume = history.groupBy { it.extWorkoutId }.values
            .maxOf { workoutHistory -> workoutHistory.sumOf { it.workingVolume().toDouble() } }
        if (previousVolume > EPSILON && volume - previousVolume > EPSILON)
            add(AchievementType.EXERCISE_VOLUME, volume, previousVolume, exerciseId)
    }

    // whole workout
    if (workout.volume > 0.0) {
        val bestVolume = previousWorkouts.maxOfOrNull { it.volume }
        val bestProgramVolume = previousWorkouts.filter { it.extProgramId == workout.extProgramId }
            .maxOfOrNull { it.volume }
        if (bestVolume != null && bestVolume > EPSILON && workout.volume - bestVolume > EPSILON)
            add(AchievementType.WORKOUT_VOLUME, workout.volume, bestVolume)
        else if (bestProgramVolume != null && bestProgramVolume > EPSILON &&
            workout.volume - bestProgramVolume > EPSILON)
            add(AchievementType.PROGRAM_VOLUME, workout.volume, bestProgramVolume)
    }

    val workoutCount = previousWorkouts.size + 1
    if (isWorkoutCountMilestone(workoutCount))
        add(AchievementType.WORKOUT_COUNT, workoutCount.toDouble())

    val previousLifetimeVolume = previousWorkouts.sumOf { it.volume }
    lifetimeVolumeMilestone(previousLifetimeVolume, previousLifetimeVolume + workout.volume)?.let {
        add(AchievementType.LIFETIME_VOLUME, it)
    }

    if (newLongestStreak > previousLongestStreak && newLongestStreak >= MIN_STREAK_WEEKS)
        add(AchievementType.LONGEST_STREAK, newLongestStreak.toDouble(), previousLongestStreak.toDouble())

    return achievements
}
