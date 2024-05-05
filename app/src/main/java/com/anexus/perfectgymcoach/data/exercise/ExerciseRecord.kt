package com.anexus.perfectgymcoach.data.exercise

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
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
    ], indices = [
        Index("extWorkoutId"),
        Index("extExerciseId")
    ]
)
data class ExerciseRecord(
    @PrimaryKey(autoGenerate = true) val recordId: Long = 0L,
    val extExerciseId: Long,
    val extWorkoutId: Long,
    val exerciseInWorkout: Int, // in case there are multiple extExerciseId in the workout
    val date: Calendar, // redundant but simplifies
    val reps: List<Int>,
    val weights: List<Float>,
    val variation: String,
    val rest: List<Int>,
    val tare: Float = 0f // e.g. barbell weight or bodyweight
) : Parcelable {
    // FIXME: US weights pretty much made up
    enum class BarbellType(val barbellName: String, val weight: Map<Boolean, Float>){
        EZ_CURL_LIGHT("Light EZ curl bar", mapOf(Pair(false, 5f), Pair(true, 15f))),
        EZ_CURL("EZ curl bar", mapOf(Pair(false, 12f), Pair(true, 30f))),
        YOUNG_OLYMPIC("Young's olympic bar", mapOf(Pair(false, 10f), Pair(true, 25f))),
        WOMEN_OLYMPIC("Women's olympic bar", mapOf(Pair(false, 15f), Pair(true, 35f))),
        MEN_OLYMPIC("Men's olympic bar", mapOf(Pair(false, 20f), Pair(true, 45f))),
        SQUAT("Squat bar", mapOf(Pair(false, 25f), Pair(true, 55f))),
        OTHER("Other", mapOf(Pair(false, 0f), Pair(true, 0f)))
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
    val variation: String,
    val rest: List<Int>,
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
    val variation: String,
    val rest: List<Int>,
    val tare: Float = 0f, // e.g. barbell weight or bodyweight
    val name: String,
    val image: Int,
    val equipment: Exercise.Equipment
) : Parcelable