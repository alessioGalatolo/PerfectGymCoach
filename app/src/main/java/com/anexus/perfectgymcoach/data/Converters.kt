package com.anexus.perfectgymcoach.data

import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.anexus.perfectgymcoach.data.exercise.Exercise
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime
import java.util.*

/**
 * Type converters to allow Room to reference complex data types.
 */
class Converters {
    @TypeConverter fun dateToDatestamp(value: Calendar?): Long = value?.timeInMillis ?: 0L

    @TypeConverter fun datestampToDate(value: Long): Calendar? =
        if (value == 0L) null else Calendar.getInstance().apply { timeInMillis = value }

    @TypeConverter
    fun listIntToString(value: List<Int>): String = if (value.isEmpty()) "" else value.joinToString(",")

    @TypeConverter
    fun stringToListInt(value: String): List<Int> = if (value.isEmpty()) emptyList() else value.split(",").map { it.toInt() }

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
}