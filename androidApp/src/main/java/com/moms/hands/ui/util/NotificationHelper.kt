package com.moms.hands.ui.util

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.moms.hands.R
import com.moms.hands.ui.main.MainActivity
import com.moms.hands.ui.service.QuickAccessService

/**
 * Утилита для работы с уведомлениями в приложении Mom's Hands.
 *
 * Отвечает за:
 * - Создание каналов уведомлений (Android 8+)
 * - Отправку уведомлений о кормлении, сне, напоминаниях
 * - Запуск и остановку QuickAccessService
 * - Показ подсказок на экране блокировки
 *
 * Соответствует ТЗ: 2.1, 2.5, 4.2
 */
class NotificationHelper(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    companion object {
        const val CHANNEL_QUICK_ACCESS = "channel_quick_access"
        const val CHANNEL_REMINDERS = "channel_reminders"
        const val CHANNEL_DIGEST = "channel_digest"
        const val NOTIFICATION_ID_QUICK = 1
        const val NOTIFICATION_ID_REMINDER = 2
        const val NOTIFICATION_ID_DIGEST = 3
    }

    /**
     * Создаёт все необходимые каналы уведомлений.
     *
     * Вызывается при запуске приложения (в App.kt).
     */
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Канал 1: Быстрый доступ (экран блокировки)
            val quickChannel = NotificationChannel(
                CHANNEL_QUICK_ACCESS,
                context.getString(R.string.channel_quick_title),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_quick_desc)
                enableLights(true)
                lightColor = context.getColor(R.color.pink)
                setSound(null, null) // Без звука — не тревожить
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC // Показывать на экране блокировки
            }

            // Канал 2: Напоминания
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.channel_reminder_title),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_reminder_desc)
                enableLights(false)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }

            // Канал 3: Ежедневный дайджест
            val digestChannel = NotificationChannel(
                CHANNEL_DIGEST,
                context.getString(R.string.channel_digest_title),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_digest_desc)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }

            manager.createNotificationChannel(quickChannel)
            manager.createNotificationChannel(reminderChannel)
            manager.createNotificationChannel(digestChannel)
        }
    }

    /**
     * Показывает уведомление с кнопками быстрого доступа.
     *
     * Запускает QuickAccessService.
     */
    fun showQuickAccessNotification() {
        val startIntent = Intent(context, QuickAccessService::class.java)
        val stopIntent = Intent(context, QuickAccessService::class.java).apply {
            action = QuickAccessService.ACTION_STOP
        }

        val startPending = PendingIntent.getService(
            context,
            0,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopPending = PendingIntent.getService(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_QUICK_ACCESS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.quick_title))
            .setContentText(context.getString(R.string.quick_text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .addAction(R.drawable.ic_left, "Левая", startPending)
            .addAction(R.drawable.ic_right, "Правая", startPending)
            .addAction(R.drawable.ic_sleep, "Сон", startPending)
            .addAction(R.drawable.ic_close, "Скрыть", stopPending)
            .setStyle(
                NotificationCompat.DecoratedCustomViewStyle()
            )
            .setCustomContentView(createRemoteViews())
            .build()

        manager.notify(NOTIFICATION_ID_QUICK, notification)

        // Запускаем сервис в foreground
        context.startService(startIntent)
    }

    /**
     * Скрывает уведомление быстрого доступа и останавливает сервис.
     */
    fun hideQuickAccessNotification() {
        manager.cancel(NOTIFICATION_ID_QUICK)
        val intent = Intent(context, QuickAccessService::class.java).apply {
            action = QuickAccessService.ACTION_STOP
        }
        context.startService(intent)
    }

    /**
     * Отправляет адаптивное напоминание.
     *
     * Пример: "Похоже, ребенок проголодался — обычно в это время начинается кормление"
     */
    fun sendReminder(message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID_REMINDER, notification)
    }

    /**
     * Отправляет ежедневный дайджест активности.
     *
     * Пример: "Сегодня вы кормили 8 раз, сон в норме, срыгиваний не было."
     */
    fun sendDailyDigest(summary: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_DIGEST)
            .setSmallIcon(R.drawable.ic_digest)
            .setContentTitle(context.getString(R.string.digest_title))
            .setContentText(summary)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID_DIGEST, notification)
    }

    /**
     * Создаёт кастомный RemoteViews для уведомления.
     *
     * Позволяет добавить подсказку: "Следующая: Левая грудь"
     */
    private fun createRemoteViews(): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.notification_quick_access)
        views.setTextViewText(R.id.tv_next_hint, "Следующая: ?")
        return views
    }
}