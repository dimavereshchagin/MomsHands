package com.moms.hands.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Сущность "Кормление".
 *
 * Хранит:
 * - Какая грудь использовалась (LEFT/RIGHT)
 * - Время начала и окончания
 * - Длительность в секундах
 * - ID ребёнка (поддержка нескольких детей)
 *
 * Используется для:
 * - Контроля очередности кормления
 * - Статистики (суточные/недельные нормы)
 * - Определения "следующей груди"
 *
 * Соответствует ТЗ: 2.1, 2.2, 2.4
 */
@Entity(tableName = "feeding")
data class Feeding(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: String,
    val breast: String, // "LEFT" или "RIGHT"
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val durationSeconds: Int
) {
    companion object {
        fun createLeft(childId: String, startTime: LocalDateTime): Feeding =
            Feeding(childId = childId, breast = "LEFT", startTime = startTime, endTime = startTime, durationSeconds = 0)

        fun createRight(childId: String, startTime: LocalDateTime): Feeding =
            Feeding(childId = childId, breast = "RIGHT", startTime = startTime, endTime = startTime, durationSeconds = 0)
    }
}