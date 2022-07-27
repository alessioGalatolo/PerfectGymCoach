package com.anexus.perfectgymcoach.data.exercise

import androidx.room.PrimaryKey

data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val name: String,
    val primaryMuscle: Muscle,
    val secondaryMuscles: List<Muscle>,

){
    enum class Muscle (val muscleName: String){
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