package agdesigns.elevatefitness.data.exercise

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import agdesigns.elevatefitness.R
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
    val description: String = "Description not available",
    val difficulty: ExerciseDifficulty = ExerciseDifficulty.INTERMEDIATE,
    val probability: Double = 1.0, // weight used when randomly selecting exercises
    val variations: List<String> = emptyList()
) : Parcelable {
    enum class Muscle (val muscleName: String, val image: Int){
        EVERYTHING("See all", R.drawable.full_body), // Used when filtering by muscle to get everything
        ABS("Abs", R.drawable.abs),
        BACK("Back", R.drawable.back),
        BICEPS("Biceps", R.drawable.biceps),
        CALVES("Calves", R.drawable.calves),
        CHEST("Chest", R.drawable.chest),
        GLUTES("Glutes", R.drawable.glutes), // FIXME: different image
        HAMSTRINGS("Hamstrings", R.drawable.hamstrings),
        QUADRICEPS("Quadriceps", R.drawable.quadriceps),
        SHOULDERS("Shoulders", R.drawable.shoulders),
        TRICEPS("Triceps", R.drawable.triceps)
    }

    enum class Equipment(val equipmentName: String){
        EVERYTHING("See all"), // Used when filtering by muscle to get everything
        BARBELL("Barbell"),
        BODY_WEIGHT("Body weight"),
        CABLES("Cables"),
        DUMBBELL("Dumbbell"),
        MACHINE("Machine")
    }

    enum class ExerciseDifficulty(val difficulty: String){
        BEGINNER("Beginner"),
        INTERMEDIATE("Intermediate"),
        ADVANCED("Advanced")
    }
}