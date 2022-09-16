package com.anexus.perfectgymcoach.data.exercise

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
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
            onDelete = CASCADE  // FIXME? not sure it should cascade, probably won't be implemented
        ),
        ForeignKey(
            entity = WorkoutExercise::class,
            parentColumns = ["workoutExerciseId"],
            childColumns = ["supersetExercise"],
            onDelete = ForeignKey.SET_DEFAULT
        )
    ]
)
data class WorkoutExercise (
    @PrimaryKey(autoGenerate = true) val workoutExerciseId: Long = 0L,
    val extProgramId: Long,
    val extExerciseId: Long,
    val orderInProgram: Int,
    val reps: List<Int>,
    val rest: Int,
    val note: String,
    val supersetExercise: Long = 0L
) : Parcelable

@Parcelize
data class WorkoutExerciseReorder (
    val workoutExerciseId: Long,
    val orderInProgram: Int,
) : Parcelable

@Parcelize
data class UpdateExerciseSuperset (
    val workoutExerciseId: Long,
    val supersetExercise: Long,
) : Parcelable

@Parcelize
data class WorkoutExerciseAndInfo (
    @PrimaryKey(autoGenerate = true) val workoutExerciseId: Long = 0L,
    val extProgramId: Long,
    val extExerciseId: Long,
    val orderInProgram: Int,
    val name: String,
    val reps: List<Int>,
    val rest: Int,
    val note: String,
    val supersetExercise: Long = 0L,
    val image: Int,
    val equipment: Exercise.Equipment
) : Parcelable