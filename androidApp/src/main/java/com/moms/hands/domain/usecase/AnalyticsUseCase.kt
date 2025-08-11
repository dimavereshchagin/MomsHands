package com.moms.hands.domain.usecase

import android.content.Context
import com.moms.hands.data.dao.FeedingDao
import com.moms.hands.data.dao.SpitUpDao
import com.moms.hands.data.entity.Feeding
import com.moms.hands.data.entity.SpitUp
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Use Case: Аналитика для Mom's Hands.
 *
 * Отвечает за:
 * - Генерацию инсайтов ("Вы чаще кормите правой грудью по вечерам")
 * - Расчёт статистики (суточные/недельные нормы)
 * - Подготовку данных для графиков
 * - Тепловую карту активности
 *
 * Соответствует ТЗ: 1.1, 1.2, 1.3, 2.4
 */
class AnalyticsUseCase(
    private val context: Context,
    private val feedingDao: FeedingDao,
    private val spitUpDao: SpitUpDao
) {

    /**
     * Генерирует персонализированные инсайты.
     */
    suspend fun generateInsights(): List<String> {
        val today = LocalDate.now()
        val feedings = feedingDao.getByDate("child_1", today)
        val spitUps = spitUpDao.getByDate("child_1", today)

        val insights = mutableListOf<String>()

        val leftSec = feedings.filter { it.breast == "LEFT" }.sumOf { it.durationSeconds }
        val rightSec = feedings.filter { it.breast == "RIGHT" }.sumOf { it.durationSeconds }

        if (rightSec > leftSec * 1.5) {
            insights += "Вы чаще кормите правой грудью по вечерам"
        } else if (leftSec > rightSec * 1.5) {
            insights += "Попробуйте увеличить время кормления левой грудью"
        }

        val morningSpits = spitUps.count { it.timestamp.hour in 6..11 }
        if (morningSpits > spitUps.size * 0.7) {
            insights += "Срыгивания чаще происходят после утренних кормлений"
        }

        val sortedFeedings = feedings.sortedBy { it.startTime }
        if (sortedFeedings.size > 1) {
            val intervals = sortedFeedings.zipWithNext { a, b ->
                kotlin.time.Duration.between(a.endTime, b.startTime).inWholeMinutes
            }
            val avgInterval = intervals.average()
            val lastAvg = getLastWeekAverageInterval()
            if (lastAvg != null && avgInterval < lastAvg - 15) {
                insights += "Средний интервал между кормлениями сократился на ${lastAvg - avgInterval.toInt()} минут"
            }
        }

        return insights.ifEmpty { listOf("Все в норме! Продолжайте в том же духе 💖") }
    }

    private suspend fun getLastWeekAverageInterval(): Double? {
        val lastWeek = LocalDate.now().minusDays(7)
        val feedings = feedingDao.getSince("child_1", lastWeek)
        val sorted = feedings.sortedBy { it.startTime }
        if (sorted.size <= 1) return null
        val intervals = sorted.zipWithNext { a, b ->
            kotlin.time.Duration.between(a.endTime, b.startTime).inWholeMinutes
        }
        return intervals.average()
    }

    /**
     * Возвращает данные для тепловой карты.
     */
    suspend fun getHeatmapData(days: Int = 7): List<List<Float>> {
        val data = mutableListOf<List<Float>>()
        val now = LocalDateTime.now()

        repeat(days) { dayOffset ->
            val day = now.minusDays(dayOffset.toLong())
            val dayData = MutableList(24) { 0f }
            val feedings = feedingDao.getByDate("child_1", day.toLocalDate())
            feedings.forEach { feeding ->
                val hour = feeding.startTime.hour
                dayData[hour] += feeding.durationSeconds / 60f
            }
            data.add(0, dayData)
        }

        return data
    }

    /**
     * Возвращает соотношение кормлений (левая/правая).
     */
    suspend fun getBreastRatio(): Map<String, Float> {
        val feedings = feedingDao.getAll("child_1")
        val left = feedings.filter { it.breast == "LEFT" }.sumOf { it.durationSeconds }
        val right = feedings.filter { it.breast == "RIGHT" }.sumOf { it.durationSeconds }
        val total = (left + right).toFloat()
        return mapOf(
            "LEFT" to if (total > 0) left / total else 0.5f,
            "RIGHT" to if (total > 0) right / total else 0.5f
        )
    }
}