package com.moms.hands.domain.model

import java.time.LocalDate

/**
 * Модель "Ежедневная сводка" — основа отчёта для мамы.
 *
 * Содержит ключевые метрики за день:
 * - Общее время кормлений
 * - Соотношение левой и правой груди
 * - Количество срыгиваний
 * - Общая продолжительность сна
 * - Количество кормлений
 *
 * Используется в:
 * - Главном экране (виджеты)
 * - Экране статистики
 * - PDF-экспорте
 *
 * Соответствует ТЗ: 1.1, 2.1, 2.4, 2.6
 */
data class DailySummary(
    val date: LocalDate,
    val totalFeedingTimeMinutes: Int,
    val leftBreastSeconds: Int,
    val rightBreastSeconds: Int,
    val totalSpitUps: Int,
    val totalSleepMinutes: Int,
    val feedingCount: Int
) {
    /**
     * Возвращает соотношение кормлений в процентах.
     */
    fun getBreastRatio(): Map<String, Float> {
        val total = (leftBreastSeconds + rightBreastSeconds).toFloat()
        return mapOf(
            "LEFT" to if (total > 0) leftBreastSeconds / total else 0.5f,
            "RIGHT" to if (total > 0) rightBreastSeconds / total else 0.5f
        )
    }

    /**
     * Возвращает общее время кормлений в формате "чч:мм".
     */
    fun getFormattedTotalFeedingTime(): String {
        val hours = totalFeedingTimeMinutes / 60
        val minutes = totalFeedingTimeMinutes % 60
        return String.format("%02d:%02d", hours, minutes)
    }

    /**
     * Возвращает общее время сна в формате "чч:мм".
     */
    fun getFormattedTotalSleepTime(): String {
        val hours = totalSleepMinutes / 60
        val minutes = totalSleepMinutes % 60
        return String.format("%02d:%02d", hours, minutes)
    }
}