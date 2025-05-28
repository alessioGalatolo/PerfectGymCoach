package agdesigns.elevatefitness.data.workout_plan

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime


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
enum class WorkoutPlanGoal(val goal: String){
    HYPERTROPHY("Build muscle (hypertrophy)"),
    STRENGTH("Increase strength"),
    ENDURANCE("Increase endurance"),
    CARDIO("Lose weight (cardio training)"),
}

enum class WorkoutPlanDifficulty(val expertiseLevel: String) {
    AUTO("You should know my level"),
    BEGINNER("Beginner: just starting out"),
    INTERMEDIATE("Intermediate: feel confident to try some more advanced exercises"),
    ADVANCED("Advanced: you got this👌")
}

enum class WorkoutPlanSplit(val split: String) {
    FULL_BODY("Full-body routine: 1+ days a week"),
    BRO("Bro split (pull/push/legs): 3 days a week"),
    UPPER_LOWER("Upper/Lower body split: 2+ days a week"),
    GAINZ("1 Muscle group per day: 5 days a week"),
    AUTO("Surprise me")
}