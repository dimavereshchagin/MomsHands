package com.moms.hands.domain.usecase

import com.moms.hands.data.dao.FeedingDao
import com.moms.hands.data.dao.SpitUpDao
import com.moms.hands.data.entity.Feeding
import com.moms.hands.data.entity.SpitUp
import java.time.LocalDate

/**
 * Use Case: Генерация инсайтов для конкретного ребёнка.
 *
 * Отвечает за:
 * - Персонализированные выводы: "Вы чаще кормите правой грудью по вечерам"
 * - Рекомендации: "Попробуйте увеличить время кормления левой грудью"
 * - Анализ срыгиваний по времени и груди
 * - Поддержка нескольких детей (ключевое требование ТЗ)
 *
 * Использует данные только указанного ребёнка.
 */
class GenerateInsightsForChildUseCase(
    private val feedingDao: FeedingDao,
    private val spitUpDao: SpitUpDao
) {

    suspend fun execute(childId: String, date: LocalDate): List<String> {
        val feedings = feedingDao.getByDate(childId, date)
        val spitUps = spitUpDao.getByDate(childId, date)

        val insights = mutableListOf<String>()

        // 1. Анализ баланса грудей
        val leftSec = feedings.filter { it.breast == "LEFT" }.sumOf { it.durationSeconds }
        val rightSec = feedings.filter { it.breast == "RIGHT" }.sumOf { it.durationSeconds }

        if (rightSec > leftSec * 1.5) {
            insights += "Вы чаще кормите правой грудью по вечерам"
        } else if (leftSec > rightSec * 1.5) {
            insights += "Попробуйте увеличить время кормления левой грудью"
        }

        // 2. Анализ срыгиваний
        val morningSpits = spitUps.count { it.timestamp.hour in 6..11 }
        if (morningSpits > spitUps.size * 0.7) {
            insights += "Срыгивания чаще происходят после утренних кормлений"
        }

        // 3. Интервалы между кормлениями
        val sortedFeedings = feedings.sortedBy { it.startTime }
        if (sortedFeedings.size > 1) {
            val intervals = sortedFeedings.zipWithNext { a, b ->
                kotlin.time.Duration.between(a.endTime, b.startTime).inWholeMinutes
            }
            val avgInterval = intervals.average()
            val lastAvg = getLastWeekAverageInterval(childId)
            if (lastAvg != null && avgInterval < lastAvg - 15) {
                insights += "Средний интервал между кормлениями сократился на ${lastAvg - avgInterval.toInt()} минут"
            }
        }

        return insights.ifEmpty { listOf("Все в норме! Продолжайте в том же духе 💖") }
    }

    private suspend fun getLastWeekAverageInterval(childId: String): Double? {
        val lastWeek = LocalDate.now().minusDays(7)
        val feedings = feedingDao.getSince(childId, lastWeek)
        val sorted = feedings.sortedBy { it.startTime }
        if (sorted.size <= 1) return null
        val intervals = sorted.zipWithNext { a, b ->
            kotlin.time.Duration.between(a.endTime, b.startTime).inWholeMinutes
        }
        return intervals.average()
    }
}