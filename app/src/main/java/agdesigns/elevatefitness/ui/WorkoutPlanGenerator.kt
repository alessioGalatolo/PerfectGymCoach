package agdesigns.elevatefitness.ui

import android.icu.util.Calendar
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.exercise.Exercise
import agdesigns.elevatefitness.data.exercise.ProgramExercise
import agdesigns.elevatefitness.data.exercise.UpdateExerciseSuperset
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlan
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlanDifficulty
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlanGoal
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlanSplit
import agdesigns.elevatefitness.data.workout_program.WorkoutProgram
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.random.Random


// Extension function for Collection
fun <T> Collection<T>.weightedRandom(weights: List<Double>): T {
    // TODO: may check whether T has a weight attribute but needs to be done at runtime
    if (this.size != weights.size) {
        throw IllegalArgumentException("Items and weights must be of the same size")
    }

    // Convert collection to a list (if it isn't already a list)
    val itemList = this.toList()

    // Step 1: Calculate cumulative weights
    val cumulativeWeights = mutableListOf<Double>()
    var cumulativeSum = 0.0
    for (weight in weights) {
        cumulativeSum += weight
        cumulativeWeights.add(cumulativeSum)
    }

    // Step 2: Generate a random number
    val randomValue = Random.nextDouble() * cumulativeSum

    // Step 3: Find the item corresponding to the random number
    for ((index, cumWeight) in cumulativeWeights.withIndex()) {
        if (randomValue <= cumWeight) {
            return itemList[index]
        }
    }

    // Fallback, should not happen if weights are properly specified
    return itemList.last()
}

fun getRepsAndRest(goal: WorkoutPlanGoal): Pair<IntProgression, IntProgression> {
    // this is only used for non-compound exercises
    return when (goal) {
        WorkoutPlanGoal.STRENGTH -> 6..10 step 2 to (75..120 step 15)
        WorkoutPlanGoal.HYPERTROPHY -> 8..12 step 2 to (60..90 step 15)
        WorkoutPlanGoal.ENDURANCE -> 14..20 step 2 to (30..60 step 15)
        WorkoutPlanGoal.CARDIO -> 20..40 step 5 to 0..0
    }
}

// get muscles per day from workout split
fun getMuscleSplit(workoutSplit: WorkoutPlanSplit): List<List<Exercise.Muscle>> {
    return when (workoutSplit) {
        WorkoutPlanSplit.FULL_BODY -> listOf(Exercise.Muscle.entries.toMutableList().minus(Exercise.Muscle.EVERYTHING))
        WorkoutPlanSplit.BRO -> listOf(
            listOf(Exercise.Muscle.CHEST, Exercise.Muscle.SHOULDERS, Exercise.Muscle.TRICEPS),
            listOf(Exercise.Muscle.BACK, Exercise.Muscle.BICEPS),
            listOf(Exercise.Muscle.QUADRICEPS, Exercise.Muscle.HAMSTRINGS, Exercise.Muscle.GLUTES, Exercise.Muscle.CALVES, Exercise.Muscle.ABS)
        )
        WorkoutPlanSplit.UPPER_LOWER -> listOf(
            listOf(Exercise.Muscle.CHEST, Exercise.Muscle.BACK, Exercise.Muscle.SHOULDERS, Exercise.Muscle.TRICEPS, Exercise.Muscle.BICEPS),
            listOf(Exercise.Muscle.QUADRICEPS, Exercise.Muscle.HAMSTRINGS, Exercise.Muscle.GLUTES, Exercise.Muscle.CALVES, Exercise.Muscle.ABS)
        )
        WorkoutPlanSplit.GAINZ -> listOf(
            listOf(Exercise.Muscle.CHEST),
            listOf(Exercise.Muscle.BACK),
            listOf(Exercise.Muscle.SHOULDERS),
            listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.BICEPS),
            listOf(Exercise.Muscle.QUADRICEPS, Exercise.Muscle.HAMSTRINGS, Exercise.Muscle.GLUTES, Exercise.Muscle.CALVES),
            listOf(Exercise.Muscle.ABS)
        )
        WorkoutPlanSplit.AUTO -> throw IllegalArgumentException("AUTO split should have been resolved already")
    }
}

// Function to determine if two exercises should be paired as a superset
fun shouldPairForSuperset(
    difficulty: WorkoutPlanDifficulty,
    split: WorkoutPlanSplit,
    muscle1: Exercise.Muscle?,
    muscle2: Exercise.Muscle?,
    isSameMuscleOkay: Boolean
): Boolean {
    if (difficulty == WorkoutPlanDifficulty.BEGINNER) return false
    if (split !in listOf(WorkoutPlanSplit.BRO, WorkoutPlanSplit.UPPER_LOWER, WorkoutPlanSplit.GAINZ)) return false
    if (muscle1 == null || muscle2 == null) return false

    if (isSameMuscleOkay) {
        return muscle1 == muscle2 && muscle1 in listOf(Exercise.Muscle.SHOULDERS, Exercise.Muscle.BICEPS, Exercise.Muscle.TRICEPS)
    }

    // Pairing logic: allow certain major movers (e.g., chest and back), avoid the same muscle group
    val opposingPairs = setOf(
        setOf(Exercise.Muscle.CHEST, Exercise.Muscle.BACK),
        setOf(Exercise.Muscle.QUADRICEPS, Exercise.Muscle.HAMSTRINGS)
    )
    val musclesPair = setOf(muscle1, muscle2)
    return muscle1 != muscle2 && (!isMajorMover(muscle1) || !isMajorMover(muscle2) || opposingPairs.contains(musclesPair))
}

suspend fun generatePlan(
    repository: Repository,
    goalChoice: WorkoutPlanGoal,
    expertiseLevel: WorkoutPlanDifficulty,
    workoutSplit: WorkoutPlanSplit
): Long {
    // TODO: ideally it might also take into consideration profile values e.g. sex, age, weight, etc.
    val muscle2Exercises = emptyMap<Exercise.Muscle, Array<Exercise>>().toMutableMap()
    val currentTime = Calendar.getInstance().timeInMillis
    val random = Random(currentTime)
    for (muscle in Exercise.Muscle.entries.toMutableList().minus(Exercise.Muscle.EVERYTHING)){
        muscle2Exercises[muscle] = repository.getExercises(muscle).first().toTypedArray()
    }

    // lower weights for exercises in current programs
    val currentPlanId = repository.getCurrentPlan().first()
    if (currentPlanId != null) {
        val currentPrograms = repository.getProgramsMapExercises(currentPlanId).first()
        val oldExercises = currentPrograms.values.flatten().map { it.extExerciseId }
        for ((_, exercises) in muscle2Exercises) {
            for (exIndex in exercises.indices) {
                if (oldExercises.contains(exercises[exIndex].exerciseId)) {
                    exercises[exIndex] = exercises[exIndex].copy(probability = exercises[exIndex].probability * 0.5f)
                }
            }
        }


    }
    val planId = repository.addPlan(
        WorkoutPlan(
            name = "${goalChoice.name.lowercase()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }} program ${SimpleDateFormat("d MMM (yyyy)").format(currentTime)}",
            creationDate = currentTime
        )
    )

    val nonAutoSplit = if (workoutSplit == WorkoutPlanSplit.AUTO) WorkoutPlanSplit.entries.toTypedArray()
        .random() else workoutSplit
    val muscleDays = getMuscleSplit(nonAutoSplit)

    // TODO: this is not really auto
    val nonAutoDifficulty = if (expertiseLevel == WorkoutPlanDifficulty.AUTO) WorkoutPlanDifficulty.ADVANCED else expertiseLevel

    // in beginner include at most one Intermediate exercise per program
    // in advanced include advanced exercises
    // in intermediate do nothing in particular

    var minExercises = 4
    var minSetsPerExercise = 3
    var increment = 2
    if (goalChoice == WorkoutPlanGoal.STRENGTH) {
        // lower number of exercises for strength
        minExercises = 3
        minSetsPerExercise = 4
        increment = 1
    } else if (nonAutoDifficulty != WorkoutPlanDifficulty.BEGINNER) {
        minExercises += 2
        increment += 1
        minSetsPerExercise += 1
    }
    if (nonAutoDifficulty == WorkoutPlanDifficulty.ADVANCED) {
        minExercises += 2
        increment += 1
        minSetsPerExercise += 1
    }
    // TODO: ideally, this would depend on previous program and workouts
    val exercisesPerProgram = random.nextInt(minExercises, minExercises+increment)

    val (repRange, restRange) = getRepsAndRest(goalChoice)

    muscleDays.forEachIndexed { programNumber, day ->
        val programId = repository.addProgram(
            WorkoutProgram(
                extPlanId = planId,
                name = day.joinToString(separator = ", ") { it.muscleName },
                orderInWorkoutPlan = programNumber
            )
        )

        val exercisesPerMuscle = exercisesPerProgram / day.size
        var forgottenExercises = exercisesPerProgram - exercisesPerMuscle * day.size

        // used for supersets maybe
        var lastExerciseId: Long? = null
        var lastExerciseMuscle: Exercise.Muscle? = null

        var exerciseCount = 0
        day.forEach { muscle ->
            var exerciseForThisMuscle = if (isMajorMover(muscle)) exercisesPerMuscle+1 else exercisesPerMuscle-1
            if (forgottenExercises > 0 && random.nextBoolean()) {
                exerciseForThisMuscle++
                forgottenExercises--
            }

            var exercises = muscle2Exercises[muscle]!!.toMutableList()
            if (nonAutoDifficulty == WorkoutPlanDifficulty.BEGINNER)
                exercises =
                    exercises.filter { it.difficulty == Exercise.ExerciseDifficulty.BEGINNER }.toMutableList()
            if (nonAutoDifficulty != WorkoutPlanDifficulty.ADVANCED)
                exercises =
                    exercises.filter { it.difficulty != Exercise.ExerciseDifficulty.ADVANCED }.toMutableList()

            var compoundExAtStart = if (goalChoice != WorkoutPlanGoal.STRENGTH) 1 else 2
            if (nonAutoDifficulty == WorkoutPlanDifficulty.ADVANCED)
                compoundExAtStart += 1

            for (i in 0..compoundExAtStart) {
                // If not beginner start with compound exercise
                if (nonAutoDifficulty != WorkoutPlanDifficulty.BEGINNER && isMajorMover(muscle)) {
                    val currentSets = minSetsPerExercise + random.nextInt(0, 2)
                    val compoundEx = exercises.filter { exerciseIsCompound(it) }
                    val chosenExercise =
                        compoundEx.weightedRandom(compoundEx.map { it.probability })
                    exercises.remove(chosenExercise)
                    val reps = if (goalChoice == WorkoutPlanGoal.STRENGTH) {
                        if (random.nextBoolean())
                            List(currentSets) { currentSets - it }
                        else
                            List(currentSets) { 5 }
                    } else List(currentSets) { 8 }
                    val rest =
                        if (goalChoice == WorkoutPlanGoal.STRENGTH) List(currentSets) { 180 } else List(
                            currentSets
                        ) { 120 }
                    repository.addProgramExercise(
                        ProgramExercise(
                            extProgramId = programId,
                            extExerciseId = chosenExercise.exerciseId,
                            orderInProgram = exerciseCount++,
                            reps = reps,
                            rest = rest
                        )
                    )
                    exerciseForThisMuscle--
                    exercises = exercises.filterNot { exerciseIsCompound(it) }.toMutableList()
                }
            }
            for (i in 0..exerciseForThisMuscle) {
                if (exercises.isEmpty())
                    break
                val currentSets = minSetsPerExercise + random.nextInt(0, 2)
                val currentReps = repRange.shuffled().first()
                val currentRest = restRange.shuffled().first()

                val chosenExercise = exercises.weightedRandom(exercises.map { it.probability })
                exercises.remove(chosenExercise)
                val programExerciseId = repository.addProgramExercise(
                    ProgramExercise(
                        extProgramId = programId,
                        extExerciseId = chosenExercise.exerciseId,
                        orderInProgram = exerciseCount++,
                        reps = List(currentSets) { currentReps },
                        rest = List(currentSets) { currentRest }
                    )
                )
                if (lastExerciseId != null && shouldPairForSuperset(nonAutoDifficulty, nonAutoSplit, lastExerciseMuscle, muscle, isSameMuscleOkay = true)) {
                    repository.updateExerciseSuperset(
                        listOf(
                            UpdateExerciseSuperset(lastExerciseId!!, programExerciseId),
                            UpdateExerciseSuperset(programExerciseId, lastExerciseId)
                        )
                    )
                    lastExerciseId = null
                    lastExerciseMuscle = null
                } else {
                    lastExerciseId = programExerciseId
                    lastExerciseMuscle = muscle
                }
            }
        }

    }
    return planId
}