package com.moms.hands.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Сущность "Срыгивание".
 *
 * Хранит:
 * - Время срыгивания
 * - Объём (немного/много)
 * - ID ребёнка
 * - Опциональную привязку к кормлению
 *
 * Используется для:
 * - Учёта срыгиваний
 * - Статистики по связи с грудью
 * - Персонализированных инсайтов ("Срыгивания чаще после утренних кормлений")
 *
 * Соответствует ТЗ: 2.3, 1.3
 */
@Entity(tableName = "spit_up")
data class SpitUp(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: String,
    val timestamp: LocalDateTime,
    val volume: SpitVolume,
    val associatedFeedingId: Long? = null
)

/**
 * Перечисление: объём срыгивания.
 *
 * Используется в UI и аналитике.
 */
enum class SpitVolume {
    LITTLE, MUCH
}