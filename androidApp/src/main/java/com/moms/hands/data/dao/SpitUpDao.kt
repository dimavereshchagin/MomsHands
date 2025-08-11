package com.moms.hands.data.dao

import androidx.room.*
import com.moms.hands.data.entity.SpitUp
import java.time.LocalDate

/**
 * Data Access Object для сущности "Срыгивание".
 *
 * Отвечает за:
 * - Запись новых срыгиваний
 * - Получение срыгиваний по дате и ребёнку
 * - Получение всех срыгиваний, связанных с определённым кормлением
 * - Статистику по объёму и времени
 *
 * Используется в:
 * - MainActivity (кнопка "Срыгнул")
 * - AnalyticsUseCase (связь с грудью, инсайты)
 * - StatsFragment (графики частоты срыгиваний)
 *
 * Соответствует ТЗ: 2.3, 1.3
 */
@Dao
interface SpitUpDao {

    /**
     * Добавляет новое срыгивание в базу.
     */
    @Insert
    suspend fun insert(spitUp: SpitUp)

    /**
     * Возвращает все срыгивания указанного ребёнка за указанную дату.
     *
     * Для ежедневной статистики и анализа.
     */
    @Query("SELECT * FROM spit_up WHERE childId = :childId AND DATE(timestamp) = :date ORDER BY timestamp ASC")
    suspend fun getByDate(childId: String, date: LocalDate): List<SpitUp>

    /**
     * Возвращает все срыгивания, связанные с конкретным кормлением.
     *
     * Для анализа: "Срыгивания чаще после утренних кормлений"
     */
    @Query("SELECT * FROM spit_up WHERE associatedFeedingId = :feedingId ORDER BY timestamp ASC")
    suspend fun getByFeedingId(feedingId: Long): List<SpitUp>

    /**
     * Возвращает количество срыгиваний за день, с разбивкой по объёму.
     */
    @Query("""
        SELECT 
            SUM(CASE WHEN volume = 'LITTLE' THEN 1 ELSE 0 END) AS littleCount,
            SUM(CASE WHEN volume = 'MUCH' THEN 1 ELSE 0 END) AS muchCount
        FROM spit_up 
        WHERE childId = :childId AND DATE(timestamp) = :date
    """)
    suspend fun getVolumeStats(childId: String, date: LocalDate): VolumeStats

    /**
     * Удаляет все срыгивания указанного ребёнка.
     *
     * Для тестирования или сброса данных.
     */
    @Query("DELETE FROM spit_up WHERE childId = :childId")
    suspend fun deleteAllForChild(childId: String)

    /**
     * Обновляет существующее срыгивание.
     */
    @Update
    suspend fun update(spitUp: SpitUp)

    /**
     * Удаляет одно срыгивание по ID.
     */
    @Delete
    suspend fun delete(spitUp: SpitUp)
}

/**
 * Вспомогательный класс для статистики объёма срыгиваний.
 */
data class VolumeStats(
    val littleCount: Int,
    val muchCount: Int
)