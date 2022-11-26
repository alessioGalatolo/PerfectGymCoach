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
            childColumns = ["extExerciseId"]
        )
    ]
)
data class ExerciseRecord( // FIXME: blends in different variation of the exercise in the records
    @PrimaryKey(autoGenerate = true) val recordId: Long = 0L,
    val extExerciseId: Long,
    val extWorkoutId: Long,
    val exerciseInWorkout: Int, // in case there are multiple extExerciseId in the workout
    val date: Calendar, // redundant but simplifies
    val reps: List<Int>,
    val weights: List<Float>,
    val tare: Float = 0f // e.g. barbell weight or bodyweight
) : Parcelable {
    enum class BarbellType(val barbellName: String, val weight: Float){
        EZ_CURL("EZ curl bar (5kg)", 5f),
        YOUNG_OLYMPIC("Young's olympic bar (10kg)", 10f),
        WOMEN_OLYMPIC("Women's olympic bar (15kg)", 15f),
        MEN_OLYMPIC("Men's olympic bar (20kg)", 20f),
        SQUAT("Squat bar (25kg)", 25f)
    }
}


@Parcelize
data class ExerciseRecordAndEquipment(
    @PrimaryKey(autoGenerate = true) val recordId: Long = 0L,
    val extExerciseId: Long,
    val extWorkoutId: Long,
    val exerciseInWorkout: Int, // in case there are multiple extExerciseId in the workout
    val date: Calendar, // redundant but simplifies
    val reps: List<Int>,
    val weights: List<Float>,
    val tare: Float = 0f,
    val equipment: Exercise.Equipment
) : Parcelable


@Parcelize
data class ExerciseRecordAndInfo(
    val recordId: Long = 0L,
    val extExerciseId: Long,
    val extWorkoutId: Long,
    val exerciseInWorkout: Int, // in case there are multiple extExerciseId in the workout
    val date: Calendar, // redundant but simplifies
    val reps: List<Int>,
    val weights: List<Float>,
    val tare: Float = 0f, // e.g. barbell weight or bodyweight
    val name: String,
    val variation: String,
    val rest: Int,
    val image: Int
) : Parcelable