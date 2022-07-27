package com.anexus.perfectgymcoach.data.workout_program

import androidx.room.PrimaryKey

data class WorkoutProgram(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val exercisesId: List<Int>
)