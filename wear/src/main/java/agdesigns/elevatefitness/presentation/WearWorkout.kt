package agdesigns.elevatefitness.presentation

import android.graphics.Bitmap

data class WearWorkout(
    val exerciseName: String? = null,
    val setsDone: Int? = null,
    val rest: List<Int>? = null,
    val reps: List<Int>? = null,
    val weight: Float? = null,
    val note: String? = null,
    val restTimestamp: Long? = null,
    val exerciseIncrement: Float? = null,
    val nextExerciseName: String? = null,
)
