package agdesigns.elevatefitness.data.db.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import com.agdesignes.shared.Equipment
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = WorkoutRecord::class,
            parentColumns = ["workoutId"],
            childColumns = ["extWorkoutId"],
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["exerciseId"],
            childColumns = ["extExerciseId"],
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = ProgramExercise::class,
            parentColumns = ["programExerciseId"],
            childColumns = ["supersetExercise"],
            onDelete = ForeignKey.SET_DEFAULT
        )
//        ForeignKey(  // Commented as it gives problems when exercise is in no programs
//            entity = ProgramExercise::class,
//            parentColumns = ["programExerciseId"],
//            childColumns = ["extProgramExerciseId"],
//            onDelete = ForeignKey.SET_DEFAULT
//        )
    ],
    indices = [
        Index("extWorkoutId"),
        Index("extExerciseId"),
        Index("supersetExercise")
    ]
)
data class WorkoutExercise (
    @PrimaryKey(autoGenerate = true) val workoutExerciseId: Long = 0L,
    val extWorkoutId: Long,
    val extProgramExerciseId: Long? = null,
    val extExerciseId: Long,
    @Deprecated("Unless user-defined exercise, use nameResKey instead")
    val name: String,
    @ColumnInfo(defaultValue = "")
    val nameResKey: String, // key of the string resource
    @Deprecated("Use imageResKey instead")
    val image: Int,
    @ColumnInfo(defaultValue = "")
    val imageResKey: String,
    @Deprecated("Use descriptionResKey instead")
    val description: String,
    @ColumnInfo(defaultValue = "")
    val descriptionResKey: String,
    val equipment: Equipment,
    val orderInProgram: Int,
    val reps: List<Int>,
    val rest: List<Int>,
    val note: String,
    @Deprecated("Use variationResKey instead")
    val variation: String,
    @ColumnInfo(defaultValue = "")
    val variationResKey: String,
    val supersetExercise: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val userDefined: Boolean = false
) : Parcelable {
    val nameResource: Int
        get() = getNameDescriptionResource(nameResKey)
    val descriptionResource: Int
        get() = getNameDescriptionResource(descriptionResKey)
    val variationResource: Int
        get() = getVariation(variationResKey)
    val imageResource: Int
        get() = getImageResource(imageResKey)
}

@Parcelize
data class WorkoutExerciseReorder(
    val workoutExerciseId: Long,
    val orderInProgram: Int
): Parcelable