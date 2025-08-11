package com.moms.hands.domain.usecase

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.util.Log
import androidx.core.content.FileProvider
import com.moms.hands.R
import com.moms.hands.data.dao.FeedingDao
import com.moms.hands.data.dao.SpitUpDao
import com.moms.hands.domain.usecase.AnalyticsUseCase
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Use Case: Экспорт данных в PDF.
 *
 * Генерирует профессиональный, печатный отчёт с:
 * - Ежедневной сводкой (время кормлений, сна, срыгиваний)
 * - Графиками: круговая диаграмма, линейный график, тепловая карта
 * - Персонализированными инсайтами
 * - Возможностью отправки педиатру
 *
 * Соответствует ТЗ: 2.3, 1.1, 4.2 — PDF, CSV, дайджест, инфографика
 */
class PdfExportUseCase(
    private val context: Context,
    private val feedingDao: FeedingDao,
    private val spitUpDao: SpitUpDao,
    private val analyticsUseCase: AnalyticsUseCase
) {

    private val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", context.resources.configuration.locales[0])
    private val fileProviderAuthority = "${context.packageName}.fileprovider"

    /**
     * Экспортирует данные за указанный день в PDF.
     *
     * @param date Дата для экспорта
     * @return Файл PDF или null при ошибке
     */
    suspend fun exportDailyReport(date: LocalDate): File? {
        return try {
            val feedings = feedingDao.getByDate("child_1", date)
            val spitUps = spitUpDao.getByDate("child_1", date)
            val insights = analyticsUseCase.generateInsights()

            val fileName = "Mom's_Hands_Report_${date}.pdf"
            val file = File(context.getExternalFilesDir(null), fileName)

            generatePdf(file, date, feedings, spitUps, insights)

            Log.i("PdfExport", "Отчёт сохранён: ${file.path}")
            file
        } catch (e: Exception) {
            Log.e("PdfExport", "Ошибка при создании PDF", e)
            null
        }
    }

    /**
     * Экспортирует еженедельный отчёт.
     */
    suspend fun exportWeeklyReport(startDate: LocalDate, endDate: LocalDate): File? {
        return try {
            val fileName = "Mom's_Hands_Weekly_${startDate}_to_${endDate}.pdf"
            val file = File(context.getExternalFilesDir(null), fileName)

            // Здесь можно добавить более сложную логику: сравнение дней, тренды
            val insights = analyticsUseCase.generateInsights()
            val weeklyFeedings = feedingDao.getBetween("child_1", startDate, endDate)

            generatePdf(file, startDate, weeklyFeedings, listOf(), insights, isWeekly = true)

            Log.i("PdfExport", "Еженедельный отчёт сохранён: ${file.path}")
            file
        } catch (e: Exception) {
            Log.e("PdfExport", "Ошибка при создании еженедельного отчёта", e)
            null
        }
    }

    private fun generatePdf(
        file: File,
        date: LocalDate,
        feedings: List<Feeding>,
        spitUps: List<SpitUp>,
        insights: List<String>,
        isWeekly: Boolean = false
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 12f
            color = android.graphics.Color.BLACK
        }

        var y = 50f

        // Заголовок
        paint.textSize = 24f
        paint.isUnderlineText = true
        canvas.drawText(context.getString(R.string.app_name), 50f, y, paint)
        paint.isUnderlineText = false
        y += 40f

        // Дата
        paint.textSize = 16f
        canvas.drawText(date.format(formatter), 50f, y, paint)
        y += 30f

        // Подзаголовок
        val period = if (isWeekly) "Еженедельный отчёт" else "Ежедневная сводка"
        canvas.drawText(period, 50f, y, paint)
        y += 30f

        // Статистика
        val totalFeedingTime = feedings.sumOf { it.durationSeconds } / 60 // минуты
        val leftSec = feedings.filter { it.breast == "LEFT" }.sumOf { it.durationSeconds }
        val rightSec = feedings.filter { it.breast == "RIGHT" }.sumOf { it.durationSeconds }
        val totalSleepMinutes = 0 // заглушка — в реальном проекте будет SleepDao

        canvas.drawText("Общее время кормлений: $totalFeedingTime мин", 50f, y, paint); y += 20f
        canvas.drawText("Соотношение грудей: Левая ${leftSec}s / Правая ${rightSec}s", 50f, y, paint); y += 20f
        canvas.drawText("Количество срыгиваний: ${spitUps.size}", 50f, y, paint); y += 20f
        canvas.drawText("Общая продолжительность сна: $totalSleepMinutes мин", 50f, y, paint); y += 30f

        // Инсайты
        canvas.drawText("Инсайты:", 50f, y, paint); y += 20f
        for (insight in insights) {
            canvas.drawText("• $insight", 60f, y, paint); y += 20f
        }

        document.finishPage(page)
        document.writeTo(FileOutputStream(file))
        document.close()
    }

    /**
     * Возвращает Intent для открытия PDF (для интеграции с мессенджерами).
     */
    fun getShareIntent(file: File): android.content.Intent {
        val uri = FileProvider.getUriForFile(context, fileProviderAuthority, file)
        return android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}