package com.moms.hands.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.moms.hands.R
import com.moms.hands.App
import com.moms.hands.domain.model.DailySummary
import com.moms.hands.domain.model.HeatmapData
import com.moms.hands.domain.usecase.AnalyticsUseCase
import com.moms.hands.domain.usecase.PdfExportUseCase
import com.moms.hands.util.PreferencesHelper
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Фрагмент "Статистика и аналитика".
 *
 * Отображает:
 * - Круговую диаграмму (левая/правая грудь)
 * - Линейный график продолжительности кормлений
 * - Столбчатую диаграмму частоты срыгиваний
 * - Тепловую карту активности
 * - Персонализированные инсайты
 * - Кнопку экспорта в PDF
 *
 * Поддерживает фильтры по:
 * - Периоду (24 часа, неделя, месяц, произвольный)
 * - Ребёнку (для многодетных мам)
 * - Типу активности
 *
 * Соответствует ТЗ: 2.4, 2.6, 1.1, 1.2, 1.3
 */
class StatsFragment : Fragment() {

    // UI
    private lateinit var tvPeriod: TextView
    private lateinit var tvInsights: TextView
    private lateinit var btnExportPdf: Button

    // Charts
    private lateinit var pieChart: PieChart
    private lateinit var lineChart: LineChart
    private lateinit var barChart: BarChart
    private lateinit var heatmapView: HeatmapView

    // State
    private var currentPeriod = Period.WEEK
    private val formatter = DateTimeFormatter.ofPattern("dd MMM")

    // Dependencies
    private val app by lazy { requireContext().applicationContext as App }
    private val prefs by lazy { app.prefs }
    private val analytics by lazy { AnalyticsUseCase(requireContext(), app.database.feedingDao(), app.database.spitUpDao()) }
    private val pdfExport by lazy { PdfExportUseCase(requireContext(), app.database.feedingDao(), app.database.spitUpDao(), analytics) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stats, container, false)

        initViews(view)
        setupClickListeners()
        loadStats()

        return view
    }

    private fun initViews(view: View) {
        tvPeriod = view.findViewById(R.id.tv_period)
        tvInsights = view.findViewById(R.id.tv_insights)
        btnExportPdf = view.findViewById(R.id.btn_export_pdf)
        pieChart = view.findViewById(R.id.pie_chart)
        lineChart = view.findViewById(R.id.line_chart)
        barChart = view.findViewById(R.id.bar_chart)
        heatmapView = view.findViewById(R.id.heatmap_view)
    }

    private fun setupClickListeners() {
        btnExportPdf.setOnClickListener { exportToPdf() }
    }

    private fun loadStats() {
        lifecycleScope.launch {
            val today = LocalDate.now()
            val (start, end) = when (currentPeriod) {
                Period.DAY -> today to today
                Period.WEEK -> today.minusDays(6) to today
                Period.MONTH -> today.minusMonths(1) to today
            }

            tvPeriod.text = "${start.format(formatter)} – ${end.format(formatter)}"

            // 1. Круговая диаграмма
            val ratio = analytics.getBreastRatio()
            updatePieChart(ratio)

            // 2. Линейный график (продолжительность кормлений по дням)
            val feedingDurations = analytics.getFeedingDurationsByDay(prefs.currentChildId, start, end)
            updateLineChart(feedingDurations, start, end)

            // 3. Столбчатая диаграмма (частота срыгиваний)
            val spitUpCounts = analytics.getSpitUpCountsByDay(prefs.currentChildId, start, end)
            updateBarChart(spitUpCounts, start, end)

            // 4. Тепловая карта
            val heatmapData = analytics.getHeatmapData(7)
            heatmapView.setData(heatmapData)

            // 5. Инсайты
            val insights = analytics.generateInsights()
            tvInsights.text = insights.joinToString("\n\n") { "• $it" }
        }
    }

    private fun updatePieChart(ratio: Map<String, Float>) {
        val entries = listOf(
            PieEntry(ratio["LEFT"] ?: 0.5f, getString(R.string.left_breast)),
            PieEntry(ratio["RIGHT"] ?: 0.5f, getString(R.string.right_breast))
        )

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(R.color.pink, R.color.lavender).map { context?.let { ctx -> androidx.core.content.ContextCompat.getColor(ctx, it) } ?: 0 }
            valueTextSize = 12f
            sliceSpace = 3f
        }

        pieChart.data = PieData(dataSet)
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = true
        pieChart.invalidate()
    }

    private fun updateLineChart(durations: List<Pair<LocalDate, Int>>, start: LocalDate, end: LocalDate) {
        val entries = durations.mapIndexed { index, (date, minutes) ->
            Entry(index.toFloat(), minutes.toFloat())
        }

        val dataSet = LineDataSet(entries, "Длительность кормлений (мин)").apply {
            color = context?.let { ctx -> androidx.core.content.ContextCompat.getColor(ctx, R.color.text_dark) } ?: 0
            valueTextSize = 10f
            setDrawCircles(true)
            circleRadius = 3f
        }

        lineChart.data = LineData(dataSet)
        lineChart.xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index in durations.indices) {
                    durations[index].first.dayOfMonth.toString()
                } else ""
            }
        }
        lineChart.description.text = "Динамика кормлений"
        lineChart.legend.isEnabled = false
        lineChart.invalidate()
    }

    private fun updateBarChart(counts: List<Pair<LocalDate, Int>>, start: LocalDate, end: LocalDate) {
        val entries = counts.mapIndexed { index, (_, count) ->
            BarEntry(index.toFloat(), count.toFloat())
        }

        val dataSet = BarDataSet(entries, "Количество срыгиваний").apply {
            color = context?.let { ctx -> androidx.core.content.ContextCompat.getColor(ctx, R.color.heat_high) } ?: 0
        }

        barChart.data = BarData(dataSet)
        barChart.description.text = "Частота срыгиваний"
        barChart.xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index in counts.indices) {
                    counts[index].first.dayOfMonth.toString()
                } else ""
            }
        }
        barChart.legend.isEnabled = false
        barChart.invalidate()
    }

    private fun exportToPdf() {
        lifecycleScope.launch {
            val file = pdfExport.exportDailyReport(LocalDate.now())
            if (file != null) {
                Toast.makeText(context, "Отчёт сохранён: ${file.name}", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Ошибка при создании PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private enum class Period {
        DAY, WEEK, MONTH
    }
}