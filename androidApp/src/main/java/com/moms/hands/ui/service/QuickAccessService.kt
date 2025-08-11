package com.moms.hands.ui.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.moms.hands.R
import com.moms.hands.App
import com.moms.hands.data.entity.Feeding
import com.moms.hands.domain.usecase.GenerateInsightsForChildUseCase
import com.moms.hands.ui.main.MainActivity
import com.moms.hands.util.PreferencesHelper
import kotlinx.coroutines.*
import java.time.LocalDateTime

/**
 * Foreground Service для быстрого доступа к функциям приложения.
 *
 * Отображает уведомление с кнопками:
 * - Левая грудь
 * - Правая грудь
 * - Сон
 * - Срыгнул
 *
 * Показывается на экране блокировки и в шторке уведомлений.
 * Работает в фоне, не закрывается системой.
 *
 * Соответствует ТЗ: 2.1, 2.2, 2.3, 2.5
 */
class QuickAccessService : Service() {

    private val appScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var prefs: PreferencesHelper
    private lateinit var insightsUseCase: GenerateInsightsForChildUseCase

    private val channelId = "quick_access_channel"
    private val notificationId = 1

    override fun onCreate() {
        super.onCreate()
        val app = applicationContext as App
        prefs = app.prefs
        insightsUseCase = GenerateInsightsForChildUseCase(
            app.database.feedingDao(),
            app.database.spitUpDao()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Создаём уведомление с кастомным интерфейсом
        val notification = createNotification()
        startForeground(notificationId, notification)

        return START_STICKY // Перезапускается, если система убила
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val remoteViews = RemoteViews(packageName, R.layout.notification_quick_access)

        // Обновляем подсказку: какая грудь следующая
        updateNextBreastHint(remoteViews)

        // Настраиваем кнопки
        setupButtonIntents(remoteViews)

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.quick_access_hint))
            .setCustomContentView(remoteViews)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL) // Для отображения на экране блокировки
            .build()
    }

    private fun updateNextBreastHint(remoteViews: RemoteViews) {
        appScope.launch {
            val lastFeeding = getDatabase().feedingDao().getLastFeeding(prefs.currentChildId)
            val next = if (lastFeeding?.breast != "LEFT") "Левая" else "Правая"
            remoteViews.setTextViewText(R.id.tv_next_hint, "Следующая: $next")
        }
    }

    private fun setupButtonIntents(remoteViews: RemoteViews) {
        // Кнопка: Левая грудь
        remoteViews.setOnClickPendingIntent(R.id.btn_left, createPendingIntent(ACTION_START_LEFT))
        // Кнопка: Правая грудь
        remoteViews.setOnClickPendingIntent(R.id.btn_right, createPendingIntent(ACTION_START_RIGHT))
        // Кнопка: Сон
        remoteViews.setOnClickPendingIntent(R.id.btn_sleep, createPendingIntent(ACTION_TOGGLE_SLEEP))
        // Кнопка: Срыгнул
        remoteViews.setOnClickPendingIntent(R.id.btn_spit, createPendingIntent(ACTION_SPIT_UP))
        // Кнопка: Закрыть
        remoteViews.setOnClickPendingIntent(R.id.btn_close, createPendingIntent(ACTION_STOP))
    }

    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, QuickAccessService::class.java).apply { this.action = action }
        return PendingIntent.getService(this, action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun getDatabase() = (applicationContext as App).database

    override fun onDestroy() {
        super.onDestroy()
        appScope.cancel()
    }

    // === Действия ===

    companion object {
        const val ACTION_START_LEFT = "ACTION_START_LEFT"
        const val ACTION_START_RIGHT = "ACTION_START_RIGHT"
        const val ACTION_TOGGLE_SLEEP = "ACTION_TOGGLE_SLEEP"
        const val ACTION_SPIT_UP = "ACTION_SPIT_UP"
        const val ACTION_STOP = "ACTION_STOP"

        /**
         * Запускает сервис.
         */
        fun start(context: Context) {
            val serviceIntent = Intent(context, QuickAccessService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }

        /**
         * Останавливает сервис.
         */
        fun stop(context: Context) {
            val intent = Intent(context, QuickAccessService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}