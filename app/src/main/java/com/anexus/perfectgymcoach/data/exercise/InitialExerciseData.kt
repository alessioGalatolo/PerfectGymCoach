package com.anexus.perfectgymcoach.data.exercise

import com.anexus.perfectgymcoach.R

// TODO: add difficulty to exercises. Idea: beginner are very hard to do bad exercises e.g. machines
// Advanced are *really* advanced exercises e.g. planche
// medium is everything else
// TODO: also double check that exercise with more than one secondary muscle are compound, otherwise
// change function in utils
val INITIAL_EXERCISE_DATA = listOf(
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
        image = R.drawable.incline_bench_press
    ),
    Exercise(
        name = "Declined bench press",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.bench_press
    ),

    // Cables chest
    Exercise(
        name = "Cable crossover",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.CHEST,
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
        image = R.drawable.push_up,
        variations = listOf(
            "Wide arms",
            "Inclined",
            "Declined",
            "Single-arm"
        )
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
        image = R.drawable.dumbbell_bench_press,
        variations = listOf(
            "Inclined",
            "Declined",
            "Single-arm"
        )
    ),
    Exercise(
        name = "Dumbbell fly",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.dumbbell_bench_press,
        variations = listOf(
            "Inclined",
            "Declined"
        )
    ),
    Exercise(
        name = "Pullover",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.BACK),
        image = R.drawable.generic_dumbbell
    ),

    /*
        BACK
     */
    // Barbell back
    Exercise(
        name = "Deadlift",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.LEGS, Exercise.Muscle.ABS),
        image = R.drawable.deadlift
    ),
    Exercise(
        name = "Sumo deadlift",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.LEGS, Exercise.Muscle.ABS),
        image = R.drawable.sumo_deadlift
    ),
    Exercise(
        name = "Barbell shrug",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_barbell
    ),
    Exercise(
        name = "Barbell row",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        image = R.drawable.barbell_row
    ),
    Exercise(
        name = "Barbell t-bar row",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.LEGS, Exercise.Muscle.ABS),
        image = R.drawable.generic_barbell
    ),
    Exercise(
        name = "Barbell upright row",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.generic_barbell
    ),

    // Cables back
    Exercise(
        name = "Cable row",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        image = R.drawable.cable_row
    ),
    Exercise(
        name = "Cable pullover",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_cable
    ),
    Exercise(
        name = "Upright cable row",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        image = R.drawable.generic_cable
    ),

    // Bodyweight back
    Exercise(
        name = "Pull up",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        image = R.drawable.pull_up,
        variations = listOf(
            "Single arm"
        )
    ),
    Exercise(
        name = "Wide grip pull up",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        image = R.drawable.wide_pull_up
    ),
    Exercise(
        name = "Chin up",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        image = R.drawable.chin_up,
        variations = listOf(
            "Close grip"
        )
    ),
    Exercise(
        name = "Muscle up",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS, Exercise.Muscle.CHEST, Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.muscle_up
    ),
    Exercise(
        name = "Rope climbing",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        image = R.drawable.rope_climb
    ),

    // Machine back
    Exercise(
        name = "Machine row",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        image = R.drawable.cable_row
    ),
    Exercise(
        name = "Hyperextensions",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = emptyList(),
        image = R.drawable.hyperextensions
    ),
    Exercise(
        name = "Lat pulldown", // also called lat machine
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        image = R.drawable.lat_pulldown,
        variations = listOf(
            "V-bar"
        )
    ),
    Exercise(
        name = "Vertical traction",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        image = R.drawable.lat_pulldown
    ),

    // Dumbbell back
    Exercise(
        name = "Dumbbell row",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        image = R.drawable.dumbbell_row
    ),
    Exercise(
        name = "Dumbbell deadlift",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.LEGS, Exercise.Muscle.ABS),
        image = R.drawable.generic_dumbbell
    ),
    Exercise(
        name = "Dumbbell shrug",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = emptyList(),
        image = R.drawable.dumbbell_shrug
    ),
    Exercise(
        name = "Upright dumbbell row",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.generic_dumbbell
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
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_cable
    ),

    // Bodyweight abs
    Exercise(
        name = "Crunch",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        image = R.drawable.crunch
    ),
    Exercise(
        name = "Knee raises",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        image = R.drawable.knee_raises
    ),
    Exercise(
        name = "Leg raises",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        image = R.drawable.leg_raises
    ),
    Exercise(
        name = "Plank",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        image = R.drawable.plank
    ),
    Exercise(
        name = "Russian twist",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        image = R.drawable.russian_twist
    ),
    Exercise(
        name = "Side plank",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        image = R.drawable.side_plank
    ),
    Exercise(
        name = "Sit ups",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        image = R.drawable.sit_ups
    ),
    Exercise(
        name = "Side crunch",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        image = R.drawable.side_crunch
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
        secondaryMuscles = emptyList(),
        image = R.drawable.ab_roller
    ),

    // Machine abs
    Exercise(
        name = "Ab machine",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_machine
    ),

    // Dumbbell abs
    Exercise(
        name = "Dumbbell side bend",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_dumbbell
    ),

    /*
        BICEPS
     */
    // Barbell biceps
    Exercise(
        name = "Barbell curl",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList(),
        image = R.drawable.barbell_curl,
        variations = listOf(
            "Preacher",
            "Scott"
        )
    ),

    // Cables biceps
    Exercise(
        name = "Cable curl (with bar)",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList(),
        image = R.drawable.cable_curl,
        variations = listOf(
            "Bar",
            "Rope",
            "Handles"
        )
    ),

    // Bodyweight

    // Machine
    Exercise(
        name = "Machine biceps curl",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_machine,
        variations = listOf(
            "Scott",
            "Preacher"
        )
    ),

    // Dumbbell
    Exercise(
        name = "Dumbbell curl",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList(),
        image = R.drawable.dumbbell_curl,
        variations = listOf(
            "Alternating",
            "Inclined bench"
        )
    ),
    Exercise(
        name = "Dumbbell concentration curl",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList(),
        image = R.drawable.concentration_curl
    ),
    Exercise(
        name = "Dumbbell scott curl",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList(),
        image = R.drawable.scott_dumbbell,
        variations = listOf(
            "Preacher"
        )
    ),

    /*
        TRICEPS
     */
    // Barbell triceps
    Exercise(
        name = "Barbell triceps extensions",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_barbell
    ),
    Exercise(
        name = "Close grip bench press",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.CHEST, Exercise.Muscle.SHOULDERS),
        image = R.drawable.generic_barbell
    ),
    Exercise(
        name = "French press",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_barbell
    ),
    Exercise(
        name = "Skull crusher",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_barbell
    ),

    // Cables
    Exercise(
        name = "Cable skull crusher",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_cable
    ),
    Exercise(
        name = "Cable pushdown",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        image = R.drawable.cable_pushdown
    ),
    Exercise(
        name = "Overhead cable triceps extension",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_cable
    ),

    // Bodyweight
    Exercise(
        name = "Triceps dip",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.CHEST, Exercise.Muscle.SHOULDERS),
        image = R.drawable.chest_dip
    ),
    Exercise(
        name = "Bench dip",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.SHOULDERS),
        image = R.drawable.bench_dip
    ),
    Exercise(
        name = "Diamond push up",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.CHEST, Exercise.Muscle.SHOULDERS),
        image = R.drawable.push_up
    ),
    Exercise(
        name = "Parallel arms push up", // TODO: right name?
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.CHEST, Exercise.Muscle.SHOULDERS),
        image = R.drawable.push_up
    ),

    // Machine
    Exercise(
        name = "Machine Triceps extension",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_machine
    ),

    // Dumbbell
    Exercise(
        name = "Dumbbell triceps extensions",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_dumbbell
    ),
    Exercise(
        name = "Close grip dumbbell bench press",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.CHEST, Exercise.Muscle.SHOULDERS),
        image = R.drawable.generic_dumbbell
    ),
    Exercise(
        name = "Dumbbell skull crusher",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_dumbbell
    ),

    /*
        Legs
     */
    // Barbell
    Exercise(
        name = "Barbell clean",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.LEGS,
        secondaryMuscles = listOf(Exercise.Muscle.ABS, Exercise.Muscle.BACK),
        image = R.drawable.barbell_clean
    ),
    Exercise(
        name = "Squat",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.LEGS,
        secondaryMuscles = emptyList(),
        image = R.drawable.barbell_squat,
        variations = listOf(
            "Front",
            "Hack"
        )
    ),
    Exercise(
        name = "Barbell lunge",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.LEGS,
        secondaryMuscles = listOf(Exercise.Muscle.ABS, Exercise.Muscle.CALVES),
        image = R.drawable.barbell_lunge
    ),
    Exercise(
        name = "Romanian deadlift",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.LEGS,
        secondaryMuscles = emptyList(),
        image = R.drawable.romanian_deadlift
    ),
    // Cables
    Exercise(
        name = "Cable leg curl",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.LEGS,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_cable
    ),
    // Bodyweight
    Exercise(
        name = "Bodyweight squat",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.LEGS,
        secondaryMuscles = emptyList(),
        image = R.drawable.squat,
        variations = listOf(
            "Single leg"
        )
    ),
    Exercise(
        name = "Bodyweight step ups",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.LEGS,
        secondaryMuscles = emptyList(),
        image = R.drawable.step_ups
    ),
    Exercise(
        name = "Bodyweight lunge",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.LEGS,
        secondaryMuscles = emptyList(),
        image = R.drawable.lunge
    ),
    // Machine
    Exercise(
        name = "Machine leg curl",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.LEGS,
        secondaryMuscles = emptyList(),
        image = R.drawable.leg_machine
    ),
    Exercise(
        name = "Machine leg extension",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.LEGS,
        secondaryMuscles = emptyList(),
        image = R.drawable.leg_machine
    ),
    Exercise(
        name = "Machine hack squat",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.LEGS,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_machine
    ),
    Exercise(
        name = "Leg press",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.LEGS,
        secondaryMuscles = emptyList(),
        image = R.drawable.leg_press
    ),
    Exercise(
        name = "Abduction machine",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.LEGS,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_machine
    ),
    Exercise(
        name = "Adduction machine",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.LEGS,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_machine
    ),

    // Dumbbell
    Exercise(
        name = "Bulgarian split squat",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.LEGS,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_dumbbell
    ),
    Exercise(
        name = "Dumbbell lunges",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.LEGS,
        secondaryMuscles = emptyList(),
        image = R.drawable.dumbbell_lunge
    ),
    Exercise(
        name = "Goblet squat",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.LEGS,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_dumbbell
    ),
    Exercise(
        name = "Dumbbell step ups",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.LEGS,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_dumbbell
    ),

    /*
        Calves
     */
    // Barbell
    Exercise(
        name = "Barbell Calf raises",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.CALVES,
        secondaryMuscles = emptyList(),
        image = R.drawable.calf
    ),
    // Cables
    // Bodyweight
    Exercise(
        name = "Calf raises",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.CALVES,
        secondaryMuscles = emptyList(),
        image = R.drawable.calf
    ),
    Exercise(
        name = "Single leg calf raises",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.CALVES,
        secondaryMuscles = emptyList(),
        image = R.drawable.calf
    ),
    // Machine
    Exercise(
        name = "Machine calf raises",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.CALVES,
        secondaryMuscles = emptyList(),
        image = R.drawable.calf
    ),
    // Dumbbell
    Exercise(
        name = "Dumbbell calf raises",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.CALVES,
        secondaryMuscles = emptyList(),
        image = R.drawable.calf
    ),

    /*
        Shoulders
     */
    // Barbell
    Exercise(
        name = "Push and press",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS),
        image = R.drawable.shoulder_press
    ),
    Exercise(
        name = "Shoulder press",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS),
        image = R.drawable.shoulder_press
    ),
    Exercise(
        name = "Clean and press",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = listOf(Exercise.Muscle.BACK, Exercise.Muscle.LEGS, Exercise.Muscle.ABS, Exercise.Muscle.TRICEPS),
        image = R.drawable.clean_press
    ),
    // Cables
    Exercise(
        name = "Cable side raise",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_cable
    ),
    Exercise(
        name = "Cable rear delt fly",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_cable
    ),
    Exercise(
        name = "Cable face pull",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_cable
    ),
    Exercise(
        name = "Cable shoulder press",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS),
        image = R.drawable.generic_cable
    ),
    // Bodyweight
    Exercise(
        name = "Handstand push ups",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS),
        image = R.drawable.headstand_push_up
    ),
    // Machine
    Exercise(
        name = "Machine shoulder press",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS),
        image = R.drawable.generic_machine
    ),
    // Dumbbell
    Exercise(
        name = "Front raise",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_dumbbell
    ),
    Exercise(
        name = "Side raise",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_dumbbell
    ),
    Exercise(
        name = "Dumbbell shoulder press",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS),
        image = R.drawable.dumbbell_shoulder_press,
        variations = listOf(
            "Arnold press"
        )
    ),
    Exercise(
        name = "Dumbbell rear delt row",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_dumbbell
    ),

    // Other
    Exercise(
        name = "Can you hear the silence?",
        equipment = Exercise.Equipment.EVERYTHING,
        primaryMuscle = Exercise.Muscle.EVERYTHING,
        secondaryMuscles = emptyList(),
        image = R.drawable.gigachad
    ),
)