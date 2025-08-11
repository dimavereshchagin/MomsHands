package com.moms.hands.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Сущность "Сон".
 *
 * Хранит:
 * - Время начала и окончания сна
 * - Длительность
 * - ID ребёнка
 *
 * Используется для:
 * - Таймера сна
 * - Статистики сна
 * - Сравнительной аналитики (ТЗ 1.1)
 * - Умных напоминаний ("Пора укладывать спать")
 *
 * Соответствует ТЗ: 2.2, 2.4
 */
@Entity(tableName = "sleep")
data class Sleep(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val durationSeconds: Int
) {
    companion object {
        fun startNew(childId: String, startTime: LocalDateTime): Sleep =
            Sleep(childId = childId, startTime = startTime, endTime = startTime, durationSeconds = 0)
    }
}