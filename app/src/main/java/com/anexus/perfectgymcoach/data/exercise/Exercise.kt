package com.anexus.perfectgymcoach.data.exercise

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey
import com.anexus.perfectgymcoach.R
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity
data class Exercise(
    @PrimaryKey(autoGenerate = true) val exerciseId: Long = 0L,
    val name: String,
    val equipment: Equipment,
    val primaryMuscle: Muscle,
    val secondaryMuscles: List<Muscle> = emptyList(),
    val image: Int = R.drawable.finish_workout,
    val difficulty: ExerciseDifficulty = ExerciseDifficulty.MEDIUM,  // TODO: not actually used in the app yet
    val variations: List<String> = emptyList()

) : Parcelable {
    enum class Muscle (val muscleName: String){
        EVERYTHING("See all"), // Used when filtering by muscle to get everything
        ABS("Abs"),
        BACK("Back"),
        BICEPS("Biceps"),
        CALVES("Calves"),
        CHEST("Chest"),
        LEGS("Legs"),
        SHOULDERS("Shoulders"),
        TRICEPS("Triceps")
    }

    // TODO: assumes international system (kg), add support for americans
    enum class Equipment(val equipmentName: String, val increment: Float){
        EVERYTHING("See all", 1f), // Used when filtering by muscle to get everything
        BARBELL("Barbell", 2.5f),
        BODY_WEIGHT("Body weight", 2.5f),
        CABLES("Cables", 2.5f),
        DUMBBELL("Dumbbell", 2f),
        MACHINE("Machine", 5f)
    }

    enum class ExerciseDifficulty(val difficulty: String){
        BEGINNER("Beginner"),
        MEDIUM("Medium"),
        ADVANCED("Advanced")
    }
}