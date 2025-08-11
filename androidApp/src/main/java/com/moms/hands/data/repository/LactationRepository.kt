package com.moms.hands.data.repository

import com.moms.hands.data.dao.FeedingDao
import com.moms.hands.data.dao.SleepDao
import com.moms.hands.data.dao.SpitUpDao
import com.moms.hands.data.entity.Feeding
import com.moms.hands.data.entity.Sleep
import com.moms.hands.data.entity.SpitUp
import java.time.LocalDate

/**
 * Репозиторий лактации — центральный класс доступа к данным.
 *
 * Объединяет:
 * - Кормления (Feeding)
 * - Сон (Sleep)
 * - Срыгивания (SpitUp)
 *
 * Отвечает за:
 * - Единый интерфейс для всех операций с данными
 * - Работу с активным ребёнком (поддержка нескольких детей)
 * - Подготовку данных для аналитики, графиков, UI
 *
 * Соответствует ТЗ: Clean Architecture, MVVM, локальное хранение, несколько детей.
 */
class LactationRepository(
    private val feedingDao: FeedingDao,
    private val sleepDao: SleepDao,
    private val spitUpDao: SpitUpDao
) {

    // === Кормления ===

    suspend fun startFeeding(childId: String, breast: String): Long {
        val now = java.time.LocalDateTime.now()
        val feeding = Feeding(
            childId = childId,
            breast = breast,
            startTime = now,
            endTime = now,
            durationSeconds = 0
        )
        return feedingDao.insert(feeding)
    }

    suspend fun endFeeding(feedingId: Long, endTime: java.time.LocalDateTime) {
        val feeding = feedingDao.get(feedingId) // Предполагаем, что есть get() — можно добавить в DAO
        if (feeding != null) {
            val duration = java.time.Duration.between(feeding.startTime, endTime).seconds.toInt()
            val updated = feeding.copy(endTime = endTime, durationSeconds = duration)
            feedingDao.update(updated)
        }
    }

    suspend fun getFeedingsForDate(childId: String, date: LocalDate): List<Feeding> =
        feedingDao.getByDate(childId, date)

    suspend fun getLastFeeding(childId: String): Feeding? =
        feedingDao.getLastFeeding(childId)

    // === Сон ===

    suspend fun startSleep(childId: String): Long {
        val now = java.time.LocalDateTime.now()
        val sleep = Sleep(
            childId = childId,
            startTime = now,
            endTime = now,
            durationSeconds = 0
        )
        return sleepDao.insert(sleep)
    }

    suspend fun endSleep(sleepId: Long, endTime: java.time.LocalDateTime) {
        val sleep = sleepDao.get(sleepId) // Аналогично — можно добавить get() в DAO
        if (sleep != null) {
            val duration = java.time.Duration.between(sleep.startTime, endTime).seconds.toInt()
            val updated = sleep.copy(endTime = endTime, durationSeconds = duration)
            sleepDao.update(updated)
        }
    }

    suspend fun getSleepForDate(childId: String, date: LocalDate): List<Sleep> =
        sleepDao.getByDate(childId, date)

    suspend fun getTotalSleepSecondsForDay(childId: String, date: LocalDate): Int =
        sleepDao.getTotalSleepSecondsForDay(childId, date)

    // === Срыгивания ===

    suspend fun recordSpitUp(childId: String, volume: com.moms.hands.data.entity.SpitVolume, associatedFeedingId: Long? = null) {
        val spitUp = SpitUp(
            childId = childId,
            timestamp = java.time.LocalDateTime.now(),
            volume = volume,
            associatedFeedingId = associatedFeedingId
        )
        spitUpDao.insert(spitUp)
    }

    suspend fun getSpitUpsForDate(childId: String, date: LocalDate): List<SpitUp> =
        spitUpDao.getByDate(childId, date)

    suspend fun getSpitUpsForFeeding(feedingId: Long): List<SpitUp> =
        spitUpDao.getByFeedingId(feedingId)

    // === Универсальные операции ===

    /**
     * Возвращает все данные за указанную дату для указанного ребёнка.
     * Используется в StatsFragment и PDF-экспорте.
     */
    suspend fun getDataForDate(childId: String, date: LocalDate) = DataForDate(
        feedings = getFeedingsForDate(childId, date),
        sleepPeriods = getSleepForDate(childId, date),
        spitUps = getSpitUpsForDate(childId, date)
    )

    /**
     * Удаляет все данные указанного ребёнка.
     * Для сброса или тестирования.
     */
    suspend fun deleteAllDataForChild(childId: String) {
        feedingDao.deleteAllForChild(childId)
        sleepDao.deleteAllForChild(childId)
        spitUpDao.deleteAllForChild(childId)
    }

    // === Вспомогательные классы ===

    data class DataForDate(
        val feedings: List<Feeding>,
        val sleepPeriods: List<Sleep>,
        val spitUps: List<SpitUp>
    )
}