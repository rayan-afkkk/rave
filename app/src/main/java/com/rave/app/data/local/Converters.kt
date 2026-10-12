package com.rave.app.data.local

import androidx.room.TypeConverter

/** Room type converters for non-primitive columns. */
class Converters {

    /** Stores a list of "HH:mm" strings as a single comma-separated column. */
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(SEPARATOR)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR)

    private companion object {
        const val SEPARATOR = ","
    }
}
