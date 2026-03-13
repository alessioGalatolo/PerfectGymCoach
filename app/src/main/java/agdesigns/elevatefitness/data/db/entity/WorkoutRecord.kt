package agdesigns.elevatefitness.data.db.entity

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.shared.grpc.Workout
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
    val calories: Float = 0f, // MET value * weight_kg / 60 * n_minutes // MET value 3-6 based on intensity
    // used to suggest workout modifications, e.g., "last time you added this exercise"
    val workoutModifications: List<WorkoutModification> = emptyList()
) : Parcelable {
    enum class WorkoutIntensity(val descriptionResKey: String, val metValue: Float) {
        HIGH_INTENSITY("intensities_high", 6f),
        NORMAL_INTENSITY("intensities_medium", 4.5f),
        LOW_INTENSITY("intensities_low", 3f);

        val descriptionResource: Int
            get() = when (this) {
                HIGH_INTENSITY -> R.string.intensities_high
                NORMAL_INTENSITY -> R.string.intensities_medium
                LOW_INTENSITY -> R.string.intensities_low
            }
    }

    @Parcelize
    data class WorkoutModification(
        // the id of the ex from which the modification happened,
        // may be null if exercise was added during the workout
        val sourceProgramExerciseId: Long?,
        // is not reliable, should not be used as a unique id
        val sourceExerciseId: Long?,
        // fallback, most reliable but introduces complexity
        val sourceWorkoutExerciseId: Long?,
        // e.g., of the added exercise
        val targetWorkoutExerciseId: Long?,
        val targetExerciseId: Long?,
        val modificationType: ModificationType
    ) : Parcelable {
        override fun toString(): String {
            return "$modificationType/<mod_separator>/$sourceProgramExerciseId/<mod_separator>/$sourceExerciseId/<mod_separator>/$sourceWorkoutExerciseId/<mod_separator>/$targetWorkoutExerciseId/<mod_separator>/$targetExerciseId"
        }

        companion object {
            fun fromString(string: String): WorkoutModification {
                val parts = string.split("/<mod_separator>/")
                return WorkoutModification(
                    sourceProgramExerciseId = parts[1].toLongOrNull(),
                    sourceExerciseId = parts[2].toLongOrNull(),
                    sourceWorkoutExerciseId = parts[3].toLongOrNull(),
                    targetWorkoutExerciseId = parts[4].toLongOrNull(),
                    targetExerciseId = parts[5].toLongOrNull(),
                    modificationType = ModificationType.valueOf(parts[0])
                )
            }
        }
    }

    enum class ModificationType {
        EXERCISE_ADDED,
        EXERCISE_SKIPPED,
        EXERCISE_REPLACED;

        fun toProto(): Workout.ProtoModificationType {
            return when (this) {
                EXERCISE_ADDED -> Workout.ProtoModificationType.EXERCISE_ADDED
                EXERCISE_SKIPPED -> Workout.ProtoModificationType.EXERCISE_SKIPPED
                EXERCISE_REPLACED -> Workout.ProtoModificationType.EXERCISE_REPLACED
            }
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
    val calories: Float,
    val workoutModifications: List<WorkoutRecord.WorkoutModification>
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
    val workoutModifications: List<WorkoutRecord.WorkoutModification> = emptyList(),
    val name: String
) : Parcelable