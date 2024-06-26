package agdesigns.elevatefitness.data.workout_program

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlan
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