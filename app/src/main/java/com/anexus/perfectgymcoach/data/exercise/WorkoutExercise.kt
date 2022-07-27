package com.anexus.perfectgymcoach.data.exercise

data class WorkoutExercise (
    val exerciseId: Int,
    val sets: Int,
    val reps: Int,
    val rest: Int,
    val supersetExercise: Int
    // TODO: old record, etc.
    )