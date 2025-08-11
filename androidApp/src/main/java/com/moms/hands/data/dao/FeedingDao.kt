package com.moms.hands.data.dao

import androidx.room.*
import com.moms.hands.data.entity.Feeding
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Data Access Object для сущности "Кормление".
 *
 * Отвечает за:
 * - Вставку новых кормлений
 * - Получение кормлений по дате и ребёнку
 * - Получение последнего кормления (для определения "следующей груди")
 * - Получение кормлений за период (для аналитики)
 *
 * Использует Android Room — локальную базу данных.
 * Соответствует ТЗ: контроль очередности, статистика, аналитика.
 */
@Dao
interface FeedingDao {

    /**
     * Добавляет новое кормление в базу.
     */
    @Insert
    suspend fun insert(feeding: Feeding)

    /**
     * Возвращает все кормления указанного ребёнка за указанную дату.
     *
     * Используется в:
     * - MainActivity (для отображения)
     * - AnalyticsUseCase (для расчёта статистики)
     * - StatsFragment (для графиков)
     */
    @Query("SELECT * FROM feeding WHERE childId = :childId AND DATE(startTime) = :date ORDER BY startTime ASC")
    suspend fun getByDate(childId: String, date: LocalDate): List<Feeding>

    /**
     * Возвращает кормления за период (начало и конец).
     *
     * Для сравнительной аналитики (ТЗ 1.1).
     */
    @Query("SELECT * FROM feeding WHERE childId = :childId AND DATE(startTime) BETWEEN :start AND :end ORDER BY startTime ASC")
    suspend fun getBetween(childId: String, start: LocalDate, end: LocalDate): List<Feeding>

    /**
     * Возвращает кормления, начиная с указанной даты (включительно).
     *
     * Для анализа трендов.
     */
    @Query("SELECT * FROM feeding WHERE childId = :childId AND DATE(startTime) >= :fromDate ORDER BY startTime ASC")
    suspend fun getSince(childId: String, fromDate: LocalDate): List<Feeding>

    /**
     * Возвращает последнее кормление указанного ребёнка.
     *
     * Используется для определения, какая грудь была последней.
     * Нужно для функции "следующая грудь" на экране блокировки.
     */
    @Query("SELECT * FROM feeding WHERE childId = :childId ORDER BY startTime DESC LIMIT 1")
    suspend fun getLastFeeding(childId: String): Feeding?

    /**
     * Удаляет все кормления указанного ребёнка.
     *
     * Для тестирования или сброса данных.
     */
    @Query("DELETE FROM feeding WHERE childId = :childId")
    suspend fun deleteAllForChild(childId: String)

    /**
     * Обновляет существующее кормление (например, если изменилась длительность).
     */
    @Update
    suspend fun update(feeding: Feeding)

    /**
     * Удаляет одно кормление по ID.
     */
    @Delete
    suspend fun delete(feeding: Feeding)
}