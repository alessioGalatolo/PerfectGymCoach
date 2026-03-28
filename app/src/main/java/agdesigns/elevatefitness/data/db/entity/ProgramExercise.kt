package agdesigns.elevatefitness.data.db.entity

import agdesigns.elevatefitness.R
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import agdesigns.elevatefitness.shared.Equipment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector
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
    val overriddenDurationBased: Boolean,
    val setTypes: List<SetType>? = null
) : Parcelable {
    val variationResource: Int
        get() = getVariation(variationResKey)
}

enum class SetType(val nameResKey: String, val icon: ImageVector) {
    WARMUP("set_types_warmup", Icons.Default.Thermostat),
    NORMAL("set_types_normal", Icons.AutoMirrored.Filled.TrendingFlat),
    DROP_SET("set_types_drop_set", Icons.AutoMirrored.Filled.TrendingDown),
    FAILURE("set_types_failure", Icons.Default.Whatshot);

    val displayRes: Int
        get() = when (this) {
            WARMUP -> R.string.set_types_warmup
            NORMAL -> R.string.set_types_normal
            DROP_SET -> R.string.set_types_drop_set
            FAILURE -> R.string.set_types_failure
        }

    companion object {
        fun fromResKey(resKey: String?): SetType {
            when (resKey) {
                "set_types_warmup" -> return WARMUP
                "set_types_normal" -> return NORMAL
                "set_types_drop_set" -> return DROP_SET
                "set_types_failure" -> return FAILURE
            }
            return NORMAL
        }
    }
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
data class UpdateProgramExerciseSetTypes(
    val programExerciseId: Long,
    val setTypes: List<SetType>,
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
    val overriddenDurationBased: Boolean,
    val setTypes: List<SetType>? = null
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