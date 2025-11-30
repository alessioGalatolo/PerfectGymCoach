package agdesigns.elevatefitness.data.db.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import agdesignes.elevatefitness.shared.Equipment
import agdesignes.elevatefitness.shared.grpc.Workout
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

    fun toProto(): Workout.Exercise {
        return Workout.Exercise.newBuilder()
            .setExerciseId(this.workoutExerciseId)
            .setName(this.name)
            .setEquipment(this.equipment.equipmentResKey)
            .setOrderInProgram(this.orderInProgram)
            .addAllReps(this.reps)
            .addAllRest(this.rest)
            .setNote(this.note)
            .setVariation(this.variation)
            .setSupersetExercise(this.supersetExercise ?: 0L)
            .build()
    }
}

@Parcelize
data class WorkoutExerciseReorder(
    val workoutExerciseId: Long,
    val orderInProgram: Int
): Parcelable

@Parcelize
data class WorkoutExerciseUpdateSets(
    val workoutExerciseId: Long,
    val reps: List<Int>,
    val rest: List<Int>
): Parcelable