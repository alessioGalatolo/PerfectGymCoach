package com.anexus.perfectgymcoach.data.exercise

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val primaryMuscle: Muscle,
//    val secondaryMuscles: List<Muscle>,

){
    enum class Muscle (val muscleName: String){
        EVERYTHING("See all"), // Used when filtering by muscle to get everything
        BICEPS("Biceps"),
        TRICEPS("Triceps"),
        SHOULDERS("Shoulders"),
        LEGS("Legs"),
        CALVES("Calves"),
        ABS("Abs"),
        CHEST("Chest"),
        BACK("Back")
    }
}