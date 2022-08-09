package com.anexus.perfectgymcoach.data.exercise

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity
data class Exercise(
    @PrimaryKey(autoGenerate = true) val exerciseId: Long = 0L,
    val name: String,
    val equipment: Equipment,
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

    enum class Equipment (val equipmentName: String){
        EVERYTHING("See all"), // Used when filtering by muscle to get everything
        BARBELL("Barbell"),
        BODY_WEIGHT("Body weight"),
        DUMBBELL("Dumbbell"),
        CABLES("Cables"),
        MACHINE("Machine")
    }

    companion object{
        // TODO: assumes international system (kg), add support for americans
        val equipment2increment = mapOf(
            Pair(Equipment.BARBELL, 2.5),
            Pair(Equipment.BODY_WEIGHT, 2.5),
            Pair(Equipment.DUMBBELL, 2),
            Pair(Equipment.CABLES, 2.5),
            Pair(Equipment.MACHINE, 5),
        )
    }
}