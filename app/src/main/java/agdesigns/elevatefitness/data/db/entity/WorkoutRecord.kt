package agdesigns.elevatefitness.data.db.entity

import agdesigns.elevatefitness.R
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
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
    @Deprecated("Use intensityPercent instead")
    val intensity: WorkoutIntensity = WorkoutIntensity.NORMAL_INTENSITY,
    val intensityPercent: Float = 50f,
    val durationSeconds: Long = 0L, // seconds
    val volume: Double = 0.0,
    val activeTimeSeconds: Long = 0L,
    val calories: Float = 0f // MET value * weight_kg / 60 * n_minutes // MET value 3-6 based on intensity
) : Parcelable {
    enum class WorkoutIntensity(val descriptionResKey: String, val metValue: Float) {
        HIGH_INTENSITY("intensities_high", 6f),  // TODO: add description
        NORMAL_INTENSITY("intensities_medium", 4.5f),
        LOW_INTENSITY("intensities_low", 3f);

        val descriptionResource: Int
            get() = when (this) {
                HIGH_INTENSITY -> R.string.intensities_high
                NORMAL_INTENSITY -> R.string.intensities_medium
                LOW_INTENSITY -> R.string.intensities_low
            }
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
    @Deprecated("Use intensityPercent instead")
    val intensity: WorkoutRecord.WorkoutIntensity,
    @ColumnInfo(defaultValue = "50.0")
    val intensityPercent: Float,
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
    @Deprecated("Use intensityPercent instead")
    val intensity: WorkoutRecord.WorkoutIntensity,
    @ColumnInfo(defaultValue = "50.0")
    val intensityPercent: Float,
    val durationSeconds: Long = 0L, // seconds
    val volume: Float = 0f,
    val activeTimeSeconds: Long = 0L,
    val calories: Float = 0f,
    val name: String
) : Parcelable