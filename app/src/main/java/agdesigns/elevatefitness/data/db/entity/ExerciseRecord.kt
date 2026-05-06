package agdesigns.elevatefitness.data.db.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import agdesigns.elevatefitness.shared.Equipment
import agdesigns.elevatefitness.shared.SetType
import agdesigns.elevatefitness.shared.grpc.Workout
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

@Parcelize
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = WorkoutRecord::class,
            parentColumns = ["workoutId"],
            childColumns = ["extWorkoutId"],
//            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["exerciseId"],
            childColumns = ["extExerciseId"]
        ),
        // Commented as it requires creating a new table in migration
//        ForeignKey(
//            entity = WorkoutExercise::class,
//            parentColumns = ["workoutExerciseId"],
//            childColumns = ["extWorkoutExerciseId"]
//        )
    ], indices = [
        Index("extWorkoutId"),
        Index("extExerciseId"),
        Index("extWorkoutExerciseId")
    ]
)
data class ExerciseRecord(
    @PrimaryKey(autoGenerate = true) val recordId: Long = 0L,
    val extExerciseId: Long,
    val extWorkoutId: Long,
    @ColumnInfo(defaultValue = "0")
    val extWorkoutExerciseId: Long,
    val exerciseInWorkout: Int,
    val date: ZonedDateTime, // redundant but simplifies
    val reps: List<Int>,
    val weights: List<Float>,
    val variation: String,
    @ColumnInfo(defaultValue = "")
    val variationResKey: String,
    val rest: List<Int>,
    val tare: Float = 0f, // e.g. barbell weight or bodyweight
    @ColumnInfo(defaultValue = "")
    val barbellTypeResKey: String = "",
    @ColumnInfo(defaultValue = "0")
    val overriddenDurationBased: Boolean,
    val setTypes: List<SetType>? = null,
    val trackingResults: List<TrackingResult?>,
) : Parcelable {
    val variationResource: Int
        get() = getVariation(variationResKey)
}


@Parcelize
data class ExerciseRecordAndEquipment(
    @PrimaryKey(autoGenerate = true) val recordId: Long = 0L,
    val extExerciseId: Long,
    val extWorkoutId: Long,
    val extWorkoutExerciseId: Long,
    val exerciseInWorkout: Int,
    val date: ZonedDateTime, // redundant but simplifies
    val reps: List<Int>,
    val weights: List<Float>,
    val tare: Float = 0f,
    val barbellTypeResKey: String = "",
    val variation: String,
    val variationResKey: String,
    val rest: List<Int>,
    val equipment: Equipment,
    val overriddenDurationBased: Boolean,
    val setTypes: List<SetType>?,
    val trackingResults: List<TrackingResult?>,
) : Parcelable {
    val variationResource: Int
        get() = getVariation(variationResKey)
}

@Parcelize
data class UpdateExerciseRecordSetTypes(
    val recordId: Long,
    val setTypes: List<SetType>,
) : Parcelable

@Parcelize
data class ExerciseRecordAndInfo(
    val recordId: Long = 0L,
    val extExerciseId: Long,
    val extWorkoutId: Long,
    val extWorkoutExerciseId: Long,
    val exerciseInWorkout: Int,
    val date: ZonedDateTime, // redundant but simplifies
    val reps: List<Int>,
    val weights: List<Float>,
    val variation: String,
    val variationResKey: String,
    val rest: List<Int>,
    val tare: Float = 0f, // e.g. barbell weight or bodyweight
    val barbellTypeResKey: String = "",
    val name: String,
    val nameResKey: String, // key of the string resource
    val image: Int,
    val imageResKey: String,
    val userDefined: Boolean = false,
    val equipment: Equipment,
    val overriddenDurationBased: Boolean,
    val setTypes: List<SetType>?
) : Parcelable {
    val nameResource: Int
        get() = getNameDescriptionResource(nameResKey)
    val imageResource: Int
        get() = getImageResource(imageResKey)
    val variationResource: Int
        get() = getVariation(variationResKey)

}

@Serializable
@Parcelize
data class RepMetric(
    val index: Int,
    val concentricMs: Long,
    val eccentricMs: Long,
    val rangeOfMotionM: Float,
    val peakVelocity: Float
): Parcelable

fun Workout.ProtoRepMetrics.toRepMetric() = RepMetric(
    index = index,
    concentricMs = concentricMs,
    eccentricMs = eccentricMs,
    rangeOfMotionM = rangeOfMotionM,
    peakVelocity = peakVelocity
)

@Serializable
@Parcelize
data class TrackingResult(
    val detectedReps: Int,
    val avgConcentricMs: Long,
    val avgEccentricMs: Long,
    val avgRangeOfMotionM: Float,
    val repMetrics: List<RepMetric>
): Parcelable

fun Workout.ProtoSetTrackingResult.toTrackingResult() = if (dataAvailable)
    TrackingResult(
        detectedReps = detectedReps,
        avgConcentricMs = avgConcentricMs,
        avgEccentricMs = avgEccentricMs,
        avgRangeOfMotionM = avgRangeOfMotionM,
        repMetrics = repsList.map { it.toRepMetric() }
    ) else null