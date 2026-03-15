package agdesigns.elevatefitness.data.db.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import agdesigns.elevatefitness.shared.Equipment
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = WorkoutProgram::class,
            parentColumns = ["programId"],
            childColumns = ["extProgramId"],
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
    ],
    indices = [
        Index("extProgramId"),
        Index("extExerciseId"),
        Index("supersetExercise")
    ]
)
data class ProgramExercise (
    @PrimaryKey(autoGenerate = true) val programExerciseId: Long = 0L,
    val extProgramId: Long,
    val extExerciseId: Long,
    val orderInProgram: Int,
    val reps: List<Int>,
    val rest: List<Int>,
    val note: String = "",
    val variation: String = "",
    @ColumnInfo(defaultValue = "")
    val variationResKey: String = "",
    val supersetExercise: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val overriddenDurationBased: Boolean
) : Parcelable {
    val variationResource: Int
        get() = getVariation(variationResKey)
}

@Parcelize
data class ProgramExerciseReorder (
    val programExerciseId: Long,
    val orderInProgram: Int,
) : Parcelable

@Parcelize
data class UpdateExerciseSuperset(
    val programExerciseId: Long,
    val supersetExercise: Long?,
) : Parcelable

@Parcelize
data class ProgramExerciseAndInfo (
    @PrimaryKey(autoGenerate = true) val programExerciseId: Long = 0L,
    val extProgramId: Long,
    val extExerciseId: Long,
    val orderInProgram: Int,
    val name: String,
    val nameResKey: String, // key of the string resource
    val description: String,
    val descriptionResKey: String,
    val reps: List<Int>,
    val rest: List<Int>,
    val note: String,
    val variation: String,
    val variationResKey: String,
    val supersetExercise: Long? = null,
    val image: Int,
    val imageResKey: String,
    val equipment: Equipment,
    val userDefined: Boolean,
    val overriddenDurationBased: Boolean
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

data class ProgramExerciseWithExercise(
    @Embedded val programExercise: ProgramExercise,
    @Embedded val exercise: Exercise? // nullable
)