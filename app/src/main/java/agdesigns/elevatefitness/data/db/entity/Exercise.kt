package agdesigns.elevatefitness.data.db.entity

import agdesigns.elevatefitness.R
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import agdesigns.elevatefitness.shared.Equipment
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity
data class Exercise(
    @PrimaryKey(autoGenerate = true) val exerciseId: Long = 0L,
    @Deprecated("Unless user-defined exercise, use nameResKey instead")
    val name: String,
    @ColumnInfo(defaultValue = "")
    val nameResKey: String, // key of the string resource
    val equipment: Equipment,
    val primaryMuscle: Muscle,
    val secondaryMuscles: List<Muscle> = emptyList(),
    @Deprecated("Use imageResKey instead")
    val image: Int = R.drawable.finish_workout,
    @ColumnInfo(defaultValue = "finish_workout")
    val imageResKey: String = "finish_workout",
    @Deprecated("Use descriptionResKey instead")
    val description: String = "Description not available",
    @ColumnInfo(defaultValue = "description_not_available")
    val descriptionResKey: String = "description_not_available",
    val difficulty: ExerciseDifficulty = ExerciseDifficulty.INTERMEDIATE,
    val probability: Double = 1.0, // weight used when randomly selecting exercises
    @Deprecated("Use variationsResIds instead")
    val variations: List<String> = emptyList(),
    @ColumnInfo(defaultValue = "")
    val variationsResKeys: List<String> = emptyList(),
    @ColumnInfo(defaultValue = "0")
    val userDefined: Boolean = false,  // exercise was created by user
    @ColumnInfo(defaultValue = "1")  // This is set when adding a column in the migration, sets everything to true
    val needsMigration: Boolean = false // this is set when adding an exercise, doesn't need migration
) : Parcelable {
    // Helper properties for UI
    val nameResource: Int
        get() = getNameDescriptionResource(nameResKey)

    val imageResource: Int
        get() = getImageResource(imageResKey)

    val descriptionResource: Int
        get() = getNameDescriptionResource(descriptionResKey)

    val variationsResource: List<Int>
        get() = getVariations(variationsResKeys)

    enum class Muscle (val muscleResKey: String, val imageResKey: String){
        EVERYTHING("muscles_see_all", "full_body"), // Used when filtering by muscle to get everything
        ABS("muscles_abs", "abs"),
        BACK("muscles_back", "back"),
        BICEPS("muscles_biceps", "biceps"),
        CALVES("muscles_calves", "calves"),
        CHEST("muscles_chest", "chest"),
        GLUTES("muscles_glutes", "glutes"),
        HAMSTRINGS("muscles_hamstrings", "hamstrings"),
        QUADRICEPS("muscles_quadriceps", "quadriceps"),
        SHOULDERS("muscles_shoulders", "shoulders"),
        TRICEPS("muscles_triceps", "triceps");
        
        val imageRes: Int
            get() = when (this) {
                EVERYTHING -> R.drawable.full_body
                ABS -> R.drawable.abs
                BACK -> R.drawable.back
                BICEPS -> R.drawable.biceps
                CALVES -> R.drawable.calves
                CHEST -> R.drawable.chest
                GLUTES -> R.drawable.glutes
                HAMSTRINGS -> R.drawable.hamstrings
                QUADRICEPS -> R.drawable.quadriceps
                SHOULDERS -> R.drawable.shoulders
                TRICEPS -> R.drawable.triceps
            }
        val muscleNameResource: Int
            get() = getMuscleResource(this.muscleResKey)
    }

    enum class ExerciseDifficulty(val difficultyResKey: String){
        BEGINNER("difficulties_beginner"),
        INTERMEDIATE("difficulties_intermediate"),
        ADVANCED("difficulties_advanced");

        val difficultyResource: Int
            get() = when (this) {
                BEGINNER -> R.string.difficulties_beginner
                INTERMEDIATE -> R.string.difficulties_intermediate
                ADVANCED -> R.string.difficulties_advanced
            }

    }
}

fun getImageResource(name: String): Int = when(name) {
    "ab_roller" -> R.drawable.ab_roller
    "barbell_clean" -> R.drawable.barbell_clean
    "barbell_curl" -> R.drawable.barbell_curl
    "barbell_lunge" -> R.drawable.barbell_lunge
    "barbell_row" -> R.drawable.barbell_row
    "barbell_squat" -> R.drawable.barbell_squat
    "bench_dip" -> R.drawable.bench_dip
    "bench_press" -> R.drawable.bench_press
    "cable_crossover" -> R.drawable.cable_crossover
    "cable_curl" -> R.drawable.cable_curl
    "cable_pushdown" -> R.drawable.cable_pushdown
    "cable_row" -> R.drawable.cable_row
    "calf" -> R.drawable.calf
    "chest_dip" -> R.drawable.chest_dip
    "chest_press" -> R.drawable.chest_press
    "chin_up" -> R.drawable.chin_up
    "clean_press" -> R.drawable.clean_press
    "concentration_curl" -> R.drawable.concentration_curl
    "crunch" -> R.drawable.crunch
    "deadlift" -> R.drawable.deadlift
    "dumbbell_bench_press" -> R.drawable.dumbbell_bench_press
    "dumbbell_curl" -> R.drawable.dumbbell_curl
    "dumbbell_lunge" -> R.drawable.dumbbell_lunge
    "dumbbell_row" -> R.drawable.dumbbell_row
    "dumbbell_shoulder_press" -> R.drawable.dumbbell_shoulder_press
    "dumbbell_shrug" -> R.drawable.dumbbell_shrug
    "generic_barbell" -> R.drawable.generic_barbell
    "generic_cable" -> R.drawable.generic_cable
    "generic_dumbbell" -> R.drawable.generic_dumbbell
    "generic_machine" -> R.drawable.generic_machine
    "headstand_push_up" -> R.drawable.headstand_push_up
    "hyperextensions" -> R.drawable.hyperextensions
    "incline_bench_press" -> R.drawable.incline_bench_press
    "knee_raises" -> R.drawable.knee_raises
    "lat_pulldown" -> R.drawable.lat_pulldown
    "leg_machine" -> R.drawable.leg_machine
    "leg_press" -> R.drawable.leg_press
    "leg_raises" -> R.drawable.leg_raises
    "lunge" -> R.drawable.lunge
    "machine_fly" -> R.drawable.machine_fly
    "muscle_up" -> R.drawable.muscle_up
    "plank" -> R.drawable.plank
    "pull_up" -> R.drawable.pull_up
    "push_up" -> R.drawable.push_up
    "romanian_deadlift" -> R.drawable.romanian_deadlift
    "rope_climb" -> R.drawable.rope_climb
    "russian_twist" -> R.drawable.russian_twist
    "scott_dumbbell" -> R.drawable.scott_dumbbell
    "shoulder_press" -> R.drawable.shoulder_press
    "side_crunch" -> R.drawable.side_crunch
    "side_plank" -> R.drawable.side_plank
    "sit_ups" -> R.drawable.sit_ups
    "squat" -> R.drawable.squat
    "step_ups" -> R.drawable.step_ups
    "sumo_deadlift" -> R.drawable.sumo_deadlift
    "wide_pull_up" -> R.drawable.wide_pull_up
    else -> R.drawable.finish_workout
}

fun getNameDescriptionResource(key: String): Int = when (key) {
    "exercise_bench_press_name" -> R.string.exercise_bench_press_name
    "exercise_bench_press_description" -> R.string.exercise_bench_press_description
    "exercise_incline_bench_press_name" -> R.string.exercise_incline_bench_press_name
    "exercise_incline_bench_press_description" -> R.string.exercise_incline_bench_press_description
    "exercise_decline_bench_press_name" -> R.string.exercise_decline_bench_press_name
    "exercise_decline_bench_press_description" -> R.string.exercise_decline_bench_press_description
    "exercise_cable_crossover_name" -> R.string.exercise_cable_crossover_name
    "exercise_cable_crossover_description" -> R.string.exercise_cable_crossover_description
    "exercise_chest_dip_name" -> R.string.exercise_chest_dip_name
    "exercise_chest_dip_description" -> R.string.exercise_chest_dip_description
    "exercise_push_up_name" -> R.string.exercise_push_up_name
    "exercise_push_up_description" -> R.string.exercise_push_up_description
    "exercise_chest_press_name" -> R.string.exercise_chest_press_name
    "exercise_chest_press_description" -> R.string.exercise_chest_press_description
    "exercise_machine_fly_name" -> R.string.exercise_machine_fly_name
    "exercise_machine_fly_description" -> R.string.exercise_machine_fly_description
    "exercise_dumbbell_bench_press_name" -> R.string.exercise_dumbbell_bench_press_name
    "exercise_dumbbell_bench_press_description" -> R.string.exercise_dumbbell_bench_press_description
    "exercise_dumbbell_fly_name" -> R.string.exercise_dumbbell_fly_name
    "exercise_dumbbell_fly_description" -> R.string.exercise_dumbbell_fly_description
    "exercise_pullover_name" -> R.string.exercise_pullover_name
    "exercise_pullover_description" -> R.string.exercise_pullover_description
    "exercise_deadlift_name" -> R.string.exercise_deadlift_name
    "exercise_deadlift_description" -> R.string.exercise_deadlift_description
    "exercise_sumo_deadlift_name" -> R.string.exercise_sumo_deadlift_name
    "exercise_sumo_deadlift_description" -> R.string.exercise_sumo_deadlift_description
    "exercise_barbell_shrug_name" -> R.string.exercise_barbell_shrug_name
    "exercise_barbell_shrug_description" -> R.string.exercise_barbell_shrug_description
    "exercise_barbell_row_name" -> R.string.exercise_barbell_row_name
    "exercise_barbell_row_description" -> R.string.exercise_barbell_row_description
    "exercise_barbell_tbar_row_name" -> R.string.exercise_barbell_tbar_row_name
    "exercise_barbell_tbar_row_description" -> R.string.exercise_barbell_tbar_row_description
    "exercise_barbell_upright_row_name" -> R.string.exercise_barbell_upright_row_name
    "exercise_barbell_upright_row_description" -> R.string.exercise_barbell_upright_row_description
    "exercise_cable_row_name" -> R.string.exercise_cable_row_name
    "exercise_cable_row_description" -> R.string.exercise_cable_row_description
    "exercise_cable_pullover_name" -> R.string.exercise_cable_pullover_name
    "exercise_cable_pullover_description" -> R.string.exercise_cable_pullover_description
    "exercise_cable_upright_row_name" -> R.string.exercise_cable_upright_row_name
    "exercise_cable_upright_row_description" -> R.string.exercise_cable_upright_row_description
    "exercise_pullup_name" -> R.string.exercise_pullup_name
    "exercise_pullup_description" -> R.string.exercise_pullup_description
    "exercise_wide_grip_pullup_name" -> R.string.exercise_wide_grip_pullup_name
    "exercise_wide_grip_pullup_description" -> R.string.exercise_wide_grip_pullup_description
    "exercise_chinup_name" -> R.string.exercise_chinup_name
    "exercise_chinup_description" -> R.string.exercise_chinup_description
    "exercise_muscleup_name" -> R.string.exercise_muscleup_name
    "exercise_muscleup_description" -> R.string.exercise_muscleup_description
    "exercise_rope_climb_name" -> R.string.exercise_rope_climb_name
    "exercise_rope_climb_description" -> R.string.exercise_rope_climb_description
    "exercise_machine_row_name" -> R.string.exercise_machine_row_name
    "exercise_machine_row_description" -> R.string.exercise_machine_row_description
    "exercise_hyperextensions_name" -> R.string.exercise_hyperextensions_name
    "exercise_hyperextensions_description" -> R.string.exercise_hyperextensions_description
    "exercise_lat_pulldown_name" -> R.string.exercise_lat_pulldown_name
    "exercise_lat_pulldown_description" -> R.string.exercise_lat_pulldown_description
    "exercise_vertical_traction_name" -> R.string.exercise_vertical_traction_name
    "exercise_vertical_traction_description" -> R.string.exercise_vertical_traction_description
    "exercise_dumbbell_row_name" -> R.string.exercise_dumbbell_row_name
    "exercise_dumbbell_row_description" -> R.string.exercise_dumbbell_row_description
    "exercise_dumbbell_deadlift_name" -> R.string.exercise_dumbbell_deadlift_name
    "exercise_dumbbell_deadlift_description" -> R.string.exercise_dumbbell_deadlift_description
    "exercise_dumbbell_shrug_name" -> R.string.exercise_dumbbell_shrug_name
    "exercise_dumbbell_shrug_description" -> R.string.exercise_dumbbell_shrug_description
    "exercise_upright_dumbbell_row_name" -> R.string.exercise_upright_dumbbell_row_name
    "exercise_upright_dumbbell_row_description" -> R.string.exercise_upright_dumbbell_row_description
    "exercise_cable_crunch_name" -> R.string.exercise_cable_crunch_name
    "exercise_cable_crunch_desc" -> R.string.exercise_cable_crunch_desc
    "exercise_crunch_name" -> R.string.exercise_crunch_name
    "exercise_crunch_desc" -> R.string.exercise_crunch_desc
    "exercise_knee_raises_name" -> R.string.exercise_knee_raises_name
    "exercise_knee_raises_desc" -> R.string.exercise_knee_raises_desc
    "exercise_leg_raises_name" -> R.string.exercise_leg_raises_name
    "exercise_leg_raises_desc" -> R.string.exercise_leg_raises_desc
    "exercise_plank_name" -> R.string.exercise_plank_name
    "exercise_plank_desc" -> R.string.exercise_plank_desc
    "exercise_russian_twist_name" -> R.string.exercise_russian_twist_name
    "exercise_russian_twist_desc" -> R.string.exercise_russian_twist_desc
    "exercise_side_plank_name" -> R.string.exercise_side_plank_name
    "exercise_side_plank_desc" -> R.string.exercise_side_plank_desc
    "exercise_sit_ups_name" -> R.string.exercise_sit_ups_name
    "exercise_sit_ups_desc" -> R.string.exercise_sit_ups_desc
    "exercise_side_crunch_name" -> R.string.exercise_side_crunch_name
    "exercise_side_crunch_desc" -> R.string.exercise_side_crunch_desc
    "exercise_dragon_flag_name" -> R.string.exercise_dragon_flag_name
    "exercise_dragon_flag_desc" -> R.string.exercise_dragon_flag_desc
    "exercise_ab_roller_name" -> R.string.exercise_ab_roller_name
    "exercise_ab_roller_desc" -> R.string.exercise_ab_roller_desc
    "exercise_ab_machine_name" -> R.string.exercise_ab_machine_name
    "exercise_ab_machine_desc" -> R.string.exercise_ab_machine_desc
    "exercise_dumbbell_side_bend_name" -> R.string.exercise_dumbbell_side_bend_name
    "exercise_dumbbell_side_bend_desc" -> R.string.exercise_dumbbell_side_bend_desc
    "exercise_barbell_curl_name" -> R.string.exercise_barbell_curl_name
    "exercise_barbell_curl_description" -> R.string.exercise_barbell_curl_description
    "exercise_cable_curl_name" -> R.string.exercise_cable_curl_name
    "exercise_cable_curl_description" -> R.string.exercise_cable_curl_description
    "exercise_machine_biceps_curl_name" -> R.string.exercise_machine_biceps_curl_name
    "exercise_machine_biceps_curl_description" -> R.string.exercise_machine_biceps_curl_description
    "exercise_dumbbell_curl_name" -> R.string.exercise_dumbbell_curl_name
    "exercise_dumbbell_curl_description" -> R.string.exercise_dumbbell_curl_description
    "exercise_dumbbell_concentration_curl_name" -> R.string.exercise_dumbbell_concentration_curl_name
    "exercise_dumbbell_concentration_curl_description" -> R.string.exercise_dumbbell_concentration_curl_description
    "exercise_dumbbell_scott_curl_name" -> R.string.exercise_dumbbell_scott_curl_name
    "exercise_dumbbell_scott_curl_description" -> R.string.exercise_dumbbell_scott_curl_description
    "exercise_barbell_triceps_extensions_name" -> R.string.exercise_barbell_triceps_extensions_name
    "exercise_barbell_triceps_extensions_description" -> R.string.exercise_barbell_triceps_extensions_description
    "exercise_close_grip_bench_press_name" -> R.string.exercise_close_grip_bench_press_name
    "exercise_close_grip_bench_press_description" -> R.string.exercise_close_grip_bench_press_description
    "exercise_french_press_name" -> R.string.exercise_french_press_name
    "exercise_french_press_description" -> R.string.exercise_french_press_description
    "exercise_skull_crusher_name" -> R.string.exercise_skull_crusher_name
    "exercise_skull_crusher_description" -> R.string.exercise_skull_crusher_description
    "exercise_cable_skull_crusher_name" -> R.string.exercise_cable_skull_crusher_name
    "exercise_cable_skull_crusher_description" -> R.string.exercise_cable_skull_crusher_description
    "exercise_cable_pushdown_name" -> R.string.exercise_cable_pushdown_name
    "exercise_cable_pushdown_description" -> R.string.exercise_cable_pushdown_description
    "exercise_overhead_cable_triceps_extension_name" -> R.string.exercise_overhead_cable_triceps_extension_name
    "exercise_overhead_cable_triceps_extension_description" -> R.string.exercise_overhead_cable_triceps_extension_description
    "exercise_triceps_dip_name" -> R.string.exercise_triceps_dip_name
    "exercise_triceps_dip_description" -> R.string.exercise_triceps_dip_description
    "exercise_bench_dip_name" -> R.string.exercise_bench_dip_name
    "exercise_bench_dip_description" -> R.string.exercise_bench_dip_description
    "exercise_diamond_push_up_name" -> R.string.exercise_diamond_push_up_name
    "exercise_diamond_push_up_description" -> R.string.exercise_diamond_push_up_description
    "exercise_parallel_arms_push_up_name" -> R.string.exercise_parallel_arms_push_up_name
    "exercise_parallel_arms_push_up_description" -> R.string.exercise_parallel_arms_push_up_description
    "exercise_machine_triceps_extension_name" -> R.string.exercise_machine_triceps_extension_name
    "exercise_machine_triceps_extension_description" -> R.string.exercise_machine_triceps_extension_description
    "exercise_dumbbell_triceps_extensions_name" -> R.string.exercise_dumbbell_triceps_extensions_name
    "exercise_dumbbell_triceps_extensions_description" -> R.string.exercise_dumbbell_triceps_extensions_description
    "exercise_close_grip_dumbbell_bench_press_name" -> R.string.exercise_close_grip_dumbbell_bench_press_name
    "exercise_close_grip_dumbbell_bench_press_description" -> R.string.exercise_close_grip_dumbbell_bench_press_description
    "exercise_dumbbell_skull_crusher_name" -> R.string.exercise_dumbbell_skull_crusher_name
    "exercise_dumbbell_skull_crusher_description" -> R.string.exercise_dumbbell_skull_crusher_description
    "exercise_barbell_hip_thrust_name" -> R.string.exercise_barbell_hip_thrust_name
    "exercise_barbell_hip_thrust_description" -> R.string.exercise_barbell_hip_thrust_description
    "exercise_barbell_clean_name" -> R.string.exercise_barbell_clean_name
    "exercise_barbell_clean_description" -> R.string.exercise_barbell_clean_description
    "exercise_squat_name" -> R.string.exercise_squat_name
    "exercise_squat_description" -> R.string.exercise_squat_description
    "exercise_barbell_lunge_name" -> R.string.exercise_barbell_lunge_name
    "exercise_barbell_lunge_description" -> R.string.exercise_barbell_lunge_description
    "exercise_romanian_deadlift_name" -> R.string.exercise_romanian_deadlift_name
    "exercise_romanian_deadlift_description" -> R.string.exercise_romanian_deadlift_description
    "exercise_cable_leg_curl_name" -> R.string.exercise_cable_leg_curl_name
    "exercise_cable_leg_curl_description" -> R.string.exercise_cable_leg_curl_description
    "exercise_cable_pull_through_name" -> R.string.exercise_cable_pull_through_name
    "exercise_cable_pull_through_description" -> R.string.exercise_cable_pull_through_description
    "exercise_bodyweight_squat_name" -> R.string.exercise_bodyweight_squat_name
    "exercise_bodyweight_squat_description" -> R.string.exercise_bodyweight_squat_description
    "exercise_bodyweight_step_ups_name" -> R.string.exercise_bodyweight_step_ups_name
    "exercise_bodyweight_step_ups_description" -> R.string.exercise_bodyweight_step_ups_description
    "exercise_bodyweight_lunge_name" -> R.string.exercise_bodyweight_lunge_name
    "exercise_bodyweight_lunge_description" -> R.string.exercise_bodyweight_lunge_description
    "exercise_machine_leg_curl_name" -> R.string.exercise_machine_leg_curl_name
    "exercise_machine_leg_curl_description" -> R.string.exercise_machine_leg_curl_description
    "exercise_machine_leg_extension_name" -> R.string.exercise_machine_leg_extension_name
    "exercise_machine_leg_extension_description" -> R.string.exercise_machine_leg_extension_description
    "exercise_machine_hack_squat_name" -> R.string.exercise_machine_hack_squat_name
    "exercise_machine_hack_squat_description" -> R.string.exercise_machine_hack_squat_description
    "exercise_leg_press_name" -> R.string.exercise_leg_press_name
    "exercise_leg_press_description" -> R.string.exercise_leg_press_description
    "exercise_abduction_machine_name" -> R.string.exercise_abduction_machine_name
    "exercise_abduction_machine_description" -> R.string.exercise_abduction_machine_description
    "exercise_adduction_machine_name" -> R.string.exercise_adduction_machine_name
    "exercise_adduction_machine_description" -> R.string.exercise_adduction_machine_description
    "exercise_machine_glute_kickback_name" -> R.string.exercise_machine_glute_kickback_name
    "exercise_machine_glute_kickback_description" -> R.string.exercise_machine_glute_kickback_description
    "exercise_bulgarian_split_squat_name" -> R.string.exercise_bulgarian_split_squat_name
    "exercise_bulgarian_split_squat_description" -> R.string.exercise_bulgarian_split_squat_description
    "exercise_dumbbell_lunges_name" -> R.string.exercise_dumbbell_lunges_name
    "exercise_dumbbell_lunges_description" -> R.string.exercise_dumbbell_lunges_description
    "exercise_goblet_squat_name" -> R.string.exercise_goblet_squat_name
    "exercise_goblet_squat_description" -> R.string.exercise_goblet_squat_description
    "exercise_dumbbell_step_ups_name" -> R.string.exercise_dumbbell_step_ups_name
    "exercise_dumbbell_step_ups_description" -> R.string.exercise_dumbbell_step_ups_description
    "exercise_dumbbell_romanian_deadlift_name" -> R.string.exercise_dumbbell_romanian_deadlift_name
    "exercise_dumbbell_romanian_deadlift_description" -> R.string.exercise_dumbbell_romanian_deadlift_description
    "exercise_barbell_calf_raises_name" -> R.string.exercise_barbell_calf_raises_name
    "exercise_barbell_calf_raises_desc" -> R.string.exercise_barbell_calf_raises_desc
    "exercise_calf_raises_name" -> R.string.exercise_calf_raises_name
    "exercise_calf_raises_desc" -> R.string.exercise_calf_raises_desc
    "exercise_single_leg_calf_raises_name" -> R.string.exercise_single_leg_calf_raises_name
    "exercise_single_leg_calf_raises_desc" -> R.string.exercise_single_leg_calf_raises_desc
    "exercise_machine_calf_raises_name" -> R.string.exercise_machine_calf_raises_name
    "exercise_machine_calf_raises_desc" -> R.string.exercise_machine_calf_raises_desc
    "exercise_dumbbell_calf_raises_name" -> R.string.exercise_dumbbell_calf_raises_name
    "exercise_dumbbell_calf_raises_desc" -> R.string.exercise_dumbbell_calf_raises_desc
    "exercise_push_and_press_name" -> R.string.exercise_push_and_press_name
    "exercise_push_and_press_desc" -> R.string.exercise_push_and_press_desc
    "exercise_shoulder_press_name" -> R.string.exercise_shoulder_press_name
    "exercise_shoulder_press_desc" -> R.string.exercise_shoulder_press_desc
    "exercise_clean_and_press_name" -> R.string.exercise_clean_and_press_name
    "exercise_clean_and_press_desc" -> R.string.exercise_clean_and_press_desc
    "exercise_cable_side_raise_name" -> R.string.exercise_cable_side_raise_name
    "exercise_cable_side_raise_desc" -> R.string.exercise_cable_side_raise_desc
    "exercise_cable_rear_delt_fly_name" -> R.string.exercise_cable_rear_delt_fly_name
    "exercise_cable_rear_delt_fly_desc" -> R.string.exercise_cable_rear_delt_fly_desc
    "exercise_cable_face_pull_name" -> R.string.exercise_cable_face_pull_name
    "exercise_cable_face_pull_desc" -> R.string.exercise_cable_face_pull_desc
    "exercise_cable_shoulder_press_name" -> R.string.exercise_cable_shoulder_press_name
    "exercise_cable_shoulder_press_desc" -> R.string.exercise_cable_shoulder_press_desc
    "exercise_handstand_pushups_name" -> R.string.exercise_handstand_pushups_name
    "exercise_handstand_pushups_desc" -> R.string.exercise_handstand_pushups_desc
    "exercise_machine_shoulder_press_name" -> R.string.exercise_machine_shoulder_press_name
    "exercise_machine_shoulder_press_desc" -> R.string.exercise_machine_shoulder_press_desc
    "exercise_front_raise_name" -> R.string.exercise_front_raise_name
    "exercise_front_raise_desc" -> R.string.exercise_front_raise_desc
    "exercise_side_raise_name" -> R.string.exercise_side_raise_name
    "exercise_side_raise_desc" -> R.string.exercise_side_raise_desc
    "exercise_dumbbell_shoulder_press_name" -> R.string.exercise_dumbbell_shoulder_press_name
    "exercise_dumbbell_shoulder_press_desc" -> R.string.exercise_dumbbell_shoulder_press_desc
    "exercise_dumbbell_rear_delt_row_name" -> R.string.exercise_dumbbell_rear_delt_row_name
    "exercise_dumbbell_rear_delt_row_desc" -> R.string.exercise_dumbbell_rear_delt_row_desc
    else -> R.string.exercise_name_error
}

fun getVariations(keys: List<String>): List<Int> = keys.map {
    getVariation(it)
}

fun getVariation(key: String): Int = when (key) {
    "exercise_variation_pushup_wide" -> R.string.exercise_variation_pushup_wide
    "exercise_variation_pushup_incline" -> R.string.exercise_variation_pushup_incline
    "exercise_variation_pushup_decline" -> R.string.exercise_variation_pushup_decline
    "exercise_variation_pushup_single_arm" -> R.string.exercise_variation_pushup_single_arm
    "exercise_variation_dumbbell_incline" -> R.string.exercise_variation_dumbbell_incline
    "exercise_variation_dumbbell_decline" -> R.string.exercise_variation_dumbbell_decline
    "exercise_variation_dumbbell_single_arm" -> R.string.exercise_variation_dumbbell_single_arm
    "exercise_variation_dumbbell_fly_incline" -> R.string.exercise_variation_dumbbell_fly_incline
    "exercise_variation_dumbbell_fly_decline" -> R.string.exercise_variation_dumbbell_fly_decline
    "exercise_variation_preacher" -> R.string.exercise_variation_preacher
    "exercise_variation_scott" -> R.string.exercise_variation_scott
    "exercise_variation_bar" -> R.string.exercise_variation_bar
    "exercise_variation_rope" -> R.string.exercise_variation_rope
    "exercise_variation_handles" -> R.string.exercise_variation_handles
    "exercise_variation_alternating" -> R.string.exercise_variation_alternating
    "exercise_variation_inclined_bench" -> R.string.exercise_variation_dumbbell_inclined_bench
    "exercise_variation_pullup_single_arm" -> R.string.exercise_variation_pullup_single_arm
    "exercise_variation_chinup_close_grip" -> R.string.exercise_variation_chinup_close_grip
    "exercise_variation_lat_pulldown_vbar" -> R.string.exercise_variation_lat_pulldown_vbar
    "exercise_variation_squat_front" -> R.string.exercise_variation_squat_front
    "exercise_variation_squat_hack" -> R.string.exercise_variation_squat_hack
    "exercise_variation_bodyweight_squat_single_leg" -> R.string.exercise_variation_squat_bodyweight_single_leg
    "exercise_variation_arnold_press" -> R.string.exercise_variation_arnold_press
    "" -> R.string.empty_variation  // no variation
    "no_variation" -> R.string.no_variation
    else -> R.string.exercise_variation_error
}

fun getMuscleResource(muscleResKey: String): Int = when (muscleResKey) {
    "muscles_see_all" -> R.string.muscles_see_all
    "muscles_abs" -> R.string.muscles_abs
    "muscles_back" -> R.string.muscles_back
    "muscles_biceps" -> R.string.muscles_biceps
    "muscles_calves" -> R.string.muscles_calves
    "muscles_chest" -> R.string.muscles_chest
    "muscles_glutes" -> R.string.muscles_glutes
    "muscles_hamstrings" -> R.string.muscles_hamstrings
    "muscles_quadriceps" -> R.string.muscles_quadriceps
    "muscles_shoulders" -> R.string.muscles_shoulders
    "muscles_triceps" -> R.string.muscles_triceps
    else -> R.string.muscles_error
}