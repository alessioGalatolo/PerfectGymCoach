package agdesigns.elevatefitness.data

import agdesigns.elevatefitness.data.db.entity.Exercise
import agdesigns.elevatefitness.shared.Equipment
import agdesigns.elevatefitness.shared.SetType
import kotlinx.serialization.Serializable

const val ELEVATE_FITNESS_SHARE_MIME_TYPE = "application/vnd.agdesigns.elevatefitness"
const val ELEVATE_FITNESS_SHARE_EXTENSION = ".elfit"

// a generic element that can be shared from this app
@Serializable
data class SharableElement(
    val type: Type,
    val element: String
) {
    enum class Type {
        WORKOUT_PLAN
    }
}


@Serializable
data class SharedWorkoutPlanModel(
    val version: Int = 1,
    val planName: String,
    val programs: List<SharedProgramModel>,
    val missingExercises: List<SharedBaseExercise>  // e.g., user-defined exercises
)

@Serializable
data class SharedProgramModel(
    val name: String,
    val orderInWorkoutPlan: Int,
    val exercises: List<SharedExerciseModel>
)

// an exercise that is part of a program
@Serializable
data class SharedExerciseModel(
    val nameResKey: String = "",
    val localizedName: String = "",
    val reps: List<Int>,
    val rest: List<Int>,
    val note: String = "",
    val variationResKey: String = "", // TODO: change when we allow custom variations
    val orderInProgram: Int,
    val overriddenDurationBased: Boolean = false,
    val setTypes: List<SetType>?,
    // cannot use Id, use nameResKey and name as a fallback
    val supersetExerciseNameResKey: String?,
    val supersetExerciseName: String?
)

// a base exercise
@Serializable
data class SharedBaseExercise(
    val name: String,
    val equipment: Equipment,
    val primaryMuscle: Exercise.Muscle,
    val secondaryMuscles: List<Exercise.Muscle> = emptyList(),
    val difficulty: Exercise.ExerciseDifficulty,
    val isDurationBased: Boolean,
) {
    fun toExercise() = Exercise(
        name = name,
        nameResKey = "",
        equipment = equipment,
        primaryMuscle = primaryMuscle,
        secondaryMuscles = secondaryMuscles,
        difficulty = difficulty,
        isDurationBased = isDurationBased,
        userDefined = true
    )
}

fun Exercise.toSharedBaseExercise() = SharedBaseExercise(
    name = name,
    equipment = equipment,
    primaryMuscle = primaryMuscle,
    secondaryMuscles = secondaryMuscles,
    difficulty = difficulty,
    isDurationBased = isDurationBased,
)