package com.moms.hands.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.moms.hands.R
import com.moms.hands.App
import com.moms.hands.util.PreferencesHelper

/**
 * Экран обучения при первом запуске.
 *
 * Цель:
 * - Познакомить маму с ключевыми функциями
 * - Создать тёплое, заботливое впечатление
 * - Объяснить, как приложение упростит её жизнь
 * - Запросить разрешение на уведомления
 *
 * Структура:
 * 1. Добро пожаловать — приветствие
 * 2. Как это работает — основные функции
 * 3. Включите уведомления — адаптивные напоминания
 *
 * Тон: тёплый, заботливый, без давления.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var ivImage: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvMessage: TextView
    private lateinit var btnAction: Button
    private lateinit var btnSkip: TextView

    private var currentPage = 0

    // Слайды онбординга
    private val pages = listOf(
        Page(
            imageRes = R.drawable.onboarding_welcome,
            title = "Добро пожаловать, мама 💖",
            message = "С Mom's Hands вы больше не будете беспокоиться о том, какой грудью кормили в последний раз. Мы поможем вам найти свой ритм и заботиться о себе с лёгкостью."
        ),
        Page(
            imageRes = R.drawable.onboarding_tracking,
            title = "Простое отслеживание",
            message = "Нажмите «Левая» или «Правая» грудь — таймер запустится автоматически. Фиксируйте сон и срыгивания одним касанием. Всё сохраняется и анализируется."
        ),
        Page(
            imageRes = R.drawable.onboarding_insights,
            title = "Умные подсказки",
            message = "Приложение заметит, что вы чаще кормите правой грудью по вечерам, или что срыгивания происходят после утренних кормлений — и подскажет, как всё сбалансировать."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        initViews()
        setupClickListeners()
        showPage(0)
    }

    private fun initViews() {
        ivImage = findViewById(R.id.iv_onboarding_image)
        tvTitle = findViewById(R.id.tv_onboarding_title)
        tvMessage = findViewById(R.id.tv_onboarding_message)
        btnAction = findViewById(R.id.btn_onboarding_action)
        btnSkip = findViewById(R.id.btn_onboarding_skip)
    }

    private fun setupClickListeners() {
        btnAction.setOnClickListener { nextOrFinish() }
        btnSkip.setOnClickListener { finishOnboarding() }
    }

    private fun showPage(index: Int) {
        currentPage = index
        val page = pages[index]

        ivImage.setImageResource(page.imageRes)
        tvTitle.text = page.title
        tvMessage.text = page.message

        btnAction.text = if (index == pages.size - 1) "Начать" else "Далее"
        btnSkip.visibility = if (index == pages.size - 1) View.GONE else View.VISIBLE
    }

    private fun nextOrFinish() {
        if (currentPage < pages.size - 1) {
            showPage(currentPage + 1)
        } else {
            finishOnboarding()
        }
    }

    private fun finishOnboarding() {
        // Сохраняем, что онбординг пройден
        val app = application as App
        app.prefs.markFirstLaunchDone()

        // Запрашиваем разрешение на уведомления (на Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }

        // Переходим в главное окно
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun requestNotificationPermission() {
        // В реальном приложении: ActivityCompat.requestPermissions
        // Здесь: просто продолжаем
    }

    // Вспомогательный класс для слайдов
    data class Page(
        val imageRes: Int,
        val title: String,
        val message: String
    )
}