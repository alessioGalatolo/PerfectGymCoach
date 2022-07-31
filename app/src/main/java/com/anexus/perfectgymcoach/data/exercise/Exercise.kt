package com.anexus.perfectgymcoach.data.exercise

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val primaryMuscle: Muscle,
//    val secondaryMuscles: List<Muscle>,

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
}