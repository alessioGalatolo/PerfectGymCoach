package agdesigns.elevatefitness.data.db.entity

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanGoal.Companion.getGoalResource
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.text.replaceFirstChar


@Entity(tableName = "plan")
@Parcelize
data class WorkoutPlan(
    @PrimaryKey(autoGenerate = true) val planId: Long = 0L,
    val name: String,
    val creationDate: ZonedDateTime, // in millis; should be used to e.g. suggest to create new plan
    val currentProgram: Int = 0, // The index of the upcoming program after ordering
    val archived: Boolean = false,  // instead of deleting the plan
): Parcelable

@Parcelize
data class WorkoutPlanUpdateProgram(
    val planId: Long,
    val currentProgram: Int
): Parcelable

@Parcelize
data class ArchiveWorkoutPlan(
    val planId: Long,
    val archived: Boolean = true
): Parcelable

// classes below are used when generating a plan
enum class WorkoutPlanGoal(val descResKey: String, val goalResKey: String){
    HYPERTROPHY("goals_hypertrophy_desc", "goals_hypertrophy"),
    STRENGTH("goals_strength_desc", "goals_strength"),
    ENDURANCE("goals_endurance_desc", "goals_endurance"),
    CARDIO("goals_cardio_desc", "goals_cardio");

    val descResource: Int
        get() = when (this) {
            HYPERTROPHY -> R.string.goals_hypertrophy_desc
            STRENGTH -> R.string.goals_strength_desc
            ENDURANCE -> R.string.goals_endurance_desc
            CARDIO -> R.string.goals_cardio_desc
        }

    val goalResource: Int
        get() = when (this) {
            HYPERTROPHY -> R.string.goals_hypertrophy
            STRENGTH -> R.string.goals_strength
            ENDURANCE -> R.string.goals_endurance
            CARDIO -> R.string.goals_cardio
        }

    companion object {
        fun getGoalResource(key: String): Int = when (key) {
            "goals_hypertrophy" -> R.string.goals_hypertrophy
            "goals_strength" -> R.string.goals_strength
            "goals_endurance" -> R.string.goals_endurance
            "goals_cardio" -> R.string.goals_cardio
            else -> R.string.goals_error
        }
    }
}


// generated programs should be named with this + comma separated muscleResKeys
const val GENERATED_PLAN_PREFIX = "[GENERATED PLAN]"
fun getGeneratedPlanName(goalChoice: WorkoutPlanGoal, date: ZonedDateTime): String {
    return GENERATED_PLAN_PREFIX + "/****/" + goalChoice.goalResKey + "/****/" + date.toInstant().toEpochMilli().toString()
}

@Composable
fun getPlanDisplayName(name: String): String {
    // check if generated program, otherwise return original name
    return if (name.startsWith(GENERATED_PLAN_PREFIX)) {
        val noPrefix = name.removePrefix(GENERATED_PLAN_PREFIX)
        val parts = noPrefix.split("/****/")
        val millis = parts[2].toLong()
        val date = ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("d MMM (yyyy)")
        val goal = stringResource(getGoalResource(parts[1]))
        val goalFormatted = goal.lowercase().replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
        goalFormatted + " - " + date.format(formatter)
    } else {
        name
    }
}


enum class WorkoutPlanDifficulty(val expertiseResKey: String) {
    AUTO("plan_diff_guess"),
    BEGINNER("plan_diff_beginner"),
    INTERMEDIATE("plan_diff_intermediate"),
    ADVANCED("plan_diff_advanced");

    val expertiseResource: Int
        get() = when (this) {
            BEGINNER -> R.string.plan_diff_beginner
            INTERMEDIATE -> R.string.plan_diff_intermediate
            ADVANCED -> R.string.plan_diff_advanced
            AUTO -> R.string.plan_diff_guess
        }
}

enum class WorkoutPlanSplit(val splitResKey: String) {
    FULL_BODY("splits_fullbody"),
    BRO("splits_bro"),
    UPPER_LOWER("splits_upper_lower"),
    GAINZ("splits_gainz"),
    AUTO("splits_auto");

    val splitResource: Int
        get() = when (this) {
            FULL_BODY -> R.string.splits_fullbody
            BRO -> R.string.splits_bro
            UPPER_LOWER -> R.string.splits_upper_lower
            GAINZ -> R.string.splits_gainz
            AUTO -> R.string.splits_auto
        }
}