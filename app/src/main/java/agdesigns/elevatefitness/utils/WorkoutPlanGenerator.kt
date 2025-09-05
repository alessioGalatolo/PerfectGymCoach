package agdesigns.elevatefitness.utils

import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.db.entity.Exercise
import agdesigns.elevatefitness.data.db.entity.Exercise.Muscle
import agdesigns.elevatefitness.data.db.entity.ProgramExercise
import agdesigns.elevatefitness.data.db.entity.UpdateExerciseSuperset
import agdesigns.elevatefitness.data.db.entity.WorkoutPlan
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanDifficulty
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanGoal
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanSplit
import agdesigns.elevatefitness.data.db.entity.WorkoutProgram
import agdesigns.elevatefitness.data.db.entity.getGeneratedPlanName
import agdesigns.elevatefitness.data.db.entity.getGeneratedProgramName
import android.util.Log
import com.agdesignes.shared.Equipment
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random


// Extension function for Collection
fun <T> Collection<T>.weightedRandom(weights: List<Double>): T {
    if (this.size != weights.size) {
        throw IllegalArgumentException("Items and weights must be of the same size")
    }
    val items = this.toList()
    if (items.isEmpty()) throw NoSuchElementException("Cannot choose from empty collection")

    // Normalise and handle edge cases (negative or all-zero -> uniform)
    val clipped = weights.map { w -> if (w.isFinite() && w > 0.0) w else 0.0 }
    val sum = clipped.sum()
    if (sum <= 0.0) {
        return items.random()
    }

    var cumulative = 0.0
    val cumulativeWeights = DoubleArray(clipped.size) { idx ->
        cumulative += clipped[idx] / sum
        cumulative
    }
    val r = Random.nextDouble()
    for (i in cumulativeWeights.indices) {
        if (r <= cumulativeWeights[i]) return items[i]
    }
    return items.last() // numerical safety
}

// Use these for *non-compound* default prescriptions.
fun getRepsAndRest(goal: WorkoutPlanGoal): Pair<IntProgression, IntProgression> {
    return when (goal) {
        WorkoutPlanGoal.STRENGTH -> (6..10 step 2) to (90..150 step 30)  // iso/accessory strength
        WorkoutPlanGoal.HYPERTROPHY -> (8..12 step 1) to (60..90 step 15)
        WorkoutPlanGoal.ENDURANCE -> (12..20 step 2) to (45..90 step 15)
        WorkoutPlanGoal.CARDIO -> (15..25 step 5) to (0..0)
    }
}

// Split mapping; we’ll optionally duplicate later to ensure ≥2×/week frequency.
fun getMuscleSplit(workoutSplit: WorkoutPlanSplit, goal: WorkoutPlanGoal): List<List<Muscle>> {
    return when (workoutSplit) {
        WorkoutPlanSplit.FULL_BODY -> {
            // Create 3 distinct full-body sessions with exercise variety and recovery consideration
            when (goal) {
                WorkoutPlanGoal.STRENGTH -> listOf(
                    // Day A: Focus on horizontal movements
                    listOf(Muscle.CHEST, Muscle.BACK, Muscle.QUADRICEPS, Muscle.SHOULDERS, Muscle.TRICEPS),
                    // Day B: Focus on vertical movements + posterior chain
                    listOf(Muscle.BACK, Muscle.SHOULDERS, Muscle.HAMSTRINGS, Muscle.GLUTES, Muscle.BICEPS),
                    // Day C: Balanced with core emphasis
                    listOf(Muscle.CHEST, Muscle.QUADRICEPS, Muscle.HAMSTRINGS, Muscle.TRICEPS, Muscle.ABS, Muscle.CALVES)
                )
                WorkoutPlanGoal.HYPERTROPHY -> listOf(
                    // Day A: Upper emphasis
                    listOf(Muscle.CHEST, Muscle.BACK, Muscle.SHOULDERS, Muscle.QUADRICEPS, Muscle.ABS),
                    // Day B: Lower emphasis
                    listOf(Muscle.QUADRICEPS, Muscle.HAMSTRINGS, Muscle.GLUTES, Muscle.CHEST, Muscle.TRICEPS, Muscle.BICEPS),
                    // Day C: Balanced with arms
                    listOf(Muscle.BACK, Muscle.SHOULDERS, Muscle.HAMSTRINGS, Muscle.TRICEPS, Muscle.BICEPS, Muscle.CALVES)
                )
                else -> listOf(
                    // Simplified 3-day rotation for endurance/cardio
                    listOf(Muscle.CHEST, Muscle.BACK, Muscle.QUADRICEPS, Muscle.ABS),
                    listOf(Muscle.SHOULDERS, Muscle.HAMSTRINGS, Muscle.GLUTES, Muscle.TRICEPS, Muscle.BICEPS),
                    listOf(Muscle.BACK, Muscle.QUADRICEPS, Muscle.HAMSTRINGS, Muscle.CALVES)
                )
            }
        }
        WorkoutPlanSplit.BRO -> listOf(
            listOf(Muscle.CHEST, Muscle.SHOULDERS, Muscle.TRICEPS),
            listOf(Muscle.BACK, Muscle.BICEPS, Muscle.ABS),  // I'd prefer to put it in legs but then it's too long
            listOf(Muscle.QUADRICEPS, Muscle.HAMSTRINGS, Muscle.GLUTES, Muscle.CALVES)
        )
        WorkoutPlanSplit.UPPER_LOWER -> listOf(
            listOf(Muscle.CHEST, Muscle.BACK, Muscle.SHOULDERS, Muscle.TRICEPS, Muscle.BICEPS),
            listOf(Muscle.QUADRICEPS, Muscle.HAMSTRINGS, Muscle.GLUTES, Muscle.CALVES, Muscle.ABS)
        )
        WorkoutPlanSplit.GAINZ -> listOf(
            listOf(Muscle.CHEST),
            listOf(Muscle.BACK),
            listOf(Muscle.SHOULDERS, Muscle.ABS),
            listOf(Muscle.TRICEPS, Muscle.BICEPS),
            listOf(Muscle.QUADRICEPS,  Muscle.CALVES),
            listOf(Muscle.HAMSTRINGS, Muscle.GLUTES)
        )
        WorkoutPlanSplit.AUTO -> throw IllegalArgumentException("AUTO split should have been resolved already")
    }
}

// No supersets for high-skill compounds; prefer antagonists/non-competing; allow same small muscle pairing when requested.
fun shouldPairForSuperset(
    difficulty: WorkoutPlanDifficulty,
    split: WorkoutPlanSplit,
    muscle1: Muscle?,
    muscle2: Muscle?,
    isSameMuscleOkay: Boolean
): Boolean {
    if (difficulty == WorkoutPlanDifficulty.BEGINNER) return false
    if (muscle1 == null || muscle2 == null) return false

    // Only allow on splits that typically support density work
    if (split !in listOf(WorkoutPlanSplit.BRO, WorkoutPlanSplit.UPPER_LOWER, WorkoutPlanSplit.GAINZ)) return false

    // Same-muscle small accessory pairing allowed optionally (laterals + rear delts; curls + hammer curls)
    if (isSameMuscleOkay) {
        val small = setOf(Muscle.SHOULDERS, Muscle.BICEPS, Muscle.TRICEPS, Muscle.ABS, Muscle.CALVES)
        if (muscle1 == muscle2 && muscle1 in small) return true
    }

    // Antagonist pairs are ideal
    val antagonists = setOf(
        setOf(Muscle.CHEST, Muscle.BACK),
        setOf(Muscle.TRICEPS, Muscle.BICEPS),
        setOf(Muscle.QUADRICEPS, Muscle.HAMSTRINGS)
    )
    val pair = setOf(muscle1, muscle2)
    if (pair in antagonists) return true

    // Otherwise allow non-competing upper vs lower accessories
    val upper = setOf(Muscle.CHEST, Muscle.BACK, Muscle.SHOULDERS, Muscle.BICEPS, Muscle.TRICEPS)
    val lower = setOf(Muscle.QUADRICEPS, Muscle.HAMSTRINGS, Muscle.GLUTES, Muscle.CALVES)
    return (muscle1 in upper && muscle2 in lower) || (muscle1 in lower && muscle2 in upper)
}

fun isMajorMover(muscle: Muscle): Boolean {
    return when (muscle) {
        Muscle.CHEST,
        Muscle.BACK,
        Muscle.SHOULDERS,
        Muscle.QUADRICEPS -> true
        else -> false
    }
}

fun isFreeWeight(equipment: Equipment): Boolean {
    return when (equipment) {
        Equipment.BARBELL,
        Equipment.DUMBBELL,
        Equipment.BODY_WEIGHT -> true
        else -> false
    }
}

fun exerciseIsCompound(exercise: Exercise): Boolean {
    if (!isFreeWeight(exercise.equipment))
        return false
    if (!isMajorMover(exercise.primaryMuscle))
        return false
    if (exercise.secondaryMuscles.size > 1)
        return true
    return false
}

// Enhanced muscle selection logic for full-body workouts
fun selectFullBodyExercises(
    muscle: Muscle,
    dayIndex: Int,
    goal: WorkoutPlanGoal,
    exercisePool: List<Exercise>
): List<Exercise> {
    // Prioritize compound movements that work multiple muscles efficiently
    val compounds = exercisePool.filter { exerciseIsCompound(it)  }
    val isolation = exercisePool.filter { !exerciseIsCompound(it) }

    return when (goal) {
        WorkoutPlanGoal.STRENGTH -> {
            // Prioritize heavy compounds, minimal isolation
            compounds.take(1) + isolation.take(0)
        }
        WorkoutPlanGoal.HYPERTROPHY -> {
            // Mix of compound and isolation for volume
            compounds.take(1) + isolation.take(1)
        }
        else -> {
            // Endurance/cardio: lighter compounds, some isolation
            compounds.take(1) + isolation.take(if (dayIndex % 2 == 0) 1 else 0)
        }
    }
}

/**
 * Evidence-based plan generator:
 * - Ensures ~2x/week frequency for muscles on hypertrophy/strength (duplicating BRO as PPLx2 pattern).
 * - Uses weekly hard-set targets by goal x difficulty.
 * - Prioritises compounds, caps per-session sets per muscle, assigns reps/rest by goal.
 * - Deprioritises recently used exercises.
 * - Supersets only non-competing accessories.
 */
suspend fun generatePlan(
    repository: Repository,
    goalChoice: WorkoutPlanGoal,
    expertiseLevel: WorkoutPlanDifficulty,
    workoutSplit: WorkoutPlanSplit
): Long {
    // TODO: ideally it might also take into consideration profile values e.g. sex, age, weight, etc.
    // TODO: manipulate probabilities -> decrease prob of selected exs. and increase prob of older ones
    val now = ZonedDateTime.now()
    val seededRandom = Random(now.toInstant().toEpochMilli())

    // ---------- Resolve split & difficulty ----------
    val resolvedDifficulty = resolveDifficulty(expertiseLevel)
    val resolvedSplit = resolveSplit(goalChoice, resolvedDifficulty, workoutSplit)

    // Base split → day → muscles
    val muscleDays = getMuscleSplit(resolvedSplit, goalChoice)

    // ---------- Fetch exercise pools ----------
    // Build map muscle -> mutable list of exercises
    val muscle2Exercises = mutableMapOf<Muscle, MutableList<Exercise>>()
    val allMuscles = Muscle.entries.toMutableList().minus(Muscle.EVERYTHING)
    for (m in allMuscles) {
        val pool = repository.getExercises(m).first().toMutableList()
        // Filter by difficulty
        val filtered = when (resolvedDifficulty) {
            WorkoutPlanDifficulty.BEGINNER -> pool.filter { it.difficulty != Exercise.ExerciseDifficulty.ADVANCED }
            else -> pool
        }.toMutableList()
        muscle2Exercises[m] = filtered
    }

    // ---------- Deprioritise exercises used in current plan ----------
    repository.getCurrentPlan().first()?.let { currentPlanId ->
        val programsMap = repository.getProgramsMapExercises(currentPlanId).first() // Map<ProgramId, List<ProgramExercise>>
        val oldExerciseIds = programsMap.values.flatten().map { it.extExerciseId }.toSet()
        for ((_, list) in muscle2Exercises) {
            for (i in list.indices) {
                if (list[i].exerciseId in oldExerciseIds) {
                    list[i] = list[i].copy(probability = (list[i].probability * 0.5).coerceAtLeast(0.01))
                }
            }
        }
    }

    // ---------- Create plan ----------
    val planId = repository.addPlan(
        WorkoutPlan(
            name = getGeneratedPlanName(goalChoice, now),
            creationDate = now
        )
    )

    // ---------- Volume targets ----------
    val weeklyTargetSets = targetWeeklySetsByMuscle(goalChoice, resolvedDifficulty, allMuscles)

    // Track used exercises across plan to diversify accessories; allow compounds to repeat
    val usedAccessoryIds = mutableSetOf<Long>()

    // Cached reps/rest defaults for accessories
    val (accessoryRepRange, accessoryRestRange) = getRepsAndRest(goalChoice)

    // Count how many times each muscle appears in the final schedule
    val appearances = countOccurrences(muscleDays).withDefault { 1 }

    muscleDays.forEachIndexed { dayIndex, musclesOfDay ->
        val programId = repository.addProgram(
            WorkoutProgram(
                extPlanId = planId,
                name = getGeneratedProgramName(musclesOfDay),
                orderInWorkoutPlan = dayIndex
            )
        )

        var lastAccessoryForSuperset: ProgramExercise? = null
        var lastAccessoryMuscle: Muscle? = null
        var orderInProgram = 0

        // Sort muscles so big movers scheduled first (compounds first while fresh)
        val musclesOrdered = musclesOfDay.sortedByDescending {
            if (isMajorMover(it)) 2
            else if (listOf(Muscle.ABS, Muscle.CALVES).contains(it)) 0
            else 1
        }
        var sanityCheckTotalTime = 0.0

        musclesOrdered.forEach { muscle ->
            val weekly = weeklyTargetSets[muscle] ?: 0
            if (weekly <= 0) return@forEach
            val times = max(appearances.getValue(muscle), 1)
            var setsThisSession = floor(weekly.toDouble() / times).toInt()

            // Per-session caps by muscle size
//            setsThisSession = min(setsThisSession, if (isMajorMover(muscle)) perSessionSetCapMajor else perSessionSetCapMinor)
            if (setsThisSession <= 0) return@forEach

            val pool = muscle2Exercises[muscle].orEmpty()
            if (pool.isEmpty()) return@forEach

            // Split sets between compounds and accessories
            val compounds = pool.filter { exerciseIsCompound(it) }
            val accessories = pool.filter { !exerciseIsCompound(it) }

            // Decide how many compound exercises
            val wantCompounds = when {
                goalChoice == WorkoutPlanGoal.STRENGTH && isMajorMover(muscle) -> min(2, compounds.size)
                isMajorMover(muscle) && resolvedDifficulty != WorkoutPlanDifficulty.BEGINNER -> min(1, compounds.size)
                else -> 0
            }

            var accessorySets = setsThisSession
            // --- Add compound lifts first ---
            if (wantCompounds > 0) {
                val chosenCompounds = chooseExercisesWeighted(
                    compounds,
                    count = wantCompounds,
                    avoid = emptySet(), // allow repeats across days for compounds
                    seededRandom = seededRandom
                )

                // TODO: this is assuming one compound only
                val minCompoundSets = 4 * wantCompounds
                var candidateCompoundSets = minCompoundSets
                if (goalChoice == WorkoutPlanGoal.STRENGTH || resolvedDifficulty > WorkoutPlanDifficulty.BEGINNER) {
                    candidateCompoundSets++ // even if we have two compounds we only add one more set
                }
                if (setsThisSession - candidateCompoundSets <= 1){
                    // don't leave an exercise with just one set
                    candidateCompoundSets = setsThisSession
                }
                accessorySets -= candidateCompoundSets
                val setsPerCompound = splitSets(candidateCompoundSets, chosenCompounds.size, minPer = 3, maxPer = 6)

                chosenCompounds.forEachIndexed { idx, ex ->
                    val (repsEachSet, restEachSet) = prescribeCompound(goalChoice)
                    val repsList = MutableList(setsPerCompound[idx]) { repsEachSet }
                    val restList = MutableList(setsPerCompound[idx]) { restEachSet }
                    repository.addProgramExercise(
                        ProgramExercise(
                            extProgramId = programId,
                            extExerciseId = ex.exerciseId,
                            orderInProgram = orderInProgram++,
                            reps = repsList,
                            rest = restList,
                            variation = "",
                            variationResKey = ""
                        )
                    )
                    // add rest time plus 2 seconds per rep
                    sanityCheckTotalTime += restList.sum() + 2.0 * repsList.sum()
                }
            }

            // --- Add accessories ---
            if (accessorySets > 0 && accessories.isNotEmpty()) {
                // Spread across 1–3 accessories
                val accessoryExerciseCount = min(when {
                    accessorySets <= 4 -> 1
                    accessorySets <= 6 -> 2
                    else -> 3
                }, accessories.size)

                val chosenAccessories = chooseExercisesWeighted(
                    accessories,
                    count = accessoryExerciseCount,
                    avoid = usedAccessoryIds, // diversify across the week
                    seededRandom = seededRandom
                )

                val setsPerAccessory = splitSets(accessorySets, chosenAccessories.size, minPer = 2, maxPer = 5)

                chosenAccessories.forEachIndexed { idx, ex ->
                    val reps = sampleFromProgression(accessoryRepRange, seededRandom)
                    val rest = sampleFromProgression(accessoryRestRange, seededRandom)
                    val repsList = MutableList(setsPerAccessory[idx]) { reps }
                    val restList = MutableList(setsPerAccessory[idx]) { rest }

                    // wait adding program exercise to check if it should be in superset
                    var programExercise = ProgramExercise(
                        extProgramId = programId,
                        extExerciseId = ex.exerciseId,
                        orderInProgram = orderInProgram++,
                        reps = repsList,
                        rest = restList,
                        variation = "",
                        variationResKey = ""
                    )
                    sanityCheckTotalTime += restList.sum() + 2.0 * repsList.sum()

                    // Try to make a superset with the previous accessory if valid
                    val shouldBeSuperset = lastAccessoryForSuperset != null &&
                        shouldPairForSuperset(
                            difficulty = resolvedDifficulty,
                            split = resolvedSplit,
                            muscle1 = lastAccessoryMuscle,
                            muscle2 = muscle,
                            isSameMuscleOkay = true
                        )
                    if (shouldBeSuperset) {
                        val supersetSets = lastAccessoryForSuperset!!.reps.size
                        if (repsList.size > supersetSets) {
                            programExercise = programExercise.copy(
                                reps = repsList.take(supersetSets),
                                rest = restList.take(supersetSets)
                            )
                        } else if (repsList.size < supersetSets) {
                            programExercise = programExercise.copy(
                                reps = repsList + List(supersetSets - repsList.size) { repsList.last() },
                                rest = restList + List(supersetSets - restList.size) { restList.last() }
                            )
                        }
                        programExercise = programExercise.copy(
                            supersetExercise = lastAccessoryForSuperset!!.programExerciseId,

                        )
                    }
                    programExercise = programExercise.copy(
                        programExerciseId = repository.addProgramExercise(programExercise)
                    )
                    if (shouldBeSuperset) {
                        repository.updateExerciseSuperset(
                            listOf(
                                UpdateExerciseSuperset(
                                    lastAccessoryForSuperset!!.programExerciseId,
                                    programExercise.programExerciseId
                                )
                            )
                        )
                        lastAccessoryForSuperset = null
                        lastAccessoryMuscle = null
                    } else {
                        lastAccessoryForSuperset = programExercise
                        lastAccessoryMuscle = muscle

                    }

                    usedAccessoryIds += ex.exerciseId
                }
            }
        }
        Log.d("Plan", "sanity check for $musclesOfDay: ${sanityCheckTotalTime / 60}")
    }

    return planId
}


// Target weekly sets per muscle by goal × difficulty. Conservative, evidence-based anchors.
private fun targetWeeklySetsByMuscle(
    goal: WorkoutPlanGoal,
    difficulty: WorkoutPlanDifficulty,
    muscles: List<Muscle>
): Map<Muscle, Int> {
    // Baselines
    val (majorBase, minorBase) = when (goal) {
        WorkoutPlanGoal.HYPERTROPHY -> when (difficulty) {
            WorkoutPlanDifficulty.BEGINNER -> 10 to 8
            WorkoutPlanDifficulty.INTERMEDIATE, WorkoutPlanDifficulty.AUTO -> 14 to 10
            WorkoutPlanDifficulty.ADVANCED -> 16 to 12
        }
        WorkoutPlanGoal.STRENGTH -> when (difficulty) {
            WorkoutPlanDifficulty.BEGINNER -> 8 to 6
            WorkoutPlanDifficulty.INTERMEDIATE, WorkoutPlanDifficulty.AUTO -> 10 to 8
            WorkoutPlanDifficulty.ADVANCED -> 12 to 8
        }
        WorkoutPlanGoal.ENDURANCE -> 8 to 6
        WorkoutPlanGoal.CARDIO -> 6 to 4
    } // workaround for when clauses; set below

    val majors = setOf(
        Muscle.CHEST,
        Muscle.BACK,
        Muscle.QUADRICEPS,
        Muscle.HAMSTRINGS
    )

    val map = mutableMapOf<Muscle, Int>()
    muscles.forEach { m ->
        val isMajor = m in majors
        val base = if (isMajor) majorBase else minorBase
        map[m] = base
    }
    return map
}

// Compound prescription per set by goal (for big lifts)
private fun prescribeCompound(goal: WorkoutPlanGoal): Pair<Int, Int> {
    return when (goal) {
        WorkoutPlanGoal.STRENGTH -> {
            // 3–5 reps primary strength work, long rest
            3 to 210 // seconds; caller repeats for set count
        }
        WorkoutPlanGoal.HYPERTROPHY -> {
            // 5–8 reps compounds, moderate rest
            6 to 150
        }
        WorkoutPlanGoal.ENDURANCE -> {
            // keep compounds lighter, more reps but still controlled
            10 to 90
        }
        WorkoutPlanGoal.CARDIO -> {
            8 to 90
        }
    }
}

private fun resolveDifficulty(d: WorkoutPlanDifficulty): WorkoutPlanDifficulty {
    return when (d) {
        // TODO: use heuristic to estimate expertise
        WorkoutPlanDifficulty.AUTO -> WorkoutPlanDifficulty.INTERMEDIATE
        else -> d
    }
}

private fun resolveSplit(
    goal: WorkoutPlanGoal,
    difficulty: WorkoutPlanDifficulty,
    requested: WorkoutPlanSplit
): WorkoutPlanSplit {
    if (requested != WorkoutPlanSplit.AUTO) return requested
    return when (goal) {
        WorkoutPlanGoal.STRENGTH -> if (difficulty == WorkoutPlanDifficulty.BEGINNER) WorkoutPlanSplit.FULL_BODY else WorkoutPlanSplit.UPPER_LOWER
        WorkoutPlanGoal.HYPERTROPHY -> if (difficulty == WorkoutPlanDifficulty.BEGINNER) WorkoutPlanSplit.UPPER_LOWER else if (difficulty == WorkoutPlanDifficulty.INTERMEDIATE) WorkoutPlanSplit.BRO else WorkoutPlanSplit.GAINZ
        WorkoutPlanGoal.ENDURANCE, WorkoutPlanGoal.CARDIO -> WorkoutPlanSplit.FULL_BODY
    }
}

private fun countOccurrences(days: List<List<Muscle>>): Map<Muscle, Int> {
    val counts = mutableMapOf<Muscle, Int>().withDefault { 0 }
    days.flatten().forEach { m -> counts[m] = counts.getValue(m) + 1 }
    return counts
}

private fun chooseExercisesWeighted(
    pool: List<Exercise>,
    count: Int,
    avoid: Set<Long>,
    seededRandom: Random
): List<Exercise> {
    if (pool.isEmpty() || count <= 0) return emptyList()
    val mutable = pool.toMutableList()
    val chosen = mutableListOf<Exercise>()
    repeat(count) {
        val candidates = mutable.filter { it.exerciseId !in avoid }
        val pickFrom = candidates.ifEmpty { mutable }
        val picked = pickFrom.weightedRandom(pickFrom.map { it.probability.toDouble() })
        chosen += picked
        mutable.remove(picked)
        if (mutable.isEmpty()) return@repeat
    }
    return chosen
}

private fun splitSets(total: Int, parts: Int, minPer: Int, maxPer: Int): List<Int> {
    if (total <= 0 || parts <= 0) return emptyList()
    val effectiveParts = min(parts, total) // you can’t have more “buckets” than sets
    // Try to keep >= minPer, but if that’s impossible, use floor(total/effectiveParts)
    val basePer = min(max(minPer, total / effectiveParts), maxPer)
    var remaining = total
    val out = MutableList(effectiveParts) { 0 }
    // first pass: assign base
    for (i in 0 until effectiveParts) {
        val give = min(basePer, remaining)
        out[i] = give
        remaining -= give
    }
    // second pass: distribute any remainder up to maxPer
    var i = 0
    while (remaining > 0 && out.any { it < maxPer }) {
        if (out[i] < maxPer) { out[i]++; remaining-- }
        i = (i + 1) % effectiveParts
    }
    // if still remainder (shouldn’t), tack onto last
    if (remaining > 0) out[effectiveParts - 1] += remaining
    return out.filter { it > 0 }
}


private fun sampleFromProgression(p: IntProgression, rng: Random): Int {
    val list = p.toList()
    return list[rng.nextInt(list.size)]
}
