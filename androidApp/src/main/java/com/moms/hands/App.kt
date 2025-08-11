package com.moms.hands

import android.app.Application
import android.util.Log
import com.moms.hands.data.AppDatabase
import com.moms.hands.domain.AnalyticsEngine
import com.moms.hands.domain.NotificationManager
import com.moms.hands.util.PreferencesHelper

/**
 * Главный класс приложения Mom's Hands.
 *
 * Инициализирует:
 * - Локальную базу данных с шифрованием (SQLCipher)
 * - Движок аналитики и умных инсайтов
 * - Менеджер уведомлений
 * - Настройки (темная тема, язык, активный ребёнок)
 *
 * Соответствует ТЗ: Clean Architecture, MVVM, локальное хранение, безопасность, поддержка нескольких детей.
 */
class App : Application() {

    // Глобальный доступ к базе данных
    lateinit var database: AppDatabase
        private set

    // Движок аналитики — генерация инсайтов, PDF, графиков
    lateinit var analytics: AnalyticsEngine
        private set

    // Менеджер уведомлений — умные напоминания
    lateinit var notifications: NotificationManager
        private set

    // Хранилище настроек (через SharedPreferences)
    lateinit var prefs: PreferencesHelper
        private set

    override fun onCreate() {
        super.onCreate()

        // Инициализация компонентов в правильном порядке
        initPreferences()
        initDatabase()
        initAnalytics()
        initNotifications()

        // Логируем успешный запуск
        Log.i("App", "Mom's Hands запущено. Версия: ${BuildConfig.VERSION_NAME}")
    }

    private fun initPreferences() {
        prefs = PreferencesHelper(this)
        // При первом запуске — установить значения по умолчанию
        if (prefs.isFirstLaunch) {
            prefs.apply {
                isDarkTheme = false
                appLanguage = "ru"
                currentChildId = "child_1"
                enableFeedingReminders = true
                enableSleepReminders = true
                enableInsights = true
            }
        }
    }

    private fun initDatabase() {
        // AppDatabase использует SQLCipher — шифрование резервных копий
        database = AppDatabase.getDatabase(this, prefs.getEncryptionKey())
        Log.d("App", "База данных инициализирована с шифрованием")
    }

    private fun initAnalytics() {
        analytics = AnalyticsEngine(this, database.feedingDao(), database.spitUpDao())
        Log.d("App", "Аналитика инициализирована")
    }

    private fun initNotifications() {
        notifications = NotificationManager(this)
        notifications.createNotificationChannels()
        Log.d("App", "Уведомления инициализированы")
    }
}