package com.moms.hands.domain.model

import java.time.LocalDateTime

/**
 * Модель "Тепловая карта активности" — визуализация "часов пик" кормлений и сна.
 *
 * Представляет данные в виде матрицы:
 * - Строки: дни (например, последние 7 дней)
 * - Столбцы: часы суток (0–23)
 * - Значение: интенсивность (например, длительность кормления в минутах)
 *
 * Используется в:
 * - Тепловой карте на экране статистики
 * - Анализе ритма ребёнка
 * - Умных напоминаниях
 *
 * Соответствует ТЗ: 1.2, 2.4, 2.5
 */
data class HeatmapData(
    val data: List<List<Float>>, // [день][час] → интенсивность
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val dataType: DataType
) {
    enum class DataType {
        FEEDING, SLEEP, SPIT_UP
    }

    /**
     * Возвращает интенсивность для конкретного дня и часа.
     */
    fun getValue(dayOffset: Int, hour: Int): Float {
        return if (dayOffset in data.indices && hour in 0..23) {
            data[dayOffset][hour]
        } else {
            0f
        }
    }

    /**
     * Возвращает максимальное значение интенсивности (для нормализации цветов).
     */
    fun getMaxIntensity(): Float {
        return data.flatten().maxOrNull() ?: 0f
    }

    /**
     * Возвращает количество дней в данных.
     */
    fun getDayCount(): Int = data.size

    companion object {
        /**
         * Создаёт пустую тепловую карту.
         */
        fun empty(): HeatmapData = HeatmapData(
            data = List(7) { List(24) { 0f } },
            startDate = LocalDateTime.now().minusDays(6),
            endDate = LocalDateTime.now(),
            dataType = DataType.FEEDING
        )
    }
}