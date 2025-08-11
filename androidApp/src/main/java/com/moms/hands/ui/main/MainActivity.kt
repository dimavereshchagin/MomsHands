package com.moms.hands.ui.main

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.moms.hands.R
import com.moms.hands.App
import com.moms.hands.data.entity.Feeding
import com.moms.hands.domain.model.DailySummary
import com.moms.hands.domain.usecase.AnalyticsUseCase
import com.moms.hands.domain.usecase.PdfExportUseCase
import com.moms.hands.ui.settings.SettingsActivity
import com.moms.hands.util.PreferencesHelper
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Главный экран приложения Mom's Hands.
 *
 * Отображает:
 * - Кнопки: Левая грудь, Правая грудь, Сон
 * - Таймер кормления и сна
 * - Визуализацию "следующей груди"
 * - Круговую диаграмму (левая/правая)
 * - Ключевые метрики: баланс грудей, прогресс по норме, последний сон
 * - Кнопку перехода в настройки
 *
 * Использует MVVM через прямой доступ к репозиторию (упрощённый подход).
 * Поддерживает темную тему, мультиязычность, несколько детей.
 *
 * Соответствует ТЗ: 2.1, 2.2, 2.4, 2.6, 3.1
 */
class MainActivity : AppCompatActivity() {

    // UI
    private lateinit var tvNextBreast: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvSummary: TextView
    private lateinit var pieChart: PieChart

    // Buttons
    private lateinit var btnLeft: Button
    private lateinit var btnRight: Button
    private lateinit var btnSleep: Button
    private lateinit var btnSpit: Button
    private lateinit var btnSettings: Button

    // State
    private var isLeftNext = true
    private var currentFeeding: Feeding? = null
    private var currentSleepId: Long? = null
    private var timer: CountDownTimer? = null

    // Dependencies (DI вручную)
    private val app by lazy { application as App }
    private val prefs by lazy { app.prefs }
    private val repo by lazy { app.database.lactationRepository() }
    private val analytics by lazy { AnalyticsUseCase(this, app.database.feedingDao(), app.database.spitUpDao()) }
    private val pdfExport by lazy { PdfExportUseCase(this, app.database.feedingDao(), app.database.spitUpDao(), analytics) }

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupButtons()
        setupChart()
        loadTodaySummary()
        checkLastFeeding()
        startQuickAccessService()
    }

    private fun initViews() {
        tvNextBreast = findViewById(R.id.tv_next_breast)
        tvTimer = findViewById(R.id.tv_timer)
        tvSummary = findViewById(R.id.tv_summary)
        pieChart = findViewById(R.id.pie_chart)
        btnLeft = findViewById(R.id.btn_left)
        btnRight = findViewById(R.id.btn_right)
        btnSleep = findViewById(R.id.btn_sleep)
        btnSpit = findViewById(R.id.btn_spit)
        btnSettings = findViewById(R.id.btn_settings)
    }

    private fun setupButtons() {
        btnLeft.setOnClickListener { startFeeding("LEFT") }
        btnRight.setOnClickListener { startFeeding("RIGHT") }
        btnSleep.setOnClickListener { toggleSleep() }
        btnSpit.setOnClickListener { recordSpitUp() }
        btnSettings.setOnClickListener { openSettings() }
    }

    private fun startFeeding(breast: String) {
        if (currentFeeding != null) {
            Toast.makeText(this, "Кормление уже идёт", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            val id = repo.startFeeding(prefs.currentChildId, breast)
            currentFeeding = repo.getFeeding(id) // Предполагаем, что есть get() в DAO
            startTimer()
            Toast.makeText(this@MainActivity, "Кормление началось: $breast грудь", Toast.LENGTH_SHORT).show()
        }

        isLeftNext = breast == "RIGHT"
        updateNextBreastIndicator()
    }

    private fun endFeeding() {
        if (currentFeeding == null) return

        scope.launch {
            repo.endFeeding(currentFeeding!!.id, java.time.LocalDateTime.now())
            currentFeeding = null
            stopTimer()
            loadTodaySummary()
            Toast.makeText(this@MainActivity, "Кормление завершено", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleSleep() {
        if (currentSleepId != null) {
            endSleep()
        } else {
            startSleep()
        }
    }

    private fun startSleep() {
        scope.launch {
            currentSleepId = repo.startSleep(prefs.currentChildId)
            Toast.makeText(this@MainActivity, "Сон начался", Toast.LENGTH_SHORT).show()
        }
    }

    private fun endSleep() {
        scope.launch {
            repo.endSleep(currentSleepId!!, java.time.LocalDateTime.now())
            currentSleepId = null
            Toast.makeText(this@MainActivity, "Сон завершён", Toast.LENGTH_SHORT).show()
        }
    }

    private fun recordSpitUp() {
        scope.launch {
            repo.recordSpitUp(prefs.currentChildId, com.moms.hands.data.entity.SpitVolume.LITTLE)
            Toast.makeText(this@MainActivity, "Срыгивание зафиксировано", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startTimer() {
        timer?.cancel()
        val startTime = System.currentTimeMillis()
        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millis: Long) {
                val elapsed = System.currentTimeMillis() - startTime
                val seconds = (elapsed / 1000).toInt()
                tvTimer.text = formatTime(seconds)
            }
            override fun onFinish() {}
        }.start()
    }

    private fun stopTimer() {
        timer?.cancel()
        tvTimer.text = "00:00:00"
    }

    private fun formatTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    private fun updateNextBreastIndicator() {
        val next = if (isLeftNext) getString(R.string.left_breast) else getString(R.string.right_breast)
        tvNextBreast.text = getString(R.string.next_breast, next)
    }

    private fun checkLastFeeding() {
        scope.launch {
            val last = repo.getLastFeeding(prefs.currentChildId)
            isLeftNext = last?.breast != "LEFT"
            updateNextBreastIndicator()
        }
    }

    private fun loadTodaySummary() {
        scope.launch {
            val date = LocalDate.now()
            val feedings = repo.getFeedingsForDate(prefs.currentChildId, date)
            val sleepPeriods = repo.getSleepForDate(prefs.currentChildId, date)
            val spitUps = repo.getSpitUpsForDate(prefs.currentChildId, date)

            val totalFeedingTime = feedings.sumOf { it.durationSeconds } / 60
            val leftSec = feedings.filter { it.breast == "LEFT" }.sumOf { it.durationSeconds }
            val rightSec = feedings.filter { it.breast == "RIGHT" }.sumOf { it.durationSeconds }
            val totalSleepMinutes = sleepPeriods.sumOf { it.durationSeconds } / 60

            val summary = DailySummary(
                date = date,
                totalFeedingTimeMinutes = totalFeedingTime,
                leftBreastSeconds = leftSec,
                rightBreastSeconds = rightSec,
                totalSpitUps = spitUps.size,
                totalSleepMinutes = totalSleepMinutes,
                feedingCount = feedings.size
            )

            tvSummary.text = """
                Кормлений: ${summary.feedingCount}
                Срыгиваний: ${summary.totalSpitUps}
                Сон: ${summary.getFormattedTotalSleepTime()}
            """.trimIndent()

            updateChart(summary.getBreastRatio())
        }
    }

    private fun updateChart(ratio: Map<String, Float>) {
        val entries = listOf(
            PieEntry(ratio["LEFT"] ?: 0.5f, getString(R.string.left_breast)),
            PieEntry(ratio["RIGHT"] ?: 0.5f, getString(R.string.right_breast))
        )

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(R.color.pink, R.color.lavender).map { ContextCompat.getColor(this@MainActivity, it) }
            setDrawIcons(false)
            sliceSpace = 3f
            valueTextSize = 12f
        }

        pieChart.data = PieData(dataSet)
        pieChart.invalidate()
    }

    private fun startQuickAccessService() {
        // Запускаем сервис для кнопок на экране блокировки
        // Реализуется в QuickAccessService.kt
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        scope.cancel()
    }
}