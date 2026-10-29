package agdesigns.elevatefitness.data.db.entity

import agdesigns.elevatefitness.R
import android.os.Parcelable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import agdesigns.elevatefitness.shared.Equipment
import kotlinx.parcelize.Parcelize

/**
 * Achievement unlocked by a workout. Computed once, when the workout is finished, and never
 * recomputed: later workouts or edits to the history do not change it.
 */
@Parcelize
@Entity(
    foreignKeys = [ForeignKey(
        entity = WorkoutRecord::class,
        parentColumns = ["workoutId"],
        childColumns = ["extWorkoutId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("extWorkoutId"), Index("extExerciseId")]
)
data class WorkoutAchievement(
    @PrimaryKey(autoGenerate = true) val achievementId: Long = 0L,
    val extWorkoutId: Long,
    val type: AchievementType,
    // null for achievements of the whole workout
    val extExerciseId: Long? = null,
    // unit depends on the type, see AchievementType
    val value: Double,
    // best value before this workout, null if there is nothing to compare to (e.g., milestones)
    val previousValue: Double? = null,
    // reps of the set for HEAVIEST_WEIGHT, weight (kg) of the set for REP_RECORD
    val secondaryValue: Double? = null,
) : Parcelable

enum class AchievementType(val icon: ImageVector, val isPersonalRecord: Boolean) {
    // kg, total load lifted in a single set (added weight only for body weight exercises)
    HEAVIEST_WEIGHT(Icons.Default.FitnessCenter, true),
    // kg, best estimated one rep max (Epley)
    ESTIMATED_ONE_REP_MAX(Icons.AutoMirrored.Filled.TrendingUp, true),
    // reps, most reps done with at least secondaryValue kg
    REP_RECORD(Icons.Default.Repeat, true),
    // kg, most volume lifted for the exercise in a single workout
    EXERCISE_VOLUME(Icons.Default.Layers, true),
    // seconds, longest set of a duration based exercise
    LONGEST_DURATION(Icons.Default.Timer, true),
    // kg, most volume lifted in a single workout
    WORKOUT_VOLUME(Icons.Default.EmojiEvents, false),
    // kg, most volume lifted in a workout of this program
    PROGRAM_VOLUME(Icons.Default.MilitaryTech, false),
    // number of workouts completed
    WORKOUT_COUNT(Icons.Default.WorkspacePremium, false),
    // kg, lifetime volume milestone reached
    LIFETIME_VOLUME(Icons.Default.AllInclusive, false),
    // weeks, new longest weekly streak
    LONGEST_STREAK(Icons.Default.LocalFireDepartment, false);

    val titleResource: Int
        get() = when (this) {
            HEAVIEST_WEIGHT -> R.string.achievement_heaviest_weight
            ESTIMATED_ONE_REP_MAX -> R.string.achievement_one_rep_max
            REP_RECORD -> R.string.achievement_rep_record
            EXERCISE_VOLUME -> R.string.achievement_exercise_volume
            LONGEST_DURATION -> R.string.achievement_longest_duration
            WORKOUT_VOLUME -> R.string.achievement_workout_volume
            PROGRAM_VOLUME -> R.string.achievement_program_volume
            WORKOUT_COUNT -> R.string.achievement_workout_count
            LIFETIME_VOLUME -> R.string.achievement_lifetime_volume
            LONGEST_STREAK -> R.string.achievement_longest_streak
        }
}

@Parcelize
data class WorkoutAchievementAndInfo(
    val achievementId: Long,
    val extWorkoutId: Long,
    val type: AchievementType,
    val extExerciseId: Long?,
    val value: Double,
    val previousValue: Double?,
    val secondaryValue: Double?,
    // exercise info, null for achievements of the whole workout
    val name: String?,
    val nameResKey: String?,
    val userDefined: Boolean?,
    val equipment: Equipment?,
) : Parcelable {
    val nameResource: Int
        get() = getNameDescriptionResource(nameResKey ?: "")
}
