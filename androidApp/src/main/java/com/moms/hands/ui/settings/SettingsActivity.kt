package com.moms.hands.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.moms.hands.R
import com.moms.hands.App
import com.moms.hands.util.PreferencesHelper

/**
 * Экран настроек приложения Mom's Hands.
 *
 * Позволяет маме настроить:
 * - Язык интерфейса (русский/английский)
 * - Темную тему
 * - Целевые нормы (интервалы кормления и сна)
 * - Уведомления (кормление, сон, смена груди)
 * - Активного ребёнка (поддержка нескольких детей)
 * - Резервное копирование и экспорт
 *
 * Соответствует ТЗ: 2.6, 3.2, 4.2
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Настраиваем кнопку "Назад"
        findViewById<Button>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // Добавляем фрагмент настроек
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        private lateinit var prefs: PreferencesHelper

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            prefs = PreferencesHelper(requireContext())

            // === Язык интерфейса ===
            findPreference<Preference>("pref_language")?.summary = when (prefs.appLanguage) {
                "ru" -> "Русский"
                "en" -> "English"
                else -> "Русский"
            }
            findPreference<Preference>("pref_language")?.setOnPreferenceClickListener {
                // В реальном приложении: показать диалог выбора языка
                // После выбора — перезапустить приложение
                true
            }

            // === Темная тема ===
            findPreference<SwitchPreferenceCompat>("pref_dark_theme")?.apply {
                isChecked = prefs.isDarkTheme
                setOnPreferenceChangeListener { _, newValue ->
                    val isDark = newValue as Boolean
                    prefs.isDarkTheme = isDark
                    // Перезапуск активности для применения темы
                    val intent = Intent(requireContext(), requireActivity().javaClass)
                    startActivity(intent)
                    requireActivity().finish()
                    true
                }
            }

            // === Целевые нормы ===
            setupTargetPreferences()

            // === Уведомления ===
            setupNotificationPreferences()

            // === Дети ===
            setupChildPreferences()

            // === Резервное копирование ===
            setupBackupPreferences()
        }

        private fun setupTargetPreferences() {
            val prefFeeding = findPreference<Preference>("pref_target_feeding")
            val prefSleep = findPreference<Preference>("pref_target_sleep")

            prefFeeding?.summary = "${prefs.targetFeedingIntervalMinutes} мин"
            prefSleep?.summary = "${prefs.targetSleepDurationMinutes} мин"

            // В реальном приложении: клик открывает NumberPicker
        }

        private fun setupNotificationPreferences() {
            findPreference<SwitchPreferenceCompat>("pref_enable_feeding_reminders")?.apply {
                isChecked = prefs.enableFeedingReminders
                setOnPreferenceChangeListener { _, newValue ->
                    prefs.enableFeedingReminders = newValue as Boolean
                    true
                }
            }

            findPreference<SwitchPreferenceCompat>("pref_enable_sleep_reminders")?.apply {
                isChecked = prefs.enableSleepReminders
                setOnPreferenceChangeListener { _, newValue ->
                    prefs.enableSleepReminders = newValue as Boolean
                    true
                }
            }
        }

        private fun setupChildPreferences() {
            val prefChild = findPreference<Preference>("pref_active_child")
            prefChild?.summary = "Ребёнок ${prefs.currentChildId.takeLast(1)}"
            // Клик — выбор из списка детей
        }

        private fun setupBackupPreferences() {
            findPreference<Preference>("pref_backup")?.setOnPreferenceClickListener {
                // Запуск резервного копирования
                true
            }

            findPreference<Preference>("pref_restore")?.setOnPreferenceClickListener {
                // Запуск восстановления
                true
            }
        }
    }
}