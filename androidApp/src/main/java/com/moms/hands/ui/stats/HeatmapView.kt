package com.moms.hands.ui.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.moms.hands.R
import com.moms.hands.domain.model.HeatmapData

/**
 * Кастомный View для отображения тепловой карты активности.
 *
 * Визуализирует "часы пик" кормлений и сна.
 * Цветовая градация: от голубого (низкая активность) до красного (высокая).
 *
 * Используется в StatsFragment для анализа ритма ребёнка.
 * Соответствует ТЗ: 1.2
 */
class HeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply { isAntiAlias = true }
    private var data: HeatmapData? = null

    fun setData(heatmapData: HeatmapData) {
        this.data = heatmapData
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val data = data ?: return

        val cellWidth = width / 24f
        val cellHeight = height / data.getDayCount().toFloat()

        val maxIntensity = data.getMaxIntensity()

        for (dayOffset in data.data.indices) {
            for (hour in 0 until 24) {
                val intensity = data.getValue(dayOffset, hour)
                val normalized = if (maxIntensity > 0) intensity / maxIntensity else 0f

                paint.color = when {
                    normalized > 0.8 -> ContextCompat.getColor(context, R.color.heat_peak) // красный
                    normalized > 0.6 -> ContextCompat.getColor(context, R.color.heat_high) // оранжевый
                    normalized > 0.3 -> ContextCompat.getColor(context, R.color.heat_medium) // жёлтый
                    normalized > 0.1 -> ContextCompat.getColor(context, R.color.heat_low) // голубой
                    else -> ContextCompat.getColor(context, R.color.cream) // кремовый
                }

                canvas.drawRect(
                    hour * cellWidth,
                    dayOffset * cellHeight,
                    (hour + 1) * cellWidth,
                    (dayOffset + 1) * cellHeight,
                    paint
                )
            }
        }

        // Опционально: добавить сетку, подписи часов
    }
}