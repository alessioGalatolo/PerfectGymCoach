package agdesigns.elevatefitness.data.exercise

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import agdesigns.elevatefitness.data.workout_program.WorkoutProgram
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
    val supersetExercise: Long? = null
) : Parcelable

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
    val description: String,
    val reps: List<Int>,
    val rest: List<Int>,
    val note: String,
    val variation: String,
    val supersetExercise: Long? = null,
    val image: Int,
    val equipment: Exercise.Equipment
) : Parcelable