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
            onDelete = CASCADE  // FIXME? not sure it should cascade
        )
    ]
)
data class WorkoutExercise (
    @PrimaryKey(autoGenerate = true) val workoutExerciseId: Long = 0L,
    val extProgramId: Long,
    val extExerciseId: Long,
    val name: String, // FIXME: redundant but simplifies a lot
    val reps: List<Int>,
    val rest: Int,
    val supersetExercise: Int = 0 // TODO: should be foreign key
    // TODO: old record, etc. <- do not put here
    ) : Parcelable