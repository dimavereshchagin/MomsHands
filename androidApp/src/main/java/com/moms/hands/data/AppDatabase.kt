package com.moms.hands.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.moms.hands.data.dao.FeedingDao
import com.moms.hands.data.dao.SleepDao
import com.moms.hands.data.dao.SpitUpDao
import com.moms.hands.data.entity.Feeding
import com.moms.hands.data.entity.Sleep
import com.moms.hands.data.entity.SpitUp
import com.moms.hands.util.LocalDateTimeConverter

/**
 * Главная база данных приложения Mom's Hands.
 *
 * Использует Android Room — локальное, типизированное хранение данных.
 *
 * Содержит таблицы:
 * - Кормления (Feeding)
 * - Сон (Sleep)
 * - Срыгивания (SpitUp)
 *
 * Особенности:
 * - Поддержка нескольких детей (через childId)
 * - Шифрование с помощью SQLCipher
 * - Конвертеры для LocalDateTime
 * - Единый точечный доступ через синглтон-фабрику
 *
 * Соответствует ТЗ: локальная база данных, безопасность, производительность, несколько детей.
 */
@Database(
    entities = [Feeding::class, Sleep::class, SpitUp::class],
    version = 1,
    exportSchema = false // Отключаем экспорт схемы — не нужен для приложения
)
@TypeConverters(LocalDateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun feedingDao(): FeedingDao
    abstract fun sleepDao(): SleepDao
    abstract fun spitUpDao(): SpitUpDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Получает экземпляр базы данных.
         *
         * Использует синглтон-паттерн для избежания множественных подключений.
         *
         * @param context Контекст приложения
         * @param encryptionKey Ключ шифрования (для SQLCipher)
         * @return Экземпляр AppDatabase
         */
        fun getDatabase(context: Context, encryptionKey: String): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "moms_hands_database"
                )
                    .openHelperFactory(getEncryptionFactory(encryptionKey)) // Шифрование
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Возвращает фабрику с шифрованием (SQLCipher).
         *
         * Используется для шифрования резервных копий (ТЗ: "Шифрование резервных копий").
         */
        private fun getEncryptionFactory(key: String): SupportFactory {
            val passphrase: ByteArray = key.toCharArray().toByteArray()
            val factory = SupportFactory(DatabasePassword(passphrase))
            return factory
        }
    }
}