package com.anexus.perfectgymcoach.data.exercise

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.anexus.perfectgymcoach.data.workout_record.WorkoutRecord
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = WorkoutRecord::class,
            parentColumns = ["workoutId"],
            childColumns = ["extWorkoutId"],
//            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["exerciseId"],
            childColumns = ["extExerciseId"],
//            onDelete = ForeignKey.CASCADE  // FIXME? not sure it should cascade
        )
    ]
)
data class ExerciseRecord(
    @PrimaryKey(autoGenerate = true) val recordId: Long = 0L,
    val extExerciseId: Long,
    val extWorkoutId: Long,
    val date: Calendar, // redundant but simplifies
    val reps: List<Int>,
    val weights: List<Float>,
    val tare: Float = 0f // e.g. barbell weight or bodyweight
) : Parcelable