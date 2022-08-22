package com.anexus.perfectgymcoach.data.exercise

import com.anexus.perfectgymcoach.R

val INITIAL_EXERCISE_DATA = listOf(
    // TODO: should have some kind of variation system

    /*
        CHEST EXERCISES
     */
    // Barbell chest
    Exercise(
        name = "Bench press",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.bench_press
    ),
    Exercise(
        name = "Inclined bench press",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.bench_press  // FIXME
    ),
    Exercise(
        name = "Declined bench press",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.bench_press // FIXME
    ),

    // Cables chest
    Exercise(
        name = "Cable crossover",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = emptyList(),
        image = R.drawable.cable_crossover
    ),

    // Bodyweight chest
    Exercise(
        name = "Chest dip",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.chest_dip
    ),
    Exercise(
        name = "Push up",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.push_up
    ),
    Exercise(
        name = "Inclined push up",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.push_up // FIXME
    ),
    Exercise(
        name = "Declined push up",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.push_up // FIXME
    ),
    Exercise(
        name = "Single arm push up",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.push_up // FIXME
    ),
    Exercise(
        name = "Wide arms push up",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.push_up // FIXME
    ),

    // Machine chest
    Exercise(
        name = "Chest press",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.chest_press
    ),
    Exercise(
        name = "Machine fly",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = emptyList(),
        image = R.drawable.machine_fly
    ),

    // Dumbbell chest
    Exercise(
        name = "Dumbbell bench press",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.dumbbell_bench_press
    ),
    Exercise(
        name = "Dumbbell inclined bench press",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.dumbbell_bench_press // Fixme
    ),
    Exercise(
        name = "Dumbbell declined bench press",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.dumbbell_bench_press // Fixme
    ),
    Exercise(
        name = "Dumbbell fly",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.dumbbell_bench_press // Fixme
    ),
    Exercise(
        name = "Pullover",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.BACK),
        image = R.drawable.dumbbell_bench_press // Fixme
    ),
    Exercise(
        name = "Single arm bench press",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.dumbbell_bench_press // Fixme
    ),

    /*
        BACK
     */
    // Barbell back
    Exercise(
        name = "Deadlift",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.LEGS, Exercise.Muscle.ABS)
    ),
    Exercise(
        name = "Sumo deadlift",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.LEGS, Exercise.Muscle.ABS)
    ),
    Exercise(
        name = "Barbell shrug",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Barbell row",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS)
    ),
    Exercise(
        name = "Barbell t-bar row",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.LEGS, Exercise.Muscle.ABS)
    ),
    Exercise(
        name = "Barbell upright row",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS, Exercise.Muscle.SHOULDERS)
    ),

    // Cables back
    Exercise(
        name = "Cable row",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS)
    ),
    Exercise(
        name = "Cable pullover",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Upright cable row",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS, Exercise.Muscle.SHOULDERS)
    ),

    // Bodyweight back
    Exercise(
        name = "Pull up",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS)
    ),
    Exercise(
        name = "Wide grip pull up",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS)
    ),
    Exercise(
        name = "Chin up",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS)
    ),
    Exercise(
        name = "Close grip chin up",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS)
    ),
    Exercise(
        name = "Single arm pull up",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS)
    ),
    Exercise(
        name = "Muscle up",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS, Exercise.Muscle.CHEST, Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS)
    ),
    Exercise(
        name = "Rope climbing",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS)
    ),

    // Machine back
    Exercise(
        name = "Machine row",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS)
    ),
    Exercise(
        name = "Hyperextensions",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Lat pulldown", // also called lat machine
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS)
    ),
    Exercise(
        name = "V-bar lat pulldown",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS)
    ),
    Exercise(
        name = "Vertical traction",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS)
    ),

    // Dumbbell back
    Exercise(
        name = "Dumbbell row",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS)
    ),
    Exercise(
        name = "Dumbbell deadlift",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.LEGS, Exercise.Muscle.ABS)
    ),
    Exercise(
        name = "Dumbbell shrug",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Upright dumbbell row",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS, Exercise.Muscle.SHOULDERS)
    ),

    /*
        ABS
     */
    // Barbell abs

    // Cables abs
    Exercise(
        name = "Cable crunch",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList()
    ),

    // Bodyweight abs
    Exercise(
        name = "Crunch",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Knee raises",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Leg raises",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Plank",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Russian twist",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Crunch",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Side plank",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Sit up",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Side crunch",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Crunch",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Dragon fly",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Ab roller",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList()
    ),

    // Machine abs
    Exercise(
        name = "Ab machine",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList()
    ),

    // Dumbbell abs
    Exercise(
        name = "Dumbbell side bend",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList()
    ),

    /*
        BICEPS
     */
    // Barbell biceps
    Exercise(
        name = "Barbell curl",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Scott curl",  // Also Preacher curl
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Preacher curl",  // same as above
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList()
    ),

    // Cables biceps
    Exercise(
        name = "Cable curl (with bar)",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Cable curl (with rope)",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList()
    ),

    // Bodyweight

    // Machine
    Exercise(
        name = "Machine biceps curl",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Machine scott curl",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Machine preacher curl",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList()
    ),

    // Dumbbell
    Exercise(
        name = "Dumbbell curl",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Alternating dumbbell curl",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Dumbbell concentration curl",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Dumbbell scott curl",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Dumbbell preacher curl",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Incline dumbbell curl",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList()
    ),

    /*
        TRICEPS
     */
    // Barbell triceps
    Exercise(
        name = "Barbell triceps extensions",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Close grip bench press",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.CHEST, Exercise.Muscle.SHOULDERS)
    ),
    Exercise(
        name = "French press",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList()
    ),
    Exercise(
        name = "Skull crusher",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList()
    ),
)