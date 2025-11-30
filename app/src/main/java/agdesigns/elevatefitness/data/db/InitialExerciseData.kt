package agdesigns.elevatefitness.data.db

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.Exercise
import android.content.Context
import agdesignes.elevatefitness.shared.Equipment

// TODO: Fix general spelling and capitalisation, missing exercises e.g. planche
val INITIAL_EXERCISE_DATA = listOf(
    /*
    CHEST
     */
    // Barbell chest
    Exercise(
        name = "",
        nameResKey = "exercise_bench_press_name",
        equipment = Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        imageResKey = "bench_press",
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_bench_press_description"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_incline_bench_press_name",
        equipment = Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        imageResKey = "incline_bench_press",
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_incline_bench_press_description"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_decline_bench_press_name",
        equipment = Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        imageResKey = "bench_press",
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_decline_bench_press_description",
    ),
    // Cables chest
    Exercise(
        name = "",
        nameResKey = "exercise_cable_crossover_name",
        equipment = Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.CHEST,
        imageResKey = "cable_crossover",
        descriptionResKey = "exercise_cable_crossover_description",
    ),
    // Bodyweight chest
    Exercise(
        name = "",
        nameResKey = "exercise_chest_dip_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        imageResKey = "chest_dip",
        descriptionResKey = "exercise_chest_dip_description",
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_push_up_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        imageResKey = "push_up",
        descriptionResKey = "exercise_push_up_description",
        variationsResKeys = listOf(
            "exercise_variation_pushup_wide",
            "exercise_variation_pushup_incline",
            "exercise_variation_pushup_decline",
            "exercise_variation_pushup_single_arm",
        )
    ),
    // Machine chest
    Exercise(
        name = "",
        nameResKey = "exercise_chest_press_name",
        equipment = Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        imageResKey = "chest_press",
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_chest_press_description",
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_machine_fly_name",
        equipment = Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = emptyList(),
        imageResKey = "machine_fly",
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_machine_fly_description",
    ),
    // Dumbbell chest
    Exercise(
        name = "",
        nameResKey = "exercise_dumbbell_bench_press_name",
        equipment = Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        imageResKey = "dumbbell_bench_press",
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_dumbbell_bench_press_description",
        variationsResKeys = listOf(
            "exercise_variation_dumbbell_incline",
            "exercise_variation_dumbbell_decline",
            "exercise_variation_dumbbell_single_arm",
        )
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_dumbbell_fly_name",
        equipment = Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.SHOULDERS),
        imageResKey = "dumbbell_bench_press",
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_dumbbell_fly_description",
        variationsResKeys = listOf(
            "exercise_variation_dumbbell_fly_incline",
            "exercise_variation_dumbbell_fly_decline",
        )
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_pullover_name",
        equipment = Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.BACK),
        imageResKey = "generic_dumbbell",
        descriptionResKey = "exercise_pullover_description",
    ),

    /*
        BACK
     */
    // Barbell back
    Exercise(
        name = "",
        nameResKey = "exercise_deadlift_name",
        equipment = Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.QUADRICEPS, Exercise.Muscle.ABS),
        imageResKey = "deadlift",
        descriptionResKey = "exercise_deadlift_description",
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_sumo_deadlift_name",
        equipment = Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.QUADRICEPS, Exercise.Muscle.ABS),
        imageResKey = "sumo_deadlift",
        descriptionResKey = "exercise_sumo_deadlift_description",
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_barbell_shrug_name",
        equipment = Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = emptyList(),
        imageResKey = "generic_barbell",
        descriptionResKey = "exercise_barbell_shrug_description",
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_barbell_row_name",
        equipment = Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS, Exercise.Muscle.ABS),
        imageResKey = "barbell_row",
        descriptionResKey = "exercise_barbell_row_description",
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_barbell_tbar_row_name",
        equipment = Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.QUADRICEPS, Exercise.Muscle.ABS),
        imageResKey = "generic_barbell",
        descriptionResKey = "exercise_barbell_tbar_row_description",
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_barbell_upright_row_name",
        equipment = Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.SHOULDERS),
        imageResKey = "generic_barbell",
        descriptionResKey = "exercise_barbell_upright_row_description",
    ),

    // Cables back
    Exercise(
        name = "",
        nameResKey = "exercise_cable_row_name",
        equipment = Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        imageResKey = "cable_row",
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_cable_row_description",
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_cable_pullover_name",
        equipment = Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = emptyList(),
        imageResKey = "generic_cable",
        descriptionResKey = "exercise_cable_pullover_description",
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_cable_upright_row_name",
        equipment = Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        imageResKey = "generic_cable",
        descriptionResKey = "exercise_cable_upright_row_description",
    ),

    // Bodyweight back
    Exercise(
        name = "",
        nameResKey = "exercise_pullup_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(
            Exercise.Muscle.BICEPS,
            Exercise.Muscle.SHOULDERS
        ), // Shoulders have been added to make the exercise compound
        imageResKey = "pull_up",
        descriptionResKey = "exercise_pullup_description",
        variationsResKeys = listOf(
            "exercise_variation_pullup_single_arm",
        )
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_wide_grip_pullup_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        imageResKey = "wide_pull_up",
        descriptionResKey = "exercise_wide_grip_pullup_description",
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_chinup_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        imageResKey = "chin_up",
        descriptionResKey = "exercise_chinup_description",
        variationsResKeys = listOf(
            "exercise_variation_chinup_close_grip",
        )
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_muscleup_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(
            Exercise.Muscle.BICEPS,
            Exercise.Muscle.CHEST,
            Exercise.Muscle.TRICEPS,
            Exercise.Muscle.SHOULDERS
        ),
        imageResKey = "muscle_up",
        difficulty = Exercise.ExerciseDifficulty.ADVANCED,
        descriptionResKey = "exercise_muscleup_description",
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_rope_climb_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        imageResKey = "rope_climb",
        descriptionResKey = "exercise_rope_climb_description",
        difficulty = Exercise.ExerciseDifficulty.ADVANCED
    ),

    // Machine back
    Exercise(
        name = "",
        nameResKey = "exercise_machine_row_name",
        equipment = Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_machine_row_description",
        imageResKey = "cable_row"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_hyperextensions_name",
        equipment = Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_hyperextensions_description",
        imageResKey = "hyperextensions"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_lat_pulldown_name",
        equipment = Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        imageResKey = "lat_pulldown",
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_lat_pulldown_description",
        variationsResKeys = listOf(
            "exercise_variation_lat_pulldown_vbar"
        )
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_vertical_traction_name",
        equipment = Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        descriptionResKey = "exercise_vertical_traction_description",
        imageResKey = "lat_pulldown"
    ),

    // Dumbbell back
    Exercise(
        name = "",
        nameResKey = "exercise_dumbbell_row_name",
        equipment = Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_dumbbell_row_description",
        imageResKey = "dumbbell_row"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_dumbbell_deadlift_name",
        equipment = Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.QUADRICEPS, Exercise.Muscle.ABS),
        descriptionResKey = "exercise_dumbbell_deadlift_description",
        imageResKey = "generic_dumbbell"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_dumbbell_shrug_name",
        equipment = Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = emptyList(),
        imageResKey = "dumbbell_shrug",
        descriptionResKey = "exercise_dumbbell_shrug_description"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_upright_dumbbell_row_name",
        equipment = Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.SHOULDERS),
        descriptionResKey = "exercise_upright_dumbbell_row_description",
        imageResKey = "generic_dumbbell"
    ),

    /*
        ABS
     */
    // Barbell abs

    // Cables abs
    Exercise(
        name = "",
        nameResKey = "exercise_cable_crunch_name",
        equipment = Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_cable_crunch_desc",
        imageResKey = "generic_cable"
    ),

    // Bodyweight abs
    Exercise(
        name = "",
        nameResKey = "exercise_crunch_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_crunch_desc",
        imageResKey = "crunch"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_knee_raises_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_knee_raises_desc",
        imageResKey = "knee_raises"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_leg_raises_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_leg_raises_desc",
        imageResKey = "leg_raises"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_plank_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_plank_desc",
        imageResKey = "plank"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_russian_twist_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_russian_twist_desc",
        imageResKey = "russian_twist"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_side_plank_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_side_plank_desc",
        imageResKey = "side_plank"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_sit_ups_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_sit_ups_desc",
        imageResKey = "sit_ups"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_side_crunch_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_side_crunch_desc",
        imageResKey = "side_crunch"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_dragon_flag_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_dragon_flag_desc",
        difficulty = Exercise.ExerciseDifficulty.ADVANCED
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_ab_roller_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_ab_roller_desc",
        imageResKey = "ab_roller"
    ),

    // Machine abs
    Exercise(
        name = "",
        nameResKey = "exercise_ab_machine_name",
        equipment = Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_ab_machine_desc",
        imageResKey = "generic_machine"
    ),

    // Dumbbell abs
    Exercise(
        name = "",
        nameResKey = "exercise_dumbbell_side_bend_name",
        equipment = Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_dumbbell_side_bend_desc",
        imageResKey = "generic_dumbbell"
    ),

    /*
        BICEPS
     */
    // Barbell biceps
    Exercise(
        name = "",
        nameResKey = "exercise_barbell_curl_name",
        equipment = Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList(),
        imageResKey = "barbell_curl",
        descriptionResKey = "exercise_barbell_curl_description",
        variationsResKeys = listOf(
            "exercise_variation_preacher",
            "exercise_variation_scott"
        )
    ),

    // Cables biceps
    Exercise(
        name = "",
        nameResKey = "exercise_cable_curl_name",
        equipment = Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList(),
        imageResKey = "cable_curl",
        descriptionResKey = "exercise_cable_curl_description",
        variationsResKeys = listOf(
            "exercise_variation_bar",
            "exercise_variation_rope",
            "exercise_variation_handles"
        )
    ),

    // Bodyweight

    // Machine
    Exercise(
        name = "",
        nameResKey = "exercise_machine_biceps_curl_name",
        equipment = Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList(),
        imageResKey = "generic_machine",
        descriptionResKey = "exercise_machine_biceps_curl_description",
        variationsResKeys = listOf(
            "exercise_variation_scott",
            "exercise_variation_preacher"
        )
    ),

    // Dumbbell
    Exercise(
        name = "",
        nameResKey = "exercise_dumbbell_curl_name",
        equipment = Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList(),
        imageResKey = "dumbbell_curl",
        descriptionResKey = "exercise_dumbbell_curl_description",
        variationsResKeys = listOf(
            "exercise_variation_alternating",
            "exercise_variation_inclined_bench"
        )
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_dumbbell_concentration_curl_name",
        equipment = Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_dumbbell_concentration_curl_description",
        imageResKey = "concentration_curl"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_dumbbell_scott_curl_name",
        equipment = Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList(),
        imageResKey = "scott_dumbbell",
        descriptionResKey = "exercise_dumbbell_scott_curl_description",
        variationsResKeys = listOf(
            "exercise_variation_preacher"
        )
    ),

    /*
            TRICEPS
         */
    // Barbell triceps
    Exercise(
        name = "",
        nameResKey = "exercise_barbell_triceps_extensions_name",
        equipment = Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_barbell_triceps_extensions_description",
        imageResKey = "generic_barbell"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_close_grip_bench_press_name",
        equipment = Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.CHEST, Exercise.Muscle.SHOULDERS),
        descriptionResKey = "exercise_close_grip_bench_press_description",
        imageResKey = "generic_barbell"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_french_press_name",
        equipment = Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_french_press_description",
        imageResKey = "generic_barbell"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_skull_crusher_name",
        equipment = Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_skull_crusher_description",
        imageResKey = "generic_barbell"
    ),

    // Cables
    Exercise(
        name = "",
        nameResKey = "exercise_cable_skull_crusher_name",
        equipment = Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_cable_skull_crusher_description",
        imageResKey = "generic_cable"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_cable_pushdown_name",
        equipment = Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_cable_pushdown_description",
        imageResKey = "cable_pushdown"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_overhead_cable_triceps_extension_name",
        equipment = Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_overhead_cable_triceps_extension_description",
        imageResKey = "generic_cable"
    ),

    // Bodyweight
    Exercise(
        name = "",
        nameResKey = "exercise_triceps_dip_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.CHEST, Exercise.Muscle.SHOULDERS),
        descriptionResKey = "exercise_triceps_dip_description",
        imageResKey = "chest_dip"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_bench_dip_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.SHOULDERS),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_bench_dip_description",
        imageResKey = "bench_dip"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_diamond_push_up_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.CHEST, Exercise.Muscle.SHOULDERS),
        descriptionResKey = "exercise_diamond_push_up_description",
        imageResKey = "push_up"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_parallel_arms_push_up_name", // TODO: right name?
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.CHEST, Exercise.Muscle.SHOULDERS),
        descriptionResKey = "exercise_parallel_arms_push_up_description",
        imageResKey = "push_up"
    ),

    // Machine
    Exercise(
        name = "",
        nameResKey = "exercise_machine_triceps_extension_name",
        equipment = Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_machine_triceps_extension_description",
        imageResKey = "generic_machine"
    ),

    // Dumbbell
    Exercise(
        name = "",
        nameResKey = "exercise_dumbbell_triceps_extensions_name",
        equipment = Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_dumbbell_triceps_extensions_description",
        imageResKey = "generic_dumbbell"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_close_grip_dumbbell_bench_press_name",
        equipment = Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.CHEST, Exercise.Muscle.SHOULDERS),
        descriptionResKey = "exercise_close_grip_dumbbell_bench_press_description",
        imageResKey = "generic_dumbbell"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_dumbbell_skull_crusher_name",
        equipment = Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_dumbbell_skull_crusher_description",
        imageResKey = "generic_dumbbell"
    ),

    /*
        QUADS, GLUTES, HAMSTRINGS
     */
    // Barbell
    Exercise(
        name = "",
        nameResKey = "exercise_barbell_hip_thrust_name",
        equipment = Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.GLUTES,
        secondaryMuscles = listOf(Exercise.Muscle.HAMSTRINGS, Exercise.Muscle.ABS),
        imageResKey = "generic_barbell",
        descriptionResKey = "exercise_barbell_hip_thrust_description",
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_barbell_clean_name",
        equipment = Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.QUADRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.ABS, Exercise.Muscle.BACK),
        imageResKey = "barbell_clean",
        descriptionResKey = "exercise_barbell_clean_description",
        difficulty = Exercise.ExerciseDifficulty.ADVANCED
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_squat_name",
        equipment = Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.QUADRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.GLUTES, Exercise.Muscle.HAMSTRINGS),
        imageResKey = "barbell_squat",
        descriptionResKey = "exercise_squat_description",
        variationsResKeys = listOf(
            "exercise_variation_squat_front",
            "exercise_variation_squat_hack",
        )
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_barbell_lunge_name",
        equipment = Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.QUADRICEPS,
        secondaryMuscles = listOf(
            Exercise.Muscle.GLUTES,
            Exercise.Muscle.ABS,
            Exercise.Muscle.CALVES
        ),
        descriptionResKey = "exercise_barbell_lunge_description",
        imageResKey = "barbell_lunge"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_romanian_deadlift_name",
        equipment = Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.HAMSTRINGS,
        secondaryMuscles = listOf(Exercise.Muscle.GLUTES),
        imageResKey = "romanian_deadlift",
        descriptionResKey = "exercise_romanian_deadlift_description",
        difficulty = Exercise.ExerciseDifficulty.ADVANCED
    ),

    // Cables
    Exercise(
        name = "",
        nameResKey = "exercise_cable_leg_curl_name",
        equipment = Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.HAMSTRINGS,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_cable_leg_curl_description",
        imageResKey = "generic_cable"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_cable_pull_through_name",
        equipment = Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.GLUTES,
        secondaryMuscles = listOf(Exercise.Muscle.HAMSTRINGS, Exercise.Muscle.BACK),
        descriptionResKey = "exercise_cable_pull_through_description",
        imageResKey = "cable_row"
    ),

    // Bodyweight
    Exercise(
        name = "",
        nameResKey = "exercise_bodyweight_squat_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.QUADRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.GLUTES, Exercise.Muscle.HAMSTRINGS),
        imageResKey = "squat",
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_bodyweight_squat_description",
        variationsResKeys = listOf(
            "exercise_variation_bodyweight_squat_single_leg"
        )
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_bodyweight_step_ups_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.GLUTES,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_bodyweight_step_ups_description",
        imageResKey = "step_ups"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_bodyweight_lunge_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.QUADRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.GLUTES),
        descriptionResKey = "exercise_bodyweight_lunge_description",
        imageResKey = "lunge"
    ),

    // Machine
    Exercise(
        name = "",
        nameResKey = "exercise_machine_leg_curl_name",
        equipment = Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.HAMSTRINGS,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_machine_leg_curl_description",
        imageResKey = "leg_machine"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_machine_leg_extension_name",
        equipment = Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.QUADRICEPS,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_machine_leg_extension_description",
        imageResKey = "leg_machine"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_machine_hack_squat_name",
        equipment = Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.QUADRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.GLUTES, Exercise.Muscle.HAMSTRINGS),
        descriptionResKey = "exercise_machine_hack_squat_description",
        imageResKey = "generic_machine"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_leg_press_name",
        equipment = Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.QUADRICEPS,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_leg_press_description",
        imageResKey = "leg_press"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_abduction_machine_name",
        equipment = Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.GLUTES,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_abduction_machine_description",
        imageResKey = "generic_machine"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_adduction_machine_name",
        equipment = Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.GLUTES,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_adduction_machine_description",
        imageResKey = "generic_machine"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_machine_glute_kickback_name",
        equipment = Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.GLUTES,
        secondaryMuscles = listOf(Exercise.Muscle.HAMSTRINGS),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_machine_glute_kickback_description",
        imageResKey = "generic_machine"
    ),

    // Dumbbell
    Exercise(
        name = "",
        nameResKey = "exercise_bulgarian_split_squat_name",
        equipment = Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.QUADRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.GLUTES),
        descriptionResKey = "exercise_bulgarian_split_squat_description",
        imageResKey = "generic_dumbbell"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_dumbbell_lunges_name",
        equipment = Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.QUADRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.GLUTES),
        descriptionResKey = "exercise_dumbbell_lunges_description",
        imageResKey = "dumbbell_lunge"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_goblet_squat_name",
        equipment = Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.QUADRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.GLUTES, Exercise.Muscle.HAMSTRINGS),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_goblet_squat_description",
        imageResKey = "generic_dumbbell"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_dumbbell_step_ups_name",
        equipment = Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.GLUTES,
        secondaryMuscles = listOf(Exercise.Muscle.QUADRICEPS),
        descriptionResKey = "exercise_dumbbell_step_ups_description",
        imageResKey = "generic_dumbbell"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_dumbbell_romanian_deadlift_name",
        equipment = Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.HAMSTRINGS,
        secondaryMuscles = listOf(Exercise.Muscle.GLUTES, Exercise.Muscle.BACK),
        imageResKey = "generic_dumbbell",
        descriptionResKey = "exercise_dumbbell_romanian_deadlift_description",
    ),



    /*
        Calves
     */
    // Barbell
    Exercise(
        name = "",
        nameResKey = "exercise_barbell_calf_raises_name",
        equipment = Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.CALVES,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_barbell_calf_raises_desc",
        imageResKey = "calf"
    ),
    // Cables
    // Bodyweight
    Exercise(
        name = "",
        nameResKey = "exercise_calf_raises_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.CALVES,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_calf_raises_desc",
        imageResKey = "calf"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_single_leg_calf_raises_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.CALVES,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_single_leg_calf_raises_desc",
        imageResKey = "calf"
    ),
    // Machine
    Exercise(
        name = "",
        nameResKey = "exercise_machine_calf_raises_name",
        equipment = Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.CALVES,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_machine_calf_raises_desc",
        imageResKey = "calf"
    ),
    // Dumbbell
    Exercise(
        name = "",
        nameResKey = "exercise_dumbbell_calf_raises_name",
        equipment = Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.CALVES,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_dumbbell_calf_raises_desc",
        imageResKey = "calf"
    ),

    /*
        Shoulders
     */
    // Barbell
    Exercise(
        name = "",
        nameResKey = "exercise_push_and_press_name",
        equipment = Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.CHEST),
        descriptionResKey = "exercise_push_and_press_desc",
        imageResKey = "shoulder_press"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_shoulder_press_name",
        equipment = Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_shoulder_press_desc",
        imageResKey = "shoulder_press"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_clean_and_press_name",
        equipment = Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = listOf(
            Exercise.Muscle.BACK,
            Exercise.Muscle.TRICEPS,
            Exercise.Muscle.CHEST,
            Exercise.Muscle.QUADRICEPS,
            Exercise.Muscle.ABS
        ),
        imageResKey = "clean_press",
        descriptionResKey = "exercise_clean_and_press_desc",
        difficulty = Exercise.ExerciseDifficulty.ADVANCED
    ),
    // Cables
    Exercise(
        name = "",
        nameResKey = "exercise_cable_side_raise_name",
        equipment = Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_cable_side_raise_desc",
        imageResKey = "generic_cable"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_cable_rear_delt_fly_name",
        equipment = Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_cable_rear_delt_fly_desc",
        imageResKey = "generic_cable"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_cable_face_pull_name",
        equipment = Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_cable_face_pull_desc",
        imageResKey = "generic_cable"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_cable_shoulder_press_name",
        equipment = Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS),
        descriptionResKey = "exercise_cable_shoulder_press_desc",
        imageResKey = "generic_cable"
    ),
    // Bodyweight
    Exercise(
        name = "",
        nameResKey = "exercise_handstand_pushups_name",
        equipment = Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.CHEST),
        imageResKey = "headstand_push_up",
        descriptionResKey = "exercise_handstand_pushups_desc",
        difficulty = Exercise.ExerciseDifficulty.ADVANCED
    ),
    // Machine
    Exercise(
        name = "",
        nameResKey = "exercise_machine_shoulder_press_name",
        equipment = Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_machine_shoulder_press_desc",
        imageResKey = "generic_machine"
    ),
    // Dumbbell
    Exercise(
        name = "",
        nameResKey = "exercise_front_raise_name",
        equipment = Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_front_raise_desc",
        imageResKey = "generic_dumbbell"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_side_raise_name",
        equipment = Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_side_raise_desc",
        imageResKey = "generic_dumbbell"
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_dumbbell_shoulder_press_name",
        equipment = Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.CHEST),
        imageResKey = "dumbbell_shoulder_press",
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        descriptionResKey = "exercise_dumbbell_shoulder_press_desc",
        variationsResKeys = listOf(
            "exercise_variation_arnold_press"
        )
    ),
    Exercise(
        name = "",
        nameResKey = "exercise_dumbbell_rear_delt_row_name",
        equipment = Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = emptyList(),
        descriptionResKey = "exercise_dumbbell_rear_delt_row_desc",
        imageResKey = "generic_dumbbell"
    ),
)
