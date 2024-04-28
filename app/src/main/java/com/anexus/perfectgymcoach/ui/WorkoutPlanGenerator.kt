package com.anexus.perfectgymcoach.ui

import com.anexus.perfectgymcoach.data.Repository
import com.anexus.perfectgymcoach.data.exercise.Exercise
import com.anexus.perfectgymcoach.data.exercise.ProgramExercise
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlan
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlanDifficulty
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlanGoal
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlanSplit
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import kotlinx.coroutines.flow.first
import kotlin.random.Random

suspend fun generatePlan(
    repository: Repository,
    goalChoice: WorkoutPlanGoal,
    expertiseLevel: WorkoutPlanDifficulty,
    workoutSplit: WorkoutPlanSplit
): Long {
    val muscle2Exercises = emptyMap<Exercise.Muscle, List<Exercise>>().toMutableMap()
    val random = Random(42)  // FIXME: should not have fixed seed
    for (muscle in Exercise.Muscle.entries.toMutableList().minus(Exercise.Muscle.EVERYTHING)){
        muscle2Exercises[muscle] = repository.getExercises(muscle).first()
    }
    val planId = repository.addPlan(
        WorkoutPlan(
            name = "Generated program ${random.nextInt()}"
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
                listOf(Exercise.Muscle.LEGS, Exercise.Muscle.CALVES, Exercise.Muscle.ABS)
            )
        )
        WorkoutPlanSplit.UPPER_LOWER -> muscleDays.addAll(
            listOf(
                listOf(Exercise.Muscle.CHEST, Exercise.Muscle.BACK, Exercise.Muscle.SHOULDERS, Exercise.Muscle.TRICEPS, Exercise.Muscle.BICEPS),
                listOf(Exercise.Muscle.LEGS, Exercise.Muscle.CALVES, Exercise.Muscle.ABS)
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
            var exerciseForThisMuscle = if (isPrimaryMuscle(muscle)) exercisesPerMuscle+1 else exercisesPerMuscle-1

            var exercises = muscle2Exercises[muscle]!!.toMutableList()
            if (nonAutoDifficulty == WorkoutPlanDifficulty.BEGINNER)
                exercises =
                    exercises.filter { it.difficulty == Exercise.ExerciseDifficulty.BEGINNER }.toMutableList()
            if (nonAutoDifficulty != WorkoutPlanDifficulty.ADVANCED)
                exercises =
                    exercises.filter { it.difficulty != Exercise.ExerciseDifficulty.ADVANCED }.toMutableList()

            // If not beginner start with compound exercise
            if (nonAutoDifficulty != WorkoutPlanDifficulty.BEGINNER && isPrimaryMuscle(muscle)) {
                val currentSets = minSetsPerExercise + random.nextInt(1, 3)
                val chosenExercise = exercises.filter { exerciseIsCompound(it) }
                    .random()
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

                val chosenExercise = exercises.random()
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