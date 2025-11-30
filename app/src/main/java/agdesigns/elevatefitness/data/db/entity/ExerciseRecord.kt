package agdesigns.elevatefitness.data.db.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import agdesignes.elevatefitness.shared.Equipment
import kotlinx.parcelize.Parcelize
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
        )
    ], indices = [
        Index("extWorkoutId"),
        Index("extExerciseId")
    ]
)
data class ExerciseRecord(
    @PrimaryKey(autoGenerate = true) val recordId: Long = 0L,
    val extExerciseId: Long,
    val extWorkoutId: Long,
    val exerciseInWorkout: Int, // in case there are multiple extExerciseId in the workout
    val date: ZonedDateTime, // redundant but simplifies
    val reps: List<Int>,
    val weights: List<Float>,
    @Deprecated("Use variationResKey instead")
    val variation: String,
    @ColumnInfo(defaultValue = "")
    val variationResKey: String,
    val rest: List<Int>,
    val tare: Float = 0f // e.g. barbell weight or bodyweight
) : Parcelable {
    val variationResource: Int
        get() = getVariation(variationResKey)
}


@Parcelize
data class ExerciseRecordAndEquipment(
    @PrimaryKey(autoGenerate = true) val recordId: Long = 0L,
    val extExerciseId: Long,
    val extWorkoutId: Long,
    val exerciseInWorkout: Int, // in case there are multiple extExerciseId in the workout
    val date: ZonedDateTime, // redundant but simplifies
    val reps: List<Int>,
    val weights: List<Float>,
    val tare: Float = 0f,
    @Deprecated("Use variationResKey instead")
    val variation: String,
    val variationResKey: String,
    val rest: List<Int>,
    val equipment: Equipment
) : Parcelable {
    val variationResource: Int
        get() = getVariation(variationResKey)
}


@Parcelize
data class ExerciseRecordAndInfo(
    val recordId: Long = 0L,
    val extExerciseId: Long,
    val extWorkoutId: Long,
    val exerciseInWorkout: Int, // in case there are multiple extExerciseId in the workout
    val date: ZonedDateTime, // redundant but simplifies
    val reps: List<Int>,
    val weights: List<Float>,
    @Deprecated("Use variationResKey instead")
    val variation: String,
    val variationResKey: String,
    val rest: List<Int>,
    val tare: Float = 0f, // e.g. barbell weight or bodyweight
    @Deprecated("Unless user-defined exercise, use nameResKey instead")
    val name: String,
    val nameResKey: String, // key of the string resource
    @Deprecated("Use imageResKey instead")
    val image: Int,
    val imageResKey: String,
    val userDefined: Boolean = false,
    val equipment: Equipment
) : Parcelable {
    val nameResource: Int
        get() = getNameDescriptionResource(nameResKey)
    val imageResource: Int
        get() = getImageResource(imageResKey)
    val variationResource: Int
        get() = getVariation(variationResKey)

}