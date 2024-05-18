package agdesigns.elevatefitness.ui

import android.icu.util.Calendar
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.exercise.Exercise
import agdesigns.elevatefitness.data.exercise.ProgramExercise
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlan
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlanDifficulty
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlanGoal
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlanSplit
import agdesigns.elevatefitness.data.workout_program.WorkoutProgram
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
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
            name = "Generated program ${SimpleDateFormat("d MMM (yyyy)").format(currentTime)}",
            creationDate = currentTime
        )
    )

    val muscleDays = emptyList<List<Exercise.Muscle>>().toMutableList()
    val nonAutoSplit = if (workoutSplit == WorkoutPlanSplit.AUTO) WorkoutPlanSplit.entries.toTypedArray()
        .random() else workoutSplit

    when (nonAutoSplit) {
        WorkoutPlanSplit.FULL_BODY -> muscleDays.add(
            Exercise.Muscle.entries.toMutableList().minus(Exercise.Muscle.EVERYTHING)
        )
        WorkoutPlanSplit.BRO -> muscleDays.addAll(
            listOf(
                listOf(Exercise.Muscle.CHEST, Exercise.Muscle.SHOULDERS, Exercise.Muscle.TRICEPS),
                listOf(Exercise.Muscle.BACK, Exercise.Muscle.BICEPS),
                listOf(Exercise.Muscle.QUADRICEPS, Exercise.Muscle.HAMSTRINGS, Exercise.Muscle.GLUTES, Exercise.Muscle.CALVES, Exercise.Muscle.ABS)
            )
        )
        WorkoutPlanSplit.UPPER_LOWER -> muscleDays.addAll(
            listOf(
                listOf(Exercise.Muscle.CHEST, Exercise.Muscle.BACK, Exercise.Muscle.SHOULDERS, Exercise.Muscle.TRICEPS, Exercise.Muscle.BICEPS),
                listOf(Exercise.Muscle.QUADRICEPS, Exercise.Muscle.HAMSTRINGS, Exercise.Muscle.GLUTES, Exercise.Muscle.CALVES, Exercise.Muscle.ABS)
            )
        )
        WorkoutPlanSplit.GAINZ -> muscleDays.addAll(
            Exercise.Muscle.entries.toMutableList().minus(Exercise.Muscle.EVERYTHING).map {
                listOf(it)
            }
        )
        WorkoutPlanSplit.AUTO -> TODO()  // this is impossible
    }

    // TODO: this is not really auto
    val nonAutoDifficulty = if (expertiseLevel == WorkoutPlanDifficulty.AUTO) WorkoutPlanDifficulty.ADVANCED else expertiseLevel

    // in beginner include at most one Intermediate exercise per program
    // in advanced include advanced exercises
    // in intermediate do nothing in particular

    var minExercises = 4
    var minSetsPerExercise = 3
    var incremenet = 2
    if (nonAutoDifficulty != WorkoutPlanDifficulty.BEGINNER) {
        minExercises += 2
        incremenet += 1
        minSetsPerExercise += 1
    }
    if (nonAutoDifficulty == WorkoutPlanDifficulty.ADVANCED) {
        minExercises += 2
        incremenet += 1
        minSetsPerExercise += 1
    }
    // TODO: ideally, this would depend on previous program and workouts
    val exercisesPerProgram = random.nextInt(minExercises, minExercises+incremenet)
    muscleDays.forEachIndexed { programNumber, day ->
        val programId = repository.addProgram(
            WorkoutProgram(
                extPlanId = planId,
                name = day.joinToString(separator = ", ") { it.muscleName },
                orderInWorkoutPlan = programNumber
            )
        )

        val exercisesPerMuscle = exercisesPerProgram / day.size
        var exerciseCount = 0
        day.forEach { muscle ->
            var exerciseForThisMuscle = if (isMajorMover(muscle)) exercisesPerMuscle+1 else exercisesPerMuscle-1

            var exercises = muscle2Exercises[muscle]!!.toMutableList()
            if (nonAutoDifficulty == WorkoutPlanDifficulty.BEGINNER)
                exercises =
                    exercises.filter { it.difficulty == Exercise.ExerciseDifficulty.BEGINNER }.toMutableList()
            if (nonAutoDifficulty != WorkoutPlanDifficulty.ADVANCED)
                exercises =
                    exercises.filter { it.difficulty != Exercise.ExerciseDifficulty.ADVANCED }.toMutableList()

            // If not beginner start with compound exercise
            if (nonAutoDifficulty != WorkoutPlanDifficulty.BEGINNER && isMajorMover(muscle)) {
                val currentSets = minSetsPerExercise + random.nextInt(1, 3)
                val compoundEx = exercises.filter { exerciseIsCompound(it) }
                val chosenExercise = compoundEx.weightedRandom(compoundEx.map { it.probability })
                exercises.remove(chosenExercise)
                repository.addProgramExercise(
                    ProgramExercise(
                        extProgramId = programId,
                        extExerciseId = chosenExercise.exerciseId,
                        orderInProgram = exerciseCount++,
                        reps = List(currentSets) { 8 },  // FIXME: change if strength
                        rest = List(currentSets) { 120 }
                    )
                )
                exerciseForThisMuscle--
                exercises = exercises.filterNot { exerciseIsCompound(it) }.toMutableList()
            }
            for (i in 0..exerciseForThisMuscle) {
                if (exercises.isEmpty())
                    break
                val currentSets = minSetsPerExercise + random.nextInt(0, 2)

                val chosenExercise = exercises.weightedRandom(exercises.map { it.probability })
                exercises.remove(chosenExercise)
                repository.addProgramExercise(
                    ProgramExercise(
                        extProgramId = programId,
                        extExerciseId = chosenExercise.exerciseId,
                        orderInProgram = exerciseCount++,
                        reps = List(currentSets) { random.nextInt(8 / 2, 12 / 2) * 2 },
                        rest = List(currentSets) { random.nextInt(60 / 30, 120 / 30) * 30 }
                    )
                )
            }
        }

    }
    return planId

    when (goalChoice) {
        WorkoutPlanGoal.MUSCLE -> TODO()
        WorkoutPlanGoal.STRENGTH -> TODO()
        WorkoutPlanGoal.ENDURANCE -> TODO()
        WorkoutPlanGoal.WEIGHT_LOSS -> TODO()
    }
    return 0L
}