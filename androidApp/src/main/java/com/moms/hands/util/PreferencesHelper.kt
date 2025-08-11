package com.moms.hands.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Утилита для безопасного хранения настроек приложения.
 *
 * Хранит:
 * - Темную тему
 * - Язык интерфейса
 * - ID активного ребёнка (поддержка нескольких детей)
 * - Настройки уведомлений
 * - Целевые нормы (интервал кормления, длительность сна)
 * - Ключ шифрования базы данных
 *
 * Использует Android SharedPreferences — надёжное, локальное хранение.
 */
class PreferencesHelper(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    // === Настройки интерфейса ===
    var isDarkTheme: Boolean
        get() = prefs.getBoolean("dark_theme", false)
        set(value) = prefs.edit { putBoolean("dark_theme", value) }

    var appLanguage: String
        get() = prefs.getString("app_language", "ru") ?: "ru"
        set(value) = prefs.edit { putString("app_language", value) }

    // === Управление детьми ===
    var currentChildId: String
        get() = prefs.getString("current_child_id", "child_1") ?: "child_1"
        set(value) = prefs.edit { putString("current_child_id", value) }

    val isFirstLaunch: Boolean
        get() = prefs.getBoolean("first_launch", true)

    fun markFirstLaunchDone() {
        prefs.edit { putBoolean("first_launch", false) }
    }

    // === Целевые нормы ===
    /**
     * Целевой интервал между кормлениями (в минутах).
     * По умолчанию: 150 минут (2.5 часа)
     */
    var targetFeedingIntervalMinutes: Int
        get() = prefs.getInt("target_feeding_interval", 150)
        set(value) = prefs.edit { putInt("target_feeding_interval", value) }

    /**
     * Целевая продолжительность одного сна (в минутах).
     * По умолчанию: 45 минут
     */
    var targetSleepDurationMinutes: Int
        get() = prefs.getInt("target_sleep_duration", 45)
        set(value) = prefs.edit { putInt("target_sleep_duration", value) }

    // === Уведомления ===
    var enableFeedingReminders: Boolean
        get() = prefs.getBoolean("enable_feeding_reminders", true)
        set(value) = prefs.edit { putBoolean("enable_feeding_reminders", value) }

    var enableSleepReminders: Boolean
        get() = prefs.getBoolean("enable_sleep_reminders", true)
        set(value) = prefs.edit { putBoolean("enable_sleep_reminders", value) }

    var enableInsights: Boolean
        get() = prefs.getBoolean("enable_insights", true)
        set(value) = prefs.edit { putBoolean("enable_insights", value) }

    // === Безопасность ===
    fun getEncryptionKey(): String {
        return prefs.getString("db_encryption_key", null)
            ?: generateAndStoreKey()
    }

    private fun generateAndStoreKey(): String {
        val key = java.util.UUID.randomUUID().toString().substring(0, 16) // 16 символов
        prefs.edit { putString("db_encryption_key", key) }
        return key
    }
}