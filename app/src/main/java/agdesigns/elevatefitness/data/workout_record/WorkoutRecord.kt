package agdesigns.elevatefitness.data.workout_record

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import agdesigns.elevatefitness.data.workout_program.WorkoutProgram
import kotlinx.parcelize.Parcelize
import java.util.*

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
    val startDate: Calendar? = null,
    val intensity: WorkoutIntensity = WorkoutIntensity.NORMAL_INTENSITY,
    val duration: Long = 0L, // seconds
    val volume: Double = 0.0,
    val activeTime: Long = 0L,
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
    val startDate: Calendar
): Parcelable

@Parcelize
data class WorkoutRecordFinish(
    val workoutId: Long,
    val intensity: WorkoutRecord.WorkoutIntensity,
    val duration: Long,
    val volume: Double,
    val activeTime: Long,
    val calories: Float
): Parcelable

@Parcelize
data class WorkoutRecordAndName(
    @PrimaryKey(autoGenerate = true) val workoutId: Long = 0L,
    val extProgramId: Long,
    val startDate: Calendar?,
    val intensity: WorkoutRecord.WorkoutIntensity,
    val duration: Long = 0L, // seconds
    val volume: Float = 0f,
    val activeTime: Long = 0L,
    val calories: Float = 0f,
    val name: String
) : Parcelable