package agdesigns.elevatefitness.data.workout_record

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import agdesigns.elevatefitness.data.workout_program.WorkoutProgram
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@Entity(
    foreignKeys = [ForeignKey(
        entity = WorkoutProgram::class,
        parentColumns = ["programId"],
        childColumns = ["extProgramId"],
//        onDelete = CASCADE
    )],
    indices = [Index("extProgramId")]
)
@Parcelize
data class WorkoutRecord(
    @PrimaryKey(autoGenerate = true) val workoutId: Long = 0L,
    val extProgramId: Long,
    val startDate: ZonedDateTime? = null,
    val intensity: WorkoutIntensity = WorkoutIntensity.NORMAL_INTENSITY,
    val durationSeconds: Long = 0L, // seconds
    val volume: Double = 0.0,
    val activeTimeSeconds: Long = 0L,
    val calories: Float = 0f // MET value * weight_kg / 60 * n_minutes // MET value 3-6 based on intensity
) : Parcelable {
    enum class WorkoutIntensity(val description: String, val metValue: Float) {
        HIGH_INTENSITY("High intensity (...)", 6f),  // TODO: add description
        NORMAL_INTENSITY("Normal intensity (...)", 4.5f),
        LOW_INTENSITY("Low intensity (...)", 3f)
    }
}

@Parcelize
data class WorkoutRecordStart(
    val workoutId: Long,
    val startDate: ZonedDateTime
): Parcelable

@Parcelize
data class WorkoutRecordFinish(
    val workoutId: Long,
    val intensity: WorkoutRecord.WorkoutIntensity,
    val durationSeconds: Long,
    val volume: Double,
    val activeTimeSeconds: Long,
    val calories: Float
): Parcelable

@Parcelize
data class WorkoutRecordAndName(
    @PrimaryKey(autoGenerate = true) val workoutId: Long = 0L,
    val extProgramId: Long,
    val startDate: ZonedDateTime?,
    val intensity: WorkoutRecord.WorkoutIntensity,
    val durationSeconds: Long = 0L, // seconds
    val volume: Float = 0f,
    val activeTimeSeconds: Long = 0L,
    val calories: Float = 0f,
    val name: String
) : Parcelable