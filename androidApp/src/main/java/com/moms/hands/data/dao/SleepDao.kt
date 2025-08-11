package com.moms.hands.data.dao

import androidx.room.*
import com.moms.hands.data.entity.Sleep
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Data Access Object для сущности "Сон".
 *
 * Отвечает за:
 * - Запись начала и окончания сна
 * - Получение сна по дате
 * - Получение общего времени сна за день
 * - Поиск последнего сна (для напоминаний)
 *
 * Используется в:
 * - Таймере сна
 * - Аналитике (тепловая карта, линейный график)
 * - Умных напоминаниях ("Пора укладывать спать")
 *
 * Соответствует ТЗ: 2.2, 2.4, 2.5
 */
@Dao
interface SleepDao {

    /**
     * Добавляет новую запись сна.
     */
    @Insert
    suspend fun insert(sleep: Sleep)

    /**
     * Возвращает все периоды сна указанного ребёнка за указанную дату.
     */
    @Query("SELECT * FROM sleep WHERE childId = :childId AND DATE(startTime) = :date ORDER BY startTime ASC")
    suspend fun getByDate(childId: String, date: LocalDate): List<Sleep>

    /**
     * Возвращает периоды сна в указанном диапазоне дат.
     *
     * Для сравнительной аналитики (ТЗ 1.1).
     */
    @Query("SELECT * FROM sleep WHERE childId = :childId AND DATE(startTime) BETWEEN :start AND :end ORDER BY startTime ASC")
    suspend fun getBetween(childId: String, start: LocalDate, end: LocalDate): List<Sleep>

    /**
     * Возвращает общую продолжительность сна за день (в секундах).
     *
     * Для ежедневной сводки.
     */
    @Query("SELECT SUM(durationSeconds) FROM sleep WHERE childId = :childId AND DATE(startTime) = :date")
    suspend fun getTotalSleepSecondsForDay(childId: String, date: LocalDate): Int

    /**
     * Возвращает последний период сна.
     *
     * Для определения ритма сна и адаптивных напоминаний.
     */
    @Query("SELECT * FROM sleep WHERE childId = :childId ORDER BY startTime DESC LIMIT 1")
    suspend fun getLastSleep(childId: String): Sleep?

    /**
     * Удаляет все записи сна указанного ребёнка.
     */
    @Query("DELETE FROM sleep WHERE childId = :childId")
    suspend fun deleteAllForChild(childId: String)

    /**
     * Обновляет существующую запись сна.
     */
    @Update
    suspend fun update(sleep: Sleep)

    /**
     * Удаляет одну запись сна по ID.
     */
    @Delete
    suspend fun delete(sleep: Sleep)
}