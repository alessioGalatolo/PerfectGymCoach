package agdesigns.elevatefitness.data.db.entity

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(
    tableName = "program",
    foreignKeys = [ForeignKey(
        entity = WorkoutPlan::class,
        parentColumns = ["planId"],
        childColumns = ["extPlanId"],
        onDelete = CASCADE
    )],
    indices = [
        Index("extPlanId")
    ]
)
@Parcelize
data class WorkoutProgram(
    @PrimaryKey(autoGenerate = true) val programId: Long = 0L,
    val extPlanId: Long?,
    val orderInWorkoutPlan: Int,
    val name: String
) : Parcelable

// generated programs should be named with this + comma separated muscleResKeys
const val GENERATED_PROGRAM_PREFIX = "[GENERATED PROGRAM] "
fun getGeneratedProgramName(muscles: List<Exercise.Muscle>): String {
    return GENERATED_PROGRAM_PREFIX + muscles.joinToString(", ") { it.muscleResKey }
}

@Suppress("SimplifiableCallChain") // if we simplify, stringResource won't work
@Composable
fun getProgramDisplayName(name: String): String {
    // check if generated program, otherwise return original name
    return if (name.startsWith(GENERATED_PROGRAM_PREFIX)) {
        name.removePrefix(GENERATED_PROGRAM_PREFIX).split(", ").map {
            stringResource(getMuscleResource(it))
        }.joinToString(", ")
    } else {
        name
    }
}

@Parcelize
data class WorkoutProgramRename(
    val programId: Long,
    val name: String
): Parcelable

@Parcelize
data class WorkoutProgramReorder(
    val programId: Long,
    val orderInWorkoutPlan: Int
): Parcelable

@Parcelize
data class RemovePlan(
    val programId: Long,
    val extPlanId: Long? = null
): Parcelable