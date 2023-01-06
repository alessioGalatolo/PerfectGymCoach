package com.anexus.perfectgymcoach.data.workout_exercise

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import com.anexus.perfectgymcoach.data.exercise.Exercise
import com.anexus.perfectgymcoach.data.exercise.ProgramExercise
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import com.anexus.perfectgymcoach.data.workout_record.WorkoutRecord
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
    ]
)
data class WorkoutExercise (
    @PrimaryKey(autoGenerate = true) val workoutExerciseId: Long = 0L,
    val extWorkoutId: Long,
    val extProgramExerciseId: Long? = null,
    val extExerciseId: Long,
    val name: String,
    val image: Int,
    val equipment: Exercise.Equipment,
    val orderInProgram: Int,
    val reps: List<Int>,
    val rest: Int,
    val note: String,
    val variation: String,
    val supersetExercise: Long? = null
) : Parcelable

@Parcelize
data class WorkoutExerciseReorder(
    val workoutExerciseId: Long,
    val orderInProgram: Int
): Parcelable