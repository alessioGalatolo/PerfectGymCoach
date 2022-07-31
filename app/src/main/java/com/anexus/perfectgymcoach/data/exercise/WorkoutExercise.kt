package com.anexus.perfectgymcoach.data.exercise

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = WorkoutProgram::class,
            parentColumns = ["id"],
            childColumns = ["programId"],
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = CASCADE  // FIXME? not sure it should cascade
        )
    ]
)
data class WorkoutExercise (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val programId: Int,
    val exerciseId: Int,
    val sets: Int,
    val reps: Int,
    val rest: Int,
    val supersetExercise: Int = 0 // TODO: should be foreign key
    // TODO: old record, etc. <- do not put here
    )