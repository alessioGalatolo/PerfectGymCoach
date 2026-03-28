package agdesigns.elevatefitness.data.db

import agdesigns.elevatefitness.data.db.entity.Exercise
import agdesigns.elevatefitness.data.db.entity.SetType
import agdesigns.elevatefitness.data.db.entity.WorkoutRecord
import androidx.room.TypeConverter
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Type converters to allow Room to reference complex data types.
 */
class Converters {
    private val zoneId = ZoneId.systemDefault()

    @TypeConverter
    fun zonedDateTimeToTimestamp(value: ZonedDateTime?): Long =
        value?.toInstant()?.toEpochMilli() ?: 0L

    @TypeConverter
    fun timestampToZonedDateTime(value: Long): ZonedDateTime? =
        if (value == 0L) null else Instant.ofEpochMilli(value).atZone(zoneId)

    @TypeConverter
    fun listIntToString(value: List<Int>?): String? = value?.let {
        if (value.isEmpty()) "" else value.joinToString(",")
    }

    @TypeConverter
    fun stringToListInt(value: String?): List<Int>? = value?.let {
        if (value.isEmpty()) emptyList() else value.split(",").map { it.toInt() }
    }

    @TypeConverter
    fun listFloatToString(value: List<Float>): String = if (value.isEmpty()) "" else value.joinToString(",")

    @TypeConverter
    fun stringToListFloat(value: String): List<Float> {
        return if (value.isEmpty()) emptyList() else value.split(",").map { it.toFloat() }
    }

    @TypeConverter
    fun listMuscleToListInt(value: List<Exercise.Muscle>): List<Int> = value.map { it.ordinal }

    @TypeConverter
    fun listIntToListMuscle(value: List<Int>): List<Exercise.Muscle> = value.map {
        Exercise.Muscle.entries[it]
    }

    @TypeConverter
    fun listStringToString(value: List<String>): String {
        return if (value.isEmpty()) "" else value.joinToString("/****/")
    }

    @TypeConverter
    fun stringToListString(value: String): List<String>{
        return if (value.isEmpty())
            emptyList()
        else
            value.split("/****/")
    }

    @TypeConverter
    fun workoutModificationsToString(value: List<WorkoutRecord.WorkoutModification>): String {
        return value.joinToString("/****/") { it.toString() }
    }

    @TypeConverter
    fun stringToWorkoutModifications(value: String): List<WorkoutRecord.WorkoutModification> {
        return if (value.isBlank())
            emptyList()
        else
            value.split("/****/").map {
                WorkoutRecord.WorkoutModification.fromString(it)
            }
    }

    @TypeConverter
    fun listSetTypeToListInt(value: List<SetType>): List<Int> = value.map { it.ordinal }

    @TypeConverter
    fun listIntToListSetType(value: List<Int>): List<SetType> = value.map {
        SetType.entries[it]
    }
}