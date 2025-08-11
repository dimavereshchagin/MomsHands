package com.moms.hands.util

import androidx.room.TypeConverter
import java.time.LocalDateTime

class LocalDateTimeConverter {
    @TypeConverter
    fun fromLocalDateTime(date: LocalDateTime?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it) }
    }
}