package com.anexus.perfectgymcoach.data

import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

/**
 * Type converters to allow Room to reference complex data types.
 */
class Converters {
    @TypeConverter fun calendarToDatestamp(calendar: Calendar): Long = calendar.timeInMillis

    @TypeConverter fun datestampToCalendar(value: Long): Calendar =
        Calendar.getInstance().apply { timeInMillis = value }

    @TypeConverter
    fun listIntToString(value: List<Int>): String {
        val gson = Gson()
        val type = object : TypeToken<List<Int>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun stringToListInt(value: String): List<Int> {
        val gson = Gson()
        val type = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun listFloatToString(value: List<Float>): String {
        val gson = Gson()
        val type = object : TypeToken<List<Float>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun stringToListFloat(value: String): List<Float> {
        val gson = Gson()
        val type = object : TypeToken<List<Float>>() {}.type
        return gson.fromJson(value, type)
    }

}